package cl.prueba.taller;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;

/** Lee el texto de la foto del tablero con ML Kit (en el teléfono, sin internet). */
final class OdometroOcr {

    interface Callback {
        void listo(List<Long> candidatos);

        void error(Exception e);
    }

    private OdometroOcr() {
    }

    static void leer(Context c, Uri foto, long ultimoKm, Callback cb) {
        InputImage img;
        try {
            img = InputImage.fromFilePath(c, foto);
        } catch (Exception e) {
            cb.error(e);
            return;
        }
        TextRecognizer r = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        r.process(img)
                .addOnSuccessListener(texto -> {
                    List<LectorKm.Linea> lineas = new ArrayList<>();
                    for (Text.TextBlock b : texto.getTextBlocks()) {
                        for (Text.Line l : b.getLines()) {
                            Rect box = l.getBoundingBox();
                            lineas.add(new LectorKm.Linea(l.getText(), box == null ? 0 : box.height()));
                        }
                    }
                    r.close();
                    cb.listo(LectorKm.candidatos(lineas, ultimoKm));
                })
                .addOnFailureListener(e -> {
                    r.close();
                    cb.error(e);
                });
    }
}
