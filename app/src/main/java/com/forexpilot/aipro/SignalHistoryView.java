
package com.forexpilot.aipro;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class SignalHistoryView extends ScrollView {

    private final Activity activity;
    private final SignalHistoryStore store;
    private final LinearLayout content;

    private final int background = Color.rgb(7, 11, 18);
    private final int cardColor = Color.rgb(15, 22, 33);
    private final int borderColor = Color.rgb(35, 47, 64);
    private final int white = Color.rgb(245, 248, 252);
    private final int muted = Color.rgb(145, 158, 175);
    private final int green = Color.rgb(20, 190, 100);
    private final int red = Color.rgb(230, 65, 75);
    private final int yellow = Color.rgb(225, 175, 45);
    private final int blue = Color.rgb(45, 120, 230);

    public SignalHistoryView(
            Activity activity,
            SignalHistoryStore store
    ) {
        super(activity);
        this.activity = activity;
        this.store = store;

        setFillViewport(true);
        setBackgroundColor(background);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(18));

        addView(content, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        refresh();
    }

    public void refresh() {
        content.removeAllViews();

        TextView title = text(
                "SIGNAL HISTORY",
                22,
                white,
                true
        );
        content.addView(title);

        TextView subtitle = text(
                "YOUR SAVED FOREXPILOT SIGNALS",
                11,
                blue,
                true
        );
        addTop(subtitle, 4);

        int count = store == null ? 0 : store.size();

        TextView total = text(
                "SAVED RECORDS  •  " + count,
                12,
                muted,
                true
        );
        addTop(total, 10);

        Button refresh = button("REFRESH HISTORY");
        refresh.setOnClickListener(v -> refresh());
        addTop(refresh, 10);

        Button clear = button("CLEAR HISTORY");
        clear.setBackground(round(
                Color.rgb(65, 25, 32),
                red,
                10
        ));
        clear.setOnClickListener(v -> confirmClear());
        addTop(clear, 7);

        if (store == null) {
            showEmpty("HISTORY STORAGE NOT AVAILABLE");
            return;
        }

        List<JSONObject> records = store.getAll();

        if (records.isEmpty()) {
            showEmpty("NO SAVED SIGNALS YET");
            return;
        }

        for (JSONObject record : records) {
            if (record != null) {
                addRecord(record);
            }
        }
    }

    private void addRecord(JSONObject record) {
        String symbol = record.optString("symbol", "--");
        String timeframe = record.optString("timeframe", "--");
        String direction = record.optString("direction", "WAIT");
        String trend = record.optString("trend", "--");
        long timestamp = record.optLong("timestamp", 0);

        int directionColor = "BUY".equalsIgnoreCase(direction)
                ? green
                : "SELL".equalsIgnoreCase(direction)
                ? red : yellow;

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(round(cardColor, borderColor, 13));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                );
        params.topMargin = dp(10);
        content.addView(card, params);

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);

        TextView market = text(
                symbol + "  •  " + timeframe,
                16,
                white,
                true
        );
        heading.addView(market, new LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView signal = text(
                direction,
                15,
                directionColor,
                true
        );
        heading.addView(signal);
        card.addView(heading);

        TextView date = text(
                SignalHistoryStore.formatTimestamp(timestamp),
                11,
                muted,
                false
        );
        addToCard(card, date, 7);

        addToCard(card, text(
                "TREND  •  " + trend,
                12,
                blue,
                true
        ), 7);

        addToCard(card, text(
                "ENTRY          " + price(symbol, record.optDouble("entry", Double.NaN))
                        + "\nSTOP LOSS     " + price(symbol, record.optDouble("stopLoss", Double.NaN))
                        + "\nTAKE PROFIT 1 " + price(symbol, record.optDouble("takeProfit1", Double.NaN))
                        + "\nTAKE PROFIT 2 " + price(symbol, record.optDouble("takeProfit2", Double.NaN))
                        + "\nTAKE PROFIT 3 " + price(symbol, record.optDouble("takeProfit3", Double.NaN)),
                12,
                white,
                false
        ), 9);
    }

    private void confirmClear() {
        new AlertDialog.Builder(activity)
                .setTitle("CLEAR SIGNAL HISTORY?")
                .setMessage(
                        "This permanently clears all saved signal records."
                )
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CLEAR", (dialog, which) -> {
                    store.clear();
                    refresh();
                })
                .show();
    }

    private void showEmpty(String message) {
        TextView empty = text(message, 13, muted, true);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(dp(12), dp(35), dp(12), dp(35));
        empty.setBackground(round(cardColor, borderColor, 12));
        addTop(empty, 15);
    }

    private String price(String symbol, double value) {
        if (!Double.isFinite(value)) return "--";

        if ("USD/JPY".equalsIgnoreCase(symbol)) {
            return String.format(Locale.US, "%.3f", value);
        }

        if ("XAU/USD".equalsIgnoreCase(symbol)) {
            return String.format(Locale.US, "%.2f", value);
        }

        return String.format(Locale.US, "%.5f", value);
    }

    private Button button(String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setBackground(round(blue, blue, 10));
        return button;
    }

    private TextView text(
            String value,
            float size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private void addTop(View view, int margin) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                );
        params.topMargin = dp(margin);
        content.addView(view, params);
    }

    private void addToCard(
            LinearLayout card,
            View view,
            int margin
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                );
        params.topMargin = dp(margin);
        card.addView(view, params);
    }

    private GradientDrawable round(
            int fill,
            int stroke,
            int radius
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(float value) {
        return (int) (
                value * activity.getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }
}
