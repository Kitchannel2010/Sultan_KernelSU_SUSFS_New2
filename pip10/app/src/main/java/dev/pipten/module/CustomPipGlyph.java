package dev.pipten.module;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Independently painted iOS-inspired PiP controls. Drawn in a View foreground:
 * the framework's 20dp ImageView/icon size no longer affects rendered artwork.
 * The existing framework action view retains the click target/behavior.
 */
public final class CustomPipGlyph extends Drawable {
    public static final int BACK = -1;
    public static final int PLAY = 0;
    public static final int FORWARD = 1;
    public static final int PAUSE = 2;

    private final int type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;

    public CustomPipGlyph(int type) {
        this.type = type;
    }

    public int controlType() {
        return type;
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float edge = Math.min(b.width(), b.height());
        if (edge <= 1f) return;
        int saved = canvas.save();
        canvas.translate(b.exactCenterX(), b.exactCenterY());
        canvas.scale(edge / 52f, edge / 52f);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setAlpha(alpha);

        // Entire foreground is our own drawing, not an enlarged stock icon.
        // Dark disk obscures the native 20dp glyph underneath.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha * 210 / 255, 19, 22, 28));
        canvas.drawCircle(0f, 0f, 25.0f, paint);
        paint.setColor(Color.argb(alpha * 155 / 255, 255, 255, 255));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        canvas.drawCircle(0f, 0f, 24.4f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(alpha);

        if (type == PLAY) {
            Path play = new Path();
            play.moveTo(-6.8f, -11.7f);
            play.lineTo(12f, 0f);
            play.lineTo(-6.8f, 11.7f);
            play.close();
            canvas.drawPath(play, paint);
        } else if (type == PAUSE) {
            canvas.drawRoundRect(-10.3f, -11.7f, -2.6f, 11.7f, 1.0f, 1.0f, paint);
            canvas.drawRoundRect(2.6f, -11.7f, 10.3f, 11.7f, 1.0f, 1.0f, paint);
        } else {
            // Curved arrows plus readable '10' independent of device icon density.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.8f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            if (type == BACK)
                canvas.drawArc(-15.5f, -15.5f, 15.5f, 15.5f,
                        -64f, -257f, false, paint);
            else
                canvas.drawArc(-15.5f, -15.5f, 15.5f, 15.5f,
                        -116f, 257f, false, paint);
            paint.setStyle(Paint.Style.FILL);
            Path tip = new Path();
            if (type == BACK) {
                tip.moveTo(-17.3f, -15.4f);
                tip.lineTo(-16.7f, -3.2f);
                tip.lineTo(-6.4f, -11.2f);
            } else {
                tip.moveTo(17.3f, -15.4f);
                tip.lineTo(16.7f, -3.2f);
                tip.lineTo(6.4f, -11.2f);
            }
            tip.close();
            canvas.drawPath(tip, paint);
            paint.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(15.5f);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText("10", 0, -(fm.ascent + fm.descent) / 2f + 1.5f, paint);
        }
        canvas.restoreToCount(saved);
    }

    @Override public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
        invalidateSelf();
    }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
