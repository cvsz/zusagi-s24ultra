package dev.zeaz.ultimate;

import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
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
        byte[] bytes = stream.readNBytes(262145);
        if (bytes.length > 262144) throw new IllegalStateException("Response too large");
        JSONObject payload = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        JSONObject scores = payload.optJSONObject("scores");
        String scoreText = scores == null ? "Unavailable" :
            "Opportunity " + scores.optInt("opportunity") +
            "  •  Risk " + scores.optInt("risk") +
            "  •  Confidence " + scores.optInt("confidence");
        return "ZEAZ INTELLIGENCE (ADVISORY ONLY)\n" +
            symbol.trim() + " / " + chain.trim() + "\n" +
            scoreText + "\n" +
            "Decision: " + payload.optString("action", "UNKNOWN") + "\n" +
            "Rug risk: " + payload.optString("rug_risk", "UNKNOWN") +
            "\nNo trades are initiated from this screen.";
      }
    } finally { connection.disconnect(); }
  }
}
