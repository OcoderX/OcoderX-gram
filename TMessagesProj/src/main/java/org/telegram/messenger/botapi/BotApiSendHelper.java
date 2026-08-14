/*
 * Outbound side of bot-mode accounts: called from SendMessagesHelper's send path
 * instead of the normal MTProto messages.sendMessage/sendMedia RPCs (which would
 * require an access_hash our synthesized peers can never have). Sends over the
 * Bot API, then echoes the real server-assigned message back through
 * BotApiTranslator so it appears in the chat with a genuine message_id.
 */

package org.telegram.messenger.botapi;

import org.telegram.messenger.BaseController;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;

import java.io.File;

public class BotApiSendHelper extends BaseController {

    private static final BotApiSendHelper[] Instance = new BotApiSendHelper[UserConfig.MAX_ACCOUNT_COUNT];

    public static BotApiSendHelper getInstance(int num) {
        BotApiSendHelper local = Instance[num];
        if (local == null) {
            synchronized (BotApiSendHelper.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new BotApiSendHelper(num);
                }
            }
        }
        return local;
    }

    private final DispatchQueue queue;
    private final BotApiClient client = new BotApiClient();

    private BotApiSendHelper(int num) {
        super(num);
        queue = new DispatchQueue("BotApiSendHelper_" + num);
    }

    public void sendText(long peer, String text) {
        queue.postRunnable(() -> {
            String token = getUserConfig().botApiToken;
            if (token == null) {
                return;
            }
            try {
                BotApiModels.Message result = client.sendText(token, peer, text, null);
                if (result != null) {
                    BotApiTranslator.getInstance(currentAccount).ingestMessage(result, false);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void sendPhoto(long peer, String localPath, String caption) {
        queue.postRunnable(() -> {
            String token = getUserConfig().botApiToken;
            if (token == null || localPath == null) {
                return;
            }
            try {
                File file = new File(localPath);
                BotApiModels.Message result = client.sendPhoto(token, peer, file, caption);
                if (result != null) {
                    BotApiTranslator.getInstance(currentAccount).ingestMessage(result, false);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void sendDocument(long peer, String localPath, String caption, String mimeType) {
        queue.postRunnable(() -> {
            String token = getUserConfig().botApiToken;
            if (token == null || localPath == null) {
                return;
            }
            try {
                File file = new File(localPath);
                BotApiModels.Message result = client.sendDocument(token, peer, file, caption, mimeType);
                if (result != null) {
                    BotApiTranslator.getInstance(currentAccount).ingestMessage(result, false);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
