package com.forexpilot.aipro;

import java.util.List;

public class SignalEngine {

    private static final int FAST_EMA = 20;
    private static final int SLOW_EMA = 50;

    private static final int RSI_PERIOD = 14;
    private static final int ATR_PERIOD = 14;
    private static final int MOMENTUM_PERIOD = 5;

    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;

    private static final double BUY_RSI_MIN = 50.0;
    private static final double BUY_RSI_MAX = 70.0;

    private static final double SELL_RSI_MIN = 30.0;
    private static final double SELL_RSI_MAX = 50.0;

    private SignalEngine() {
        // Utility class
    }

    public static Signal analyze(
            String symbol,
            String timeframe,
            List<Candle> candles
    ) {
        if (symbol == null
                || timeframe == null
                || candles == null
                || candles.size() < 60) {

            return createWaitSignal(
                    symbol,
                    timeframe,
                    "INSUFFICIENT LIVE DATA"
            );
        }

        double entry =
                candles.get(candles.size() - 1).getClose();

        if (!isValidPrice(entry)) {
            return createWaitSignal(
                    symbol,
                    timeframe,
                    "INVALID LIVE PRICE"
            );
        }

        double fastEma =
                IndicatorEngine.ema(
                        candles,
                        FAST_EMA
                );

        double slowEma =
                IndicatorEngine.ema(
                        candles,
                        SLOW_EMA
                );

        double rsi =
                IndicatorEngine.rsi(
                        candles,
                        RSI_PERIOD
                );

        double atr =
                IndicatorEngine.atr(
                        candles,
                        ATR_PERIOD
                );

        double momentum =
                IndicatorEngine.momentum(
                        candles,
                        MOMENTUM_PERIOD
                );

        String trend =
                IndicatorEngine.trend(
                        candles,
                        FAST_EMA,
                        SLOW_EMA
                );

        /*
         * MACD confirmation.
         *
         * MACD line =
         * EMA(12) - EMA(26)
         *
         * Signal line =
         * EMA(9) of the MACD values.
         */
        double[] macd =
                calculateMacd(
                        candles,
                        MACD_FAST,
                        MACD_SLOW,
                        MACD_SIGNAL
                );

        double macdLine = macd[0];
        double macdSignal = macd[1];

        if (Double.isNaN(fastEma)
                || Double.isNaN(slowEma)
                || Double.isNaN(rsi)
                || Double.isNaN(atr)
                || Double.isNaN(momentum)
                || Double.isNaN(macdLine)
                || Double.isNaN(macdSignal)
                || atr <= 0) {

            return createWaitSignal(
                    symbol,
                    timeframe,
                    "INDICATOR DATA UNAVAILABLE"
            );
        }

        int buyScore = 0;
        int sellScore = 0;

        /*
         * 1. EMA TREND
         */
        if (fastEma > slowEma) {
            buyScore++;
        } else if (fastEma < slowEma) {
            sellScore++;
        }

        /*
         * 2. RSI
         *
         * BUY:
         * RSI between 50 and 70
         *
         * SELL:
         * RSI between 30 and 50
         */
        if (rsi >= BUY_RSI_MIN
                && rsi <= BUY_RSI_MAX) {

            buyScore++;
        }

        if (rsi >= SELL_RSI_MIN
                && rsi <= SELL_RSI_MAX) {

            sellScore++;
        }

        /*
         * 3. MOMENTUM
         */
        if (momentum > 0) {
            buyScore++;

        } else if (momentum < 0) {
            sellScore++;
        }

        /*
         * 4. MACD
         */
        if (macdLine > macdSignal
                && macdLine > 0) {

            buyScore++;

        } else if (macdLine < macdSignal
                && macdLine < 0) {

            sellScore++;
        }

        Signal.Direction direction;

        /*
         * Four possible confirmations:
         *
         * EMA
         * RSI
         * Momentum
         * MACD
         *
         * Require at least 3/4 agreement.
         */
        if (buyScore >= 3
                && buyScore > sellScore) {

            direction =
                    Signal.Direction.BUY;

        } else if (sellScore >= 3
                && sellScore > buyScore) {

            direction =
                    Signal.Direction.SELL;

        } else {

            direction =
                    Signal.Direction.WAIT;
        }

        /*
         * No trade without sufficient confirmation.
         */
        if (direction == Signal.Direction.WAIT) {

            return new Signal(
                    symbol,
                    timeframe,
                    Signal.Direction.WAIT,
                    entry,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    rsi,
                    atr,
                    trend,
                    System.currentTimeMillis()
            );
        }

        /*
         * ATR-based risk management.
         */
        double stopLoss =
                TradeLevelEngine.calculateStopLoss(
                        direction,
                        entry,
                        atr
                );

        if (Double.isNaN(stopLoss)) {

            return createWaitSignal(
                    symbol,
                    timeframe,
                    "TRADE LEVEL CALCULATION FAILED"
            );
        }

        double tp1 =
                TradeLevelEngine.calculateTakeProfit1(
                        direction,
                        entry,
                        stopLoss
                );

        double tp2 =
                TradeLevelEngine.calculateTakeProfit2(
                        direction,
                        entry,
                        stopLoss
                );

        double tp3 =
                TradeLevelEngine.calculateTakeProfit3(
                        direction,
                        entry,
                        stopLoss
                );

        return new Signal(
                symbol,
                timeframe,
                direction,
                entry,
                stopLoss,
                tp1,
                tp2,
                tp3,
                rsi,
                atr,
                trend,
                System.currentTimeMillis()
        );
    }

