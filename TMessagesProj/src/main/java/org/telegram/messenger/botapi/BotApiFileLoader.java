/*
 * Bypasses the native MTProto downloader (FileLoader/FileLoadOperation) for bot-mode
 * accounts: resolves our synthesized TL_document/TL_photo ids back to their Bot API
 * file_id via BotApiFileMap, downloads the bytes over plain HTTPS, writes them to the
 * exact path FileLoader.getPathToAttach(...) would have used, then fires the same
 * NotificationCenter.fileLoaded event FileLoader itself fires on completion so
 * ImageReceiver/ChatMessageCell don't need to know the transport was different.
 */

package org.telegram.messenger.botapi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BotApiFileLoader extends BaseController {

    private static final BotApiFileLoader[] Instance = new BotApiFileLoader[UserConfig.MAX_ACCOUNT_COUNT];

    public static BotApiFileLoader getInstance(int num) {
        BotApiFileLoader local = Instance[num];
        if (local == null) {
            synchronized (BotApiFileLoader.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new BotApiFileLoader(num);
                }
            }
        }
        return local;
    }

    private final DispatchQueue queue;
    private final BotApiClient client = new BotApiClient();
    private final Set<String> inFlight = Collections.synchronizedSet(new HashSet<>());

    private BotApiFileLoader(int num) {
        super(num);
        queue = new DispatchQueue("BotApiFileLoader_" + num);
    }

    public void downloadDocument(TLRPC.Document document) {
        if (document == null) {
            return;
        }
        String key = "doc_" + document.id;
        if (!inFlight.add(key)) {
            return;
        }
        queue.postRunnable(() -> {
            try {
                String fileId = BotApiFileMap.getInstance(currentAccount).get(document.id);
                if (fileId == null) {
                    return;
                }
                String token = getUserConfig().botApiToken;
                if (token == null) {
                    return;
                }
                File dest = getFileLoader().getPathToAttach(document);
                if (dest.exists() && dest.length() > 0) {
                    notifyLoaded(FileLoader.getAttachFileName(document), dest);
                    return;
                }
                String filePath = client.getFilePath(token, fileId);
                if (filePath == null) {
                    return;
                }
                if (client.downloadToFile(token, filePath, dest)) {
                    notifyLoaded(FileLoader.getAttachFileName(document), dest);
                }
            } finally {
                inFlight.remove(key);
            }
        });
    }

    public void downloadPhoto(TLRPC.Photo photo, TLRPC.PhotoSize size) {
        if (size == null || size.location == null) {
            return;
        }
        String key = "photo_" + size.location.volume_id;
        if (!inFlight.add(key)) {
            return;
        }
        queue.postRunnable(() -> {
            try {
                String fileId = BotApiFileMap.getInstance(currentAccount).get(size.location.volume_id);
                if (fileId == null) {
                    return;
                }
                String token = getUserConfig().botApiToken;
                if (token == null) {
                    return;
                }
                File dest = getFileLoader().getPathToAttach(size);
                if (dest.exists() && dest.length() > 0) {
                    notifyLoaded(FileLoader.getAttachFileName(size), dest);
                    return;
                }
                String filePath = client.getFilePath(token, fileId);
                if (filePath == null) {
                    return;
                }
                if (client.downloadToFile(token, filePath, dest)) {
                    notifyLoaded(FileLoader.getAttachFileName(size), dest);
                }
            } finally {
                inFlight.remove(key);
            }
        });
    }

    private void notifyLoaded(String attachName, File dest) {
        if (attachName == null || attachName.isEmpty()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.fileLoaded, attachName, dest));
    }
}
