package com.forexpilot.aipro;

import java.util.List;

public class MarketDataManager {

    public interface MarketDataCallback {
        void onCandlesReceived(List<Candle> candles);
        void onPriceReceived(double price);
        void onError(String message);
    }

    private final MarketDataProvider provider;

    public MarketDataManager(MarketDataProvider provider) {
        this.provider = provider;
    }

    public void requestCandles(
            String symbol,
            String timeframe,
            MarketDataCallback callback
    ) {
        if (provider == null) {
            callback.onError("LIVE DATA PROVIDER NOT CONFIGURED");
            return;
        }

        if (symbol == null || symbol.trim().isEmpty()) {
            callback.onError("INVALID MARKET SYMBOL");
            return;
        }

        if (timeframe == null || timeframe.trim().isEmpty()) {
            callback.onError("INVALID TIMEFRAME");
            return;
        }

        provider.requestCandles(
                symbol,
                timeframe,
                callback
        );
    }

    public void stop() {
        if (provider != null) {
            provider.stop();
        }
    }
}