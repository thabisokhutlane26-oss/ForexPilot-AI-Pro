package com.forexpilot.aipro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MarketWatchEngine {

    private static final int SWING_RADIUS = 3;
    private static final int DEFAULT_LOOKBACK = 24;

    private MarketWatchEngine() {}

    public static class Result {
        public final String symbol;
        public final double price;
        public final double changePercent;
        public final String direction;
        public final String structure;
        public final String event;
        public final double support;
        public final double resistance;
        public final long timestamp;

        Result(String symbol, double price, double changePercent,
               String direction, String structure, String event,
               double support, double resistance, long timestamp) {
            this.symbol = symbol;
            this.price = price;
            this.changePercent = changePercent;
            this.direction = direction;
            this.structure = structure;
            this.event = event;
            this.support = support;
            this.resistance = resistance;
            this.timestamp = timestamp;
        }

        public String getChangeLabel() {
            return String.format(Locale.US, "%+.2f%%", changePercent);
        }
    }

    private static class Pivot {
        final int index;
        final double price;

        Pivot(int index, double price) {
            this.index = index;
            this.price = price;
        }
    }

    public static Result analyze(String symbol, List<Candle> candles) {
        return analyze(symbol, candles, DEFAULT_LOOKBACK);
    }

    public static Result analyze(
            String symbol,
            List<Candle> candles,
            int lookback
    ) {
        if (candles == null || candles.size() < 10) {
            return null;
        }

        List<Candle> data = new ArrayList<>(candles);
        Candle latest = data.get(data.size() - 1);

        if (!valid(latest.getClose())) return null;

        int start = Math.max(0, data.size() - Math.max(2, lookback) - 1);
        double firstClose = data.get(start).getClose();

        if (!valid(firstClose)) return null;

        double change = ((latest.getClose() - firstClose)
                / firstClose) * 100.0;

        List<Pivot> highs = findHighs(data);
        List<Pivot> lows = findLows(data);

        String structure = determineStructure(highs, lows);
        String direction = determineDirection(change, structure);

        String event = detectBreakout(
                data, highs, lows, structure
        );

        double support = nearestBelow(
                lows, latest.getClose()
        );

        double resistance = nearestAbove(
                highs, latest.getClose()
        );

        return new Result(
                symbol,
                latest.getClose(),
                change,
                direction,
                structure,
                event,
                support,
                resistance,
                latest.getTimestamp()
        );
    }

    private static List<Pivot> findHighs(List<Candle> candles) {
        List<Pivot> result = new ArrayList<>();

        for (int i = SWING_RADIUS;
             i < candles.size() - SWING_RADIUS; i++) {

            double high = candles.get(i).getHigh();
            boolean pivot = true;

            for (int j = 1; j <= SWING_RADIUS; j++) {
                if (high <= candles.get(i - j).getHigh()
                        || high <= candles.get(i + j).getHigh()) {
                    pivot = false;
                    break;
                }
            }

            if (pivot) result.add(new Pivot(i, high));
        }

        return result;
    }

    private static List<Pivot> findLows(List<Candle> candles) {
        List<Pivot> result = new ArrayList<>();

        for (int i = SWING_RADIUS;
             i < candles.size() - SWING_RADIUS; i++) {

            double low = candles.get(i).getLow();
            boolean pivot = true;

            for (int j = 1; j <= SWING_RADIUS; j++) {
                if (low >= candles.get(i - j).getLow()
                        || low >= candles.get(i + j).getLow()) {
                    pivot = false;
                    break;
                }
            }

            if (pivot) result.add(new Pivot(i, low));
        }

        return result;
    }

    private static String determineStructure(
            List<Pivot> highs,
            List<Pivot> lows
    ) {
        if (highs.size() < 2 || lows.size() < 2) {
            return "RANGING";
        }

        Pivot h1 = highs.get(highs.size() - 2);
        Pivot h2 = highs.get(highs.size() - 1);

        Pivot l1 = lows.get(lows.size() - 2);
        Pivot l2 = lows.get(lows.size() - 1);

        boolean higherHigh = h2.price > h1.price;
        boolean higherLow = l2.price > l1.price;

        boolean lowerHigh = h2.price < h1.price;
        boolean lowerLow = l2.price < l1.price;

        if (higherHigh && higherLow) return "HH • HL";
        if (lowerHigh && lowerLow) return "LH • LL";

        if (higherHigh) return "HH";
        if (higherLow) return "HL";
        if (lowerHigh) return "LH";
        if (lowerLow) return "LL";

        return "RANGING";
    }

    private static String determineDirection(
            double change,
            String structure
    ) {
        if (structure.contains("HH") || structure.contains("HL")) {
            return "BULLISH";
        }

        if (structure.contains("LH") || structure.contains("LL")) {
            return "BEARISH";
        }

        if (change > 0.05) return "BULLISH";
        if (change < -0.05) return "BEARISH";

        return "RANGING";
    }

    private static String detectBreakout(
            List<Candle> candles,
            List<Pivot> highs,
            List<Pivot> lows,
            String structure
    ) {
        Candle latest = candles.get(candles.size() - 1);

        if (highs.isEmpty() || lows.isEmpty()) {
            return "NO CONFIRMED BREAK";
        }

        Pivot lastHigh = highs.get(highs.size() - 1);
        Pivot lastLow = lows.get(lows.size() - 1);

        if (latest.getClose() > lastHigh.price) {
            return structure.startsWith("LH")
                    ? "BULLISH CHoCH"
                    : "BULLISH BOS";
        }

        if (latest.getClose() < lastLow.price) {
            return structure.startsWith("HH")
                    ? "BEARISH CHoCH"
                    : "BEARISH BOS";
        }

        return "NO CONFIRMED BREAK";
    }

    private static double nearestBelow(
            List<Pivot> pivots,
            double price
    ) {
        double result = Double.NaN;

        for (Pivot pivot : pivots) {
            if (pivot.price < price
                    && (Double.isNaN(result)
                    || pivot.price > result)) {
                result = pivot.price;
            }
        }

        return result;
    }

    private static double nearestAbove(
            List<Pivot> pivots,
            double price
    ) {
        double result = Double.NaN;

        for (Pivot pivot : pivots) {
            if (pivot.price > price
                    && (Double.isNaN(result)
                    || pivot.price < result)) {
                result = pivot.price;
            }
        }

        return result;
    }

    private static boolean valid(double value) {
        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value > 0;
    }
}