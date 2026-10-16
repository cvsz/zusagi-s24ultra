package dev.zeaz.ultimate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

/** Strict bounded parser for ZEAZ authenticated SSE protocol version 1. */
public final class AssistantStreamParser {
  private static final int MAX_LINE_BYTES = 16384;
  private static final int MAX_STREAM_BYTES = 524288;
  private static final long MAX_STREAM_DURATION_NANOS = 120_000_000_000L;

  interface NanoClock { long now(); }

  public interface DeltaConsumer {
    void accept(String text);
  }

  private AssistantStreamParser() {}

  public static int parse(InputStream input, DeltaConsumer consumer) throws IOException {
    return parse(input, consumer, System::nanoTime, MAX_STREAM_DURATION_NANOS);
  }

  static int parse(InputStream input, DeltaConsumer consumer, NanoClock clock,
                   long maxDurationNanos) throws IOException {
    if (input == null || consumer == null) throw new IllegalArgumentException("Input and consumer required");
    if (clock == null || maxDurationNanos <= 0) throw new IllegalArgumentException("Valid stream duration required");
    BoundedLines lines = new BoundedLines(input, clock, maxDurationNanos);
    String event = null;
    String data = null;
    int expectedSequence = 0;
    boolean done = false;
    String line;
    while ((line = lines.readLine()) != null) {
      if (line.isEmpty()) {
        if (event != null || data != null) {
          if (event == null || data == null || done) throw invalidStream();
          JSONObject payload;
          try { payload = new JSONObject(data); }
          catch (JSONException ex) { throw invalidStream(); }
          Object rawSequence = payload.opt("sequence");
          if (!(rawSequence instanceof Number)
              || ((Number) rawSequence).intValue() != expectedSequence
              || ((Number) rawSequence).doubleValue() != expectedSequence) throw invalidStream();
          if ("delta".equals(event)) {
            Object rawText = payload.opt("text");
            if (!(rawText instanceof String) || ((String) rawText).isEmpty()) throw invalidStream();
            consumer.accept((String) rawText);
            expectedSequence++;
          } else if ("done".equals(event)) {
            if (payload.length() != 1) throw invalidStream();
            done = true;
          } else if ("error".equals(event)) {
            throw new IOException("Gateway stream reported an error");
          } else {
            throw invalidStream();
          }
          event = null;
          data = null;
        }
        continue;
      }
      if (done) throw invalidStream();
      if (line.startsWith(":")) continue;
      if (line.startsWith("event: ") && event == null) event = line.substring(7);
      else if (line.startsWith("data: ") && data == null) data = line.substring(6);
      else throw invalidStream();
    }
    if (!done || event != null || data != null) throw invalidStream();
    return expectedSequence;
  }

  private static IOException invalidStream() {
    return new IOException("Invalid gateway stream");
  }

  private static final class BoundedLines {
    private final InputStream input;
    private final NanoClock clock;
    private final long startedAt;
    private final long maxDurationNanos;
    private int total;

    BoundedLines(InputStream input, NanoClock clock, long maxDurationNanos) {
      this.input = input;
      this.clock = clock;
      this.startedAt = clock.now();
      this.maxDurationNanos = maxDurationNanos;
    }

    String readLine() throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      boolean sawByte = false;
      while (true) {
        checkDuration();
        int value = input.read();
        checkDuration();
        if (value < 0) return sawByte ? decode(bytes) : null;
        sawByte = true;
        if (++total > MAX_STREAM_BYTES) throw new IOException("Gateway stream too large");
        if (value == '\n') return decode(bytes);
        if (bytes.size() >= MAX_LINE_BYTES) throw new IOException("Gateway stream line too large");
        bytes.write(value);
      }
    }

    private void checkDuration() throws IOException {
      if (clock.now() - startedAt >= maxDurationNanos) {
        throw new IOException("Gateway stream duration exceeded");
      }
    }

    private static String decode(ByteArrayOutputStream bytes) {
      byte[] data = bytes.toByteArray();
      int length = data.length;
      if (length > 0 && data[length - 1] == '\r') length--;
      return new String(data, 0, length, StandardCharsets.UTF_8);
    }
  }
}
