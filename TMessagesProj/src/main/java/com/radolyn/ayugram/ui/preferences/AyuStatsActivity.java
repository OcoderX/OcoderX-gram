/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.entities.DialogCount;
import com.radolyn.ayugram.messages.AyuMessagesController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AyuStatsActivity extends BasePreferencesActivity {

    private int generalHeaderRow;
    private int deletedCountRow;
    private int editedCountRow;
    private int oldestEntryRow;
    private int databaseSizeRow;
    private int generalDividerRow;

    private int topChatsHeaderRow;
    private int topChatsHintRow;

    private List<DialogCount> topDialogs;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        generalHeaderRow = newRow();
        deletedCountRow = newRow();
        editedCountRow = newRow();
        oldestEntryRow = newRow();
        databaseSizeRow = newRow();
        generalDividerRow = newRow();

        topDialogs = new ArrayList<>();
        try {
            topDialogs = AyuMessagesController.getInstance().getTopDialogs(5);
        } catch (Exception ignore) {
        }

        if (!topDialogs.isEmpty()) {
            topChatsHeaderRow = newRow();
            rowCount += topDialogs.size();
        } else {
            topChatsHeaderRow = -1;
        }

        topChatsHintRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        // stat rows are informational only
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.AyuStatsTitle);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private String formatDate(int unixSeconds) {
        if (unixSeconds <= 0) {
            return "—";
        }
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(new Date(unixSeconds * 1000L));
    }

    private String resolveDialogName(long dialogId) {
        var controller = MessagesController.getInstance(UserConfig.selectedAccount);

        TLObject obj;
        if (dialogId > 0) {
            obj = controller.getUser(dialogId);
        } else {
            obj = controller.getChat(-dialogId);
        }

        if (obj == null) {
            return "#" + dialogId;
        }

        return DialogObject.getDialogTitle(obj);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == deletedCountRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AyuStatsDeletedCount), String.valueOf(AyuMessagesController.getInstance().getDeletedMessagesCount()), true);
                    } else if (position == editedCountRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AyuStatsEditedCount), String.valueOf(AyuMessagesController.getInstance().getEditedMessagesCount()), true);
                    } else if (position == oldestEntryRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AyuStatsOldestEntry), formatDate(AyuMessagesController.getInstance().getOldestEntryDate()), true);
                    } else if (position == databaseSizeRow) {
                        var file = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
                        var size = file.exists() ? file.length() : 0;
                        textCell.setTextAndValue(LocaleController.getString(R.string.AyuStatsDatabaseSize), AndroidUtilities.formatFileSize(size), false);
                    } else if (topChatsHeaderRow != -1 && position > topChatsHeaderRow && position < topChatsHeaderRow + 1 + topDialogs.size()) {
                        var entry = topDialogs.get(position - topChatsHeaderRow - 1);
                        var needDivider = position != topChatsHeaderRow + topDialogs.size();
                        textCell.setTextAndValue(resolveDialogName(entry.dialogId), String.valueOf(entry.cnt), needDivider);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.AyuStatsGeneralHeader));
                    } else if (position == topChatsHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.AyuStatsTopChatsHeader));
                    }
                    break;
                case 8:
                    TextInfoPrivacyCell hintCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == topChatsHintRow) {
                        hintCell.setText(LocaleController.getString(R.string.AyuStatsHint));
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == generalDividerRow) {
                return 1;
            } else if (position == generalHeaderRow || position == topChatsHeaderRow) {
                return 3;
            } else if (position == topChatsHintRow) {
                return 8;
            }
            return 2;
        }
    }
}
