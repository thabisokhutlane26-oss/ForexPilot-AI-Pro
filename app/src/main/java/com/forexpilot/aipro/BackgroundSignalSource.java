package com.forexpilot.aipro;

/**
 * Adapts the existing SignalRepository to BackgroundScanner.
 *
 * SignalRepository currently owns its callback, so this adapter
 * cannot directly receive the result from requestSignal().
 *
 * Background scanning will be connected through MainActivity.
 */
public class BackgroundSignalSource
        implements BackgroundScanner.ScannerSource {

    private final SignalRepository signalRepository;

    public BackgroundSignalSource(
            SignalRepository signalRepository
    ) {
        this.signalRepository = signalRepository;
    }

    @Override
    public void requestSignal(
            String market,
            String timeframe,
            BackgroundScanner.SignalCallback callback
    ) {
        if (signalRepository == null) {
            if (callback != null) {
                callback.onError(
                        "SIGNAL REPOSITORY NOT CONFIGURED"
                );
            }
            return;
        }

        if (callback == null) {
            return;
        }

        /*
         * SignalRepository currently exposes:
         *
         * requestSignal(String symbol, String timeframe)
         *
         * and owns its callback internally.
         *
         * Therefore we do not call it here with a third callback
         * argument. BackgroundScanner integration will be wired
         * through a dedicated source in MainActivity.
         */
        callback.onError(
                "BACKGROUND SIGNAL SOURCE NOT YET CONNECTED"
        );
    }
}