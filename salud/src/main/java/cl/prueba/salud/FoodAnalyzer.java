package cl.prueba.salud;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.beta.messages.BetaBase64ImageSource;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaJsonOutputFormat;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Envía la foto de un plato a Claude y obtiene una estimación de calorías y
 * macronutrientes en JSON estructurado.
 */
public final class FoodAnalyzer {

    public static final String MODEL = "claude-opus-5-5";

    private static final String PROMPT =
            "Eres un nutricionista experto. Analiza la foto de comida.\n"
            + "1. Identifica cada alimento o preparación visible (usa nombres comunes en español de Chile cuando corresponda, "
            + "por ejemplo: marraqueta, palta, porotos, pastel de choclo, cazuela, completo).\n"
            + "2. Estima el tamaño de la porción usando referencias visuales (plato, cubiertos, manos) y exprésalo en gramos.\n"
            + "3. Calcula calorías y macronutrientes de cada alimento según tablas nutricionales estándar, "
            + "considerando el método de cocción y aceites o salsas visibles.\n"
            + "4. Si la imagen no muestra comida, responde es_comida=false con la lista vacía.\n"
            + "Sé realista: no subestimes frituras, aderezos ni bebidas azucaradas. "
            + "En 'comentario' da una observación breve y útil para una alimentación saludable (máximo 2 frases).";

    public static final class Item {
        public String name;
        public String portion;
        public double grams;
        public double kcal, protein, carbs, fat;
    }

    public static final class Result {
        public boolean isFood;
        public List<Item> items = new ArrayList<>();
        public double totalKcal;
        public String confidence;
        public String comment;
    }

    /** Error con un mensaje listo para mostrar al usuario. */
    public static final class AnalysisException extends Exception {
        public AnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private AnthropicClient client;
    private String clientKey;

    private synchronized AnthropicClient client(String apiKey) {
        if (client == null || !apiKey.equals(clientKey)) {
            client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            clientKey = apiKey;
        }
        return client;
    }

    /** Analiza una imagen JPEG. Llamar fuera del hilo principal. */
    public Result analyze(String apiKey, byte[] jpeg) throws AnalysisException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AnalysisException("Falta la clave de API de Claude. Ingrésala en Ajustes.", null);
        }
        String b64 = Base64.getEncoder().encodeToString(jpeg);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                // Si el modelo declina la solicitud, la API reintenta con un modelo alternativo.
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .outputConfig(BetaOutputConfig.builder()
                        .effort(BetaOutputConfig.Effort.MEDIUM)
                        .format(BetaJsonOutputFormat.builder().schema(schema()).build())
                        .build())
                .addUserMessageOfContentBlockParams(List.of(
                        BetaContentBlockParam.ofImage(BetaBase64ImageSource.builder()
                                .mediaType(BetaBase64ImageSource.MediaType.IMAGE_JPEG)
                                .data(b64)
                                .build()),
                        BetaContentBlockParam.ofText(PROMPT)))
                .build();

        BetaMessage response;
        try {
            response = client(apiKey.trim()).beta().messages().create(params);
        } catch (UnauthorizedException | PermissionDeniedException e) {
            throw new AnalysisException("La clave de API no es válida o no tiene permisos. Revísala en Ajustes.", e);
        } catch (RateLimitException e) {
            throw new AnalysisException("Demasiadas solicitudes seguidas. Espera un momento y vuelve a intentar.", e);
        } catch (AnthropicIoException e) {
            throw new AnalysisException("No hay conexión a internet o el servidor no respondió.", e);
        } catch (AnthropicServiceException e) {
            throw new AnalysisException("El servicio respondió con un error (" + e.statusCode() + "). Intenta de nuevo.", e);
        } catch (AnthropicException e) {
            throw new AnalysisException("No se pudo completar el análisis. Intenta de nuevo.", e);
        }

        BetaStopReason stop = response.stopReason().orElse(null);
        if (BetaStopReason.REFUSAL.equals(stop)) {
            throw new AnalysisException("No fue posible analizar esta imagen. Prueba con otra foto.", null);
        }
        if (BetaStopReason.MAX_TOKENS.equals(stop)) {
            throw new AnalysisException("La respuesta quedó incompleta. Intenta de nuevo.", null);
        }

        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        try {
            return parse(text.toString());
        } catch (JSONException e) {
            throw new AnalysisException("No se pudo interpretar la respuesta. Intenta de nuevo.", e);
        }
    }

    static Result parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        Result r = new Result();
        r.isFood = o.getBoolean("es_comida");
        r.confidence = o.getString("confianza");
        r.comment = o.getString("comentario");
        JSONArray arr = o.getJSONArray("alimentos");
        double sum = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject a = arr.getJSONObject(i);
            Item it = new Item();
            it.name = a.getString("nombre");
            it.portion = a.getString("porcion");
            it.grams = a.getDouble("gramos");
            it.kcal = a.getDouble("calorias");
            it.protein = a.getDouble("proteinas_g");
            it.carbs = a.getDouble("carbohidratos_g");
            it.fat = a.getDouble("grasas_g");
            sum += it.kcal;
            r.items.add(it);
        }
        r.totalKcal = sum;
        return r;
    }

    private static BetaJsonOutputFormat.Schema schema() {
        Map<String, Object> item = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("nombre", "porcion", "gramos", "calorias", "proteinas_g", "carbohidratos_g", "grasas_g"),
                "properties", Map.of(
                        "nombre", Map.of("type", "string", "description", "Nombre del alimento en español"),
                        "porcion", Map.of("type", "string", "description", "Porción en palabras, ej: '1 taza', '2 rebanadas'"),
                        "gramos", Map.of("type", "number"),
                        "calorias", Map.of("type", "number", "description", "kcal de la porción visible"),
                        "proteinas_g", Map.of("type", "number"),
                        "carbohidratos_g", Map.of("type", "number"),
                        "grasas_g", Map.of("type", "number")));
        return BetaJsonOutputFormat.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .putAdditionalProperty("required", JsonValue.from(List.of("es_comida", "alimentos", "confianza", "comentario")))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "es_comida", Map.of("type", "boolean"),
                        "alimentos", Map.of("type", "array", "items", item),
                        "confianza", Map.of("type", "string", "enum", List.of("alta", "media", "baja")),
                        "comentario", Map.of("type", "string"))))
                .build();
    }
}
