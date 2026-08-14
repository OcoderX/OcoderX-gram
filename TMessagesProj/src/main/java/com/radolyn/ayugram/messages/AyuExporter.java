/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.messages;

import android.os.Environment;
import com.google.android.exoplayer2.util.Log;
import com.google.gson.GsonBuilder;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuData;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AyuExporter {
    public static final File exportsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            "Exports"
    );

    public static File exportToJson() {
        try {
            if (!exportsPath.exists()) {
                exportsPath.mkdirs();
            }

            var deleted = AyuData.getDeletedMessageDao().getAll();
            var edited = AyuData.getEditedMessageDao().getAll();

            var payload = new HashMap<String, Object>();
            payload.put("exportedAt", System.currentTimeMillis() / 1000L);
            payload.put("deletedMessages", deleted);
            payload.put("editedMessages", edited);

            var gson = new GsonBuilder().setPrettyPrinting().create();
            var json = gson.toJson(payload);

            var fileName = "export-" + new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(new Date()) + ".json";
            var file = new File(exportsPath, fileName);

            try (var writer = new FileWriter(file)) {
                writer.write(json);
            }

            return file;
        } catch (Exception e) {
            Log.e("AyuGram", "error exportToJson", e);
            FileLog.e("exportToJson", e);
            return null;
        }
    }

    public static void exportChatHistory(ChatActivity fragment, long dialogId, boolean asJson) {
        if (fragment == null) {
            return;
        }

        BulletinFactory.of(fragment).createSimpleBulletin(
                R.raw.info,
                LocaleController.getString("ExportingChatHistory", R.string.ExportingChatHistory)
        ).show();

        Utilities.globalQueue.postRunnable(() -> {
            try {
                if (!exportsPath.exists()) {
                    exportsPath.mkdirs();
                }

                int account = fragment.getCurrentAccount();
                MessagesStorage storage = MessagesStorage.getInstance(account);
                MessagesController controller = MessagesController.getInstance(account);

                TLRPC.Chat chat = dialogId < 0 ? controller.getChat(-dialogId) : null;
                TLRPC.User user = dialogId > 0 ? controller.getUser(dialogId) : null;
                String chatTitle = chat != null ? chat.title : (user != null ? ContactsController.formatName(user.first_name, user.last_name) : String.valueOf(dialogId));

                ArrayList<MessageObject> messageObjects = new ArrayList<>();
                // Load messages from memory or storage
                if (fragment.getMessages() != null) {
                    messageObjects.addAll(fragment.getMessages());
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                String fileTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String extension = asJson ? ".json" : ".txt";
                String fileName = "chat_" + Math.abs(dialogId) + "_" + fileTimestamp + extension;
                File exportFile = new File(exportsPath, fileName);

                if (asJson) {
                    List<Map<String, Object>> jsonList = new ArrayList<>();
                    for (MessageObject msg : messageObjects) {
                        if (msg == null || msg.messageOwner == null) continue;
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", msg.getId());
                        map.put("date", dateFormat.format(new Date(msg.messageOwner.date * 1000L)));
                        map.put("timestamp", msg.messageOwner.date);
                        map.put("from_id", msg.getFromChatId());
                        map.put("out", msg.isOut());
                        map.put("text", msg.messageText != null ? msg.messageText.toString() : (msg.caption != null ? msg.caption.toString() : ""));
                        map.put("is_media", msg.hasMedia());
                        if (msg.messageOwner.media != null) {
                            map.put("media_type", msg.messageOwner.media.getClass().getSimpleName());
                        }
                        jsonList.add(map);
                    }

                    Map<String, Object> root = new HashMap<>();
                    root.put("chat_id", dialogId);
                    root.put("chat_title", chatTitle);
                    root.put("exported_at", dateFormat.format(new Date()));
                    root.put("messages_count", jsonList.size());
                    root.put("messages", jsonList);

                    var gson = new GsonBuilder().setPrettyPrinting().create();
                    try (FileWriter writer = new FileWriter(exportFile)) {
                        writer.write(gson.toJson(root));
                    }
                } else {
                    try (FileWriter writer = new FileWriter(exportFile)) {
                        writer.write("============================================================\n");
                        writer.write(AyuConstants.APP_NAME + " Chat History Export\n");
                        writer.write("Chat: " + chatTitle + " (ID: " + dialogId + ")\n");
                        writer.write("Export Date: " + dateFormat.format(new Date()) + "\n");
                        writer.write("Total Messages: " + messageObjects.size() + "\n");
                        writer.write("============================================================\n\n");

                        for (int i = messageObjects.size() - 1; i >= 0; i--) {
                            MessageObject msg = messageObjects.get(i);
                            if (msg == null || msg.messageOwner == null) continue;

                            String dateStr = dateFormat.format(new Date(msg.messageOwner.date * 1000L));
                            String senderName = msg.isOut() ? "You" : (msg.messageOwner.from_id != null ? String.valueOf(msg.getFromChatId()) : chatTitle);
                            if (!msg.isOut() && msg.messageOwner.from_id != null) {
                                TLRPC.User fromUser = controller.getUser(msg.messageOwner.from_id.user_id);
                                if (fromUser != null) {
                                    senderName = ContactsController.formatName(fromUser.first_name, fromUser.last_name);
                                }
                            }

                            writer.write("[" + dateStr + "] " + senderName + ":\n");
                            if (msg.messageText != null && msg.messageText.length() > 0) {
                                writer.write(msg.messageText.toString() + "\n");
                            }
                            if (msg.caption != null && msg.caption.length() > 0) {
                                writer.write("[Caption]: " + msg.caption.toString() + "\n");
                            }
                            if (msg.isVideo()) {
                                writer.write("[Attached Video]\n");
                            } else if (msg.isPhoto()) {
                                writer.write("[Attached Photo]\n");
                            } else if (msg.isVoice()) {
                                writer.write("[Attached Voice Message]\n");
                            } else if (msg.isRoundVideo()) {
                                writer.write("[Attached Video Note]\n");
                            } else if (msg.isDocument()) {
                                writer.write("[Attached Document: " + msg.getDocumentName() + "]\n");
                            }
                            writer.write("\n");
                        }
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    BulletinFactory.of(fragment).createSimpleBulletin(
                            R.raw.info,
                            LocaleController.formatString("ExportHistoryDone", R.string.ExportHistoryDone, exportFile.getAbsolutePath())
                    ).show();
                });
            } catch (Exception e) {
                FileLog.e("exportChatHistory", e);
                AndroidUtilities.runOnUIThread(() -> {
                    BulletinFactory.of(fragment).createSimpleBulletin(
                            R.raw.error,
                            LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred)
                    ).show();
                });
            }
        });
    }
}
