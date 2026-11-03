package cl.prueba.alerta;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * App de prueba: al presionar el botón reproduce un sonido de alerta tipo sirena
 * generado en tiempo real (no necesita archivos de audio).
 */
public class MainActivity extends Activity {

    private static final int SAMPLE_RATE = 44100;
    private static final double DURATION_SECONDS = 3.0;

    private AudioTrack track;
    private short[] sirenSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        sirenSamples = buildSiren();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("Presiona el botón para sonar la alerta");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 64);
        root.addView(title);

        Button button = new Button(this);
        button.setText("ALERTA");
        button.setTextSize(32);
        button.setTextColor(Color.WHITE);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.rgb(211, 47, 47));
        button.setBackground(circle);
        int size = (int) (240 * getResources().getDisplayMetrics().density);
        root.addView(button, new LinearLayout.LayoutParams(size, size));

        button.setOnClickListener(v -> playAlert());

        setContentView(root);
    }

    /** Genera una sirena que sube y baja de tono entre 600 Hz y 1400 Hz. */
    private short[] buildSiren() {
        int total = (int) (SAMPLE_RATE * DURATION_SECONDS);
        short[] samples = new short[total];
        double phase = 0;
        double sweepHz = 2.0; // ciclos de subida/bajada por segundo
        for (int i = 0; i < total; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 1000 + 400 * Math.sin(2 * Math.PI * sweepHz * t);
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            // Fundido corto al inicio y al final para evitar "clicks"
            double env = Math.min(1.0, Math.min(t, DURATION_SECONDS - t) / 0.02);
            samples[i] = (short) (Math.sin(phase) * env * Short.MAX_VALUE * 0.9);
        }
        return samples;
    }

    private void playAlert() {
        releaseTrack();
        int bytes = sirenSamples.length * 2;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(bytes)
                    .build();
        } else {
            track = new AudioTrack(AudioManager.STREAM_ALARM, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bytes, AudioTrack.MODE_STATIC);
        }
        track.write(sirenSamples, 0, sirenSamples.length);
        track.play();
    }

    private void releaseTrack() {
        if (track != null) {
            try {
                track.stop();
            } catch (IllegalStateException ignored) {
            }
            track.release();
            track = null;
        }
    }

    @Override
    protected void onDestroy() {
        releaseTrack();
        super.onDestroy();
    }
}
