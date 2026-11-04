package cl.prueba.taller;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Pequeños ayudantes para armar las pantallas por código. */
final class Ui {
    static final int PRIMARIO = 0xFF1E3A5F;
    static final int PRIMARIO_OSCURO = 0xFF15293F;
    static final int ACENTO = 0xFFE09B00;
    static final int PELIGRO = 0xFFC62828;
    static final int OK = 0xFF2E7D32;
    static final int WHATSAPP = 0xFF1DA851;
    static final int FONDO = 0xFFF2F4F7;
    static final int TEXTO = 0xFF1B1F24;
    static final int TEXTO_SEC = 0xFF5F6B7A;
    static final int BORDE = 0xFFD5DAE1;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Crea una pantalla con scroll y devuelve el contenedor vertical donde agregar vistas. */
    static LinearLayout pagina(Activity a) {
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(FONDO);
        sv.setFillViewport(true);
        LinearLayout l = vertical(a);
        int p = dp(a, 12);
        l.setPadding(p, p, p, dp(a, 32));
        sv.addView(l, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        a.setContentView(sv);
        return l;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Tarjeta blanca con bordes redondeados. */
    static LinearLayout tarjeta(Context c) {
        LinearLayout l = vertical(c);
        l.setBackground(redondeado(0xFFFFFFFF, dp(c, 10), 1, BORDE));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    static GradientDrawable redondeado(int color, float radio, int borde, int colorBorde) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radio);
        if (borde > 0) g.setStroke(borde, colorBorde);
        return g;
    }

    static TextView texto(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.1f);
        return t;
    }

    static TextView texto(Context c, CharSequence s) {
        return texto(c, s, 15, TEXTO);
    }

    static TextView secundario(Context c, CharSequence s) {
        return texto(c, s, 13.5f, TEXTO_SEC);
    }

    static TextView titulo(Context c, CharSequence s) {
        TextView t = texto(c, s, 19, PRIMARIO);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView negrita(Context c, CharSequence s, float sp) {
        TextView t = texto(c, s, sp, TEXTO);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    /** Encabezado de sección en mayúsculas. */
    static TextView seccion(Context c, String s) {
        TextView t = texto(c, s.toUpperCase(), 12.5f, TEXTO_SEC);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.06f);
        t.setPadding(dp(c, 4), dp(c, 14), 0, dp(c, 6));
        return t;
    }

    static EditText campo(Context c, String pista, int inputType) {
        EditText e = new EditText(c);
        e.setHint(pista);
        e.setInputType(inputType);
        e.setTextSize(16);
        e.setSingleLine((inputType & android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        return e;
    }

    /** Campo con etiqueta arriba. */
    static EditText campoConEtiqueta(LinearLayout padre, String etiqueta, String pista, int inputType) {
        Context c = padre.getContext();
        TextView et = texto(c, etiqueta, 12.5f, TEXTO_SEC);
        et.setPadding(dp(c, 4), dp(c, 8), 0, 0);
        padre.addView(et);
        EditText e = campo(c, pista, inputType);
        padre.addView(e, anchoCompleto());
        return e;
    }

    static Button boton(Context c, String s, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(0xFFFFFFFF);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF),
                redondeado(color, dp(c, 8), 0, 0), null));
        int p = dp(c, 12);
        b.setPadding(p, 0, p, 0);
        b.setMinHeight(dp(c, 46));
        b.setOnClickListener(l);
        return b;
    }

    static Button botonPlano(Context c, String s, View.OnClickListener l) {
        Button b = boton(c, s, 0xFFFFFFFF, l);
        b.setTextColor(PRIMARIO);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201E3A5F),
                redondeado(0xFFFFFFFF, dp(c, 8), dp(c, 1), PRIMARIO), null));
        return b;
    }

    static LinearLayout.LayoutParams anchoCompleto() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Agrega la vista a lo ancho, con un margen superior en dp. */
    static <T extends View> T agregar(LinearLayout padre, T v, int margenSuperiorDp) {
        LinearLayout.LayoutParams lp = anchoCompleto();
        lp.topMargin = dp(padre.getContext(), margenSuperiorDp);
        padre.addView(v, lp);
        return v;
    }

    /** Fila de botones que se reparten el ancho por igual. */
    static LinearLayout fila(Context c, View... vistas) {
        LinearLayout f = horizontal(c);
        for (int i = 0; i < vistas.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) lp.leftMargin = dp(c, 8);
            f.addView(vistas[i], lp);
        }
        return f;
    }

    static void alCambiar(EditText e, Runnable r) {
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                r.run();
            }
        });
    }

    static String txt(EditText e) {
        return e.getText().toString().trim();
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show();
    }
}
