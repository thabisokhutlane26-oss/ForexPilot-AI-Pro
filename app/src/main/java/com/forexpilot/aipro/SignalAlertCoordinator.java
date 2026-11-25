package com.forexpilot.aipro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalAlertCoordinator {

    private static final String[] TIMEFRAMES = {
            "5M", "15M", "30M", "1H", "4H", "1D"
    };

    private final BackgroundScanner scanner;
    private final SignalNotificationManager notificationManager;

    private final Map<String, List<Signal>> signalCache =
            new HashMap<>();

    private String currentMarket = "";
    private boolean enabled = true;

    public SignalAlertCoordinator(
            BackgroundScanner scanner,
            SignalNotificationManager notificationManager) {

        this.scanner = scanner;
        this.notificationManager = notificationManager;

        if (this.scanner != null) {
            this.scanner.setCallback(new BackgroundScanner.ScanCallback() {
                @Override
                public void onScanStarted() {
                    // A background scan has started.
                }

                @Override
                public void onTimeframeResult(
                        String market, String timeframe, Signal signal) {
                    SignalAlertCoordinator.this.onTimeframeResult(
                            market, timeframe, signal);
                }

                @Override
                public void onScanCompleted(
                        String market, List<Signal> signals) {
                    SignalAlertCoordinator.this.onScanCompleted(
                            market, signals);
                }

                @Override
                public void onScanError(
                        String market, String timeframe, String message) {
                    SignalAlertCoordinator.this.onScanError(
                            market, timeframe, message);
                }
            });
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        if (scanner != null && enabled) {
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

    public void scanNow(String market) {
        if (scanner == null || !enabled) return;

        setMarket(market);
        scanner.scanNow();
    }

    public void setMarket(String market) {
        if (market == null || market.trim().isEmpty()) return;

        String normalized = market.trim();

        if (!normalized.equals(currentMarket)) {
            currentMarket = normalized;
            signalCache.clear();
        }

        if (scanner != null) {
            scanner.setMarket(normalized);
        }
    }

    public String getCurrentMarket() {
        return currentMarket;
    }

    public void onTimeframeResult(
            String market, String timeframe, Signal signal) {

        if (!enabled || market == null
                || timeframe == null || signal == null) return;

        if (!market.equals(currentMarket)) return;

        List<Signal> signals = signalCache.get(market);

        if (signals == null) {
            signals = new ArrayList<>();
            signalCache.put(market, signals);
        }

        replaceTimeframeSignal(signals, timeframe, signal);
        evaluateConfluence(market, signals);
    }

    public void onScanCompleted(
            String market, List<Signal> signals) {

        if (!enabled || market == null || signals == null) return;
        if (!market.equals(currentMarket)) return;

        signalCache.put(market, new ArrayList<>(signals));
        evaluateConfluence(market, signals);
    }

    public void onScanError(
            String market, String timeframe, String message) {
        // Failed timeframes are not treated as trading signals.
    }

    private void replaceTimeframeSignal(
            List<Signal> signals,
            String timeframe,
            Signal newSignal) {

        for (int i = 0; i < signals.size(); i++) {
            Signal existing = signals.get(i);

            if (existing != null
                    && timeframe.equalsIgnoreCase(
                    existing.getTimeframe())) {
                signals.set(i, newSignal);
                return;
            }
        }

        signals.add(newSignal);
    }

    private void evaluateConfluence(
            String market, List<Signal> signals) {

        if (!enabled || notificationManager == null
                || signals == null
                || signals.size() < TIMEFRAMES.length) return;

        List<MultiTimeframeEngine.TimeframeResult> results =
                new ArrayList<>();

        for (String timeframe : TIMEFRAMES) {
            Signal signal = findSignal(signals, timeframe);

            if (signal == null) return;

            results.add(new MultiTimeframeEngine.TimeframeResult(
                    timeframe,
                    signal.getDirection(),
                    signal.getTrend(),
                    signal.getRsi(),
                    signal.getAtr()
            ));
        }

        MultiTimeframeEngine.ConfluenceResult confluence =
                MultiTimeframeEngine.calculateConfluence(results);

        if (confluence == null) return;

        Signal.Direction direction = confluence.getDirection();

        if (direction == null
                || direction == Signal.Direction.WAIT
                || confluence.getStrength() < 4) return;

        Signal notificationSignal =
                selectNotificationSignal(signals, direction);

        if (notificationSignal != null) {
            notificationManager.notifySignal(notificationSignal);
        }
    }

    private Signal findSignal(
            List<Signal> signals, String timeframe) {

        for (Signal signal : signals) {
            if (signal != null
                    && timeframe.equalsIgnoreCase(
                    signal.getTimeframe())) {
                return signal;
            }
        }

        return null;
    }

    private Signal selectNotificationSignal(
            List<Signal> signals,
            Signal.Direction direction) {

        String[] priority = {
                "15M", "30M", "5M", "1H", "4H", "1D"
        };

        for (String timeframe : priority) {
            Signal signal = findSignal(signals, timeframe);

            if (signal != null
                    && signal.getDirection() == direction) {
                return signal;
            }
        }

        return null;
    }

    public void clearMarket(String market) {
        if (market != null) {
            signalCache.remove(market.trim());
        }
    }

    public void clearAll() {
        signalCache.clear();
    }
}
