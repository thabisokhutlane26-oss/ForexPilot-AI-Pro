package com.forexpilot.aipro;

import java.util.List;

public class SignalRepository {

    private final MarketDataManager marketDataManager;

    public interface SignalCallback {
        void onSignal(Signal signal);
        void onError(String message);
    }

    public SignalRepository(SignalCallback callback) {

        marketDataManager =
                new MarketDataManager(
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

                                /*
                                 * The symbol and timeframe used for
                                 * this request are supplied through
                                 * requestSignal().
                                 *
                                 * They are temporarily stored in
                                 * requestSymbol/requestTimeframe.
                                 */
                                Signal signal =
                                        SignalEngine.analyze(
                                                requestSymbol,
                                                requestTimeframe,
                                                candles
                                        );

                                callback.onSignal(signal);
                            }

                            @Override
                            public void onPriceReceived(
                                    double price
                            ) {
                                // Live price updates can be handled later.
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

    private String requestSymbol;
    private String requestTimeframe;

    public void requestSignal(
            String symbol,
            String timeframe
    ) {
        if (symbol == null
                || symbol.trim().isEmpty()) {

            return;
        }

        if (timeframe == null
                || timeframe.trim().isEmpty()) {

            return;
        }

        requestSymbol = symbol;
        requestTimeframe = timeframe;

        marketDataManager.requestCandles(
                symbol,
                timeframe
        );
    }

    public void stop() {
        marketDataManager.stop();
    }
}
