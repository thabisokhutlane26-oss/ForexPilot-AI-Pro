
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
                String reason
        ) {
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
                    && direction != Signal.Direction.WAIT;
        }
    }

    private static final class StyleCandidate {

        final Style style;
        final String reviewTime;
        final int requiredAgreement;
        final int minimumConfidence;
        final String reason;

        StyleCandidate(
                Style style,
                String reviewTime,
                int requiredAgreement,
                int minimumConfidence,
                String reason
        ) {
            this.style = style;
            this.reviewTime = reviewTime;
            this.requiredAgreement = requiredAgreement;
            this.minimumConfidence = minimumConfidence;
            this.reason = reason;
        }
    }

    private TradeStyleEngine() {
        // Utility class.
    }

    /**
     * Selects a style using the primary signal and the available
     * multi-timeframe results.
     *
     * Confidence is a rule-based score, not a probability of profit.
     */
    public static TradeStylePlan selectBestStyle(
            Signal primarySignal,
            List<MultiTimeframeEngine.TimeframeResult> results
    ) {
        if (primarySignal == null
                || results == null
                || primarySignal.getDirection() == null
                || primarySignal.getDirection() == Signal.Direction.WAIT) {
            return waitPlan("No confirmed directional signal.");
        }

        Signal.Direction direction = primarySignal.getDirection();

        int fiveMinute = countDirection(
                results, direction, "5M"
        );

        int fifteenMinute = countDirection(
                results, direction, "15M"
        );

        int thirtyMinute = countDirection(
                results, direction, "30M"
        );

        int oneHour = countDirection(
                results, direction, "1H"
        );

        int fourHour = countDirection(
                results, direction, "4H"
        );

        int oneDay = countDirection(
                results, direction, "1D"
        );

        List<StyleCandidate> candidates = new ArrayList<>();

        // Scalping needs agreement across short timeframes.
        int scalpAgreement = fiveMinute
                + fifteenMinute
                + thirtyMinute;

        if (fiveMinute > 0
                && fifteenMinute > 0
                && thirtyMinute > 0
                && scalpAgreement >= 3) {
            candidates.add(new StyleCandidate(
                    Style.SCALP,
                    "Review in 30 minutes",
                    3,
                    65,
                    "5M, 15M and 30M agree with the signal."
            ));
        }

        // Day trading needs intraday agreement and higher-timeframe context.
        int dayAgreement = fifteenMinute
                + thirtyMinute
                + oneHour
                + fourHour;

        if (fifteenMinute > 0
                && thirtyMinute > 0
                && oneHour > 0
                && dayAgreement >= 3) {
            candidates.add(new StyleCandidate(
                    Style.DAY_TRADE,
                    "Review in 4 hours",
                    3,
                    65,
                    "Intraday timeframes support the signal."
            ));
        }

        // Swing trading requires agreement from higher timeframes.
        int swingAgreement = oneHour
                + fourHour
                + oneDay;

        if (oneHour > 0
                && fourHour > 0
                && oneDay > 0
                && swingAgreement >= 3) {
            candidates.add(new StyleCandidate(
                    Style.SWING,
                    "Review in 24 hours",
                    3,
                    65,
                    "1H, 4H and 1D agree with the signal."
            ));
        }

        if (candidates.isEmpty()) {
            return waitPlan(
                    "Timeframes do not provide enough agreement for a trade style."
            );
        }

        int bestScore = -1;
        StyleCandidate best = null;

        for (StyleCandidate candidate : candidates) {
            int score = getAgreementScore(
                    candidate.style,
                    fiveMinute,
                    fifteenMinute,
                    thirtyMinute,
                    oneHour,
                    fourHour,
                    oneDay
            );

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null) {
            return waitPlan("No suitable trade style was found.");
        }

        int confidence = Math.min(
                95,
                50 + (bestScore * 10)
        );

        if (confidence < best.minimumConfidence) {
            return waitPlan(
                    "The available timeframe agreement is too weak."
            );
        }

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
            String timeframe
    ) {
        int count = 0;

        for (MultiTimeframeEngine.TimeframeResult result : results) {
            if (result == null
                    || result.getTimeframe() == null
                    || result.getDirection() == null) {
                continue;
            }

            if (timeframe.equalsIgnoreCase(result.getTimeframe())
                    && result.getDirection() == direction) {
                count++;
            }
        }

        return Math.min(count, 1);
    }

    private static int getAgreementScore(
            Style style,
            int fiveMinute,
            int fifteenMinute,
            int thirtyMinute,
            int oneHour,
            int fourHour,
            int oneDay
    ) {
        switch (style) {
            case SCALP:
                return fiveMinute + fifteenMinute + thirtyMinute;

            case DAY_TRADE:
                return fifteenMinute + thirtyMinute
                        + oneHour + fourHour;

            case SWING:
                return oneHour + fourHour + oneDay;

            default:
                return 0;
        }
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
