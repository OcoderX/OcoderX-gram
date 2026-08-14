/*
 * This is the source code of AyuGram / OcoderX for Android.
 *
 * Copyright @Radolyn, 2023-2026.
 */

package com.radolyn.ayugram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.radolyn.ayugram.utils.CodeSyntaxHighlighter;
import com.radolyn.ayugram.utils.TLJsonConverter;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

public class RawMessageViewerBottomSheet extends BottomSheet {

    private final BaseFragment fragment;
    private final MessageObject messageObject;
    private final String formattedJson;
    private final TLJsonConverter.KeyParameters keyParams;

    public RawMessageViewerBottomSheet(Context context, BaseFragment fragment, MessageObject messageObject) {
        super(context, false);
        this.fragment = fragment;
        this.messageObject = messageObject;
        this.keyParams = TLJsonConverter.extractKeyParameters(messageObject);
        this.formattedJson = TLJsonConverter.toFormattedJson(messageObject);

        setOpenNoDelay(true);
        fixNavigationBar();

        boolean isDark = Theme.isCurrentThemeDark();

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // 1. Header Frame
        FrameLayout headerLayout = new FrameLayout(context);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(R.drawable.msg_log);
        iconView.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
        headerLayout.addView(iconView, LayoutHelper.createFrame(32, 32, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        LinearLayout titleContainer = new LinearLayout(context);
        titleContainer.setOrientation(LinearLayout.VERTICAL);

        SimpleTextView titleView = new SimpleTextView(context);
        titleView.setTextSize(18);
        titleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setText(LocaleController.getString("RawMessageTitle", R.string.RawMessageTitle));
        titleContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        SimpleTextView subtitleView = new SimpleTextView(context);
        subtitleView.setTextSize(13);
        subtitleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        subtitleView.setText("ID: " + keyParams.messageId + " · " + keyParams.type + (keyParams.dateFormatted.isEmpty() ? "" : " · " + keyParams.dateFormatted));
        titleContainer.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        headerLayout.addView(titleContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 44, 0, 40, 0));

        ImageView closeBtn = new ImageView(context);
        closeBtn.setImageResource(R.drawable.ic_close_white);
        closeBtn.setColorFilter(Theme.getColor(Theme.key_dialogTextGray2));
        closeBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        closeBtn.setOnClickListener(v -> dismiss());
        headerLayout.addView(closeBtn, LayoutHelper.createFrame(28, 28, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        contentLayout.addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // 2. Chips Scroll View
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsLayout = new LinearLayout(context);
        chipsLayout.setOrientation(LinearLayout.HORIZONTAL);

        addChip(chipsLayout, context, "Msg ID", String.valueOf(keyParams.messageId));
        if (keyParams.peerId != 0) {
            addChip(chipsLayout, context, "Peer ID", String.valueOf(keyParams.peerId));
        }
        if (keyParams.fromId != 0 && keyParams.fromId != keyParams.peerId) {
            addChip(chipsLayout, context, "From ID", String.valueOf(keyParams.fromId));
        }
        if (keyParams.accessHash != 0) {
            addChip(chipsLayout, context, "Access Hash", String.valueOf(keyParams.accessHash));
        }
        if (keyParams.dcId > 0) {
            addChip(chipsLayout, context, "DC", "DC" + keyParams.dcId);
        }
        if (!TextUtils.isEmpty(keyParams.fileSizeFormatted)) {
            addChip(chipsLayout, context, "Size", keyParams.fileSizeFormatted);
        }
        if (!TextUtils.isEmpty(keyParams.flagsSummary)) {
            addChip(chipsLayout, context, "Flags", keyParams.flagsSummary);
        }

        chipsScroll.addView(chipsLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        contentLayout.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // 3. JSON Container (scrollable text with syntax coloring)
        ScrollView jsonScrollView = new ScrollView(context);
        int maxCodeHeight = Math.min(AndroidUtilities.displaySize.y / 2, AndroidUtilities.dp(360));

        GradientDrawable codeBg = new GradientDrawable();
        codeBg.setCornerRadius(AndroidUtilities.dp(10));
        codeBg.setColor(isDark ? 0xFF181C24 : 0xFFF0F4F8);

        jsonScrollView.setBackground(codeBg);
        jsonScrollView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        HorizontalScrollView codeHScroll = new HorizontalScrollView(context);
        codeHScroll.setHorizontalScrollBarEnabled(false);

        TextView codeTextView = new TextView(context);
        codeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        codeTextView.setTypeface(Typeface.MONOSPACE);
        codeTextView.setTextColor(isDark ? 0xFFE0E6ED : 0xFF24292E);
        codeTextView.setTextIsSelectable(true);

        SpannableStringBuilder syntaxJson = CodeSyntaxHighlighter.highlightJson(formattedJson, isDark);
        codeTextView.setText(syntaxJson);

        codeHScroll.addView(codeTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        jsonScrollView.addView(codeHScroll, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        contentLayout.addView(jsonScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, maxCodeHeight, 0, 0, 0, 12));

        // 4. Action Buttons (Copy JSON, Share JSON)
        LinearLayout buttonsLayout = new LinearLayout(context);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView copyJsonBtn = createActionButton(context, LocaleController.getString("CopyJson", R.string.CopyJson), R.drawable.msg_copy, isDark, true);
        copyJsonBtn.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(formattedJson);
            if (fragment != null && fragment.getParentActivity() != null) {
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString("JsonCopiedToast", R.string.JsonCopiedToast)).show();
            }
        });
        buttonsLayout.addView(copyJsonBtn, LayoutHelper.createLinear(0, 44, 1.0f, 0, 0, 8, 0));

        TextView shareJsonBtn = createActionButton(context, LocaleController.getString("ShareJson", R.string.ShareJson), R.drawable.msg_shareout, isDark, false);
        shareJsonBtn.setOnClickListener(v -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Telegram Message " + keyParams.messageId + " JSON");
                shareIntent.putExtra(Intent.EXTRA_TEXT, formattedJson);
                getContext().startActivity(Intent.createChooser(shareIntent, LocaleController.getString("ShareJson", R.string.ShareJson)));
            } catch (Throwable ignored) {
            }
        });
        buttonsLayout.addView(shareJsonBtn, LayoutHelper.createLinear(0, 44, 1.0f, 8, 0, 0, 0));

        contentLayout.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(contentLayout);
    }

    private void addChip(LinearLayout container, Context context, String label, String value) {
        boolean isDark = Theme.isCurrentThemeDark();
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(6));
        bg.setColor(isDark ? 0xFF282E3A : 0xFFE2E8F0);
        chip.setBackground(bg);
        chip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));

        TextView labelView = new TextView(context);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        labelView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        labelView.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        labelView.setText(label + ": ");
        chip.addView(labelView);

        TextView valView = new TextView(context);
        valView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        valView.setTypeface(Typeface.MONOSPACE);
        valView.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1A202C);
        valView.setText(value);
        chip.addView(valView);

        chip.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(value);
            if (fragment != null && fragment.getParentActivity() != null) {
                BulletinFactory.of(fragment).createCopyBulletin(label + " " + LocaleController.getString("IDCopiedToast", R.string.IDCopiedToast) + " (" + value + ")").show();
            }
        });

        container.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
    }

    private TextView createActionButton(Context context, String text, int iconRes, boolean isDark, boolean primary) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(8));
        if (primary) {
            bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            btn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        } else {
            bg.setColor(isDark ? 0xFF242A35 : 0xFFE2E8F0);
            btn.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        }
        btn.setBackground(bg);
        return btn;
    }
}
