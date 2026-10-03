package com.forexpilot.aipro;

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
        this.marketDataManager =
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
                            java.util.List<Candle> candles
                    ) {
                        if (candles == null
                                || candles.size() < 60) {

                            callback.onError(
                                    "NOT ENOUGH LIVE CANDLE DATA"
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
                        // Price updates can be used by the UI later.
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