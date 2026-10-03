package com.forexpilot.aipro;

public class Signal {

    public enum Direction {
        BUY,
        SELL,
        WAIT
    }

    private final String symbol;
    private final String timeframe;
    private final Direction direction;

    private final double entry;
    private final double stopLoss;
    private final double takeProfit1;
    private final double takeProfit2;
    private final double takeProfit3;

    private final double rsi;
    private final double atr;
    private final String trend;

    private final long timestamp;

    public Signal(
            String symbol,
            String timeframe,
            Direction direction,
            double entry,
            double stopLoss,
            double takeProfit1,
            double takeProfit2,
            double takeProfit3,
            double rsi,
            double atr,
            String trend,
            long timestamp
    ) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
        this.entry = entry;
        this.stopLoss = stopLoss;
        this.takeProfit1 = takeProfit1;
        this.takeProfit2 = takeProfit2;
        this.takeProfit3 = takeProfit3;
        this.rsi = rsi;
        this.atr = atr;
        this.trend = trend;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getEntry() {
        return entry;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public double getTakeProfit1() {
        return takeProfit1;
    }

    public double getTakeProfit2() {
        return takeProfit2;
    }

    public double getTakeProfit3() {
        return takeProfit3;
    }

    public double getRsi() {
        return rsi;
    }

    public double getAtr() {
        return atr;
    }

    public String getTrend() {
        return trend;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
