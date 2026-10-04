package com.forexpilot.aipro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 11 integration coordinator.
 *
 * Connects the existing BackgroundScanner to:
 * - MultiTimeframeEngine
 * - SignalNotificationManager
 *
 * MainActivity remains untouched.
 */
public class SignalAlertCoordinator {

    private static final String[] TIMEFRAMES = {
            "5M",
            "15M",
            "30M",
            "1H",
            "4H",
            "1D"
    };

    private final BackgroundScanner scanner;
    private final MultiTimeframeEngine multiTimeframeEngine;
    private final SignalNotificationManager notificationManager;

    private final Map<String, List<Signal>> signalCache =
            new HashMap<>();

    private String currentMarket = "";
    private boolean enabled = true;

    public SignalAlertCoordinator(
            BackgroundScanner scanner,
            MultiTimeframeEngine multiTimeframeEngine,
            SignalNotificationManager notificationManager
    ) {
        this.scanner = scanner;
        this.multiTimeframeEngine = multiTimeframeEngine;
        this.notificationManager = notificationManager;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        if (scanner != null) {
            scanner.start();
        }
    }

    public void stop() {
        if (scanner != null) {
            scanner.stop();
        }

        signalCache.clear();
        currentMarket = "";
    }

    /**
     * BackgroundScanner.scanNow() takes no arguments.
     */
    public void scanNow(String market) {
        if (scanner == null) {
            return;
        }

        if (market != null && !market.trim().isEmpty()) {
            currentMarket = market.trim();
        }

        scanner.scanNow();
    }

    public void setMarket(String market) {
        if (market == null) {
            currentMarket = "";
            signalCache.clear();
            return;
        }

        String normalized = market.trim();

        if (!normalized.equals(currentMarket)) {
            currentMarket = normalized;
            signalCache.clear();
        }
    }

    public String getCurrentMarket() {
        return currentMarket;
    }

    /**
     * This coordinator intentionally uses its own public method for receiving
     * scanner results. The existing BackgroundScanner remains untouched.
     */
    public void onTimeframeResult(
            String market,
            String timeframe,
            Signal signal
    ) {
        if (market == null ||
                timeframe == null ||
                signal == null) {
            return;
        }

        currentMarket = market;

        List<Signal> signals = signalCache.get(market);

        if (signals == null) {
            signals = new ArrayList<>();
            signalCache.put(market, signals);
        }

        replaceTimeframeSignal(
                signals,
                timeframe,
                signal
        );

        evaluateConfluence(
                market,
                signals
        );
    }

    public void onScanCompleted(
            String market,
            List<Signal> signals
    ) {
        if (market == null || signals == null) {
            return;
        }

        signalCache.put(
                market,
                new ArrayList<>(signals)
        );

        evaluateConfluence(
                market,
                signals
        );
    }

    public void onScanError(
            String market,
            String timeframe,
            String message
    ) {
        // Errors must never create fake signals.
    }

    private void replaceTimeframeSignal(
            List<Signal> signals,
            String timeframe,
            Signal newSignal
    ) {
        for (int i = 0; i < signals.size(); i++) {

            Signal existing = signals.get(i);

            if (existing != null &&
                    timeframe.equalsIgnoreCase(
                            existing.getTimeframe())) {

                signals.set(i, newSignal);
                return;
            }
        }

        signals.add(newSignal);
    }

    private void evaluateConfluence(
            String market,
            List<Signal> signals
    ) {
        if (!enabled ||
                signals == null ||
                signals.size() < TIMEFRAMES.length) {
            return;
        }

        List<MultiTimeframeEngine.TimeframeResult> results =
                new ArrayList<>();

        for (String timeframe : TIMEFRAMES) {

            Signal signal =
                    findSignal(
                            signals,
                            timeframe
                    );

            if (signal == null) {
                return;
            }

            results.add(
                    new MultiTimeframeEngine.TimeframeResult(
                            timeframe,
                            signal.getDirection(),
                            "",
                            signal.getRsi(),
                            signal.getAtr()
                    )
            );
        }

        MultiTimeframeEngine.ConfluenceResult confluence =
                multiTimeframeEngine.calculateConfluence(
                        results
                );

        if (confluence == null) {
            return;
        }

        Signal.Direction direction =
                confluence.getDirection();

        int strength =
                confluence.getStrength();

        if (direction == Signal.Direction.WAIT) {
            return;
        }

        if (strength < 4) {
            return;
        }

        Signal notificationSignal =
                selectNotificationSignal(
                        signals,
                        direction
                );

        if (notificationSignal == null) {
            return;
        }

        /*
         * Existing SignalNotificationManager accepts only Signal.
         * It already handles the notification details and duplicate
         * protection internally.
         */
        notificationManager.notifySignal(
                notificationSignal
        );
    }

    private Signal findSignal(
            List<Signal> signals,
            String timeframe
    ) {
        for (Signal signal : signals) {

            if (signal == null) {
                continue;
            }

            if (timeframe.equalsIgnoreCase(
                    signal.getTimeframe())) {

                return signal;
            }
        }

        return null;
    }

    private Signal selectNotificationSignal(
            List<Signal> signals,
            Signal.Direction direction
    ) {
        String[] priority = {
                "15M",
                "30M",
                "5M",
                "1H",
                "4H",
                "1D"
        };

        for (String timeframe : priority) {

            Signal signal =
                    findSignal(
                            signals,
                            timeframe
                    );

            if (signal == null) {
                continue;
            }

            if (signal.getDirection() == direction) {
                return signal;
            }
        }

        return null;
    }

    public void clearMarket(String market) {
        if (market == null) {
            return;
        }

        signalCache.remove(
                market.trim()
        );
    }

    public void clearAll() {
        signalCache.clear();
    }
}