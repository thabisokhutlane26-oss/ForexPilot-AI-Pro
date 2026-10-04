package com.forexpilot.aipro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForexPilotIndicator {

    public enum SignalType {
        BUY,
        SELL,
        NONE
    }

    public static class Result {

        private final SignalType signalType;
        private final double trailingStop;
        private final double atr;
        private final double ema;
        private final double rsi;

        public Result(
                SignalType signalType,
                double trailingStop,
                double atr,
                double ema,
                double rsi
        ) {
            this.signalType = signalType;
            this.trailingStop = trailingStop;
            this.atr = atr;
            this.ema = ema;
            this.rsi = rsi;
        }

        public SignalType getSignalType() {
            return signalType;
        }

        public double getTrailingStop() {
            return trailingStop;
        }

        public double getAtr() {
            return atr;
        }

        public double getEma() {
            return ema;
        }

        public double getRsi() {
            return rsi;
        }
    }

    private static final int ATR_PERIOD = 10;
    private static final int EMA_PERIOD = 20;
    private static final int RSI_PERIOD = 14;

    /*
     * This controls how far the indicator's trailing stop
     * sits from price.
     *
     * It is intentionally an original ForexPilot parameter,
     * not copied from another indicator implementation.
     */
    private static final double TRAILING_MULTIPLIER = 1.5;

    private ForexPilotIndicator() {
    }

    public static List<Result> calculate(List<Candle> candles) {

        if (candles == null || candles.size() < 30) {
            return Collections.emptyList();
        }

        List<Result> results = new ArrayList<>();

        double previousTrailingStop = candles.get(0).getClose();
        int previousTrend = 0;

        for (int i = 0; i < candles.size(); i++) {

            Candle candle = candles.get(i);

            double close = candle.getClose();

            double atr = calculateAtr(candles, i, ATR_PERIOD);
            double ema = calculateEma(candles, i, EMA_PERIOD);
            double rsi = calculateRsi(candles, i, RSI_PERIOD);

            if (i < Math.max(
                    ATR_PERIOD,
                    Math.max(EMA_PERIOD, RSI_PERIOD)
            )) {

                results.add(
                        new Result(
                                SignalType.NONE,
                                close,
                                atr,
                                ema,
                                rsi
                        )
                );

                previousTrailingStop = close;
                continue;
            }

            double distance = atr * TRAILING_MULTIPLIER;

            double trailingStop;

            if (close > previousTrailingStop) {

                trailingStop = Math.max(
                        previousTrailingStop,
                        close - distance
                );

            } else {

                trailingStop = Math.min(
                        previousTrailingStop,
                        close + distance
                );
            }

            int currentTrend;

            if (close > trailingStop) {
                currentTrend = 1;
            } else if (close < trailingStop) {
                currentTrend = -1;
            } else {
                currentTrend = previousTrend;
            }

            SignalType signalType = SignalType.NONE;

            /*
             * A signal is generated only when the indicator
             * changes direction. This prevents every candle
             * in the same trend from becoming a new signal.
             */
            if (previousTrend <= 0 && currentTrend > 0) {

                /*
                 * BUY confirmation:
                 * price above EMA and RSI showing bullish momentum.
                 */
                if (close > ema && rsi >= 50.0) {
                    signalType = SignalType.BUY;
                }

            } else if (previousTrend >= 0 && currentTrend < 0) {

                /*
                 * SELL confirmation:
                 * price below EMA and RSI showing bearish momentum.
                 */
                if (close < ema && rsi <= 50.0) {
                    signalType = SignalType.SELL;
                }
            }

            results.add(
                    new Result(
                            signalType,
                            trailingStop,
                            atr,
                            ema,
                            rsi
                    )
            );

            previousTrailingStop = trailingStop;
            previousTrend = currentTrend;
        }

        return results;
    }

    private static double calculateAtr(
            List<Candle> candles,
            int index,
            int period
    ) {

        if (index <= 0) {
            return 0.0;
        }

        int start = Math.max(1, index - period + 1);

        double total = 0.0;
        int count = 0;

        for (int i = start; i <= index; i++) {

            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);

            double high = current.getHigh();
            double low = current.getLow();
            double previousClose = previous.getClose();

            double trueRange = Math.max(
                    high - low,
                    Math.max(
                            Math.abs(high - previousClose),
                            Math.abs(low - previousClose)
                    )
            );

            total += trueRange;
            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        return total / count;
    }

    private static double calculateEma(
            List<Candle> candles,
            int index,
            int period
    ) {

        if (index < 0) {
            return 0.0;
        }

        int start = Math.max(0, index - period + 1);

        double ema = candles.get(start).getClose();

        double multiplier =
                2.0 / (period + 1.0);

        for (int i = start + 1; i <= index; i++) {

            double close = candles.get(i).getClose();

            ema =
                    (close - ema) * multiplier
                            + ema;
        }

        return ema;
    }

    private static double calculateRsi(
            List<Candle> candles,
            int index,
            int period
    ) {

        if (index <= 0) {
            return 50.0;
        }

        int start = Math.max(1, index - period + 1);

        double gains = 0.0;
        double losses = 0.0;

        for (int i = start; i <= index; i++) {

            double change =
                    candles.get(i).getClose()
                            - candles.get(i - 1).getClose();

            if (change > 0) {
                gains += change;
            } else {
                losses -= change;
            }
        }

        int count = index - start + 1;

        if (count <= 0) {
            return 50.0;
        }

        double averageGain = gains / count;
        double averageLoss = losses / count;

        if (averageLoss == 0.0) {

            if (averageGain == 0.0) {
                return 50.0;
            }

            return 100.0;
        }

        double relativeStrength =
                averageGain / averageLoss;

        return 100.0
                - (100.0
                / (1.0 + relativeStrength));
    }
}