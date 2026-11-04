package cl.prueba.taller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Recibe la alarma diaria y el reinicio del teléfono (para volver a programarla). */
public class AvisoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Avisos.ACCION.equals(i.getAction())) {
            Avisos.notificar(c);
        } else {
            Avisos.programar(c);
        }
    }
}
