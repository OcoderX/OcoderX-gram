/*
 * OcoderX-gram — custom drawer section header cell
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class DrawerSectionCell extends FrameLayout {

    private final TextView textView;

    public DrawerSectionCell(Context context) {
        super(context);

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setAllCaps(true);
        textView.setLetterSpacing(0.08f);
        addView(textView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                LocaleController.isRTL ? 0 : 19, 0, LocaleController.isRTL ? 19 : 0, 0
        ));
        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateColors();
    }

    public void updateColors() {
        int color = Theme.getColor(Theme.key_chats_menuItemIcon);
        textView.setTextColor((color & 0x00FFFFFF) | 0x99000000);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY)
        );
    }
}
