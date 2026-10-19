package dev.pipten.module;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * PiP 10s floating controls — iOS-inspired translucent glass aesthetic.
 *
 * Draws a SINGLE compact, transparent pill with 3 independent glass buttons.
 * Does not capture video, read pixels, or apply a fake rectangle-wide dark scrim.
 * Actual backdrop blur is not exposed to a plain accessibility overlay; this
 * implementation uses layered translucent highlights and gradients instead.
 */
public final class ControlStrip extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final RectF r = new RectF();
    private final Path clip = new Path();
    private final Typeface numberTypeface = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private boolean playing;
    private int pressed = -1;

    public ControlStrip(Context ctx) {
        super(ctx);
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
        setContentDescription("PiP media controls: back 10 seconds, play or pause, forward 10 seconds");
    }

    private float dp(float value) { return value * density; }

    void updatePlayState(boolean active) {
        if (playing != active) {
            playing = active;
            invalidate();
        }
    }

    private int indexAt(float x) {
        return Math.max(0, Math.min(2, (int)(x * 3f / Math.max(1, getWidth()))));
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = indexAt(event.getX());
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int selected = indexAt(event.getX());
                boolean activate = pressed == selected;
                pressed = -1;
                invalidate();
                if (activate) {
                    MediaBridge.control(getContext(), selected - 1);
                    performClick();
                }
                return true;
            default:
                return true;
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        // A deliberately subtle shadow outside the capsule; the overlay window
        // itself is transparent. No full-width opaque control backdrop.
        r.set(dp(3), dp(3), w - dp(3), h - dp(3));
        float arc = r.height() / 2f;

        reset();
        paint.setColor(0x37000000);
        paint.setShadowLayer(dp(5), 0, dp(2.5f), 0x6B000000);
        c.drawRoundRect(r, arc, arc, paint);
        paint.clearShadowLayer();

        // Smoky titanium glass: transparent and gently luminous in the center.
        reset();
        paint.setShader(new LinearGradient(
                r.left, r.top, r.left, r.bottom,
                new int[]{0xBC768393, 0xAB34404E, 0xB31C2633},
                new float[]{0f, 0.41f, 1f}, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, arc, arc, paint);
        paint.setShader(null);

        // Upper sheen: clipped capsule to mimic refracted liquid glass.
        clip.reset();
        clip.addRoundRect(r, arc, arc, Path.Direction.CW);
        int save = c.save();
        c.clipPath(clip);
        reset();
        paint.setShader(new LinearGradient(
                0, r.top, 0, r.centerY() + dp(5),
                new int[]{0x7CFFFFFF, 0x1EFFFFFF, 0x00FFFFFF},
                new float[]{0, 0.48f, 1}, Shader.TileMode.CLAMP));
        RectF gloss = new RectF(r.left + dp(1), r.top, r.right - dp(1), r.centerY() + dp(5));
        c.drawOval(gloss, paint);
        paint.setShader(null);

        // Small cool-toned radiance along the bottom of the glass.
        reset();
        paint.setShader(new LinearGradient(0, r.centerY(), 0, r.bottom,
                0x003BC8FF, 0x2637B8E6, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, arc, arc, paint);
        paint.setShader(null);
        c.restoreToCount(save);

        // Reflections and outline are important for transparent appearance.
        reset();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(.9f));
        paint.setShader(new LinearGradient(0, r.top, 0, r.bottom,
                new int[]{0xD1FFFFFF, 0x58FFFFFF, 0x49D8EDFF},
                null, Shader.TileMode.CLAMP));
        c.drawRoundRect(r.left + dp(.6f), r.top + dp(.6f),
                r.right - dp(.6f), r.bottom - dp(.6f), arc, arc, paint);
        paint.setShader(null);

        // Three glass lenses: 48dpish visuals, touch area remains full cell.
        float cellWidth = w / 3f;
        float radius = Math.min(dp(24), Math.min(h * .355f, cellWidth * .35f));
        float cy = h / 2f;
        for (int i = 0; i < 3; i++) {
            float cx = cellWidth * (i + .5f);
            drawLens(c, cx, cy, radius, i);
        }
    }

    private void drawLens(Canvas canvas, float x, float y, float radius, int index) {
        int save = canvas.save();
        canvas.translate(x, y);
        boolean down = pressed == index;
        if (down) canvas.scale(.945f, .945f);

        // Darkened soft edge under the independent glass disk.
        reset();
        paint.setColor(down ? 0x46000000 : 0x34000000);
        canvas.drawCircle(0, dp(1), radius + dp(1), paint);

        reset();
        paint.setShader(new RadialGradient(
                -radius * .57f, -radius * .76f, radius * 2.05f,
                new int[]{0x89FFFFFF, 0x487687A0, down ? 0x9A27384C : 0x66333D50},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(0, 0, radius, paint);
        paint.setShader(null);

        // Precision-machined rim and specular upper arc.
        reset();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(.85f));
        paint.setColor(0x9FF6FBFF);
        canvas.drawCircle(0, 0, radius - dp(.6f), paint);
        paint.setStrokeWidth(dp(1.0f));
        paint.setColor(0xABFFFFFF);
        canvas.drawArc(-radius + dp(1), -radius + dp(1),
                radius - dp(1), radius - dp(1),
                207, 128, false, paint);

        canvas.scale(radius / 24f, radius / 24f);
        drawSymbol(canvas, index);
        canvas.restoreToCount(save);
    }

    private void drawSymbol(Canvas canvas, int index) {
        reset();
        paint.setColor(0xFFFFFFFF);
        if (index == 1) {
            if (playing) {
                canvas.drawRoundRect(-7.5f, -10, -2f, 10, 1.1f, 1.1f, paint);
                canvas.drawRoundRect(2f, -10, 7.5f, 10, 1.1f, 1.1f, paint);
            } else {
                Path play = new Path();
                play.moveTo(-7, -10.5f);
                play.lineTo(10, 0);
                play.lineTo(-7, 10.5f);
                play.close();
                canvas.drawPath(play, paint);
            }
            return;
        }

        // Lighter, consistent iOS-style curved 10s arrows.
        boolean back = index == 0;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (back)
            canvas.drawArc(-12.1f, -12.1f, 12.1f, 12.1f, -52, -263, false, paint);
        else
            canvas.drawArc(-12.1f, -12.1f, 12.1f, 12.1f, -128, 263, false, paint);

        paint.setStyle(Paint.Style.FILL);
        Path tip = new Path();
        if (back) {
            tip.moveTo(-13.6f, -13.4f);
            tip.lineTo(-12.9f, -4.3f);
            tip.lineTo(-5.6f, -10.4f);
        } else {
            tip.moveTo(13.6f, -13.4f);
            tip.lineTo(12.9f, -4.3f);
            tip.lineTo(5.6f, -10.4f);
        }
        tip.close();
        canvas.drawPath(tip, paint);

        paint.setTypeface(numberTypeface);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12.8f);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText("10", 0, -(fm.ascent + fm.descent) / 2f + 1.2f, paint);
    }

    private void reset() {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
    }
}
