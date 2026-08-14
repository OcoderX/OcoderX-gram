/*
 * This is the source code of AyuGram / OcoderX for Android.
 *
 * Copyright @Radolyn, 2023-2026.
 */

package com.radolyn.ayugram.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

public class TLJsonConverter {

    public static class KeyParameters {
        public int messageId;
        public long peerId;
        public long fromId;
        public long accessHash;
        public int dcId;
        public String dateFormatted = "";
        public String fileSizeFormatted = "";
        public String type = "";
        public String flagsSummary = "";
    }

    public static KeyParameters extractKeyParameters(MessageObject messageObject) {
        KeyParameters params = new KeyParameters();
        if (messageObject == null || messageObject.messageOwner == null) {
            return params;
        }
        TLRPC.Message owner = messageObject.messageOwner;
        params.messageId = owner.id;
        params.type = owner.getClass().getSimpleName();

        if (owner.peer_id != null) {
            params.peerId = MessageObject.getPeerId(owner.peer_id);
        } else {
            params.peerId = messageObject.getDialogId();
        }

        if (owner.from_id != null) {
            params.fromId = MessageObject.getPeerId(owner.from_id);
        } else {
            params.fromId = params.peerId;
        }

        if (owner.date > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            params.dateFormatted = sdf.format(new Date(owner.date * 1000L));
        }

        if (messageObject.getSize() > 0) {
            params.fileSizeFormatted = AndroidUtilities.formatFileSize(messageObject.getSize());
        }

        // Access hash & DC
        if (owner.media != null) {
            if (owner.media.document != null) {
                params.accessHash = owner.media.document.access_hash;
                params.dcId = owner.media.document.dc_id;
            } else if (owner.media.photo != null) {
                params.accessHash = owner.media.photo.access_hash;
                params.dcId = owner.media.photo.dc_id;
            } else if (owner.media.webpage != null) {
                if (owner.media.webpage.document != null) {
                    params.accessHash = owner.media.webpage.document.access_hash;
                    params.dcId = owner.media.webpage.document.dc_id;
                } else if (owner.media.webpage.photo != null) {
                    params.accessHash = owner.media.webpage.photo.access_hash;
                    params.dcId = owner.media.webpage.photo.dc_id;
                }
            }
        }

        ArrayList<String> flags = new ArrayList<>();
        if (owner.out) flags.add("out");
        if (owner.mentioned) flags.add("mentioned");
        if (owner.media_unread) flags.add("media_unread");
        if (owner.silent) flags.add("silent");
        if (owner.post) flags.add("post");
        if (owner.from_scheduled) flags.add("from_scheduled");
        if (owner.legacy) flags.add("legacy");
        if (owner.edit_hide) flags.add("edit_hide");
        if (owner.pinned) flags.add("pinned");
        if (owner.noforwards) flags.add("noforwards");
        if (owner.ayuDeleted) flags.add("ayu_deleted");
        if (owner.edit_date > 0) flags.add("edited");
        params.flagsSummary = String.join(", ", flags);

        return params;
    }

    public static String toFormattedJson(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "{}";
        }
        try {
            JSONObject json = toJson(messageObject.messageOwner);
            return json.toString(2);
        } catch (Throwable e) {
            return "{\n  \"error\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public static String toFormattedJson(TLObject tlObject) {
        if (tlObject == null) {
            return "{}";
        }
        try {
            JSONObject json = toJson(tlObject);
            return json.toString(2);
        } catch (Throwable e) {
            return "{\n  \"error\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public static JSONObject toJson(TLObject tlObject) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return (JSONObject) serializeValue(tlObject, visited, 0);
    }

    private static Object serializeValue(Object obj, Set<Object> visited, int depth) {
        if (obj == null) {
            return JSONObject.NULL;
        }
        if (depth > 12) {
            return "[Max depth reached]";
        }
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof String) {
            return obj;
        }
        if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            if (bytes.length == 0) {
                return "0x";
            }
            if (bytes.length > 64) {
                return "0x" + bytesToHex(bytes, 0, 32) + "... (" + bytes.length + " bytes)";
            }
            return "0x" + bytesToHex(bytes, 0, bytes.length);
        }
        if (obj instanceof ArrayList) {
            if (!visited.add(obj)) {
                return "[Circular List]";
            }
            JSONArray arr = new JSONArray();
            ArrayList<?> list = (ArrayList<?>) obj;
            for (Object item : list) {
                arr.put(serializeValue(item, visited, depth + 1));
            }
            return arr;
        }

        if (obj instanceof TLObject) {
            if (!visited.add(obj)) {
                return "[Circular: " + obj.getClass().getSimpleName() + "]";
            }
            JSONObject json = new JSONObject();
            try {
                json.put("_", obj.getClass().getSimpleName());

                Class<?> clazz = obj.getClass();
                while (clazz != null && clazz != Object.class) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        int modifiers = field.getModifiers();
                        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                            continue;
                        }
                        field.setAccessible(true);
                        String name = field.getName();
                        if (name.startsWith("this$") || name.equals("constructor")) {
                            continue;
                        }
                        try {
                            Object val = field.get(obj);
                            if (val != null) {
                                json.put(name, serializeValue(val, visited, depth + 1));
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    clazz = clazz.getSuperclass();
                }

                // Add friendly timestamps
                if (obj instanceof TLRPC.Message) {
                    TLRPC.Message msg = (TLRPC.Message) obj;
                    if (msg.date > 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        json.put("date_formatted", sdf.format(new Date(msg.date * 1000L)));
                    }
                    if (msg.edit_date > 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        json.put("edit_date_formatted", sdf.format(new Date(msg.edit_date * 1000L)));
                    }
                }
            } catch (Throwable ignored) {
            }
            return json;
        }

        return obj.toString();
    }

    private static String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xff));
        }
        return sb.toString();
    }
}
