
package com.forexpilot.aipro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MarketWatchView extends ScrollView {

    public interface OnMarketSelectedListener {
        void onMarketSelected(String symbol);
    }

    private final Activity activity;
    private final MarketDataProvider provider;
    private final OnMarketSelectedListener listener;

    private final String[] symbols = {
            "XAU/USD",
            "EUR/USD",
            "GBP/USD",
            "USD/JPY",
            "NZD/USD"
    };

    private final TextView[] priceViews = new TextView[5];
    private final TextView[] changeViews = new TextView[5];
    private final TextView[] directionViews = new TextView[5];
    private final TextView[] structureViews = new TextView[5];
    private final TextView[] eventViews = new TextView[5];
    private final TextView[] levelsViews = new TextView[5];

    private final int background = Color.rgb(7, 11, 18);
    private final int cardColor = Color.rgb(15, 22, 33);
    private final int borderColor = Color.rgb(35, 47, 64);
    private final int white = Color.rgb(245, 248, 252);
    private final int muted = Color.rgb(145, 158, 175);
    private final int green = Color.rgb(20, 190, 100);
    private final int red = Color.rgb(230, 65, 75);
    private final int yellow = Color.rgb(225, 175, 45);
    private final int blue = Color.rgb(45, 120, 230);

    private final LinearLayout content;

    public MarketWatchView(
            Activity activity,
            MarketDataProvider provider,
            OnMarketSelectedListener listener
    ) {
        super(activity);

        this.activity = activity;
        this.provider = provider;
        this.listener = listener;

        setFillViewport(true);
        setBackgroundColor(background);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(18));

        addView(content, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        buildScreen();
        refreshMarkets();
    }

    private void buildScreen() {
        TextView title = text(
                "MARKET WATCH",
                22,
                white,
                true
        );
        content.addView(title);

        TextView subtitle = text(
                "LIVE MARKET STRUCTURE • TWELVE DATA",
                11,
                blue,
                true
        );
        addTop(subtitle, 4);

        TextView explanation = text(
                "Live candle analysis across all five markets. "
                        + "Tap a pair to open its chart.",
                12,
                muted,
                false
        );
        addTop(explanation, 8);

        Button refresh = new Button(activity);
        refresh.setText("REFRESH ALL MARKETS");
        refresh.setAllCaps(false);
        refresh.setTextColor(Color.WHITE);
        refresh.setTextSize(12);
        refresh.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        refresh.setBackground(round(blue, blue, 10));
        refresh.setOnClickListener(v -> refreshMarkets());

        LinearLayout.LayoutParams refreshParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        dp(46)
                );
        refreshParams.topMargin = dp(12);
        content.addView(refresh, refreshParams);

        for (int i = 0; i < symbols.length; i++) {
            buildMarketCard(i);
        }

        TextView note = text(
                "STRUCTURE GUIDE\n"
                        + "HH = Higher High  •  HL = Higher Low\n"
                        + "LH = Lower High  •  LL = Lower Low\n"
                        + "BOS = Break of Structure\n"
                        + "CHoCH = Change of Character\n\n"
                        + "Support and resistance are estimated "
                        + "from confirmed candle swing points.",
                11,
                muted,
                false
        );
        note.setPadding(dp(12), dp(12), dp(12), dp(12));
        note.setBackground(round(cardColor, borderColor, 12));
        addTop(note, 12);
    }

    private void buildMarketCard(int index) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(round(cardColor, borderColor, 14));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                );
        cardParams.topMargin = dp(10);
        content.addView(card, cardParams);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView symbol = text(
                symbols[index],
                18,
                white,
                true
        );

        header.addView(symbol, new LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1
        ));

        changeViews[index] = text(
                "--",
                15,
                muted,
                true
        );
        header.addView(changeViews[index]);

        card.addView(header);

        priceViews[index] = text(
                "PRICE  •  WAITING FOR LIVE DATA",
                13,
                blue,
                true
        );
        addToCard(card, priceViews[index], 8);

        directionViews[index] = text(
                "DIRECTION  •  LOADING",
                12,
                yellow,
                true
        );
        addToCard(card, directionViews[index], 7);

        structureViews[index] = text(
                "STRUCTURE  •  --",
                12,
                white,
                true
        );
        addToCard(card, structureViews[index], 5);

        eventViews[index] = text(
                "MARKET EVENT  •  --",
                11,
                muted,
                true
        );
        addToCard(card, eventViews[index], 5);

        levelsViews[index] = text(
                "SUPPORT  •  --\nRESISTANCE  •  --",
                11,
                muted,
                false
        );
        addToCard(card, levelsViews[index], 7);

        card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMarketSelected(symbols[index]);
            }
        });
    }

    public void refreshMarkets() {
        if (provider == null) {
            showAllUnavailable("DATA PROVIDER NOT CONNECTED");
            return;
        }

        for (int i = 0; i < symbols.length; i++) {
            final int index = i;
            final String symbol = symbols[i];

            setLoading(index);

            provider.requestCandles(
                    symbol,
                    "15M",
                    new MarketDataManager.MarketDataCallback() {

                @Override
                public void onCandlesReceived(List<Candle> candles) {
                    activity.runOnUiThread(() -> {
                        MarketWatchEngine.Result result =
                                MarketWatchEngine.analyze(
                                        symbol,
                                        candles
                                );

                        if (result == null) {
                            showUnavailable(
                                    index,
                                    "NOT ENOUGH VALID CANDLES"
                            );
                            return;
                        }

                        showResult(index, result);
                    });
                }

                @Override
                public void onPriceReceived(double price) {
                    // Candle close remains the analysis price.
                }

                @Override
                public void onError(String message) {
                    activity.runOnUiThread(() ->
                            showUnavailable(
                                    index,
                                    "LIVE DATA UNAVAILABLE"
                            )
                    );
                }
            });
        }
    }

    private void showResult(
            int index,
            MarketWatchEngine.Result result
    ) {
        int directionColor =
                "BULLISH".equals(result.direction) ? green
                : "BEARISH".equals(result.direction) ? red
                : yellow;

        priceViews[index].setText(
                "PRICE  •  " + formatPrice(
                        result.symbol,
                        result.price
                )
        );

        changeViews[index].setText(result.getChangeLabel());
        changeViews[index].setTextColor(
                result.changePercent > 0 ? green
                        : result.changePercent < 0 ? red : muted
        );

        directionViews[index].setText(
                "DIRECTION  •  " + result.direction
        );
        directionViews[index].setTextColor(directionColor);

        structureViews[index].setText(
                "STRUCTURE  •  " + result.structure
        );

        eventViews[index].setText(
                "MARKET EVENT  •  " + result.event
        );
        eventViews[index].setTextColor(
                result.event.contains("BULLISH") ? green
                        : result.event.contains("BEARISH") ? red
                        : muted
        );

        levelsViews[index].setText(
                "SUPPORT  •  " + formatPrice(
                        result.symbol, result.support
                )
                + "\nRESISTANCE  •  " + formatPrice(
                        result.symbol, result.resistance
                )
        );
    }

    private void setLoading(int index) {
        priceViews[index].setText(
                "PRICE  •  REQUESTING LIVE CANDLES..."
        );
        changeViews[index].setText("--");
        changeViews[index].setTextColor(muted);
        directionViews[index].setText("DIRECTION  •  LOADING");
        directionViews[index].setTextColor(yellow);
        structureViews[index].setText("STRUCTURE  •  --");
        eventViews[index].setText("MARKET EVENT  •  --");
        levelsViews[index].setText(
                "SUPPORT  •  --\nRESISTANCE  •  --"
        );
    }

    private void showUnavailable(int index, String message) {
        priceViews[index].setText("PRICE  •  " + message);
        priceViews[index].setTextColor(red);

        changeViews[index].setText("--");
        changeViews[index].setTextColor(muted);

        directionViews[index].setText("DIRECTION  •  UNAVAILABLE");
        directionViews[index].setTextColor(muted);

        structureViews[index].setText("STRUCTURE  •  UNAVAILABLE");
        eventViews[index].setText("MARKET EVENT  •  UNAVAILABLE");

        levelsViews[index].setText(
                "SUPPORT  •  --\nRESISTANCE  •  --"
        );
    }

    private void showAllUnavailable(String message) {
        for (int i = 0; i < symbols.length; i++) {
            showUnavailable(i, message);
        }
    }

    private String formatPrice(String symbol, double value) {
        if (!Double.isFinite(value)) return "--";

        if ("USD/JPY".equalsIgnoreCase(symbol)) {
            return String.format(Locale.US, "%.3f", value);
        }

        if ("XAU/USD".equalsIgnoreCase(symbol)) {
            return String.format(Locale.US, "%.2f", value);
        }

        return String.format(Locale.US, "%.5f", value);
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
