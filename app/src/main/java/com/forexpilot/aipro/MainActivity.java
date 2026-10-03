package com.forexpilot.aipro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainActivity extends Activity {

    private SignalRepository signalRepository;

    private TextView connectionStatus;
    private TextView clockText;
    private TextView dateText;
    private TextView marketStatusText;
    private TextView nextOpenText;

    private TextView marketName;
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

    private final int BACKGROUND =
            Color.rgb(7, 11, 18);

    private final int CARD =
            Color.rgb(15, 22, 33);

    private final int CARD_BORDER =
            Color.rgb(35, 47, 64);

    private final int WHITE =
            Color.rgb(245, 248, 252);

    private final int MUTED =
            Color.rgb(145, 158, 175);

    private final int GREEN =
            Color.rgb(40, 220, 125);

    private final int RED =
            Color.rgb(255, 80, 90);

    private final int YELLOW =
            Color.rgb(255, 195, 70);

    private final int BLUE =
            Color.rgb(70, 150, 255);

    private final Handler clockHandler =
            new Handler();

    private final Runnable clockRunnable =
            new Runnable() {
                @Override
                public void run() {

                    updateMarketClock();

                    clockHandler.postDelayed(
                            this,
                            1000
                    );
                }
            };

    private boolean lastMarketOpenState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildDashboard();

        updateMarketClock();

        clockHandler.post(
                clockRunnable
        );

        connectToMarketData();
    }

    private void buildDashboard() {

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setBackgroundColor(
                BACKGROUND
        );

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                18,
                24,
                18,
                32
        );

        scrollView.addView(root);

        // HEADER

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.VERTICAL
        );

        header.setGravity(
                Gravity.CENTER
        );

        TextView title =
                createText(
                        "FOREXPILOT",
                        30,
                        WHITE,
                        true
                );

        title.setGravity(
                Gravity.CENTER
        );

        header.addView(title);

        TextView pro =
                createText(
                        "AI PRO  •  SIGNAL INTELLIGENCE",
                        12,
                        BLUE,
                        true
                );

        pro.setGravity(
                Gravity.CENTER
        );

        header.addView(
                pro,
                params(6)
        );

        root.addView(
                header,
                params(0)
        );

        // LIVE CLOCK CARD

        LinearLayout clockCard =
                createCard();

        clockText =
                createText(
                        "--:--:-- SAST",
                        28,
                        WHITE,
                        true
                );

        clockText.setGravity(
                Gravity.CENTER
        );

        clockCard.addView(
                clockText
        );

        dateText =
                createText(
                        "LOADING DATE",
                        12,
                        MUTED,
                        true
                );

        dateText.setGravity(
                Gravity.CENTER
        );

        clockCard.addView(
                dateText,
                params(5)
        );

        marketStatusText =
                createText(
                        "●  CHECKING MARKET",
                        16,
                        YELLOW,
                        true
                );

        marketStatusText.setGravity(
                Gravity.CENTER
        );

        clockCard.addView(
                marketStatusText,
                params(12)
        );

        nextOpenText =
                createText(
                        "NEXT OPEN: --",
                        11,
                        MUTED,
                        false
                );

        nextOpenText.setGravity(
                Gravity.CENTER
        );

        clockCard.addView(
                nextOpenText,
                params(5)
        );

        root.addView(
                clockCard,
                params(18)
        );

        // MARKET SELECTOR

        LinearLayout marketSelector =
                createCard();

        marketSelector.addView(
                createSmallLabel(
                        "MARKET"
                )
        );

        marketSpinner =
                createSpinner(
                        markets
                );

        marketSpinner.setSelection(1);

        marketSelector.addView(
                marketSpinner,
                params(6)
        );

        marketSelector.addView(
                createSmallLabel(
                        "TIMEFRAME"
                ),
                params(14)
        );

        timeframeSpinner =
                createSpinner(
                        timeframes
                );

        timeframeSpinner.setSelection(1);

        marketSelector.addView(
                timeframeSpinner,
                params(6)
        );

        root.addView(
                marketSelector,
                params(14)
        );

        // LIVE MARKET CARD

        LinearLayout priceCard =
                createCard();

        priceCard.addView(
                createSmallLabel(
                        "LIVE MARKET"
                )
        );

        marketName =
                createText(
                        selectedMarket,
                        24,
                        WHITE,
                        true
                );

        marketName.setGravity(
                Gravity.CENTER
        );

        priceCard.addView(
                marketName,
                params(8)
        );

        priceText =
                createText(
                        "PRICE  --",
                        19,
                        BLUE,
                        true
                );

        priceText.setGravity(
                Gravity.CENTER
        );

        priceCard.addView(
                priceText,
                params(5)
        );

        root.addView(
                priceCard,
                params(14)
        );

        // SIGNAL CARD

        LinearLayout signalCard =
                createCard();

        signalCard.addView(
                createSmallLabel(
                        "AI SIGNAL"
                )
        );

        signalText =
                createText(
                        "WAIT",
                        38,
                        YELLOW,
                        true
                );

        signalText.setGravity(
                Gravity.CENTER
        );

        signalCard.addView(
                signalText,
                params(8)
        );

        TextView signalDescription =
                createText(
                        "Technical market analysis",
                        12,
                        MUTED,
                        false
                );

        signalDescription.setGravity(
                Gravity.CENTER
        );

        signalCard.addView(
                signalDescription,
                params(3)
        );

        root.addView(
                signalCard,
                params(14)
        );

        // MARKET ANALYSIS

        LinearLayout indicatorsCard =
                createCard();

        indicatorsCard.addView(
                createSmallLabel(
                        "MARKET ANALYSIS"
                )
        );

        LinearLayout row1 =
                createMetricRow();

        trendText =
                createMetric(
                        "TREND",
                        "--"
                );

        rsiText =
                createMetric(
                        "RSI",
                        "--"
                );

        row1.addView(
                trendText,
                weightParams()
        );

        row1.addView(
                rsiText,
                weightParams()
        );

        indicatorsCard.addView(
                row1,
                params(10)
        );

        LinearLayout row2 =
                createMetricRow();

        atrText =
                createMetric(
                        "ATR",
                        "--"
                );

        momentumText =
                createMetric(
                        "TIMEFRAME",
                        selectedTimeframe
                );

        row2.addView(
                atrText,
                weightParams()
        );

        row2.addView(
                momentumText,
                weightParams()
        );

        indicatorsCard.addView(
                row2,
                params(8)
        );

        root.addView(
                indicatorsCard,
                params(14)
        );

        // TRADE PLAN

        LinearLayout tradeCard =
                createCard();

        tradeCard.addView(
                createSmallLabel(
                        "TRADE PLAN"
                )
        );

        entryText =
                createTradeRow(
                        "ENTRY",
                        BLUE
                );

        stopLossText =
                createTradeRow(
                        "STOP LOSS",
                        RED
                );

        tp1Text =
                createTradeRow(
                        "TAKE PROFIT 1",
                        GREEN
                );

        tp2Text =
                createTradeRow(
                        "TAKE PROFIT 2",
                        GREEN
                );

        tp3Text =
                createTradeRow(
                        "TAKE PROFIT 3",
                        GREEN
                );

        tradeCard.addView(
                entryText,
                params(10)
        );

        tradeCard.addView(
                stopLossText,
                params(5)
        );

        tradeCard.addView(
                tp1Text,
                params(5)
        );

        tradeCard.addView(
                tp2Text,
                params(5)
        );

        tradeCard.addView(
                tp3Text,
                params(5)
        );

        root.addView(
                tradeCard,
                params(14)
        );

        // MULTI-TIMEFRAME

        LinearLayout mtfCard =
                createCard();

        mtfCard.addView(
                createSmallLabel(
                        "MULTI-TIMEFRAME"
                )
        );

        HorizontalScrollView horizontal =
                new HorizontalScrollView(
                        this
                );

        horizontal.setHorizontalScrollBarEnabled(
                false
        );

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
                    createTimeframeBox(
                            label
                    );

            timeframeRow.addView(box);
        }

        horizontal.addView(
                timeframeRow
        );

        mtfCard.addView(
                horizontal,
                params(10)
        );

        root.addView(
                mtfCard,
                params(14)
        );

        // SCAN BUTTON

        scanButton =
                new Button(this);

        scanButton.setText(
                "SCAN MARKET"
        );

        scanButton.setTextSize(
                15
        );

        scanButton.setTextColor(
                WHITE
        );

        scanButton.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        scanButton.setAllCaps(
                false
        );

        scanButton.setPadding(
                20,
                15,
                20,
                15
        );

        GradientDrawable buttonBackground =
                new GradientDrawable();

        buttonBackground.setColor(
                Color.rgb(25, 95, 180)
        );

        buttonBackground.setCornerRadius(
                18
        );

        scanButton.setBackground(
                buttonBackground
        );

        scanButton.setOnClickListener(
                view -> requestCurrentSignal()
        );

        root.addView(
                scanButton,
                params(18)
        );

        // FOOTER

        TextView footer =
                createText(
                        "FOREXPILOT AI PRO\nLIVE DATA • TECHNICAL ANALYSIS • REAL-TIME SIGNALS",
                        10,
                        MUTED,
                        false
                );

        footer.setGravity(
                Gravity.CENTER
        );

        root.addView(
                footer,
                params(18)
        );

        // MARKET SELECTION

        marketSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedMarket =
                                markets[position];

                        if (marketName != null) {
                            marketName.setText(
                                    selectedMarket
                            );
                        }

                        resetForNewSelection();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        // TIMEFRAME SELECTION

        timeframeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedTimeframe =
                                timeframes[position];

                        if (momentumText != null) {
                            momentumText.setText(
                                    "TIMEFRAME\n"
                                            + selectedTimeframe
                            );
                        }

                        resetForNewSelection();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        setContentView(
                scrollView
        );
    }

    private void updateMarketClock() {

        Instant now =
                Instant.now();

        boolean marketOpen =
                MarketClock.isForexOpen(
                        now
                );

        ZonedDateTime southAfricaTime =
                now.atZone(
                        ZoneId.of(
                                "Africa/Johannesburg"
                        )
                );

        String clock =
                southAfricaTime.format(
                        DateTimeFormatter.ofPattern(
                                "HH:mm:ss"
                        )
                );

        String date =
                southAfricaTime.format(
                        DateTimeFormatter.ofPattern(
                                "EEEE • dd MMMM yyyy",
                                Locale.ENGLISH
                        )
                );

        if (clockText != null) {

            clockText.setText(
                    clock + " SAST"
            );
        }

        if (dateText != null) {

            dateText.setText(
                    date.toUpperCase(
                            Locale.ENGLISH
                    )
            );
        }

        if (marketStatusText != null) {

            if (marketOpen) {

                marketStatusText.setText(
                        "●  FOREX MARKET OPEN"
                );

                marketStatusText.setTextColor(
                        GREEN
                );

            } else {

                marketStatusText.setText(
                        "●  FOREX MARKET CLOSED"
                );

                marketStatusText.setTextColor(
                        RED
                );
            }
        }

        if (nextOpenText != null) {

            if (marketOpen) {

                nextOpenText.setText(
                        "SIGNAL SCANNER • ACTIVE"
                );

                nextOpenText.setTextColor(
                        GREEN
                );

            } else {

                ZonedDateTime nextOpen =
                        MarketClock.getNextMarketOpen(
                                now
                        );

                ZonedDateTime nextOpenSast =
                        nextOpen.withZoneSameInstant(
                                ZoneId.of(
                                        "Africa/Johannesburg"
                                )
                        );

                String nextOpenFormatted =
                        nextOpenSast.format(
                                DateTimeFormatter.ofPattern(
                                        "EEE • dd MMM • HH:mm SAST",
                                        Locale.ENGLISH
                                )
                        );

                nextOpenText.setText(
                        "NEXT MARKET OPEN • "
                                + nextOpenFormatted
                );

                nextOpenText.setTextColor(
                        MUTED
                );
            }
        }

        /*
         * If the market has just changed from OPEN
         * to CLOSED, immediately clear the dashboard
         * so old signals cannot remain visible.
         */
        if (lastMarketOpenState
                && !marketOpen) {

            showMarketClosedState();
        }

        lastMarketOpenState =
                marketOpen;

        /*
         * Disable scanning while the market is closed.
         */
        if (scanButton != null) {

            scanButton.setEnabled(
                    marketOpen
            );

            if (marketOpen) {

                scanButton.setText(
                        "SCAN MARKET"
                );

            } else {

                scanButton.setText(
                        "MARKET CLOSED"
                );
            }
        }
    }

    private void showMarketClosedState() {

        if (connectionStatus != null) {

            connectionStatus.setText(
                    "●  MARKET CLOSED • NO NEW SIGNALS"
            );

            connectionStatus.setTextColor(
                    RED
            );
        }

        if (signalText != null) {

            signalText.setText(
                    "MARKET CLOSED"
            );

            signalText.setTextColor(
                    RED
            );
        }

        if (priceText != null) {

            priceText.setText(
                    "PRICE  --"
            );
        }

        if (trendText != null) {

            trendText.setText(
                    "TREND\n--"
            );
        }

        if (rsiText != null) {

            rsiText.setText(
                    "RSI\n--"
            );
        }

        if (atrText != null) {

            atrText.setText(
                    "ATR\n--"
            );
        }

        if (momentumText != null) {

            momentumText.setText(
                    "TIMEFRAME\n"
                            + selectedTimeframe
            );
        }

        clearTradeLevels();
    }

    private void connectToMarketData() {

        String apiKey =
                AppConfig.getTwelveDataApiKey();

        MarketDataProvider provider =
                new TwelveDataProvider(
                        apiKey
                );

        signalRepository =
                new SignalRepository(
                        provider,
                        new SignalRepository.SignalCallback() {

                            @Override
                            public void onSignal(
                                    Signal signal
                            ) {

                                runOnUiThread(
                                        () -> {

                                            /*
                                             * Never display a signal
                                             * while the market is closed.
                                             */
                                            if (!MarketClock.isForexOpen(
                                                    Instant.now()
                                            )) {

                                                showMarketClosedState();
                                                return;
                                            }

                                            updateDashboard(
                                                    signal
                                            );
                                        }
                                );
                            }

                            @Override
                            public void onError(
                                    String message
                            ) {

                                runOnUiThread(
                                        () -> {

                                            if (!MarketClock.isForexOpen(
                                                    Instant.now()
                                            )) {

                                                showMarketClosedState();
                                                return;
                                            }

                                            connectionStatus.setText(
                                                    "●  DATA ERROR • "
                                                            + selectedMarket
                                            );

                                            connectionStatus.setTextColor(
                                                    RED
                                            );

                                            signalText.setText(
                                                    "WAIT"
                                            );

                                            signalText.setTextColor(
                                                    YELLOW
                                            );

                                            clearTradeLevels();

                                            priceText.setText(
                                                    "PRICE  --"
                                            );

                                            trendText.setText(
                                                    "TREND\nERROR"
                                            );

                                            rsiText.setText(
                                                    "RSI\n--"
                                            );

                                            atrText.setText(
                                                    "ATR\n--"
                                            );

                                            momentumText.setText(
                                                    "TIMEFRAME\n"
                                                            + selectedTimeframe
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

        /*
         * HARD SAFETY GATE:
         * No signal request is sent while Forex
         * is closed.
         */
        if (!MarketClock.isForexOpen(
                Instant.now()
        )) {

            showMarketClosedState();
            return;
        }

        final String marketToScan =
                selectedMarket;

        final String timeframeToScan =
                selectedTimeframe;

        connectionStatus.setText(
                "●  SCANNING "
                        + marketToScan
                        + " • "
                        + timeframeToScan
        );

        connectionStatus.setTextColor(
                YELLOW
        );

        signalText.setText(
                "WAIT"
        );

        signalText.setTextColor(
                YELLOW
        );

        clearTradeLevels();

        priceText.setText(
                "PRICE  --"
        );

        signalRepository.requestSignal(
                marketToScan,
                timeframeToScan
        );
    }

    private void resetForNewSelection() {

        if (marketName != null) {

            marketName.setText(
                    selectedMarket
            );
        }

        if (priceText != null) {

            priceText.setText(
                    "PRICE  --"
            );
        }

        if (signalText != null) {

            signalText.setText(
                    "WAIT"
            );

            signalText.setTextColor(
                    YELLOW
            );
        }

        if (trendText != null) {

            trendText.setText(
                    "TREND\n--"
            );
        }

        if (rsiText != null) {

            rsiText.setText(
                    "RSI\n--"
            );
        }

        if (atrText != null) {

            atrText.setText(
                    "ATR\n--"
            );
        }

        if (momentumText != null) {

            momentumText.setText(
                    "TIMEFRAME\n"
                            + selectedTimeframe
            );
        }

        clearTradeLevels();

        if (!MarketClock.isForexOpen(
                Instant.now()
        )) {

            showMarketClosedState();
            return;
        }

        if (connectionStatus != null) {

            connectionStatus.setText(
                    "●  READY "
                            + selectedMarket
                            + " • "
                            + selectedTimeframe
            );

            connectionStatus.setTextColor(
                    BLUE
            );
        }
    }

    private void updateDashboard(
            Signal signal
    ) {

        if (signal == null) {
            return;
        }

        /*
         * Protect against an old response arriving
         * after the user changed the selected pair.
         */
        if (!selectedMarket.equals(
                signal.getSymbol()
        )) {

            return;
        }

        if (!selectedTimeframe.equals(
                signal.getTimeframe()
        )) {

            return;
        }

        if (marketName != null) {

            marketName.setText(
                    signal.getSymbol()
            );
        }

        connectionStatus.setText(
                "●  LIVE DATA CONNECTED  •  "
                        + signal.getSymbol()
                        + " • "
                        + signal.getTimeframe()
        );

        connectionStatus.setTextColor(
                GREEN
        );

        if (!Double.isNaN(
                signal.getEntry()
        )) {

            priceText.setText(
                    "PRICE  "
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getEntry()
                    )
            );

        } else {

            priceText.setText(
                    "PRICE  --"
            );
        }

        signalText.setText(
                signal.getDirection()
                        .name()
        );

        if (signal.getDirection()
                == Signal.Direction.BUY) {

            signalText.setTextColor(
                    GREEN
            );

        } else if (
                signal.getDirection()
                        == Signal.Direction.SELL
        ) {

            signalText.setTextColor(
                    RED
            );

        } else {

            signalText.setTextColor(
                    YELLOW
            );
        }

        trendText.setText(
                "TREND\n"
                        + signal.getTrend()
        );

        if (!Double.isNaN(
                signal.getRsi()
        )) {

            rsiText.setText(
                    "RSI\n"
                            + String.format(
                            Locale.US,
                            "%.2f",
                            signal.getRsi()
                    )
            );

        } else {

            rsiText.setText(
                    "RSI\n--"
            );
        }

        if (!Double.isNaN(
                signal.getAtr()
        )) {

            atrText.setText(
                    "ATR\n"
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getAtr()
                    )
            );

        } else {

            atrText.setText(
                    "ATR\n--"
            );
        }

        momentumText.setText(
                "TIMEFRAME\n"
                        + signal.getTimeframe()
        );

        if (signal.getDirection()
                == Signal.Direction.BUY
                || signal.getDirection()
                == Signal.Direction.SELL) {

            entryText.setText(
                    "ENTRY                         "
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getEntry()
                    )
            );

            stopLossText.setText(
                    "STOP LOSS                  "
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getStopLoss()
                    )
            );

            tp1Text.setText(
                    "TAKE PROFIT 1          "
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getTakeProfit1()
                    )
            );

            tp2Text.setText(
                    "TAKE PROFIT 2          "
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getTakeProfit2()
                    )
            );

            tp3Text.setText(
                    "TAKE PROFIT 3          "
                            + formatPrice(
                            signal.getSymbol(),
                            signal.getTakeProfit3()
                    )
            );

        } else {

            clearTradeLevels();
        }
    }

    private void clearTradeLevels() {

        if (entryText != null) {

            entryText.setText(
                    "ENTRY                         --"
            );
        }

        if (stopLossText != null) {

            stopLossText.setText(
                    "STOP LOSS                  --"
            );
        }

        if (tp1Text != null) {

            tp1Text.setText(
                    "TAKE PROFIT 1          --"
            );
        }

        if (tp2Text != null) {

            tp2Text.setText(
                    "TAKE PROFIT 2          --"
            );
        }

        if (tp3Text != null) {

            tp3Text.setText(
                    "TAKE PROFIT 3          --"
            );
        }
    }

    private String formatPrice(
            String symbol,
            double value
    ) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {

            return "--";
        }

        if ("USD/JPY".equalsIgnoreCase(
                symbol
        )) {

            return String.format(
                    Locale.US,
                    "%.3f",
                    value
            );
        }

        if ("XAU/USD".equalsIgnoreCase(
                symbol
        )) {

            return String.format(
                    Locale.US,
                    "%.2f",
                    value
            );
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

        view.setText(
                text
        );

        view.setTextSize(
                size
        );

        view.setTextColor(
                color
        );

        if (bold) {

            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return view;
    }

    private TextView createSmallLabel(
            String text
    ) {

        return createText(
                text,
                11,
                MUTED,
                true
        );
    }

    private Spinner createSpinner(
            String[] values
    ) {

        Spinner spinner =
                new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(
                adapter
        );

        return spinner;
    }

    private TextView createMetric(
            String label,
            String value
    ) {

        TextView view =
                createText(
                        label + "\n" + value,
                        14,
                        WHITE,
                        true
                );

        view.setGravity(
                Gravity.CENTER
        );

        view.setPadding(
                12,
                15,
                12,
                15
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(10, 16, 25)
        );

        background.setStroke(
                1,
                CARD_BORDER
        );

        background.setCornerRadius(
                14
        );

        view.setBackground(
                background
        );

        return view;
    }

    private TextView createTradeRow(
            String label,
            int color
    ) {

        TextView view =
                createText(
                        label
                                + "                         --",
                        14,
                        color,
                        true
                );

        view.setPadding(
                14,
                14,
                14,
                14
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(10, 16, 25)
        );

        background.setCornerRadius(
                12
        );

        view.setBackground(
                background
        );

        return view;
    }

    private TextView createTimeframeBox(
            String text
    ) {

        TextView view =
                createText(
                        text,
                        12,
                        WHITE,
                        true
                );

        view.setGravity(
                Gravity.CENTER
        );

        view.setPadding(
                20,
                14,
                20,
                14
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(10, 16, 25)
        );

        background.setStroke(
                1,
                CARD_BORDER
        );

        background.setCornerRadius(
                14
        );

        view.setBackground(
                background
        );

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        boxParams.setMargins(
                0,
                0,
                8,
                0
        );

        view.setLayoutParams(
                boxParams
        );

        return view;
    }

    private LinearLayout createMetricRow() {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        return row;
    }

    private LinearLayout createCard() {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                18,
                18,
                18,
                18
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                CARD
        );

        background.setStroke(
                1,
                CARD_BORDER
        );

        background.setCornerRadius(
                18
        );

        card.setBackground(
                background
        );

        return card;
    }

    private LinearLayout.LayoutParams params(
            int topMargin
    ) {

        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        layoutParams.topMargin =
                topMargin;

        return layoutParams;
    }

    private LinearLayout.LayoutParams weightParams() {

        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        layoutParams.setMargins(
                4,
                0,
                4,
                0
        );

        return layoutParams;
    }

    @Override
    protected void onDestroy() {

        clockHandler.removeCallbacks(
                clockRunnable
        );

        if (signalRepository != null) {
            signalRepository.stop();
        }

        super.onDestroy();
    }
}