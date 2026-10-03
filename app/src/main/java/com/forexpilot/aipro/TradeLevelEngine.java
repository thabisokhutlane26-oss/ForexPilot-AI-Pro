package com.forexpilot.aipro;

public class TradeLevelEngine {

    private TradeLevelEngine() {
        // Utility class
    }

    public static double calculateStopLoss(
            Signal.Direction direction,
            double entry,
            double atr
    ) {
        if (Double.isNaN(atr) || atr <= 0 || entry <= 0) {
            return Double.NaN;
        }

        double stopDistance = atr * 1.5;

        if (direction == Signal.Direction.BUY) {
            return entry - stopDistance;
        }

        if (direction == Signal.Direction.SELL) {
            return entry + stopDistance;
        }

        return Double.NaN;
    }

    public static double calculateTakeProfit1(
            Signal.Direction direction,
            double entry,
            double stopLoss
    ) {
        return calculateTakeProfit(
                direction,
                entry,
                stopLoss,
                1.0
        );
    }

    public static double calculateTakeProfit2(
            Signal.Direction direction,
            double entry,
            double stopLoss
    ) {
        return calculateTakeProfit(
                direction,
                entry,
                stopLoss,
                2.0
        );
    }

    public static double calculateTakeProfit3(
            Signal.Direction direction,
            double entry,
            double stopLoss
    ) {
        return calculateTakeProfit(
                direction,
                entry,
                stopLoss,
                3.0
        );
    }

    private static double calculateTakeProfit(
            Signal.Direction direction,
            double entry,
            double stopLoss,
            double riskMultiple
    ) {
        if (direction == Signal.Direction.WAIT
                || entry <= 0
                || stopLoss <= 0
                || riskMultiple <= 0) {
            return Double.NaN;
        }

        double risk = Math.abs(entry - stopLoss);

        if (direction == Signal.Direction.BUY) {
            return entry + (risk * riskMultiple);
        }

        if (direction == Signal.Direction.SELL) {
            return entry - (risk * riskMultiple);
        }

        return Double.NaN;
    }
}
