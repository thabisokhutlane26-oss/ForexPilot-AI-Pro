package com.forexpilot.aipro;

import java.util.List;

public class MultiTimeframeEngine {

    private static final int EXPECTED_TIMEFRAMES = 6;

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
                waitCount++;
                continue;
            }

            Signal.Direction direction =
                    result.getDirection();

            if (direction == Signal.Direction.BUY) {

                buyCount++;

            } else if (
                    direction == Signal.Direction.SELL
            ) {

                sellCount++;

            } else {

                waitCount++;
            }
        }

        /*
         * The app is designed around six timeframes:
         *
         * 5M
         * 15M
         * 30M
         * 1H
         * 4H
         * 1D
         *
         * Do not claim full confluence until all six
         * timeframes have returned a result.
         */
        if (results.size() < EXPECTED_TIMEFRAMES) {

            return new ConfluenceResult(
                    buyCount,
                    sellCount,
                    waitCount,
                    Signal.Direction.WAIT,
                    0
            );
        }

        Signal.Direction direction =
                Signal.Direction.WAIT;

        int strength = 0;

        /*
         * Strong BUY:
         *
         * At least 4 of 6 timeframes BUY
         * and BUY must clearly exceed SELL.
         */
        if (buyCount >= 4
                && buyCount > sellCount) {

            direction =
                    Signal.Direction.BUY;

            strength =
                    calculateStrength(
                            buyCount,
                            EXPECTED_TIMEFRAMES
                    );

        /*
         * Strong SELL:
         *
         * At least 4 of 6 timeframes SELL
         * and SELL must clearly exceed BUY.
         */
        } else if (
                sellCount >= 4
                        && sellCount > buyCount
        ) {

            direction =
                    Signal.Direction.SELL;

            strength =
                    calculateStrength(
                            sellCount,
                            EXPECTED_TIMEFRAMES
                    );
        }

        /*
         * Anything below 4/6 remains WAIT.
         *
         * This deliberately reduces weak signals.
         */
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

        /*
         * 6/6 = 100%  -> strength 5
         * 5/6 = 83%   -> strength 5
         * 4/6 = 66%   -> strength 4
         */
        if (percentage >= 83.0) {
            return 5;
        }

        if (percentage >= 66.0) {
            return 4;
        }

        return 0;
    }
}