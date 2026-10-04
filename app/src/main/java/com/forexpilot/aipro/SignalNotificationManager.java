package com.forexpilot.aipro;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class SignalNotificationManager {

    private static final String CHANNEL_ID =
            "forexpilot_signals";

    private static final String CHANNEL_NAME =
            "ForexPilot Signals";

    private static final String CHANNEL_DESCRIPTION =
            "ForexPilot AI Pro trading signal alerts";

    private static final int BASE_NOTIFICATION_ID =
            7000;

    private final Context context;

    private final NotificationManager
            notificationManager;

    private String lastSignalKey = "";

    public SignalNotificationManager(
            Context context
    ) {

        this.context =
                context.getApplicationContext();

        notificationManager =
                (NotificationManager)
                        this.context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        createNotificationChannel();
    }

    /**
     * Creates the Android notification channel.
     */
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.O) {

            return;
        }

        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                CHANNEL_DESCRIPTION
        );

        channel.enableVibration(true);

        notificationManager.createNotificationChannel(
                channel
        );
    }

    /**
     * Sends a notification only for BUY or SELL.
     */
    public boolean notifySignal(
            Signal signal
    ) {

        if (signal == null) {
            return false;
        }

        Signal.Direction direction =
                signal.getDirection();

        if (direction
                == Signal.Direction.WAIT) {

            return false;
        }

        String symbol =
                signal.getSymbol();

        String timeframe =
                signal.getTimeframe();

        if (symbol == null
                || symbol.trim().isEmpty()) {

            symbol = "UNKNOWN";
        }

        if (timeframe == null
                || timeframe.trim().isEmpty()) {

            timeframe = "UNKNOWN";
        }

        String signalKey =
                createSignalKey(
                        signal,
                        symbol,
                        timeframe
                );

        /*
         * Prevent the exact same signal from
         * repeatedly notifying.
         */
        if (signalKey.equals(
                lastSignalKey
        )) {

            return false;
        }

        lastSignalKey =
                signalKey;

        String directionText;

        boolean isBuy =
                direction
                        == Signal.Direction.BUY;

        if (isBuy) {

            directionText =
                    "BUY";

        } else {

            directionText =
                    "SELL";
        }

        int accentColor;

        if (isBuy) {

            accentColor =
                    android.graphics.Color.rgb(
                            40,
                            220,
                            125
                    );

        } else {

            accentColor =
                    android.graphics.Color.rgb(
                            255,
                            70,
                            80
                    );
        }

        String title =
                directionText
                        + " • "
                        + symbol;

        String entry =
                formatPrice(
                        signal.getEntry()
                );

        String stopLoss =
                formatPrice(
                        signal.getStopLoss()
                );

        String tp1 =
                formatPrice(
                        signal.getTakeProfit1()
                );

        String tp2 =
                formatPrice(
                        signal.getTakeProfit2()
                );

        String tp3 =
                formatPrice(
                        signal.getTakeProfit3()
                );

        String content =
                timeframe
                        + "\n"
                        + "Entry: "
                        + entry
                        + "\n"
                        + "SL: "
                        + stopLoss
                        + "\n"
                        + "TP1: "
                        + tp1
                        + "\n"
                        + "TP2: "
                        + tp2
                        + "\n"
                        + "TP3: "
                        + tp3;

        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        BASE_NOTIFICATION_ID,
                        intent,
                        getPendingIntentFlags()
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable
                                        .ic_dialog_info
                        )
                        .setContentTitle(
                                "ForexPilot AI Pro"
                        )
                        .setContentText(
                                title
                                        + " • "
                                        + timeframe
                        )
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(
                                                content
                                        )
                        )
                        .setColor(
                                accentColor
                        )
                        .setPriority(
                                NotificationCompat
                                        .PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        )
                        .setCategory(
                                NotificationCompat
                                        .CATEGORY_ALARM
                        );

        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.O) {

            builder.setDefaults(
                    android.app.Notification
                            .DEFAULT_ALL
            );
        }

        if (notificationManager == null) {
            return false;
        }

        int notificationId =
                createNotificationId(
                        signal
                );

        notificationManager.notify(
                notificationId,
                builder.build()
        );

        return true;
    }

    /**
     * Allows the same signal key to be notified
     * again after the caller explicitly resets it.
     */
    public void resetDuplicateProtection() {

        lastSignalKey = "";
    }

    public String getLastSignalKey() {

        return lastSignalKey;
    }

    private String createSignalKey(
            Signal signal,
            String symbol,
            String timeframe
    ) {

        String direction =
                signal.getDirection()
                        == Signal.Direction.BUY
                        ? "BUY"
                        : "SELL";

        String entry =
                formatPrice(
                        signal.getEntry()
                );

        String stopLoss =
                formatPrice(
                        signal.getStopLoss()
                );

        String tp1 =
                formatPrice(
                        signal.getTakeProfit1()
                );

        return symbol
                + "|"
                + timeframe
                + "|"
                + direction
                + "|"
                + entry
                + "|"
                + stopLoss
                + "|"
                + tp1;
    }

    private int createNotificationId(
            Signal signal
    ) {

        String symbol =
                signal.getSymbol();

        String timeframe =
                signal.getTimeframe();

        int hash =
                (
                        String.valueOf(symbol)
                                + "|"
                                + String.valueOf(timeframe)
                                + "|"
                                + String.valueOf(
                                signal.getDirection()
                        )
                ).hashCode();

        return BASE_NOTIFICATION_ID
                + Math.abs(hash % 1000);
    }

    private String formatPrice(
            double price
    ) {

        if (Double.isNaN(price)
                || Double.isInfinite(price)) {

            return "N/A";
        }

        return String.format(
                Locale.US,
                "%.5f",
                price
        );
    }

    private int getPendingIntentFlags() {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.M) {

            return PendingIntent.FLAG_UPDATE_CURRENT
                    | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}