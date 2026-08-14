/*
 * Thin synchronous wrapper around the Telegram Bot API HTTPS/JSON endpoints.
 * Modeled after com.radolyn.ayugram.sync.AyuSyncController's existing OkHttp+Gson usage
 * in this fork -- reuses the same dependencies, just a different backend.
 */

package org.telegram.messenger.botapi;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BotApiClient {

    private static final String TAG = "BotApiClient";
    private static final Gson gson = new Gson();

    private final OkHttpClient shortClient;
    private final OkHttpClient longPollClient;

    public BotApiClient() {
        shortClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        longPollClient = shortClient.newBuilder()
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    private static String apiUrl(String token, String method) {
        return "https://api.telegram.org/bot" + token + "/" + method;
    }

    private static String fileUrl(String token, String filePath) {
        return "https://api.telegram.org/file/bot" + token + "/" + filePath;
    }

    public BotApiModels.User getMe(String token) {
        Request request = new Request.Builder().url(apiUrl(token, "getMe")).get().build();
        BotApiModels.ApiResponse<BotApiModels.User> resp = execute(shortClient, request, new TypeToken<BotApiModels.ApiResponse<BotApiModels.User>>() {}.getType());
        return resp != null && resp.ok ? resp.result : null;
    }

    public List<BotApiModels.Update> getUpdates(String token, int offset, int timeoutSeconds) {
        String url = apiUrl(token, "getUpdates") + "?timeout=" + timeoutSeconds + "&offset=" + offset;
        Request request = new Request.Builder().url(url).get().build();
        BotApiModels.ApiResponse<List<BotApiModels.Update>> resp = execute(longPollClient, request, new TypeToken<BotApiModels.ApiResponse<List<BotApiModels.Update>>>() {}.getType());
        return resp != null && resp.ok && resp.result != null ? resp.result : Collections.emptyList();
    }

    public BotApiModels.Message sendText(String token, long chatId, String text, Integer replyToMessageId) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", String.valueOf(chatId))
                .addFormDataPart("text", text != null ? text : "");
        if (replyToMessageId != null) {
            builder.addFormDataPart("reply_to_message_id", String.valueOf(replyToMessageId));
        }
        return callAndParseMessage(token, "sendMessage", builder.build());
    }

    public BotApiModels.Message sendPhoto(String token, long chatId, File file, String caption) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", String.valueOf(chatId))
                .addFormDataPart("photo", file.getName(), RequestBody.create(file, MediaType.parse("image/jpeg")));
        if (caption != null) {
            builder.addFormDataPart("caption", caption);
        }
        return callAndParseMessage(token, "sendPhoto", builder.build());
    }

    public BotApiModels.Message sendDocument(String token, long chatId, File file, String caption, String mimeType) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", String.valueOf(chatId))
                .addFormDataPart("document", file.getName(), RequestBody.create(file, MediaType.parse(mimeType != null ? mimeType : "application/octet-stream")));
        if (caption != null) {
            builder.addFormDataPart("caption", caption);
        }
        return callAndParseMessage(token, "sendDocument", builder.build());
    }

    private BotApiModels.Message callAndParseMessage(String token, String method, RequestBody body) {
        Request request = new Request.Builder().url(apiUrl(token, method)).post(body).build();
        BotApiModels.ApiResponse<BotApiModels.Message> resp = execute(shortClient, request, new TypeToken<BotApiModels.ApiResponse<BotApiModels.Message>>() {}.getType());
        return resp != null && resp.ok ? resp.result : null;
    }

    public String getFilePath(String token, String fileId) {
        String url = apiUrl(token, "getFile") + "?file_id=" + fileId;
        Request request = new Request.Builder().url(url).get().build();
        BotApiModels.ApiResponse<BotApiModels.BotFile> resp = execute(shortClient, request, new TypeToken<BotApiModels.ApiResponse<BotApiModels.BotFile>>() {}.getType());
        return resp != null && resp.ok && resp.result != null ? resp.result.filePath : null;
    }

    public boolean downloadToFile(String token, String filePath, File dest) {
        Request request = new Request.Builder().url(fileUrl(token, filePath)).get().build();
        try (Response response = shortClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return false;
            }
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (InputStream in = response.body().byteStream(); OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return true;
        } catch (IOException e) {
            Log.d(TAG, "downloadToFile failed: " + e.getMessage());
            return false;
        }
    }

    private <T> T execute(OkHttpClient client, Request request, java.lang.reflect.Type type) {
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return null;
            }
            String json = response.body().string();
            return gson.fromJson(json, type);
        } catch (IOException e) {
            Log.d(TAG, "request failed: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.d(TAG, "request parse failed: " + e.getMessage());
            return null;
        }
    }
}
