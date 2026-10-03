package com.forexpilot.aipro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OandaDataProvider implements MarketDataProvider {

    private static final String BASE_URL =
            "https://api-fxpractice.oanda.com";

    private volatile boolean stopped = false;

    private final String accessToken;

    public OandaDataProvider(String accessToken) {
        this.accessToken = accessToken;
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
                if (accessToken == null
                        || accessToken.trim().isEmpty()) {

                    callback.onError(
                            "OANDA ACCESS TOKEN NOT CONFIGURED"
                    );
                    return;
                }

                String instrument = convertSymbol(symbol);
                String granularity = convertTimeframe(timeframe);

                if (instrument == null
                        || granularity == null) {

                    callback.onError(
                            "UNSUPPORTED SYMBOL OR TIMEFRAME"
                    );
                    return;
                }

                String requestUrl =
                        BASE_URL
                                + "/v3/instruments/"
                                + instrument
                                + "/candles"
                                + "?count=200"
                                + "&granularity="
                                + granularity
                                + "&price=M";

                URL url = new URL(requestUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + accessToken
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {

                    callback.onError(
                            "OANDA DATA ERROR: HTTP "
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

                List<Candle> candles =
                        parseCandles(response.toString());

                if (candles.isEmpty()) {

                    callback.onError(
                            "OANDA RETURNED NO COMPLETE CANDLES"
                    );

                    return;
                }

                Candle latest =
                        candles.get(candles.size() - 1);

                if (!isValidCandle(latest)) {

                    callback.onError(
                            "INVALID LIVE CANDLE DATA"
                    );

                    return;
                }

                callback.onCandlesReceived(candles);

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
                            "LIVE DATA ERROR: "
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
            String response
    ) throws Exception {

        List<Candle> candles =
                new ArrayList<>();

        JSONObject root =
                new JSONObject(response);

        JSONArray array =
                root.optJSONArray("candles");

        if (array == null) {
            return candles;
        }

        for (int i = 0; i < array.length(); i++) {

            JSONObject item =
                    array.getJSONObject(i);

            if (!item.optBoolean(
                    "complete",
                    false
            )) {
                continue;
            }

            JSONObject mid =
                    item.optJSONObject("mid");

            if (mid == null) {
                continue;
            }

            double open =
                    mid.optDouble(
                            "o",
                            Double.NaN
                    );

            double high =
                    mid.optDouble(
                            "h",
                            Double.NaN
                    );

            double low =
                    mid.optDouble(
                            "l",
                            Double.NaN
                    );

            double close =
                    mid.optDouble(
                            "c",
                            Double.NaN
                    );

            if (Double.isNaN(open)
                    || Double.isNaN(high)
                    || Double.isNaN(low)
                    || Double.isNaN(close)) {

                continue;
            }

            long timestamp =
                    parseTimestamp(
                            item.optString("time")
                    );

            double volume =
                    item.optDouble(
                            "volume",
                            0.0
                    );

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

    private long parseTimestamp(
            String timestamp
    ) {
        try {
            return Instant.parse(
                    timestamp
            ).toEpochMilli();

        } catch (Exception e) {
            return 0L;
        }
    }

    private boolean isValidCandle(
            Candle candle
    ) {
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

    private boolean isValidNumber(
            double value
    ) {
        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }

    private String convertSymbol(
            String symbol
    ) {
        if (symbol == null) {
            return null;
        }

        String normalized =
                symbol.trim()
                        .toUpperCase()
                        .replace(
                                "/",
                                "_"
                        );

        switch (normalized) {

            case "EUR_USD":
                return "EUR_USD";

            case "GBP_USD":
                return "GBP_USD";

            case "USD_JPY":
                return "USD_JPY";

            case "NZD_USD":
                return "NZD_USD";

            case "AUD_USD":
                return "AUD_USD";

            case "USD_CAD":
                return "USD_CAD";

            case "USD_CHF":
                return "USD_CHF";

            case "EUR_GBP":
                return "EUR_GBP";

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
                return "M5";

            case "15M":
                return "M15";

            case "30M":
                return "M30";

            case "1H":
                return "H1";

            case "4H":
                return "H4";

            case "1D":
                return "D";

            default:
                return null;
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }
}