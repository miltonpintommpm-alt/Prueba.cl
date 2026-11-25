package cl.prueba.salud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Anillo de progreso: calorías consumidas frente a la meta diaria. */
public class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private double consumed, target = 1;

    public RingView(Context c) {
        super(c);
        float d = getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(16 * d);
        track.setColor(Ui.TRACK);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(16 * d);
        arc.setStrokeCap(Paint.Cap.ROUND);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTextSize(34 * d);
        big.setFakeBoldText(true);
        big.setColor(Ui.TEXT);
        small.setTextAlign(Paint.Align.CENTER);
        small.setTextSize(14 * d);
        small.setColor(Ui.MUTED);
    }

    public void set(double consumed, double target) {
        this.consumed = consumed;
        this.target = Math.max(1, target);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float pad = track.getStrokeWidth();
        float size = Math.min(getWidth(), getHeight()) - 2 * pad;
        float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
        oval.set(left, top, left + size, top + size);
        c.drawArc(oval, 0, 360, false, track);

        double frac = consumed / target;
        arc.setColor(frac > 1.05 ? Ui.DANGER : frac > 0.9 ? Ui.WARN : Ui.PRIMARY);
        c.drawArc(oval, -90, (float) (360 * Math.min(1, frac)), false, arc);

        double left2 = target - consumed;
        float cy = getHeight() / 2f;
        c.drawText(String.valueOf((int) Math.round(Math.abs(left2))), getWidth() / 2f, cy + big.getTextSize() * 0.2f, big);
        c.drawText(left2 >= 0 ? "kcal restantes" : "kcal de exceso", getWidth() / 2f, cy + big.getTextSize() * 0.2f + small.getTextSize() * 1.6f, small);
        c.drawText((int) Math.round(consumed) + " de " + (int) Math.round(target), getWidth() / 2f, cy - big.getTextSize() * 0.9f, small);
    }
}
