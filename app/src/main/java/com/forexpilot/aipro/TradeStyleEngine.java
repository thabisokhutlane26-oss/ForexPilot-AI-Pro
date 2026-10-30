
package com.forexpilot.aipro;

import java.util.ArrayList;
import java.util.List;

public final class TradeStyleEngine {

    public enum Style {
        SCALP,
        DAY_TRADE,
        SWING,
        NONE
    }

    public static final class TradeStylePlan {

        private final Style style;
        private final Signal.Direction direction;
        private final int confidence;
        private final String reviewTime;
        private final String reason;

        public TradeStylePlan(
                Style style,
                Signal.Direction direction,
                int confidence,
                String reviewTime,
                String reason) {
            this.style = style;
            this.direction = direction;
            this.confidence = confidence;
            this.reviewTime = reviewTime;
            this.reason = reason;
        }

        public Style getStyle() {
            return style;
        }

        public Signal.Direction getDirection() {
            return direction;
        }

        public int getConfidence() {
            return confidence;
        }

        public String getReviewTime() {
            return reviewTime;
        }

        public String getReason() {
            return reason;
        }

        public boolean isTradeAllowed() {
            return style != Style.NONE
                    && direction != null
                    && direction != Signal.Direction.WAIT;
        }
    }

    private static final class StyleCandidate {
        final Style style;
        final String reviewTime;
        final String reason;
        final int matches;
        final int total;
        final int priority;

        StyleCandidate(
                Style style,
                String reviewTime,
                String reason,
                int matches,
                int total,
                int priority) {
            this.style = style;
            this.reviewTime = reviewTime;
            this.reason = reason;
            this.matches = matches;
            this.total = total;
            this.priority = priority;
        }

        double getRatio() {
            return total == 0 ? 0.0 : (double) matches / total;
        }
    }

    private TradeStyleEngine() {
    }

    public static TradeStylePlan selectBestStyle(
            Signal primarySignal,
            List<MultiTimeframeEngine.TimeframeResult> results) {

        if (primarySignal == null
                || primarySignal.getDirection() == null
                || primarySignal.getDirection() == Signal.Direction.WAIT
                || results == null) {
            return waitPlan("No confirmed directional signal.");
        }

        Signal.Direction direction = primarySignal.getDirection();

        int fiveMinute = countDirection(results, direction, "5M");
        int fifteenMinute = countDirection(results, direction, "15M");
        int thirtyMinute = countDirection(results, direction, "30M");
        int oneHour = countDirection(results, direction, "1H");
        int fourHour = countDirection(results, direction, "4H");
        int oneDay = countDirection(results, direction, "1D");

        List<StyleCandidate> candidates = new ArrayList<>();

        // SCALP: at least two of the three short timeframes agree.
        int scalpMatches = fiveMinute + fifteenMinute + thirtyMinute;

        if (scalpMatches >= 2) {
            candidates.add(new StyleCandidate(
                    Style.SCALP,
                    "Review in 30 minutes",
                    "At least two of 5M, 15M and 30M support the signal.",
                    scalpMatches,
                    3,
                    1
            ));
        }

        // DAY TRADE: at least three of four intraday timeframes agree.
        int dayMatches = fifteenMinute + thirtyMinute
                + oneHour + fourHour;

        if (dayMatches >= 3) {
            candidates.add(new StyleCandidate(
                    Style.DAY_TRADE,
                    "Review in 4 hours",
                    "At least three intraday timeframes support the signal.",
                    dayMatches,
                    4,
                    2
            ));
        }

        // SWING: at least two of the three longer timeframes agree.
        int swingMatches = oneHour + fourHour + oneDay;

        if (swingMatches >= 2) {
            candidates.add(new StyleCandidate(
                    Style.SWING,
                    "Review in 24 hours",
                    "At least two of 1H, 4H and 1D support the signal.",
                    swingMatches,
                    3,
                    3
            ));
        }

        if (candidates.isEmpty()) {
            return waitPlan(
                    "Timeframe agreement is insufficient for a trade style."
            );
        }

        StyleCandidate best = null;

        for (StyleCandidate candidate : candidates) {
            if (best == null
                    || candidate.getRatio() > best.getRatio()
                    || (candidate.getRatio() == best.getRatio()
                    && candidate.priority > best.priority)) {
                best = candidate;
            }
        }

        if (best == null) {
            return waitPlan("No suitable trade style was found.");
        }

        // This is a rule-based agreement score, not a probability
        // of a profitable trade.
        int confidence = 60
                + (int) Math.round(best.getRatio() * 30.0);

        confidence = Math.max(60, Math.min(90, confidence));

        return new TradeStylePlan(
                best.style,
                direction,
                confidence,
                best.reviewTime,
                best.reason
        );
    }

    private static int countDirection(
            List<MultiTimeframeEngine.TimeframeResult> results,
            Signal.Direction direction,
            String timeframe) {

        for (MultiTimeframeEngine.TimeframeResult result : results) {
            if (result == null
                    || result.getTimeframe() == null
                    || result.getDirection() == null) {
                continue;
            }

            String label = result.getTimeframe()
                    .trim()
                    .toUpperCase(java.util.Locale.US);

            if (timeframe.equals(label)) {
                return result.getDirection() == direction ? 1 : 0;
            }
        }

        return 0;
    }

    private static TradeStylePlan waitPlan(String reason) {
        return new TradeStylePlan(
                Style.NONE,
                Signal.Direction.WAIT,
                0,
                "No review time",
                reason
        );
    }
}
