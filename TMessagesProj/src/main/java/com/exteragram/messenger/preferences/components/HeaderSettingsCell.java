package com.exteragram.messenger.preferences.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class HeaderSettingsCell extends FrameLayout {

    public final TextView titleTextView;
    public final TextView versionBadge;
    public final TextView subtitleTextView;

    public HeaderSettingsCell(Context context) {
        super(context);

        // Logo Container with glowing gradient squircle outline
        FrameLayout logoContainer = new FrameLayout(context) {
            private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF rect = new RectF();

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                rect.set(0, 0, getWidth(), getHeight());
                glowPaint.setStyle(Paint.Style.STROKE);
                glowPaint.setStrokeWidth(AndroidUtilities.dp(2));
                glowPaint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                        0xFF00E5FF, 0xFF7C4DFF, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(26), AndroidUtilities.dp(26), glowPaint);
            }
        };
        logoContainer.setWillNotDraw(false);

        ImageView logo = new ImageView(context);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setImageResource(R.mipmap.ic_launcher_ocoder_default);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            logo.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(24));
                }
            });
            logo.setClipToOutline(true);
        }
        logoContainer.addView(logo, LayoutHelper.createFrame(90, 90, Gravity.CENTER));
        addView(logoContainer, LayoutHelper.createFrame(94, 94, Gravity.CENTER | Gravity.TOP, 0, 20, 0, 0));

        // Horizontal title layout with version badge
        LinearLayout titleContainer = new LinearLayout(context);
        titleContainer.setOrientation(LinearLayout.HORIZONTAL);
        titleContainer.setGravity(Gravity.CENTER_VERTICAL);

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleTextView.setText("Ox-gram");
        titleContainer.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        versionBadge = new TextView(context) {
            private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF bgRect = new RectF();

            @Override
            protected void onDraw(Canvas canvas) {
                bgRect.set(0, 0, getWidth(), getHeight());
                bgPaint.setColor(0x2600E5FF);
                bgPaint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(bgRect, AndroidUtilities.dp(12), AndroidUtilities.dp(12), bgPaint);

                bgPaint.setStyle(Paint.Style.STROKE);
                bgPaint.setStrokeWidth(AndroidUtilities.dp(1));
                bgPaint.setColor(0x8000E5FF);
                canvas.drawRoundRect(bgRect, AndroidUtilities.dp(12), AndroidUtilities.dp(12), bgPaint);

                super.onDraw(canvas);
            }
        };
        versionBadge.setTextColor(0xFF00E5FF);
        versionBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        versionBadge.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        versionBadge.setText("v1.0.0");
        versionBadge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
        versionBadge.setGravity(Gravity.CENTER);
        titleContainer.addView(versionBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        addView(titleContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.TOP, 0, 128, 0, 0));

        subtitleTextView = new TextView(context);
        subtitleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        subtitleTextView.setLineSpacing(AndroidUtilities.dp(2), 1f);
        subtitleTextView.setText(LocaleController.getString("SettingsDescription", R.string.SettingsDescription));
        subtitleTextView.setGravity(Gravity.CENTER);
        subtitleTextView.setPadding(0, 0, 0, 0);
        addView(subtitleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.TOP, 40, 162, 40, 24));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
}