    private static double[] calculateMacd(
            List<Candle> candles,
            int fastPeriod,
            int slowPeriod,
            int signalPeriod
    ) {
        if (candles == null
                || candles.size()
                < slowPeriod + signalPeriod) {

            return new double[]{
                    Double.NaN,
                    Double.NaN
            };
        }

        int size = candles.size();

        double[] macdValues =
                new double[size];

        for (int i = 0; i < size; i++) {

            List<Candle> subset =
                    candles.subList(
                            0,
                            i + 1
                    );

            if (subset.size() < slowPeriod) {

                macdValues[i] =
                        Double.NaN;

                continue;
            }

            double fastEma =
                    IndicatorEngine.ema(
                            subset,
                            fastPeriod
                    );

            double slowEma =
                    IndicatorEngine.ema(
                            subset,
                            slowPeriod
                    );

            if (Double.isNaN(fastEma)
                    || Double.isNaN(slowEma)) {

                macdValues[i] =
                        Double.NaN;

            } else {

                macdValues[i] =
                        fastEma - slowEma;
            }
        }

        double[] validMacd =
                new double[size];

        int validCount = 0;

        for (double value : macdValues) {

            if (!Double.isNaN(value)) {

                validMacd[validCount] =
                        value;

                validCount++;
            }
        }

        if (validCount < signalPeriod) {

            return new double[]{
                    Double.NaN,
                    Double.NaN
            };
        }

        double multiplier =
                2.0
                        / (signalPeriod + 1.0);

        double signalLine = 0.0;

        for (int i = 0;
             i < signalPeriod;
             i++) {

            signalLine +=
                    validMacd[i];
        }

        signalLine /=
                signalPeriod;

        for (int i = signalPeriod;
             i < validCount;
             i++) {

            signalLine =
                    ((validMacd[i]
                            - signalLine)
                            * multiplier)
                            + signalLine;
        }

        double latestMacd =
                validMacd[validCount - 1];

        return new double[]{
                latestMacd,
                signalLine
        };
    }

    private static Signal createWaitSignal(
            String symbol,
            String timeframe,
            String reason
    ) {
        return new Signal(
                symbol == null
                        ? "UNKNOWN"
                        : symbol,

                timeframe == null
                        ? "UNKNOWN"
                        : timeframe,

                Signal.Direction.WAIT,

                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,

                Double.NaN,
                Double.NaN,

                reason,

                System.currentTimeMillis()
        );
    }

    private static boolean isValidPrice(
            double price
    ) {
        return !Double.isNaN(price)
                && !Double.isInfinite(price)
                && price > 0;
    }
}