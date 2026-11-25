package com.forexpilot.aipro;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class SignalNotificationManager {

    private static final String CHANNEL_ID = "forexpilot_signals";
    private static final String CHANNEL_NAME = "ForexPilot Signals";
    private static final int BASE_NOTIFICATION_ID = 7000;

    private final Context context;
    private final NotificationManager notificationManager;

    private String lastSignalKey = "";

    public SignalNotificationManager(Context context) {
        this.context = context.getApplicationContext();

        notificationManager = (NotificationManager)
                this.context.getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription(
                "ForexPilot AI Pro trading signal alerts"
        );
        channel.enableVibration(true);

        notificationManager.createNotificationChannel(channel);
    }

    public boolean notifySignal(Signal signal) {
        if (signal == null || notificationManager == null) {
            return false;
        }

        Signal.Direction direction = signal.getDirection();

        if (direction != Signal.Direction.BUY
                && direction != Signal.Direction.SELL) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                "android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        String symbol = signal.getSymbol();
        String timeframe = signal.getTimeframe();

        if (symbol == null || symbol.trim().isEmpty()) {
            symbol = "UNKNOWN";
        }

        if (timeframe == null || timeframe.trim().isEmpty()) {
            timeframe = "UNKNOWN";
        }

        String signalKey = createSignalKey(
                signal, symbol, timeframe
        );

        if (signalKey.equals(lastSignalKey)) {
            return false;
        }

        boolean isBuy = direction == Signal.Direction.BUY;

        String directionText = isBuy ? "BUY" : "SELL";

        int accentColor = isBuy
                ? android.graphics.Color.rgb(40, 220, 125)
                : android.graphics.Color.rgb(255, 70, 80);

        String title = directionText + " • " + symbol;

        String content =
                timeframe
                        + "\nEntry: " + formatPrice(signal.getEntry())
                        + "\nSL: " + formatPrice(signal.getStopLoss())
                        + "\nTP1: " + formatPrice(signal.getTakeProfit1())
                        + "\nTP2: " + formatPrice(signal.getTakeProfit2())
                        + "\nTP3: " + formatPrice(signal.getTakeProfit3());

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                BASE_NOTIFICATION_ID,
                intent,
                getPendingIntentFlags()
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("ForexPilot AI Pro")
                        .setContentText(title + " • " + timeframe)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(content)
                        )
                        .setColor(accentColor)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setCategory(NotificationCompat.CATEGORY_ALARM);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setDefaults(
                    android.app.Notification.DEFAULT_ALL
            );
        }

        try {
            notificationManager.notify(
                    createNotificationId(signal),
                    builder.build()
            );

            lastSignalKey = signalKey;
            return true;

        } catch (SecurityException exception) {
            return false;
        }
    }

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
        String direction = signal.getDirection() == Signal.Direction.BUY
                ? "BUY" : "SELL";

        return symbol
                + "|" + timeframe
                + "|" + direction
                + "|" + formatPrice(signal.getEntry())
                + "|" + formatPrice(signal.getStopLoss())
                + "|" + formatPrice(signal.getTakeProfit1());
    }

    private int createNotificationId(Signal signal) {
        String key = String.valueOf(signal.getSymbol())
                + "|" + String.valueOf(signal.getTimeframe())
                + "|" + String.valueOf(signal.getDirection());

        return BASE_NOTIFICATION_ID
                + Math.abs(key.hashCode() % 1000);
    }

    private String formatPrice(double price) {
        if (Double.isNaN(price) || Double.isInfinite(price)) {
            return "N/A";
        }

        return String.format(Locale.US, "%.5f", price);
    }

    private int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT
                    | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
