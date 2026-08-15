/*
 * Long-polls Telegram Bot API getUpdates for one bot-mode account and hands each
 * update to BotApiTranslator. One instance per account slot; started from
 * ApplicationLoader at boot (and right after bot-token login) for accounts with
 * UserConfig.botApiMode set, stopped on logout.
 */

package org.telegram.messenger.botapi;

import org.telegram.messenger.BaseController;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;

import java.util.List;

public class BotApiPoller extends BaseController {

    private static final BotApiPoller[] Instance = new BotApiPoller[UserConfig.MAX_ACCOUNT_COUNT];

    public static BotApiPoller getInstance(int num) {
        BotApiPoller local = Instance[num];
        if (local == null) {
            synchronized (BotApiPoller.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new BotApiPoller(num);
                }
            }
        }
        return local;
    }

    private final BotApiClient client = new BotApiClient();
    private DispatchQueue queue;
    private volatile boolean running;

    private BotApiPoller(int num) {
        super(num);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        if (!getUserConfig().isBotApiMode() || getUserConfig().botApiToken == null) {
            return;
        }
        running = true;
        queue = new DispatchQueue("BotApiPoller_" + currentAccount);
        queue.postRunnable(this::pollLoop);
    }

    public synchronized void stop() {
        running = false;
        if (queue != null) {
            queue.recycle();
            queue = null;
        }
    }

    private void pollLoop() {
        if (!running) {
            return;
        }
        String token = getUserConfig().botApiToken;
        if (token == null) {
            running = false;
            return;
        }
        int offset = getUserConfig().botApiUpdateOffset;
        try {
            List<BotApiModels.Update> updates = client.getUpdates(token, offset, 30);
            BotApiDebugLog.log("pollLoop got " + updates.size() + " updates, offset=" + offset);
            int newOffset = offset;
            for (BotApiModels.Update update : updates) {
                try {
                    BotApiTranslator.getInstance(currentAccount).processUpdate(update);
                } catch (Exception e) {
                    BotApiDebugLog.log("processUpdate failed for update " + update.updateId, e);
                    FileLog.e(e);
                }
                if (update.updateId >= newOffset) {
                    newOffset = update.updateId + 1;
                }
            }
            if (newOffset != offset) {
                getUserConfig().setBotApiUpdateOffset(newOffset);
            }
        } catch (Exception e) {
            BotApiDebugLog.log("pollLoop outer exception", e);
            FileLog.e(e);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }
        if (running && queue != null) {
            queue.postRunnable(this::pollLoop);
        }
    }
}
