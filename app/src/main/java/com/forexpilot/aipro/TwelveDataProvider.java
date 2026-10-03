package com.forexpilot.aipro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class TwelveDataProvider implements MarketDataProvider {

    private static final String BASE_URL =
            "https://api.twelvedata.com/time_series";

    private volatile boolean stopped = false;

    private final String apiKey;

    public TwelveDataProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void requestCandles(
            String symbol,
            String timeframe,
            MarketDataManager.MarketDataCallback callback
    ) {
        stopped = false;

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {
                if (apiKey == null
                        || apiKey.trim().isEmpty()) {

                    callback.onError(
                            "TWELVE DATA API KEY NOT CONFIGURED"
                    );
                    return;
                }

                String normalizedSymbol =
                        normalizeSymbol(symbol);

                String interval =
                        convertTimeframe(timeframe);

                if (normalizedSymbol == null
                        || interval == null) {

                    callback.onError(
                            "UNSUPPORTED SYMBOL OR TIMEFRAME"
                    );
                    return;
                }

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
                                + "?symbol="
                                + encodedSymbol
                                + "&interval="
                                + interval
                                + "&outputsize=200"
                                + "&order=asc"
                                + "&apikey="
                                + encodedApiKey;

                URL url =
                        new URL(requestUrl);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode
                        != HttpURLConnection.HTTP_OK) {

                    callback.onError(
                            "TWELVE DATA HTTP ERROR: "
                                    + responseCode
                    );
                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (stopped) {
                    return;
                }

                String responseText =
                        response.toString();

                JSONObject root =
                        new JSONObject(responseText);

                if ("error".equalsIgnoreCase(
                        root.optString("status")
                )) {

                    callback.onError(
                            "TWELVE DATA ERROR: "
                                    + root.optString(
                                    "message",
                                    "UNKNOWN API ERROR"
                            )
                    );
                    return;
                }

                List<Candle> candles =
                        parseCandles(root);

                if (candles.isEmpty()) {

                    callback.onError(
                            "TWELVE DATA RETURNED NO COMPLETE CANDLES"
                    );
                    return;
                }

                Candle latest =
                        candles.get(
                                candles.size() - 1
                        );

                if (!isValidCandle(latest)) {

                    callback.onError(
                            "INVALID LIVE CANDLE DATA"
                    );
                    return;
                }

                callback.onCandlesReceived(
                        candles
                );

                callback.onPriceReceived(
                        latest.getClose()
                );

            } catch (Exception e) {

                if (!stopped) {

                    String message =
                            e.getMessage();

                    if (message == null
                            || message.trim().isEmpty()) {

                        message =
                                "UNKNOWN NETWORK ERROR";
                    }

                    callback.onError(
                            "TWELVE DATA CONNECTION ERROR: "
                                    + message
                    );
                }

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private List<Candle> parseCandles(
            JSONObject root
    ) {

        List<Candle> candles =
                new ArrayList<>();

        JSONArray values =
                root.optJSONArray("values");

        if (values == null) {
            return candles;
        }

        for (int i = 0;
                i < values.length();
                i++) {

            JSONObject item =
                    values.optJSONObject(i);

            if (item == null) {
                continue;
            }

            double open =
                    parseDouble(
                            item.optString("open")
                    );

            double high =
                    parseDouble(
                            item.optString("high")
                    );

            double low =
                    parseDouble(
                            item.optString("low")
                    );

            double close =
                    parseDouble(
                            item.optString("close")
                    );

            if (Double.isNaN(open)
                    || Double.isNaN(high)
                    || Double.isNaN(low)
                    || Double.isNaN(close)) {

                continue;
            }

            long timestamp =
                    parseTimestamp(
                            item.optString(
                                    "datetime"
                            )
                    );

            if (timestamp <= 0) {
                continue;
            }

            double volume =
                    parseDouble(
                            item.optString(
                                    "volume",
                                    "0"
                            )
                    );

            if (Double.isNaN(volume)) {
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

    private double parseDouble(
            String value
    ) {
        try {

            if (value == null
                    || value.trim().isEmpty()) {

                return Double.NaN;
            }

            return Double.parseDouble(
                    value.trim()
            );

        } catch (Exception e) {

            return Double.NaN;
        }
    }

    private long parseTimestamp(
            String datetime
    ) {

        if (datetime == null
                || datetime.trim().isEmpty()) {

            return 0L;
        }

        try {

            String value =
                    datetime.trim();

            java.time.LocalDateTime localDateTime =
                    java.time.LocalDateTime.parse(
                            value,
                            java.time.format.DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd HH:mm:ss"
                            )
                    );

            return localDateTime
                    .atZone(
                            java.time.ZoneOffset.UTC
                    )
                    .toInstant()
                    .toEpochMilli();

        } catch (Exception e) {

            try {

                java.time.LocalDate date =
                        java.time.LocalDate.parse(
                                datetime.trim(),
                                java.time.format.DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd"
                                )
                        );

                return date
                        .atStartOfDay(
                                java.time.ZoneOffset.UTC
                        )
                        .toInstant()
                        .toEpochMilli();

            } catch (Exception ignored) {

                return 0L;
            }
        }
    }

    private boolean isValidCandle(
            Candle candle
    ) {

        return candle != null
                && candle.getTimestamp() > 0
                && isValidNumber(
                        candle.getOpen()
                )
                && isValidNumber(
                        candle.getHigh()
                )
                && isValidNumber(
                        candle.getLow()
                )
                && isValidNumber(
                        candle.getClose()
                )
                && candle.getOpen() > 0
                && candle.getHigh() > 0
                && candle.getLow() > 0
                && candle.getClose() > 0;
    }

    private boolean isValidNumber(
            double value
    ) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }

    private String normalizeSymbol(
            String symbol
    ) {

        if (symbol == null) {
            return null;
        }

        String normalized =
                symbol.trim()
                        .toUpperCase();

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

    private String convertTimeframe(
            String timeframe
    ) {

        if (timeframe == null) {
            return null;
        }

        String normalized =
                timeframe.trim()
                        .toUpperCase();

        switch (normalized) {

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