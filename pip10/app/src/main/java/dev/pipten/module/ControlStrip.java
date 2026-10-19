package dev.pipten.module;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * A restrained, iOS-inspired media card: one soft dark translucent material,
 * white unframed icons, and no nested glass discs, metallic rims or glare.
 *
 * Keeps the working v0.5 media click logic unchanged.
 */
public final class ControlStrip extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final RectF card = new RectF();
    private final Typeface numberTypeface =
            Typeface.create("sans-serif-medium", Typeface.BOLD);
    private boolean playing;
    private int pressed = -1;

    public ControlStrip(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
        setContentDescription(
                "Back 10 seconds, play or pause, forward 10 seconds");
    }

    private float dp(float d) { return d * density; }

    void updatePlayState(boolean value) {
        if (playing != value) {
            playing = value;
            invalidate();
        }
    }

    private int section(float x) {
        return Math.max(0, Math.min(2,
                (int)(3f * x / Math.max(1, getWidth()))));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = section(e.getX());
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int selected = section(e.getX());
                boolean trigger = selected == pressed;
                pressed = -1;
                invalidate();
                if (trigger) {
                    MediaBridge.control(getContext(), selected - 1);
                    performClick();
                }
                return true;
            default: return true;
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        if (width < 4 || height < 4) return;

        float inset = dp(3);
        card.set(inset, inset, width - inset, height - inset);
        float corner = Math.min(dp(25), card.height() * .38f);

        // Soft low-elevation shadow, not a glass-like white neon rim.
        reset();
        paint.setColor(0x900E1A1C);
        paint.setShadowLayer(dp(6), 0, dp(2), 0x32000000);
        canvas.drawRoundRect(card, corner, corner, paint);
        paint.clearShadowLayer();

        // Real window blur is requested by OverlayAccessibilityService when
        // Android supports it; this tint works as a graceful no-blur fallback.
        reset();
        paint.setShader(new LinearGradient(
                card.left, card.top, card.left, card.bottom,
                new int[] {0xAE293537, 0xB51D282A, 0xC0192527},
                new float[] {0f, .50f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(card, corner, corner, paint);
        paint.setShader(null);

        // Almost imperceptible light reflection and single thin glass edge.
        reset();
        paint.setColor(0x20FFFFFF);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(.75f));
        canvas.drawRoundRect(
                card.left + dp(.7f), card.top + dp(.7f),
                card.right - dp(.7f), card.bottom - dp(.7f),
                corner, corner, paint);

        reset();
        paint.setColor(0x20FFFFFF);
        paint.setStrokeWidth(dp(1.1f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(
                card.left + dp(5), card.top + dp(1),
                card.right - dp(5), card.bottom - dp(1),
                206, 128, false, paint);

        float cy = card.centerY();
        float column = card.width() / 3f;
        for (int i = 0; i < 3; i++) {
            float cx = card.left + column * (i + .5f);

            // Only on press: a quiet transparent feedback background.
            if (pressed == i) {
                reset();
                paint.setColor(0x27FFFFFF);
                float half = Math.min(dp(24), column * .31f);
                canvas.drawRoundRect(cx - half, cy - dp(21),
                        cx + half, cy + dp(21), dp(14), dp(14), paint);
            }

            int saved = canvas.save();
            canvas.translate(cx, cy);
            float iconScale = Math.min(1.22f,
                    Math.min(card.height() / dp(57),
                             column / dp(79)));
            canvas.scale(iconScale * density, iconScale * density);
            drawIcon(canvas, i);
            canvas.restoreToCount(saved);
        }
    }

    private void drawIcon(Canvas canvas, int idx) {
        reset();
        paint.setColor(Color.WHITE);
        if (idx == 1) {
            // Match the reference's clean white play / pause controls.
            if (playing) {
                canvas.drawRoundRect(-7f, -10f, -2f, 10f, .9f, .9f, paint);
                canvas.drawRoundRect(2f, -10f, 7f, 10f, .9f, .9f, paint);
            } else {
                Path play = new Path();
                play.moveTo(-7.3f, -10.8f);
                play.lineTo(10.4f, 0f);
                play.lineTo(-7.3f, 10.8f);
                play.close();
                canvas.drawPath(play, paint);
            }
            return;
        }

        boolean back = idx == 0;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (back) canvas.drawArc(
                -13f, -13f, 13f, 13f, -48f, -265f, false, paint);
        else canvas.drawArc(
                -13f, -13f, 13f, 13f, -132f, 265f, false, paint);

        paint.setStyle(Paint.Style.FILL);
        Path arrow = new Path();
        if (back) {
            arrow.moveTo(-14.6f, -14.4f);
            arrow.lineTo(-14f, -4.6f);
            arrow.lineTo(-6f, -11.3f);
        } else {
            arrow.moveTo(14.6f, -14.4f);
            arrow.lineTo(14f, -4.6f);
            arrow.lineTo(6f, -11.3f);
        }
        arrow.close();
        canvas.drawPath(arrow, paint);

        paint.setTypeface(numberTypeface);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(13f);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText("10", 0, -(fm.ascent + fm.descent) * .5f + 1f, paint);
    }

    private void reset() {
        paint.reset();
        paint.setAntiAlias(true);
    }
}
