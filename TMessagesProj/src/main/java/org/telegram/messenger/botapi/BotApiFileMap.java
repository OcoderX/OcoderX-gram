/*
 * Persisted side-table mapping our synthesized TLRPC.Document/Photo ids
 * back to the original Telegram Bot API file_id, since TL_document/TL_photo
 * have no free field to carry the file_id itself and Bot API file references
 * (dc_id/access_hash/file_reference) don't apply to bot-mode accounts.
 */

package org.telegram.messenger.botapi;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class BotApiFileMap {

    private static final BotApiFileMap[] instances = new BotApiFileMap[org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT];

    public static BotApiFileMap getInstance(int account) {
        BotApiFileMap local = instances[account];
        if (local == null) {
            synchronized (BotApiFileMap.class) {
                local = instances[account];
                if (local == null) {
                    instances[account] = local = new BotApiFileMap(account);
                }
            }
        }
        return local;
    }

    private final SharedPreferences prefs;

    private BotApiFileMap(int account) {
        prefs = ApplicationLoader.applicationContext.getSharedPreferences("botapi_filemap_" + account, Context.MODE_PRIVATE);
    }

    public void put(long syntheticId, String botFileId) {
        prefs.edit().putString(String.valueOf(syntheticId), botFileId).apply();
    }

    public String get(long syntheticId) {
        return prefs.getString(String.valueOf(syntheticId), null);
    }
}
