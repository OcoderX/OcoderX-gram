/*
 * OcoderX-gram — shimmering VIP promo banner cell for the Settings list.
 */

package org.telegram.ui.Cells;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class VipBannerCell extends FrameLayout {

    private static final int COLOR_START = 0xFFFFC94A;
    private static final int COLOR_END = 0xFF00C6FF;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clipPath = new Path();
    private final int cornerRadius = AndroidUtilities.dp(18);

    private final TextView titleView;
    private final TextView subtitleView;

    private ValueAnimator shimmerAnimator;
    private float shimmerProgress = -0.35f;
    private boolean attached;

    public VipBannerCell(Context context) {
        super(context);
        setWillNotDraw(false);
        setFocusable(true);

        shimmerPaint.setStyle(Paint.Style.FILL);

        ImageView iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER);
        iconView.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iconView.setImageResource(R.drawable.msg2_reactions2);
        iconView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), 0x33FFFFFF));
        addView(iconView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 16, 0, 16, 0));

        titleView = new TextView(context);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setText(LocaleController.getString("VipBannerTitle", R.string.VipBannerTitle));
        addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 24 : 68, 15, LocaleController.isRTL ? 68 : 24, 0));

        subtitleView = new TextView(context);
        subtitleView.setTextColor(0xCCFFFFFF);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        subtitleView.setMaxLines(2);
        subtitleView.setText(LocaleController.getString("VipBannerSubtitle", R.string.VipBannerSubtitle));
        addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 24 : 68, 38, LocaleController.isRTL ? 68 : 24, 0));

        setForeground(Theme.getRoundRectSelectorDrawable(18, 0x24FFFFFF));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = AndroidUtilities.dp(78);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(AndroidUtilities.dp(12), AndroidUtilities.dp(6), w - AndroidUtilities.dp(12), h - AndroidUtilities.dp(6));
        backgroundPaint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom, COLOR_START, COLOR_END, Shader.TileMode.CLAMP));
        clipPath.reset();
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint);

        float bandWidth = rect.width() * 0.28f;
        float centerX = rect.left + rect.width() * shimmerProgress;
        if (centerX + bandWidth >= rect.left && centerX - bandWidth <= rect.right) {
            shimmerPaint.setShader(new LinearGradient(
                    centerX - bandWidth, rect.top, centerX + bandWidth, rect.bottom,
                    new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.save();
            canvas.clipPath(clipPath);
            canvas.drawRect(rect, shimmerPaint);
            canvas.restore();
        }

        super.onDraw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attached = false;
        stopShimmer();
    }

    private void startShimmer() {
        if (shimmerAnimator != null) {
            return;
        }
        shimmerAnimator = ValueAnimator.ofFloat(-0.35f, 1.35f);
        shimmerAnimator.setDuration(2600);
        shimmerAnimator.setStartDelay(400);
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.addUpdateListener(a -> {
            shimmerProgress = (float) a.getAnimatedValue();
            if (attached) {
                invalidate();
            }
        });
        shimmerAnimator.start();
    }

    private void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }
}
