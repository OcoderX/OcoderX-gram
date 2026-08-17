package org.telegram.ui.Components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.radolyn.ayugram.AyuConfig;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

public class DynamicIslandView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final TextView brandTextView;
    private final ImageView ghostImageView;
    private final TextView telemetryTextView;

    public DynamicIslandView(Context context) {
        super(context);
        setWillNotDraw(false);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);

        // 1. Ox-gram brand text
        brandTextView = new TextView(context);
        brandTextView.setText("Ox-gram");
        brandTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        brandTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        brandTextView.setTextColor(0xFF00E5FF);
        container.addView(brandTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        // 2. Center Ghost Mode Icon
        ghostImageView = new ImageView(context);
        ghostImageView.setImageResource(R.drawable.msg2_secret);
        ghostImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        ghostImageView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2));
        updateGhostState();

        ghostImageView.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            AyuConfig.toggleGhostMode();
            updateGhostState();

            // Spring bounce animation on click
            AnimatorSet bounceAnim = new AnimatorSet();
            bounceAnim.playTogether(
                    ObjectAnimator.ofFloat(v, View.SCALE_X, 1.0f, 1.35f, 1.0f),
                    ObjectAnimator.ofFloat(v, View.SCALE_Y, 1.0f, 1.35f, 1.0f)
            );
            bounceAnim.setDuration(300);
            bounceAnim.start();

            var msg = AyuConfig.isGhostModeActive()
                    ? LocaleController.getString("GhostModeEnabled", R.string.GhostModeEnabled)
                    : LocaleController.getString("GhostModeDisabled", R.string.GhostModeDisabled);
            BulletinFactory.global().createSimpleBulletin(R.drawable.msg2_secret, msg).show();
        });

        container.addView(ghostImageView, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 8, 0, 8, 0));

        // 3. Telemetry / Speed indicator
        telemetryTextView = new TextView(context);
        telemetryTextView.setText("32ms ⚡");
        telemetryTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
        telemetryTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        telemetryTextView.setTextColor(0xFF80D8FF);
        container.addView(telemetryTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        addView(container, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
    }

    public void updateGhostState() {
        boolean active = AyuConfig.isGhostModeActive();
        ghostImageView.setColorFilter(new PorterDuffColorFilter(active ? 0xFF00E5FF : 0x66FFFFFF, PorterDuff.Mode.SRC_IN));
        ghostImageView.setAlpha(active ? 1.0f : 0.6f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        float radius = getHeight() / 2f;

        // Dark glass background
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xDD0A0F1D);
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        // Neon gradient border
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(AndroidUtilities.dp(1.2f));
        strokePaint.setShader(new LinearGradient(0, 0, getWidth(), 0,
                new int[]{0x8000E5FF, 0x807C4DFF, 0x8000E5FF}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, strokePaint);

        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = AndroidUtilities.dp(30);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateGhostState();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        updateGhostState();
    }
}
