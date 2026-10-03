package com.forexpilot.aipro;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class MarketClock {

    private static final ZoneId NEW_YORK =
            ZoneId.of("America/New_York");

    private static final LocalTime MARKET_OPEN_TIME =
            LocalTime.of(17, 0);

    private static final LocalTime MARKET_CLOSE_TIME =
            LocalTime.of(17, 0);

    private MarketClock() {
        // Utility class
    }

    /**
     * Returns true when the regular Forex market is open.
     *
     * Schedule used:
     * Sunday 17:00 New York
     * through
     * Friday 17:00 New York
     *
     * This uses America/New_York so daylight-saving
     * changes are handled automatically.
     */
    public static boolean isForexOpen(
            Instant instant
    ) {

        if (instant == null) {
            instant = Instant.now();
        }

        ZonedDateTime newYorkTime =
                instant.atZone(NEW_YORK);

        DayOfWeek day =
                newYorkTime.getDayOfWeek();

        LocalTime time =
                newYorkTime.toLocalTime();

        // Saturday is completely closed.
        if (day == DayOfWeek.SATURDAY) {
            return false;
        }

        // Sunday opens at 17:00 New York time.
        if (day == DayOfWeek.SUNDAY) {
            return !time.isBefore(
                    MARKET_OPEN_TIME
            );
        }

        // Friday closes at 17:00 New York time.
        if (day == DayOfWeek.FRIDAY) {
            return time.isBefore(
                    MARKET_CLOSE_TIME
            );
        }

        // Monday through Thursday.
        return true;
    }

    /**
     * Returns the current New York date/time.
     */
    public static ZonedDateTime getNewYorkTime(
            Instant instant
    ) {

        if (instant == null) {
            instant = Instant.now();
        }

        return instant.atZone(
                NEW_YORK
        );
    }

    /**
     * Returns the current New York time.
     */
    public static LocalTime getNewYorkLocalTime(
            Instant instant
    ) {

        return getNewYorkTime(
                instant
        ).toLocalTime();
    }

    /**
     * Returns the current New York date/time
     * without timezone information.
     */
    public static LocalDateTime getNewYorkDateTime(
            Instant instant
    ) {

        return getNewYorkTime(
                instant
        ).toLocalDateTime();
    }

    /**
     * Returns true when the current day is Saturday.
     */
    public static boolean isSaturday(
            Instant instant
    ) {

        return getNewYorkTime(
                instant
        ).getDayOfWeek()
                == DayOfWeek.SATURDAY;
    }

    /**
     * Returns true when the current day is Sunday.
     */
    public static boolean isSunday(
            Instant instant
    ) {

        return getNewYorkTime(
                instant
        ).getDayOfWeek()
                == DayOfWeek.SUNDAY;
    }

    /**
     * Returns a simple market status.
     */
    public static String getMarketStatus(
            Instant instant
    ) {

        return isForexOpen(instant)
                ? "OPEN"
                : "CLOSED";
    }

    /**
     * Returns the next regular market opening.
     */
    public static ZonedDateTime getNextMarketOpen(
            Instant instant
    ) {

        if (instant == null) {
            instant = Instant.now();
        }

        ZonedDateTime current =
                instant.atZone(NEW_YORK);

        DayOfWeek day =
                current.getDayOfWeek();

        LocalTime time =
                current.toLocalTime();

        ZonedDateTime nextOpen;

        if (day == DayOfWeek.SUNDAY) {

            if (time.isBefore(
                    MARKET_OPEN_TIME
            )) {

                nextOpen =
                        current.toLocalDate()
                                .atTime(
                                        MARKET_OPEN_TIME
                                )
                                .atZone(
                                        NEW_YORK
                                );

            } else {

                nextOpen =
                        current.plusDays(7)
                                .with(
                                        java.time.temporal.TemporalAdjusters
                                                .nextOrSame(
                                                        DayOfWeek.SUNDAY
                                                )
                                )
                                .with(
                                        java.time.temporal.TemporalAdjusters
                                                .previousOrSame(
                                                        DayOfWeek.SUNDAY
                                                )
                                )
                                .toLocalDate()
                                .atTime(
                                        MARKET_OPEN_TIME
                                )
                                .atZone(
                                        NEW_YORK
                                );
            }

        } else if (day == DayOfWeek.SATURDAY) {

            nextOpen =
                    current.with(
                            java.time.temporal.TemporalAdjusters
                                    .next(
                                            DayOfWeek.SUNDAY
                                    )
                    )
                    .toLocalDate()
                    .atTime(
                            MARKET_OPEN_TIME
                    )
                    .atZone(
                            NEW_YORK
                    );

        } else {

            nextOpen =
                    current.with(
                            java.time.temporal.TemporalAdjusters
                                    .next(
                                            DayOfWeek.SUNDAY
                                    )
                    )
                    .toLocalDate()
                    .atTime(
                            MARKET_OPEN_TIME
                    )
                    .atZone(
                            NEW_YORK
                    );
        }

        return nextOpen;
    }
}