package com.forexpilot.aipro;

import java.util.List;

public class SignalRepository {

    private final MarketDataManager marketDataManager;
    private final SignalCallback callback;

    public interface SignalCallback {
        void onSignal(Signal signal);
        void onError(String message);
    }

    public SignalRepository(
            MarketDataProvider provider,
            SignalCallback callback
    ) {
        this.callback = callback;

        marketDataManager =
                new MarketDataManager(provider);
    }

    public void requestSignal(
            String symbol,
            String timeframe
    ) {
        marketDataManager.requestCandles(
                symbol,
                timeframe,
                new MarketDataManager.MarketDataCallback() {

                    @Override
                    public void onCandlesReceived(
                            List<Candle> candles
                    ) {
                        if (candles == null
                                || candles.isEmpty()) {

                            callback.onError(
                                    "NO LIVE MARKET DATA"
                            );
                            return;
                        }

                        Signal signal =
                                SignalEngine.analyze(
                                        symbol,
                                        timeframe,
                                        candles
                                );

                        callback.onSignal(signal);
                    }

                    @Override
                    public void onPriceReceived(
                            double price
                    ) {
                        // Live price handling will be added later.
                    }

                    @Override
                    public void onError(
                            String message
                    ) {
                        callback.onError(message);
                    }
                }
        );
    }

    public void stop() {
        marketDataManager.stop();
    }
}