package dev.zeaz.ultimate;

import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Read-only intelligence sidecar adapter.
 * Never accepts orders, wallet keys, or mutation intents.
 */
public final class IntelligenceClient {
  private IntelligenceClient() {}

  public static String analyze(String endpoint, String sessionToken, String symbol,
                               String chain, String address) throws Exception {
    URL url = new URL(endpoint.replaceAll("/+$", "") + "/v1/analyze");
    if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getUserInfo() != null
        || url.getHost().isEmpty() || (url.getPort() != -1 && url.getPort() != 443)
        || !"/v1/analyze".equals(url.getPath()) || url.getQuery() != null
        || url.getRef() != null) {
      throw new IllegalArgumentException("HTTPS endpoint without embedded credentials required");
    }
    if (symbol == null || chain == null || symbol.trim().isEmpty() || chain.trim().isEmpty()) {
      throw new IllegalArgumentException("Symbol and chain are required");
    }
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    try {
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(20000);
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");
      if (sessionToken != null && !sessionToken.isEmpty()) {
        connection.setRequestProperty("Authorization", "Bearer " + sessionToken);
      }
      JSONObject token = new JSONObject().put("symbol", symbol.trim())
          .put("chain", chain.trim());
      if (address != null && !address.trim().isEmpty()) {
        token.put("address", address.trim());
      }
      byte[] body = new JSONObject().put("token", token).toString()
          .getBytes(StandardCharsets.UTF_8);
      try (OutputStream stream = connection.getOutputStream()) { stream.write(body); }
      int status = connection.getResponseCode();
      if (status < 200 || status >= 300) {
        throw new IllegalStateException("Intelligence API HTTP " + status);
      }
      try (InputStream stream = connection.getInputStream()) {
        byte[] bytes = readBounded(stream, 262144);
        return formatResponse(new String(bytes, StandardCharsets.UTF_8), symbol, chain);
      }
    } finally { connection.disconnect(); }
  }

  static byte[] readBounded(InputStream stream, int maxBytes) throws IOException {
    if (maxBytes < 0) throw new IllegalArgumentException("Maximum size must not be negative");
    ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
    byte[] buffer = new byte[8192];
    int total = 0;
    while (true) {
      int count = stream.read(buffer, 0, Math.min(buffer.length, maxBytes - total + 1));
      if (count == -1) return output.toByteArray();
      total += count;
      if (total > maxBytes) throw new IOException("Response too large");
      output.write(buffer, 0, count);
    }
  }

  static String formatResponse(String json, String symbol, String chain) throws Exception {
    JSONObject payload = new JSONObject(json);
    JSONObject scores = payload.optJSONObject("scores");
    if (scores == null) throw new IllegalArgumentException("Missing scores");
    String[] scoreNames = {"opportunity", "risk", "confidence"};
    StringBuilder scoreText = new StringBuilder();
    for (String name : scoreNames) {
      Object value = scores.opt(name);
      if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue())) {
        throw new IllegalArgumentException("Invalid score: " + name);
      }
      if (scoreText.length() > 0) scoreText.append("  •  ");
      scoreText.append(name.substring(0, 1).toUpperCase(java.util.Locale.ROOT))
          .append(name.substring(1)).append(' ').append(value);
    }
    String action = readLabel(payload, "action");
    String rugRisk = readLabel(payload, "rug_risk");
    return "ZEAZ INTELLIGENCE (ADVISORY ONLY)\n" +
        symbol.trim() + " / " + chain.trim() + "\n" +
        scoreText + "\n" +
        "Decision: " + action + "\n" +
        "Rug risk: " + rugRisk +
        "\nNo trades are initiated from this screen.";
  }

  private static String readLabel(JSONObject payload, String key) {
    Object value = payload.opt(key);
    if (!(value instanceof String)) throw new IllegalArgumentException("Invalid field: " + key);
    String label = ((String) value).trim();
    if (label.isEmpty() || label.length() > 64) {
      throw new IllegalArgumentException("Invalid field: " + key);
    }
    for (int index = 0; index < label.length(); index++) {
      if (Character.isISOControl(label.charAt(index))) {
        throw new IllegalArgumentException("Invalid field: " + key);
      }
    }
    return label;
  }
}
