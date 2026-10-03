package com.forexpilot.aipro;

import java.util.List;

public class IndicatorEngine {

    private IndicatorEngine() {
        // Utility class
    }

    public static double ema(List<Candle> candles, int period) {
        if (candles == null || candles.size() < period || period <= 0) {
            return Double.NaN;
        }

        double multiplier = 2.0 / (period + 1.0);

        double ema = 0.0;

        for (int i = 0; i < period; i++) {
            ema += candles.get(i).getClose();
        }

        ema /= period;

        for (int i = period; i < candles.size(); i++) {
            double close = candles.get(i).getClose();
            ema = ((close - ema) * multiplier) + ema;
        }

        return ema;
    }

    public static double rsi(List<Candle> candles, int period) {
        if (candles == null || candles.size() <= period || period <= 0) {
            return Double.NaN;
        }

        double gains = 0.0;
        double losses = 0.0;

        for (int i = 1; i <= period; i++) {
            double change =
                    candles.get(i).getClose()
                            - candles.get(i - 1).getClose();

            if (change > 0) {
                gains += change;
            } else {
                losses += Math.abs(change);
            }
        }

        double averageGain = gains / period;
        double averageLoss = losses / period;

        for (int i = period + 1; i < candles.size(); i++) {
            double change =
                    candles.get(i).getClose()
                            - candles.get(i - 1).getClose();

            double gain = Math.max(change, 0);
            double loss = Math.max(-change, 0);

            averageGain =
                    ((averageGain * (period - 1)) + gain) / period;

            averageLoss =
                    ((averageLoss * (period - 1)) + loss) / period;
        }

        if (averageLoss == 0) {
            return 100.0;
        }

        double relativeStrength = averageGain / averageLoss;

        return 100.0 - (100.0 / (1.0 + relativeStrength));
    }

    public static double atr(List<Candle> candles, int period) {
        if (candles == null || candles.size() <= period || period <= 0) {
            return Double.NaN;
        }

        double totalTrueRange = 0.0;

        for (int i = 1; i <= period; i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);

            double highLow =
                    current.getHigh() - current.getLow();

            double highPreviousClose =
                    Math.abs(current.getHigh()
                            - previous.getClose());

            double lowPreviousClose =
                    Math.abs(current.getLow()
                            - previous.getClose());

            double trueRange =
                    Math.max(
                            highLow,
                            Math.max(
                                    highPreviousClose,
                                    lowPreviousClose
                            )
                    );

            totalTrueRange += trueRange;
        }

        double averageTrueRange =
                totalTrueRange / period;

        for (int i = period + 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);

            double highLow =
                    current.getHigh() - current.getLow();

            double highPreviousClose =
                    Math.abs(current.getHigh()
                            - previous.getClose());

            double lowPreviousClose =
                    Math.abs(current.getLow()
                            - previous.getClose());

            double trueRange =
                    Math.max(
                            highLow,
                            Math.max(
                                    highPreviousClose,
                                    lowPreviousClose
                            )
                    );

            averageTrueRange =
                    ((averageTrueRange * (period - 1))
                            + trueRange) / period;
        }

        return averageTrueRange;
    }

    public static double momentum(List<Candle> candles, int period) {
        if (candles == null
                || candles.size() <= period
                || period <= 0) {
            return Double.NaN;
        }

        int lastIndex = candles.size() - 1;
        int previousIndex = lastIndex - period;

        return candles.get(lastIndex).getClose()
                - candles.get(previousIndex).getClose();
    }

    public static String trend(
            List<Candle> candles,
            int fastPeriod,
            int slowPeriod
    ) {
        double fastEma = ema(candles, fastPeriod);
        double slowEma = ema(candles, slowPeriod);

        if (Double.isNaN(fastEma)
                || Double.isNaN(slowEma)) {
            return "UNKNOWN";
        }

        if (fastEma > slowEma) {
            return "BULLISH";
        }

        if (fastEma < slowEma) {
            return "BEARISH";
        }

        return "NEUTRAL";
    }
}
