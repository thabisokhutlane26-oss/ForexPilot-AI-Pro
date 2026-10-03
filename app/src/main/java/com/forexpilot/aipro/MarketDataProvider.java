package com.forexpilot.aipro;

public interface MarketDataProvider {

    void requestCandles(
            String symbol,
            String timeframe,
            MarketDataManager.MarketDataCallback callback
    );

    void stop();
}
