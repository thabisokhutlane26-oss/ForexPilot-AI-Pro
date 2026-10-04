package com.forexpilot.aipro;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BackgroundScanner {

    public interface ScanCallback {

        void onScanStarted();

        void onTimeframeResult(
                String market,
                String timeframe,
                Signal signal
        );

        void onScanCompleted(
                String market,
                List<Signal> signals
        );

        void onScanError(
                String market,
                String timeframe,
                String message
        );
    }

    public interface ScannerSource {

        void requestSignal(
                String market,
                String timeframe,
                SignalCallback callback
        );
    }

    public interface SignalCallback {

        void onSignal(Signal signal);

        void onError(String message);
    }

    private static final long DEFAULT_SCAN_INTERVAL =
            5L * 60L * 1000L;

    private static final List<String> TIMEFRAMES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "5M",
                            "15M",
                            "30M",
                            "1H",
                            "4H",
                            "1D"
                    )
            );

    private final Handler handler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final ScannerSource source;

    private ScanCallback callback;

    private String market;

    private long scanInterval =
            DEFAULT_SCAN_INTERVAL;

    private boolean running = false;

    private boolean scanInProgress = false;

    private int timeframeIndex = 0;

    private final List<Signal> scanResults =
            new ArrayList<>();

    public BackgroundScanner(
            ScannerSource source
    ) {

        this.source = source;
    }

    public void setCallback(
            ScanCallback callback
    ) {

        this.callback = callback;
    }

    public void setMarket(
            String market
    ) {

        if (market == null
                || market.trim().isEmpty()) {

            return;
        }

        this.market =
                market.trim();
    }

    public String getMarket() {

        return market;
    }

    public void setScanInterval(
            long milliseconds
    ) {

        if (milliseconds <= 0) {
            return;
        }

        scanInterval =
                milliseconds;
    }

    public long getScanInterval() {

        return scanInterval;
    }

    public List<String> getTimeframes() {

        return TIMEFRAMES;
    }

    public boolean isRunning() {

        return running;
    }

    public boolean isScanInProgress() {

        return scanInProgress;
    }

    /**
     * Starts periodic scanning.
     */
    public void start() {

        if (running) {
            return;
        }

        if (source == null) {
            return;
        }

        if (market == null
                || market.trim().isEmpty()) {

            return;
        }

        running = true;

        timeframeIndex = 0;

        scanResults.clear();

        scheduleNextScan(
                0
        );
    }

    /**
     * Stops periodic scanning.
     */
    public void stop() {

        running = false;

        scanInProgress = false;

        handler.removeCallbacksAndMessages(
                null
        );

        timeframeIndex = 0;

        scanResults.clear();
    }

    /**
     * Performs one immediate scan cycle.
     */
    public void scanNow() {

        if (source == null) {
            return;
        }

        if (market == null
                || market.trim().isEmpty()) {

            return;
        }

        if (scanInProgress) {
            return;
        }

        beginScan();
    }

    private void scheduleNextScan(
            long delay
    ) {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!running) {
                            return;
                        }

                        beginScan();
                    }
                },
                Math.max(
                        0L,
                        delay
                )
        );
    }

    private void beginScan() {

        if (!running
                || scanInProgress) {

            return;
        }

        if (source == null
                || market == null
                || market.trim().isEmpty()) {

            return;
        }

        scanInProgress = true;

        timeframeIndex = 0;

        scanResults.clear();

        if (callback != null) {

            callback.onScanStarted();
        }

        requestNextTimeframe();
    }

    private void requestNextTimeframe() {

        if (!running
                || !scanInProgress) {

            return;
        }

        if (timeframeIndex
                >= TIMEFRAMES.size()) {

            finishScan();

            return;
        }

        final String currentMarket =
                market;

        final String timeframe =
                TIMEFRAMES.get(
                        timeframeIndex
                );

        source.requestSignal(
                currentMarket,
                timeframe,
                new SignalCallback() {

                    @Override
                    public void onSignal(
                            Signal signal
                    ) {

                        if (!scanInProgress) {
                            return;
                        }

                        if (signal != null) {

                            scanResults.add(
                                    signal
                            );

                            if (callback != null) {

                                callback.onTimeframeResult(
                                        currentMarket,
                                        timeframe,
                                        signal
                                );
                            }
                        }

                        timeframeIndex++;

                        requestNextTimeframe();
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        if (!scanInProgress) {
                            return;
                        }

                        if (callback != null) {

                            callback.onScanError(
                                    currentMarket,
                                    timeframe,
                                    message == null
                                            ? "SCAN ERROR"
                                            : message
                            );
                        }

                        /*
                         * A failed timeframe does not
                         * create a fake signal. We simply
                         * continue with the next timeframe.
                         */
                        timeframeIndex++;

                        requestNextTimeframe();
                    }
                }
        );
    }

    private void finishScan() {

        if (!scanInProgress) {
            return;
        }

        scanInProgress = false;

        List<Signal> completedResults =
                new ArrayList<>(
                        scanResults
                );

        if (callback != null) {

            callback.onScanCompleted(
                    market,
                    completedResults
            );
        }

        scanResults.clear();

        if (running) {

            scheduleNextScan(
                    scanInterval
            );
        }
    }
}