package com.forexpilot.aipro;

import java.util.List;

public class MarketDataManager {

    public interface MarketDataCallback {
        void onCandlesReceived(List<Candle> candles);
        void onPriceReceived(double price);
        void onError(String message);
    }

    private MarketDataCallback callback;

    public MarketDataManager(MarketDataCallback callback) {
        this.callback = callback;
    }

    public void requestCandles(String symbol, String timeframe) {
        /*
         * The real market-data provider will be connected here.
         *
         * IMPORTANT:
         * No fake prices or fake candles are generated.
         * If live data is unavailable, the app reports an error
         * instead of creating a fake signal.
         */
        if (callback != null) {
            callback.onError("LIVE DATA PROVIDER NOT CONNECTED");
        }
    }

    public void stop() {
        callback = null;
    }
}
