
package com.forexpilot.aipro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TwelveDataProvider implements MarketDataProvider {

    private static final String BASE_URL =
            "https://api.twelvedata.com/time_series";

    private static final long CACHE_DURATION_MS = 30_000L;

    private static final int CONNECTION_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;

    private volatile boolean stopped = false;

    private final String apiKey;

    private final ConcurrentHashMap<String, CacheEntry> cache =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> lastRequestTimes =
            new ConcurrentHashMap<>();

    private static class CacheEntry {
        final List<Candle> candles;
        final long savedAt;

        CacheEntry(List<Candle> candles, long savedAt) {
            this.candles = new ArrayList<>(candles);
            this.savedAt = savedAt;
        }
    }

    public TwelveDataProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void requestCandles(
            String symbol,
            String timeframe,
            MarketDataManager.MarketDataCallback callback
    ) {
        if (callback == null) {
            return;
        }

        stopped = false;

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError(
                            "API KEY NOT CONFIGURED. CHECK TWELVE DATA SETTINGS."
                    );
                    return;
                }

                String normalizedSymbol = normalizeSymbol(symbol);
                String interval = convertTimeframe(timeframe);

                if (normalizedSymbol == null || interval == null) {
                    callback.onError("UNSUPPORTED SYMBOL OR TIMEFRAME");
                    return;
                }

                String cacheKey = normalizedSymbol + "|" + interval;
                long now = System.currentTimeMillis();

                CacheEntry cached = cache.get(cacheKey);

                if (cached != null
                        && now - cached.savedAt < CACHE_DURATION_MS
                        && !cached.candles.isEmpty()) {

                    if (!stopped) {
                        deliverCandles(cached.candles, callback);
                    }
                    return;
                }

                Long lastRequest = lastRequestTimes.get(cacheKey);

                if (lastRequest != null
                        && now - lastRequest < 3000L) {

                    callback.onError(
                            "PLEASE WAIT A FEW SECONDS BEFORE SCANNING AGAIN."
                    );
                    return;
                }

                lastRequestTimes.put(cacheKey, now);

                String encodedSymbol =
                        URLEncoder.encode(
                                normalizedSymbol,
                                "UTF-8"
                        );

                String encodedApiKey =
                        URLEncoder.encode(
                                apiKey.trim(),
                                "UTF-8"
                        );

                String requestUrl =
                        BASE_URL
                                + "?symbol=" + encodedSymbol
                                + "&interval=" + interval
                                + "&outputsize=100"
                                + "&order=asc"
                                + "&apikey=" + encodedApiKey;

                URL url = new URL(requestUrl);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode = connection.getResponseCode();

                InputStream stream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                String responseText = readResponse(stream);

                if (stopped) {
                    return;
                }

                JSONObject root;

                try {
                    root = new JSONObject(responseText);
                } catch (Exception e) {
                    callback.onError(
                            "INVALID RESPONSE FROM TWELVE DATA (HTTP "
                                    + responseCode + "). TRY AGAIN LATER."
                    );
                    return;
                }

                String status = root.optString("status", "");
                String apiMessage = root.optString(
                        "message",
                        root.optString("code", "")
                );

                if (responseCode == 429) {
                    callback.onError(
                            "TWELVE DATA RATE LIMIT REACHED. WAIT BEFORE SCANNING AGAIN."
                    );
                    return;
                }

                if (responseCode == 401) {
                    callback.onError(
                            "TWELVE DATA API KEY IS INVALID. CHECK YOUR API KEY."
                    );
                    return;
                }

                if (responseCode == 403) {
                    callback.onError(
                            "TWELVE DATA ACCESS DENIED. CHECK YOUR API PLAN."
                    );
                    return;
                }

                if (responseCode < 200 || responseCode >= 300) {
                    callback.onError(
                            "TWELVE DATA HTTP ERROR "
                                    + responseCode
                                    + (apiMessage.isEmpty()
                                    ? ""
                                    : ": " + apiMessage)
                    );
                    return;
                }

                if ("error".equalsIgnoreCase(status)) {
                    callback.onError(
                            "TWELVE DATA ERROR: "
                                    + (apiMessage.isEmpty()
                                    ? "REQUEST FAILED"
                                    : apiMessage)
                    );
                    return;
                }

                List<Candle> candles = parseCandles(root);

                if (candles.isEmpty()) {
                    callback.onError(
                            "TWELVE DATA RETURNED NO VALID CANDLES."
                    );
                    return;
                }

                Candle latest = candles.get(candles.size() - 1);

                if (!isValidCandle(latest)) {
                    callback.onError("INVALID LIVE CANDLE DATA.");
                    return;
                }

                cache.put(
                        cacheKey,
                        new CacheEntry(
                                candles,
                                System.currentTimeMillis()
                        )
                );

                if (!stopped) {
                    deliverCandles(candles, callback);
                }

            } catch (Exception e) {
                if (!stopped) {
                    callback.onError(
                            "CONNECTION FAILED. CHECK INTERNET AND TRY AGAIN."
                    );
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String readResponse(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        stream,
                        StandardCharsets.UTF_8
                ))) {

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    private void deliverCandles(
            List<Candle> candles,
            MarketDataManager.MarketDataCallback callback
    ) {
        if (candles == null || candles.isEmpty() || stopped) {
            return;
        }

        List<Candle> safeCopy = new ArrayList<>(candles);
        Candle latest = safeCopy.get(safeCopy.size() - 1);

        if (!isValidCandle(latest)) {
            callback.onError("INVALID CANDLE DATA.");
            return;
        }

        callback.onCandlesReceived(safeCopy);

        if (!stopped) {
            callback.onPriceReceived(latest.getClose());
        }
    }

    private List<Candle> parseCandles(JSONObject root) {
        List<Candle> candles = new ArrayList<>();

        JSONArray values = root.optJSONArray("values");

        if (values == null) {
            return candles;
        }

        for (int i = 0; i < values.length(); i++) {
            JSONObject item = values.optJSONObject(i);

            if (item == null) {
                continue;
            }

            double open = parseDouble(item.optString("open"));
            double high = parseDouble(item.optString("high"));
            double low = parseDouble(item.optString("low"));
            double close = parseDouble(item.optString("close"));

            if (!isValidNumber(open)
                    || !isValidNumber(high)
                    || !isValidNumber(low)
                    || !isValidNumber(close)
                    || open <= 0
                    || high <= 0
                    || low <= 0
                    || close <= 0) {
                continue;
            }

            long timestamp = parseTimestamp(
                    item.optString("datetime")
            );

            if (timestamp <= 0) {
                continue;
            }

            double volume = parseDouble(
                    item.optString("volume", "0")
            );

            if (!isValidNumber(volume)) {
                volume = 0.0;
            }

            candles.add(
                    new Candle(
                            timestamp,
                            open,
                            high,
                            low,
                            close,
                            volume
                    )
            );
        }

        return candles;
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return Double.NaN;
            }

            return Double.parseDouble(value.trim());

        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private long parseTimestamp(String datetime) {
        if (datetime == null || datetime.trim().isEmpty()) {
            return 0L;
        }

        String value = datetime.trim();

        try {
            java.time.LocalDateTime dateTime =
                    java.time.LocalDateTime.parse(
                            value,
                            java.time.format.DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd HH:mm:ss"
                            )
                    );

            return dateTime.atZone(
                    java.time.ZoneOffset.UTC
            ).toInstant().toEpochMilli();

        } catch (Exception ignored) {
            try {
                java.time.LocalDate date =
                        java.time.LocalDate.parse(
                                value,
                                java.time.format.DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd"
                                )
                        );

                return date.atStartOfDay(
                        java.time.ZoneOffset.UTC
                ).toInstant().toEpochMilli();

            } catch (Exception e) {
                return 0L;
            }
        }
    }

    private boolean isValidCandle(Candle candle) {
        return candle != null
                && candle.getTimestamp() > 0
                && isValidNumber(candle.getOpen())
                && isValidNumber(candle.getHigh())
                && isValidNumber(candle.getLow())
                && isValidNumber(candle.getClose())
                && candle.getOpen() > 0
                && candle.getHigh() > 0
                && candle.getLow() > 0
                && candle.getClose() > 0;
    }

    private boolean isValidNumber(double value) {
        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }

        String normalized = symbol.trim().toUpperCase();

        switch (normalized) {
            case "EUR/USD":
            case "EUR_USD":
                return "EUR/USD";

            case "GBP/USD":
            case "GBP_USD":
                return "GBP/USD";

            case "USD/JPY":
            case "USD_JPY":
                return "USD/JPY";

            case "NZD/USD":
            case "NZD_USD":
                return "NZD/USD";

            case "AUD/USD":
            case "AUD_USD":
                return "AUD/USD";

            case "USD/CAD":
            case "USD_CAD":
                return "USD/CAD";

            case "USD/CHF":
            case "USD_CHF":
                return "USD/CHF";

            case "EUR/GBP":
            case "EUR_GBP":
                return "EUR/GBP";

            case "XAU/USD":
            case "XAU_USD":
                return "XAU/USD";

            default:
                return null;
        }
    }

    private String convertTimeframe(String timeframe) {
        if (timeframe == null) {
            return null;
        }

        switch (timeframe.trim().toUpperCase()) {
            case "5M":
                return "5min";

            case "15M":
                return "15min";

            case "30M":
                return "30min";

            case "1H":
                return "1h";

            case "4H":
                return "4h";

            case "1D":
                return "1day";

            default:
                return null;
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }
}
