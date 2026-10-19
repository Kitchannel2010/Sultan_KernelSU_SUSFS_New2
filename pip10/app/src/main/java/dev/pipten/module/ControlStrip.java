package dev.pipten.module;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

/** Full-sized, independently drawn, iOS-inspired controls anchored to PiP. */
public final class ControlStrip extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private boolean playing;
    private int pressed = -1;

    public ControlStrip(Context ctx) {
        super(ctx);
        density = getResources().getDisplayMetrics().density;
        setClickable(true);
        setContentDescription("PiP 10-second rewind, play/pause and forward");
    }

    void updatePlayState(boolean newState) {
        if (playing != newState) {
            playing = newState;
            invalidate();
        }
    }

    private int indexFor(float x) {
        return Math.max(0, Math.min(2, (int)(x * 3f / Math.max(1, getWidth()))));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = indexFor(e.getX());
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int index = indexFor(e.getX());
                if (pressed == index) {
                    MediaBridge.control(getContext(), index - 1);
                    performClick();
                }
                pressed = -1;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        float radius = Math.min(28 * density,
                Math.min(width / 7.5f, height / 2f - 2 * density));
        float centerY = height / 2f;
        for (int i = 0; i < 3; i++) {
            float centerX = width * (i + 0.5f) / 3f;
            int saved = canvas.save();
            canvas.translate(centerX, centerY);
            canvas.scale(radius / 28f, radius / 28f);
            p.reset();
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.FILL);
            p.setColor(i == pressed ? 0xD914171D : 0xBD17191F);
            canvas.drawCircle(0, 0, 27.6f, p);
            p.setColor(0xB9FFFFFF);
            p.setStrokeWidth(1.0f);
            p.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(0, 0, 27f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            if (i == 1) {
                if (playing) {
                    canvas.drawRoundRect(-10, -12, -3.2f, 12, 1, 1, p);
                    canvas.drawRoundRect(3.2f, -12, 10, 12, 1, 1, p);
                } else {
                    Path path = new Path();
                    path.moveTo(-7, -13);
                    path.lineTo(13, 0);
                    path.lineTo(-7, 13);
                    path.close();
                    canvas.drawPath(path, p);
                }
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                p.setStrokeCap(Paint.Cap.ROUND);
                if (i == 0)
                    canvas.drawArc(-17, -17, 17, 17, -60, -265, false, p);
                else
                    canvas.drawArc(-17, -17, 17, 17, -120, 265, false, p);
                p.setStyle(Paint.Style.FILL);
                Path arrow = new Path();
                if (i == 0) {
                    arrow.moveTo(-19, -17); arrow.lineTo(-18, -4); arrow.lineTo(-7, -12);
                } else {
                    arrow.moveTo(19, -17); arrow.lineTo(18, -4); arrow.lineTo(7, -12);
                }
                arrow.close();
                canvas.drawPath(arrow, p);
                p.setTypeface(android.graphics.Typeface.create("sans-serif",
                        android.graphics.Typeface.BOLD));
                p.setTextSize(16.5f);
                p.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics fm = p.getFontMetrics();
                canvas.drawText("10", 0, -(fm.ascent + fm.descent) / 2f + 2, p);
            }
            canvas.restoreToCount(saved);
        }
    }
}
