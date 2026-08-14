/*
 * Translates Telegram Bot API JSON objects (Update/Message/User/Chat/Photo/Document)
 * into the MTProto TLRPC object graph this fork's chat UI (DialogsActivity/ChatActivity)
 * already knows how to render, and injects them via the same local-ingestion pipeline
 * MessagesController.generateJoinMessage() uses for locally-synthesized messages:
 * build TL_message -> MessageObject -> MessagesStorage.putMessages -> updateInterfaceWithMessages.
 *
 * Only private (DM) and basic-group chats are supported; supergroups/channels need a
 * TL_channel mapping this fork does not attempt yet.
 */

package org.telegram.messenger.botapi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class BotApiTranslator extends BaseController {

    private static final BotApiTranslator[] Instance = new BotApiTranslator[UserConfig.MAX_ACCOUNT_COUNT];

    public static BotApiTranslator getInstance(int num) {
        BotApiTranslator local = Instance[num];
        if (local == null) {
            synchronized (BotApiTranslator.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new BotApiTranslator(num);
                }
            }
        }
        return local;
    }

    private BotApiTranslator(int num) {
        super(num);
    }

    public void processUpdate(BotApiModels.Update update) {
        BotApiModels.Message msg = update.message != null ? update.message : update.channelPost;
        if (msg == null) {
            return;
        }
        ingestMessage(msg, true);
    }

    /**
     * @param isIncoming false when this is the local echo of a message we just sent via BotApiSendHelper
     *                   (its media file already exists locally, no need to re-download it)
     */
    public TLRPC.TL_message ingestMessage(BotApiModels.Message msg, boolean isIncoming) {
        if (msg.chat == null) {
            return null;
        }
        boolean isGroup = "group".equals(msg.chat.type);
        boolean isUnsupportedChatType = "supergroup".equals(msg.chat.type) || "channel".equals(msg.chat.type);
        if (isUnsupportedChatType) {
            return null;
        }

        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = msg.messageId;
        message.date = msg.date;
        message.flags = TLRPC.MESSAGE_FLAG_HAS_FROM_ID;

        long myId = getUserConfig().getClientUserId();
        long fromId = msg.from != null ? msg.from.id : myId;
        message.out = fromId == myId;

        TLRPC.TL_peerUser fromPeer = new TLRPC.TL_peerUser();
        fromPeer.user_id = fromId;
        message.from_id = fromPeer;

        ArrayList<TLRPC.User> users = new ArrayList<>();
        ArrayList<TLRPC.Chat> chats = new ArrayList<>();

        long dialogId;
        if (isGroup) {
            long internalChatId = -msg.chat.id;
            TLRPC.TL_chat chat = buildChat(msg.chat, internalChatId);
            chats.add(chat);
            TLRPC.TL_peerChat peer = new TLRPC.TL_peerChat();
            peer.chat_id = internalChatId;
            message.peer_id = peer;
            dialogId = msg.chat.id;
        } else {
            TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser();
            peer.user_id = msg.chat.id;
            message.peer_id = peer;
            dialogId = msg.chat.id;
        }
        message.dialog_id = dialogId;

        if (msg.from != null) {
            users.add(buildUser(msg.from));
        }

        String text = msg.text != null ? msg.text : msg.caption;
        message.message = text != null ? text : "";

        boolean hasMedia = false;
        if (msg.photo != null && !msg.photo.isEmpty()) {
            attachPhoto(message, msg);
            hasMedia = true;
        } else if (msg.document != null) {
            attachDocument(message, msg);
            hasMedia = true;
        }

        if (!users.isEmpty() || !chats.isEmpty()) {
            getMessagesStorage().putUsersAndChats(users, chats, true, true);
            if (!users.isEmpty()) {
                getMessagesController().putUsers(users, false);
            }
            for (int a = 0; a < chats.size(); a++) {
                getMessagesController().putChat(chats.get(a), false);
            }
        }

        ArrayList<MessageObject> pushMessages = new ArrayList<>();
        ArrayList<TLRPC.Message> messagesArr = new ArrayList<>();
        messagesArr.add(message);
        MessageObject obj = new MessageObject(currentAccount, message, true, false);
        pushMessages.add(obj);

        getMessagesStorage().putMessages(messagesArr, true, true, false, 0, false, 0);

        boolean notify = !message.out;
        AndroidUtilities.runOnUIThread(() -> {
            getMessagesController().updateInterfaceWithMessages(dialogId, pushMessages, false);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
            if (notify) {
                getNotificationsController().processNewMessages(pushMessages, true, false, null);
            }
        });

        if (isIncoming && hasMedia) {
            downloadIncomingMedia(message);
        }

        return message;
    }

    private void downloadIncomingMedia(TLRPC.TL_message message) {
        if (message.media == null) {
            return;
        }
        if (message.media.photo != null && !(message.media.photo instanceof TLRPC.TL_photoEmpty)) {
            TLRPC.TL_photoSize size = (TLRPC.TL_photoSize) message.media.photo.sizes.get(message.media.photo.sizes.size() - 1);
            BotApiFileLoader.getInstance(currentAccount).downloadPhoto(message.media.photo, size);
        } else if (message.media.document != null && !(message.media.document instanceof TLRPC.TL_documentEmpty)) {
            BotApiFileLoader.getInstance(currentAccount).downloadDocument(message.media.document);
        }
    }

    private TLRPC.TL_user buildUser(BotApiModels.User u) {
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = u.id;
        user.first_name = u.firstName != null ? u.firstName : "";
        user.last_name = u.lastName;
        user.username = u.username;
        user.bot = u.isBot;
        user.access_hash = 0;
        return user;
    }

    private TLRPC.TL_chat buildChat(BotApiModels.Chat c, long internalChatId) {
        TLRPC.TL_chat chat = new TLRPC.TL_chat();
        chat.id = internalChatId;
        chat.title = c.title != null ? c.title : "";
        chat.photo = new TLRPC.TL_chatPhotoEmpty();
        chat.participants_count = 0;
        chat.date = (int) (System.currentTimeMillis() / 1000);
        chat.version = 1;
        chat.left = false;
        chat.kicked = false;
        return chat;
    }

    private void attachPhoto(TLRPC.TL_message message, BotApiModels.Message msg) {
        BotApiModels.PhotoSize best = msg.photo.get(msg.photo.size() - 1);

        TLRPC.TL_photo photo = new TLRPC.TL_photo();
        photo.id = stableHash(best.fileUniqueId != null ? best.fileUniqueId : best.fileId);
        photo.access_hash = 0;
        photo.file_reference = new byte[0];
        photo.date = message.date;
        photo.dc_id = 0;

        TLRPC.TL_photoSize size = new TLRPC.TL_photoSize();
        size.type = "y";
        size.w = best.width;
        size.h = best.height;
        size.size = (int) best.fileSize;
        TLRPC.TL_fileLocationToBeDeprecated location = new TLRPC.TL_fileLocationToBeDeprecated();
        location.volume_id = photo.id;
        location.local_id = 0;
        location.dc_id = 0;
        location.secret = 0;
        location.file_reference = new byte[0];
        size.location = location;
        photo.sizes.add(size);

        BotApiFileMap.getInstance(currentAccount).put(photo.id, best.fileId);

        TLRPC.TL_messageMediaPhoto media = new TLRPC.TL_messageMediaPhoto();
        media.photo = photo;
        media.flags |= 1;
        message.media = media;
        message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
    }

    private void attachDocument(TLRPC.TL_message message, BotApiModels.Message msg) {
        BotApiModels.Document d = msg.document;

        TLRPC.TL_document document = new TLRPC.TL_document();
        document.id = stableHash(d.fileUniqueId != null ? d.fileUniqueId : d.fileId);
        document.access_hash = 0;
        document.file_reference = new byte[0];
        document.date = message.date;
        document.mime_type = d.mimeType != null ? d.mimeType : "application/octet-stream";
        document.size = d.fileSize;
        document.dc_id = 0;

        if (d.fileName != null) {
            TLRPC.TL_documentAttributeFilename attr = new TLRPC.TL_documentAttributeFilename();
            attr.file_name = d.fileName;
            document.attributes.add(attr);
        }

        BotApiFileMap.getInstance(currentAccount).put(document.id, d.fileId);

        TLRPC.TL_messageMediaDocument media = new TLRPC.TL_messageMediaDocument();
        media.document = document;
        message.media = media;
        message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
    }

    static long stableHash(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h & 0x7FFFFFFFFFFFFFFFL;
    }
}
