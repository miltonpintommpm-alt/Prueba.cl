package cl.prueba.taller;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

/** Revisión diaria (10:00) que avisa al taller cuántos clientes hay que contactar. */
final class Avisos {
    static final String ACCION = "cl.prueba.taller.REVISAR_RECORDATORIOS";
    private static final String CANAL = "recordatorios";

    private Avisos() {
    }

    static void programar(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AvisoReceiver.class).setAction(ACCION);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    /** Cantidad de vehículos con cambio próximo o vencido que aún no han sido avisados. */
    static int porAvisar(Context c) {
        Prefs p = new Prefs(c);
        long hasta = System.currentTimeMillis() + p.diasAnticipacion() * Fmt.DIA;
        int n = 0;
        for (Servicio s : Db.get(c).pendientes(hasta)) if (s.avisado == 0) n++;
        return n;
    }

    @SuppressWarnings("deprecation")
    static void notificar(Context c) {
        int n = porAvisar(c);
        if (n == 0) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CANAL, "Recordatorios de cambio de aceite",
                    NotificationManager.IMPORTANCE_DEFAULT));
            b = new Notification.Builder(c, CANAL);
        } else {
            b = new Notification.Builder(c);
        }
        Intent abrir = new Intent(c, RecordatoriosActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Recordatorios de cambio de aceite")
                .setContentText(n + (n == 1 ? " cliente" : " clientes") + " para avisar por WhatsApp")
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            nm.notify(1, b.build());
        } catch (SecurityException ignored) {
            // sin permiso de notificaciones
        }
    }
}
