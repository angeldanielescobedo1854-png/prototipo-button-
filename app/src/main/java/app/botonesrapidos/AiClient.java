package app.botonesrapidos;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Manda una instrucción a una IA usando su API con la clave del usuario
 * y devuelve la respuesta en texto. Solo se usa para cambiar el estilo de los widgets.
 */
final class AiClient {
    static final int CLAUDE = 0, OPENAI = 1, GEMINI = 2;

    static final String[] NAMES = {"Claude", "ChatGPT (OpenAI)", "Gemini"};

    /** Modelos por defecto (se pueden cambiar en la app: cambian seguido). */
    static final String[] DEFAULT_MODELS = {
            "claude-haiku-5-5", "gpt-5.6-luna", "gemini-3.5-flash-lite"
    };

    private AiClient() {}

    static String ask(int provider, String key, String model, String prompt) throws Exception {
        String url;
        String body;
        HttpURLConnection c;
        switch (provider) {
            case OPENAI:
                url = "https://api.openai.com/v1/chat/completions";
                body = new JSONObject()
                        .put("model", model)
                        .put("messages", new JSONArray().put(new JSONObject()
                                .put("role", "user").put("content", prompt)))
                        .toString();
                c = open(url);
                c.setRequestProperty("Authorization", "Bearer " + key);
                break;
            case GEMINI:
                url = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + Uri.encode(model) + ":generateContent";
                body = new JSONObject()
                        .put("contents", new JSONArray().put(new JSONObject()
                                .put("parts", new JSONArray().put(new JSONObject()
                                        .put("text", prompt)))))
                        .toString();
                c = open(url);
                c.setRequestProperty("x-goog-api-key", key);
                break;
            default:
                url = "https://api.anthropic.com/v1/messages";
                body = new JSONObject()
                        .put("model", model)
                        .put("max_tokens", 600)
                        .put("messages", new JSONArray().put(new JSONObject()
                                .put("role", "user").put("content", prompt)))
                        .toString();
                c = open(url);
                c.setRequestProperty("x-api-key", key);
                c.setRequestProperty("anthropic-version", "2023-06-01");
                break;
        }

        try {
            OutputStream os = c.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();

            int code = c.getResponseCode();
            InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
            String resp = readAll(in);
            if (code >= 400) {
                throw new IOException("Error " + code + ": " + errorMessage(resp));
            }
            String text = parse(provider, resp).trim();
            if (text.isEmpty()) throw new IOException("La IA respondió vacío");
            return text;
        } finally {
            c.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("Content-Type", "application/json");
        return c;
    }

    private static String parse(int provider, String resp) throws Exception {
        JSONObject root = new JSONObject(resp);
        StringBuilder sb = new StringBuilder();
        switch (provider) {
            case OPENAI: {
                JSONObject msg = root.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message");
                sb.append(msg.optString("content", ""));
                break;
            }
            case GEMINI: {
                JSONArray parts = root.getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts");
                for (int i = 0; i < parts.length(); i++) {
                    sb.append(parts.getJSONObject(i).optString("text", ""));
                }
                break;
            }
            default: {
                JSONArray blocks = root.getJSONArray("content");
                for (int i = 0; i < blocks.length(); i++) {
                    JSONObject b = blocks.getJSONObject(i);
                    if ("text".equals(b.optString("type"))) sb.append(b.optString("text", ""));
                }
                break;
            }
        }
        return sb.toString();
    }

    private static String errorMessage(String resp) {
        try {
            JSONObject err = new JSONObject(resp).optJSONObject("error");
            if (err != null) {
                String m = err.optString("message", "");
                if (!m.isEmpty()) return m;
            }
        } catch (Exception ignored) {
        }
        return resp.length() > 200 ? resp.substring(0, 200) : resp;
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
        r.close();
        return sb.toString();
    }
}
