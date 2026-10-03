package com.forexpilot.aipro;

import java.util.List;

public class MultiTimeframeEngine {

    public static class TimeframeResult {

        private final String timeframe;
        private final Signal.Direction direction;
        private final String trend;
        private final double rsi;
        private final double atr;

        public TimeframeResult(
                String timeframe,
                Signal.Direction direction,
                String trend,
                double rsi,
                double atr
        ) {
            this.timeframe = timeframe;
            this.direction = direction;
            this.trend = trend;
            this.rsi = rsi;
            this.atr = atr;
        }

        public String getTimeframe() {
            return timeframe;
        }

        public Signal.Direction getDirection() {
            return direction;
        }

        public String getTrend() {
            return trend;
        }

        public double getRsi() {
            return rsi;
        }

        public double getAtr() {
            return atr;
        }
    }

    public static class ConfluenceResult {

        private final int buyCount;
        private final int sellCount;
        private final int waitCount;
        private final Signal.Direction direction;
        private final int strength;

        public ConfluenceResult(
                int buyCount,
                int sellCount,
                int waitCount,
                Signal.Direction direction,
                int strength
        ) {
            this.buyCount = buyCount;
            this.sellCount = sellCount;
            this.waitCount = waitCount;
            this.direction = direction;
            this.strength = strength;
        }

        public int getBuyCount() {
            return buyCount;
        }

        public int getSellCount() {
            return sellCount;
        }

        public int getWaitCount() {
            return waitCount;
        }

        public Signal.Direction getDirection() {
            return direction;
        }

        public int getStrength() {
            return strength;
        }
    }

    private MultiTimeframeEngine() {
        // Utility class
    }

    public static TimeframeResult analyzeTimeframe(
            String symbol,
            String timeframe,
            List<Candle> candles
    ) {

        if (timeframe == null) {
            timeframe = "UNKNOWN";
        }

        Signal signal =
                SignalEngine.analyze(
                        symbol,
                        timeframe,
                        candles
                );

        if (signal == null) {

            return new TimeframeResult(
                    timeframe,
                    Signal.Direction.WAIT,
                    "UNKNOWN",
                    Double.NaN,
                    Double.NaN
            );
        }

        return new TimeframeResult(
                timeframe,
                signal.getDirection(),
                signal.getTrend(),
                signal.getRsi(),
                signal.getAtr()
        );
    }

    public static ConfluenceResult calculateConfluence(
            List<TimeframeResult> results
    ) {

        if (results == null
                || results.isEmpty()) {

            return new ConfluenceResult(
                    0,
                    0,
                    0,
                    Signal.Direction.WAIT,
                    0
            );
        }

        int buyCount = 0;
        int sellCount = 0;
        int waitCount = 0;

        for (TimeframeResult result : results) {

            if (result == null) {
                continue;
            }

            if (result.getDirection()
                    == Signal.Direction.BUY) {

                buyCount++;

            } else if (
                    result.getDirection()
                            == Signal.Direction.SELL
            ) {

                sellCount++;

            } else {

                waitCount++;
            }
        }

        Signal.Direction direction =
                Signal.Direction.WAIT;

        int strength = 0;

        if (buyCount > sellCount
                && buyCount >= 3) {

            direction =
                    Signal.Direction.BUY;

            strength =
                    calculateStrength(
                            buyCount,
                            results.size()
                    );

        } else if (
                sellCount > buyCount
                        && sellCount >= 3
        ) {

            direction =
                    Signal.Direction.SELL;

            strength =
                    calculateStrength(
                            sellCount,
                            results.size()
                    );
        }

        return new ConfluenceResult(
                buyCount,
                sellCount,
                waitCount,
                direction,
                strength
        );
    }

    private static int calculateStrength(
            int directionalCount,
            int totalTimeframes
    ) {

        if (totalTimeframes <= 0) {
            return 0;
        }

        double percentage =
                ((double) directionalCount
                        / totalTimeframes)
                        * 100.0;

        if (percentage >= 83.0) {
            return 5;
        }

        if (percentage >= 66.0) {
            return 4;
        }

        if (percentage >= 50.0) {
            return 3;
        }

        if (percentage >= 33.0) {
            return 2;
        }

        return 1;
    }
}