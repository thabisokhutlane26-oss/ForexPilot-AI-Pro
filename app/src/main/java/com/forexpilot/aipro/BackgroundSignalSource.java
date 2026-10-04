package com.forexpilot.aipro;

/**
 * Adapts SignalRepository to BackgroundScanner.ScannerSource.
 *
 * Uses the existing real Twelve Data -> SignalEngine pipeline.
 * No fake signals and no second data provider.
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

        signalRepository.requestSignal(
                market,
                timeframe,
                new SignalRepository.SignalCallback() {
                    @Override
                    public void onSignal(Signal signal) {
                        callback.onSignal(signal);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                }
        );
    }
}