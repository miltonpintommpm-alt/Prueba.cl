package cl.prueba.salud;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Colores y pequeños constructores de vistas para armar la interfaz por código. */
final class Ui {

    static final int PRIMARY = 0xFF2E7D32;
    static final int PRIMARY_LIGHT = 0xFFE8F5E9;
    static final int BG = 0xFFF4F6F4;
    static final int CARD = Color.WHITE;
    static final int TEXT = 0xFF1C1C1C;
    static final int MUTED = 0xFF66706A;
    static final int TRACK = 0xFFE3E7E4;
    static final int WARN = 0xFFF9A825;
    static final int DANGER = 0xFFD32F2F;
    static final int PROTEIN = 0xFF1E88E5;
    static final int CARBS = 0xFFFB8C00;
    static final int FAT = 0xFF8E24AA;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView title(Context c, CharSequence s) {
        TextView t = text(c, s, 18, TEXT, true);
        t.setPadding(0, 0, 0, dp(c, 8));
        return t;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(c, 16));
        l.setBackground(bg);
        l.setElevation(dp(c, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    static Button button(Context c, String label, boolean filled) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(filled ? Color.WHITE : PRIMARY);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(c, 24));
        if (filled) {
            bg.setColor(PRIMARY);
        } else {
            bg.setColor(PRIMARY_LIGHT);
        }
        b.setBackground(bg);
        b.setStateListAnimator(null);
        int ph = dp(c, 16);
        b.setPadding(ph, 0, ph, 0);
        b.setMinHeight(dp(c, 48));
        return b;
    }

    static LinearLayout.LayoutParams weighted(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        return lp;
    }

    static LinearLayout.LayoutParams fullWidth(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 4), 0, dp(c, 4));
        return lp;
    }

    /** Fila "etiqueta ........ valor". */
    static LinearLayout row(Context c, CharSequence label, CharSequence value) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(c, 4), 0, dp(c, 4));
        TextView l = text(c, label, 15, MUTED, false);
        TextView v = text(c, value, 15, TEXT, true);
        v.setGravity(Gravity.END);
        r.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        r.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return r;
    }

    /** Barra de progreso con etiqueta, para macronutrientes. */
    static View macroBar(Context c, String name, double value, double goal, int color) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        box.addView(text(c, name, 13, MUTED, false));
        ProgressBar pb = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(1000);
        pb.setProgress((int) Math.min(1000, goal <= 0 ? 0 : value / goal * 1000));
        pb.setProgressTintList(ColorStateList.valueOf(color));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(TRACK));
        box.addView(pb, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 10)));
        box.addView(text(c, Math.round(value) + " / " + Math.round(goal) + " g", 13, TEXT, true));
        return box;
    }

    static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }
}
