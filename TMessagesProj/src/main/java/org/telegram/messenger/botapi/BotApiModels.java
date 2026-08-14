/*
 * Bot API DTOs for the OcoderX-gram bot-token login feature.
 * Mirrors the subset of the Telegram Bot API needed to bridge a bot token
 * into the existing MTProto-based chat UI.
 */

package org.telegram.messenger.botapi;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class BotApiModels {

    public static class ApiResponse<T> {
        public boolean ok;
        public T result;
        @SerializedName("error_code")
        public int errorCode;
        public String description;
    }

    public static class User {
        public long id;
        @SerializedName("is_bot")
        public boolean isBot;
        @SerializedName("first_name")
        public String firstName;
        @SerializedName("last_name")
        public String lastName;
        public String username;
    }

    public static class Chat {
        public long id;
        public String type; // "private", "group", "supergroup", "channel"
        public String title;
        public String username;
        @SerializedName("first_name")
        public String firstName;
        @SerializedName("last_name")
        public String lastName;
    }

    public static class PhotoSize {
        @SerializedName("file_id")
        public String fileId;
        @SerializedName("file_unique_id")
        public String fileUniqueId;
        public int width;
        public int height;
        @SerializedName("file_size")
        public long fileSize;
    }

    public static class Document {
        @SerializedName("file_id")
        public String fileId;
        @SerializedName("file_unique_id")
        public String fileUniqueId;
        @SerializedName("file_name")
        public String fileName;
        @SerializedName("mime_type")
        public String mimeType;
        @SerializedName("file_size")
        public long fileSize;
    }

    public static class Message {
        @SerializedName("message_id")
        public int messageId;
        public User from;
        public Chat chat;
        public int date;
        public String text;
        public String caption;
        public List<PhotoSize> photo = new ArrayList<>();
        public Document document;
        @SerializedName("reply_to_message")
        public Message replyToMessage;
    }

    public static class Update {
        @SerializedName("update_id")
        public int updateId;
        public Message message;
        @SerializedName("edited_message")
        public Message editedMessage;
        @SerializedName("channel_post")
        public Message channelPost;
    }

    public static class UpdatesResult {
        public List<Update> updates = new ArrayList<>();
    }

    public static class BotFile {
        @SerializedName("file_id")
        public String fileId;
        @SerializedName("file_unique_id")
        public String fileUniqueId;
        @SerializedName("file_size")
        public long fileSize;
        @SerializedName("file_path")
        public String filePath;
    }
}
