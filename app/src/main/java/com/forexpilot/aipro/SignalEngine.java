package com.forexpilot.aipro;

import java.util.List;

public class SignalEngine {

    private static final int FAST_EMA = 20;
    private static final int SLOW_EMA = 50;
    private static final int RSI_PERIOD = 14;
    private static final int ATR_PERIOD = 14;
    private static final int MOMENTUM_PERIOD = 5;

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
                IndicatorEngine.ema(candles, FAST_EMA);

        double slowEma =
                IndicatorEngine.ema(candles, SLOW_EMA);

        double rsi =
                IndicatorEngine.rsi(candles, RSI_PERIOD);

        double atr =
                IndicatorEngine.atr(candles, ATR_PERIOD);

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

        if (Double.isNaN(fastEma)
                || Double.isNaN(slowEma)
                || Double.isNaN(rsi)
                || Double.isNaN(atr)
                || Double.isNaN(momentum)
                || atr <= 0) {

            return createWaitSignal(
                    symbol,
                    timeframe,
                    "INDICATOR DATA UNAVAILABLE"
            );
        }

        int buyScore = 0;
        int sellScore = 0;

        // EMA trend
        if (fastEma > slowEma) {
            buyScore++;
        } else if (fastEma < slowEma) {
            sellScore++;
        }

        // RSI
        if (rsi >= BUY_RSI_MIN
                && rsi <= BUY_RSI_MAX) {
            buyScore++;
        }

        if (rsi >= SELL_RSI_MIN
                && rsi <= SELL_RSI_MAX) {
            sellScore++;
        }

        // Momentum
        if (momentum > 0) {
            buyScore++;
        } else if (momentum < 0) {
            sellScore++;
        }

        Signal.Direction direction;

        if (buyScore >= 3 && buyScore > sellScore) {
            direction = Signal.Direction.BUY;

        } else if (sellScore >= 3
                && sellScore > buyScore) {
            direction = Signal.Direction.SELL;

        } else {
            direction = Signal.Direction.WAIT;
        }

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

    private static Signal createWaitSignal(
            String symbol,
            String timeframe,
            String reason
    ) {
        return new Signal(
                symbol == null ? "UNKNOWN" : symbol,
                timeframe == null ? "UNKNOWN" : timeframe,
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

    private static boolean isValidPrice(double price) {
        return !Double.isNaN(price)
                && !Double.isInfinite(price)
                && price > 0;
    }
}
