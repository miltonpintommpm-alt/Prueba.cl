package cl.prueba.salud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Barras de calorías de los últimos días con una línea de la meta. */
public class BarChartView extends View {

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private double[] values = new double[0];
    private String[] labels = new String[0];
    private double target;

    public BarChartView(Context c) {
        super(c);
        float d = getResources().getDisplayMetrics().density;
        goal.setStyle(Paint.Style.STROKE);
        goal.setStrokeWidth(2 * d);
        goal.setColor(Ui.MUTED);
        goal.setPathEffect(new DashPathEffect(new float[]{6 * d, 4 * d}, 0));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(11 * d);
        label.setColor(Ui.MUTED);
    }

    public void set(double[] values, String[] labels, double target) {
        this.values = values;
        this.labels = labels;
        this.target = target;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (values.length == 0) return;
        float d = getResources().getDisplayMetrics().density;
        float labelH = label.getTextSize() * 2.2f;
        float chartH = getHeight() - labelH * 2;
        double max = target * 1.3;
        for (double v : values) max = Math.max(max, v);
        float slot = getWidth() / (float) values.length;
        float bw = Math.min(slot * 0.6f, 28 * d);
        for (int i = 0; i < values.length; i++) {
            float cx = slot * i + slot / 2f;
            float h = (float) (values[i] / max * chartH);
            r.set(cx - bw / 2, labelH + chartH - h, cx + bw / 2, labelH + chartH);
            bar.setColor(values[i] == 0 ? Ui.TRACK : values[i] > target * 1.05 ? Ui.DANGER : Ui.PRIMARY);
            c.drawRoundRect(r, 4 * d, 4 * d, bar);
            if (values[i] > 0) {
                c.drawText(String.valueOf((int) Math.round(values[i])), cx, r.top - 4 * d, label);
            }
            c.drawText(labels[i], cx, getHeight() - label.getTextSize() * 0.6f, label);
        }
        float gy = labelH + chartH - (float) (target / max * chartH);
        c.drawLine(0, gy, getWidth(), gy, goal);
    }
}
