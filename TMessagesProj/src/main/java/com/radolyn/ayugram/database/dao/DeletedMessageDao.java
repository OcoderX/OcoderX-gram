/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import com.radolyn.ayugram.database.entities.DialogCount;

import java.util.List;

@Dao
public interface DeletedMessageDao {
    @Transaction
    @Query("SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND messageId = :messageId")
    DeletedMessageFull getMessage(long userId, long dialogId, int messageId);

    @Transaction
    @Query("SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND topicId = :topicId AND :startId <= messageId AND messageId <= :endId ORDER BY messageId LIMIT :limit")
    List<DeletedMessageFull> getMessages(long userId, long dialogId, long topicId, int startId, int endId, int limit);

    @Transaction
    @Query("SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND groupedId = :groupedId ORDER BY messageId")
    List<DeletedMessageFull> getMessagesGrouped(long userId, long dialogId, long groupedId);

    @Insert
    long insert(DeletedMessage msg);

    @Insert
    void insertReaction(DeletedMessageReaction reaction);

    @Query("SELECT EXISTS(SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND topicId = :topicId AND messageId = :msgId)")
    boolean exists(long userId, long dialogId, long topicId, int msgId);

    @Query("DELETE FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND messageId = :msgId")
    void delete(long userId, long dialogId, int msgId);

    @Query("SELECT COUNT(*) FROM deletedmessage WHERE userId = :userId AND entityCreateDate > :fromDate")
    int getSyncCount(long userId, long fromDate);

    @Query("SELECT * FROM deletedmessage WHERE userId = :userId AND entityCreateDate > :fromDate ORDER BY entityCreateDate LIMIT 50 OFFSET :offset")
    List<DeletedMessage> getForSync(long userId, long fromDate, int offset);

    @Query("SELECT * FROM deletedmessage WHERE entityCreateDate < :cutoff")
    List<DeletedMessage> getOlderThan(int cutoff);

    @Query("DELETE FROM deletedmessage WHERE entityCreateDate < :cutoff")
    void deleteOlderThan(int cutoff);

    @Query("SELECT COUNT(*) FROM deletedmessage")
    int getTotalCount();

    @Query("SELECT MIN(entityCreateDate) FROM deletedmessage")
    int getOldestEntryDate();

    @Query("SELECT dialogId, COUNT(*) as cnt FROM deletedmessage GROUP BY dialogId ORDER BY cnt DESC LIMIT :limit")
    List<DialogCount> getTopDialogs(int limit);

    @Query("SELECT * FROM deletedmessage ORDER BY entityCreateDate")
    List<DeletedMessage> getAll();
}
