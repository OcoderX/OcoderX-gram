/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.sync.models;

import com.radolyn.ayugram.database.entities.DeletedMessage;

public class SyncDeletedMessage implements SyncEvent {
    public String type = "sync_deleted_message";
    public long userId;
    public DeletedMessage args;
}
