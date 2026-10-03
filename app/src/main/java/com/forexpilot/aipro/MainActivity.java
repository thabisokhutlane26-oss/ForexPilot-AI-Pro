package com.forexpilot.aipro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private SignalRepository signalRepository;

    private TextView connectionStatus;
    private TextView priceText;
    private TextView signalText;
    private TextView trendText;
    private TextView rsiText;
    private TextView atrText;
    private TextView momentumText;

    private TextView entryText;
    private TextView stopLossText;
    private TextView tp1Text;
    private TextView tp2Text;
    private TextView tp3Text;

    private Spinner marketSpinner;
    private Spinner timeframeSpinner;

    private Button scanButton;

    private String selectedMarket = "EUR/USD";
    private String selectedTimeframe = "15M";

    private final String[] markets = {
            "XAU/USD",
            "EUR/USD",
            "GBP/USD",
            "USD/JPY",
            "NZD/USD"
    };

    private final String[] timeframes = {
            "5M",
            "15M",
            "30M",
            "1H",
            "4H",
            "1D"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildDashboard();
        connectToMarketData();
    }

    private void buildDashboard() {

        ScrollView scrollView = new ScrollView(this);

        scrollView.setBackgroundColor(
                Color.rgb(8, 12, 20)
        );

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24,
                28,
                24,
                40
        );

        scrollView.addView(root);

        // HEADER

        TextView title = createText(
                "ForexPilot AI Pro",
                28,
                Color.WHITE,
                true
        );

        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                fullWidthParams(0)
        );

        TextView subtitle = createText(
                "REAL-TIME FOREX SIGNAL INTELLIGENCE",
                12,
                Color.LTGRAY,
                true
        );

        subtitle.setGravity(Gravity.CENTER);

        root.addView(
                subtitle,
                fullWidthParams(8)
        );

        connectionStatus = createText(
                "●  CONNECTING TO LIVE MARKET DATA",
                14,
                Color.YELLOW,
                true
        );

        connectionStatus.setGravity(Gravity.CENTER);

        root.addView(
                connectionStatus,
                fullWidthParams(18)
        );

        // MARKET SELECTORS

        LinearLayout selectorRow =
                new LinearLayout(this);

        selectorRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        marketSpinner =
                createSpinner(markets);

        timeframeSpinner =
                createSpinner(timeframes);

        selectorRow.addView(
                marketSpinner,
                weightParams()
        );

        selectorRow.addView(
                timeframeSpinner,
                weightParams()
        );

        root.addView(
                selectorRow,
                fullWidthParams(18)
        );

        marketSpinner.setSelection(1);
        timeframeSpinner.setSelection(1);

        marketSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedMarket =
                                markets[position];
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        timeframeSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedTimeframe =
                                timeframes[position];
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        // MARKET CARD

        LinearLayout marketCard =
                createCard();

        TextView marketTitle =
                createSectionTitle("MARKET");

        marketCard.addView(marketTitle);

        priceText =
                createValueText(
                        "Current Price: --"
                );

        marketCard.addView(priceText);

        root.addView(
                marketCard,
                fullWidthParams(18)
        );

        // SIGNAL CARD

        LinearLayout signalCard =
                createCard();

        signalCard.addView(
                createSectionTitle(
                        "AI SIGNAL"
                )
        );

        signalText =
                createSignalText(
                        "WAIT"
                );

        signalCard.addView(
                signalText,
                fullWidthParams(8)
        );

        trendText =
                createValueText(
                        "Trend: --"
                );

        rsiText =
                createValueText(
                        "RSI: --"
                );

        atrText =
                createValueText(
                        "ATR: --"
                );

        momentumText =
                createValueText(
                        "Momentum: --"
                );

        signalCard.addView(trendText);
        signalCard.addView(rsiText);
        signalCard.addView(atrText);
        signalCard.addView(momentumText);

        root.addView(
                signalCard,
                fullWidthParams(18)
        );

        // TRADE PLAN

        LinearLayout tradeCard =
                createCard();

        tradeCard.addView(
                createSectionTitle(
                        "TRADE PLAN"
                )
        );

        entryText =
                createTradeLine(
                        "ENTRY"
                );

        stopLossText =
                createTradeLine(
                        "STOP LOSS"
                );

        tp1Text =
                createTradeLine(
                        "TP1"
                );

        tp2Text =
                createTradeLine(
                        "TP2"
                );

        tp3Text =
                createTradeLine(
                        "TP3"
                );

        tradeCard.addView(entryText);
        tradeCard.addView(stopLossText);
        tradeCard.addView(tp1Text);
        tradeCard.addView(tp2Text);
        tradeCard.addView(tp3Text);

        root.addView(
                tradeCard,
                fullWidthParams(18)
        );

        // MULTI TIMEFRAME

        LinearLayout timeframeCard =
                createCard();

        timeframeCard.addView(
                createSectionTitle(
                        "MULTI-TIMEFRAME"
                )
        );

        HorizontalScrollView horizontal =
                new HorizontalScrollView(this);

        LinearLayout timeframeRow =
                new LinearLayout(this);

        timeframeRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        String[] labels = {
                "5M\nWAIT",
                "15M\nWAIT",
                "30M\nWAIT",
                "1H\nWAIT",
                "4H\nWAIT",
                "1D\nWAIT"
        };

        for (String label : labels) {

            TextView box =
                    createTimeframeBox(label);

            timeframeRow.addView(box);
        }

        horizontal.addView(timeframeRow);

        timeframeCard.addView(horizontal);

        root.addView(
                timeframeCard,
                fullWidthParams(18)
        );

        // SCAN BUTTON

        scanButton =
                new Button(this);

        scanButton.setText(
                "SCAN MARKET"
        );

        scanButton.setTextSize(15);

        scanButton.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        scanButton.setTextColor(
                Color.WHITE
        );

        scanButton.setAllCaps(false);

        scanButton.setOnClickListener(
                view -> requestCurrentSignal()
        );

        root.addView(
                scanButton,
                fullWidthParams(20)
        );

        // FOOTER

        TextView footer =
                createText(
                        "Live data only • No fabricated signals",
                        11,
                        Color.GRAY,
                        false
                );

        footer.setGravity(
                Gravity.CENTER
        );

        root.addView(
                footer,
                fullWidthParams(14)
        );

        setContentView(scrollView);
    }

    private void connectToMarketData() {

        String apiKey =
                AppConfig.getTwelveDataApiKey();

        MarketDataProvider provider =
                new TwelveDataProvider(apiKey);

        signalRepository =
                new SignalRepository(
                        provider,
                        new SignalRepository.SignalCallback() {

                            @Override
                            public void onSignal(
                                    Signal signal
                            ) {

                                runOnUiThread(
                                        () -> updateDashboard(signal)
                                );
                            }

                            @Override
                            public void onError(
                                    String message
                            ) {

                                runOnUiThread(
                                        () -> {

                                            connectionStatus.setText(
                                                    "●  DATA ERROR"
                                            );

                                            connectionStatus.setTextColor(
                                                    Color.rgb(
                                                            255,
                                                            90,
                                                            90
                                                    )
                                            );

                                            signalText.setText(
                                                    "WAIT"
                                            );

                                            clearTradeLevels();

                                            trendText.setText(
                                                    "Trend: " + message
                                            );
                                        }
                                );
                            }
                        }
                );

        requestCurrentSignal();
    }

    private void requestCurrentSignal() {

        if (signalRepository == null) {
            return;
        }

        connectionStatus.setText(
                "●  SCANNING "
                        + selectedMarket
                        + " • "
                        + selectedTimeframe
        );

        connectionStatus.setTextColor(
                Color.YELLOW
        );

        signalText.setText(
                "WAIT"
        );

        clearTradeLevels();

        signalRepository.requestSignal(
                selectedMarket,
                selectedTimeframe
        );
    }

    private void updateDashboard(
            Signal signal
    ) {

        if (signal == null) {
            return;
        }

        connectionStatus.setText(
                "●  LIVE MARKET CONNECTED"
        );

        connectionStatus.setTextColor(
                Color.rgb(
                        60,
                        220,
                        120
                )
        );

        double entry =
                signal.getEntry();

        if (!Double.isNaN(entry)) {

            priceText.setText(
                    "Current Price: "
                            + formatPrice(entry)
            );

        } else {

            priceText.setText(
                    "Current Price: --"
            );
        }

        signalText.setText(
                signal.getDirection().name()
        );

        if (signal.getDirection()
                == Signal.Direction.BUY) {

            signalText.setTextColor(
                    Color.rgb(
                            60,
                            230,
                            120
                    )
            );

        } else if (signal.getDirection()
                == Signal.Direction.SELL) {

            signalText.setTextColor(
                    Color.rgb(
                            255,
                            80,
                            80
                    )
            );

        } else {

            signalText.setTextColor(
                    Color.YELLOW
            );
        }

        trendText.setText(
                "Trend: "
                        + signal.getTrend()
        );

        if (!Double.isNaN(
                signal.getRsi()
        )) {

            rsiText.setText(
                    "RSI: "
                            + String.format(
                            Locale.US,
                            "%.2f",
                            signal.getRsi()
                    )
            );

        } else {

            rsiText.setText(
                    "RSI: --"
            );
        }

        if (!Double.isNaN(
                signal.getAtr()
        )) {

            atrText.setText(
                    "ATR: "
                            + formatPrice(
                            signal.getAtr()
                    )
            );

        } else {

            atrText.setText(
                    "ATR: --"
            );
        }

        momentumText.setText(
                "Timeframe: "
                        + signal.getTimeframe()
        );

        if (signal.getDirection()
                == Signal.Direction.BUY
                || signal.getDirection()
                == Signal.Direction.SELL) {

            entryText.setText(
                    "ENTRY       "
                            + formatPrice(
                            signal.getEntry()
                    )
            );

            stopLossText.setText(
                    "STOP LOSS   "
                            + formatPrice(
                            signal.getStopLoss()
                    )
            );

            tp1Text.setText(
                    "TP1         "
                            + formatPrice(
                            signal.getTakeProfit1()
                    )
            );

            tp2Text.setText(
                    "TP2         "
                            + formatPrice(
                            signal.getTakeProfit2()
                    )
            );

            tp3Text.setText(
                    "TP3         "
                            + formatPrice(
                            signal.getTakeProfit3()
                    )
            );

        } else {

            clearTradeLevels();
        }
    }

    private void clearTradeLevels() {

        entryText.setText(
                "ENTRY       --"
        );

        stopLossText.setText(
                "STOP LOSS   --"
        );

        tp1Text.setText(
                "TP1         --"
        );

        tp2Text.setText(
                "TP2         --"
        );

        tp3Text.setText(
                "TP3         --"
        );
    }

    private String formatPrice(
            double value
    ) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {

            return "--";
        }

        return String.format(
                Locale.US,
                "%.5f",
                value
        );
    }

    private TextView createText(
            String text,
            float size,
            int color,
            boolean bold
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return view;
    }

    private TextView createSectionTitle(
            String text
    ) {

        TextView view =
                createText(
                        text,
                        15,
                        Color.WHITE,
                        true
                );

        view.setPadding(
                0,
                0,
                0,
                12
        );

        return view;
    }

    private TextView createValueText(
            String text
    ) {

        TextView view =
                createText(
                        text,
                        15,
                        Color.LTGRAY,
                        false
                );

        view.setPadding(
                0,
                5,
                0,
                5
        );

        return view;
    }

    private TextView createTradeLine(
            String label
    ) {

        TextView view =
                createText(
                        label + "       --",
                        16,
                        Color.WHITE,
                        true
                );

        view.setPadding(
                0,
                8,
                0,
                8
        );

        return view;
    }

    private TextView createSignalText(
            String text
    ) {

        TextView view =
                createText(
                        text,
                        30,
                        Color.YELLOW,
                        true
                );

        view.setGravity(
                Gravity.CENTER
        );

        return view;
    }

    private TextView createTimeframeBox(
            String text
    ) {

        TextView view =
                createText(
                        text,
                        13,
                        Color.WHITE,
                        true
                );

        view.setGravity(
                Gravity.CENTER
        );

        view.setPadding(
                22,
                18,
                22,
                18
        );

        return view;
    }

    private Spinner createSpinner(
            String[] items
    ) {

        Spinner spinner =
                new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        items
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);

        return spinner;
    }

    private LinearLayout createCard() {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                20,
                20,
                20,
                20
        );

        card.setBackgroundColor(
                Color.rgb(
                        18,
                        25,
                        38
                )
        );

        return card;
    }

    private LinearLayout.LayoutParams
    fullWidthParams(int topMargin) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = topMargin;

        return params;
    }

    private LinearLayout.LayoutParams
    weightParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        params.setMargins(
                4,
                0,
                4,
                0
        );

        return params;
    }

    @Override
    protected void onDestroy() {

        if (signalRepository != null) {
            signalRepository.stop();
        }

        super.onDestroy();
    }
}