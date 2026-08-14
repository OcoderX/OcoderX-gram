/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.sync;

import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.EditedMessage;

public class AyuSyncControllerEmpty extends AyuSyncController {
    @Override
    public void connect() {
        // nah
    }

    @Override
    public void forceSync() {
        // nah
    }

    @Override
    public void syncRead(int accountId, long dialogId, int untilId) {
        // nah
    }

    @Override
    public void syncMessageEdited(int accountId, EditedMessage revision) {
        // nah
    }

    @Override
    public void syncMessageDeleted(int accountId, DeletedMessage deletedMessage) {
        // nah
    }
}
