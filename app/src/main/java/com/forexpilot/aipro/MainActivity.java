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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
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

    private TextView sydneyStatusText;
    private TextView tokyoStatusText;
    private TextView londonStatusText;
    private TextView newYorkStatusText;

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

        // =========================================================
        // HEADER
        // =========================================================

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

        // =========================================================
        // LIVE CLOCK + MARKET STATUS
        // =========================================================

        LinearLayout clockCard =
                createCard();

        TextView clockLabel =
                createSmallLabel(
                        "LOCAL MARKET CLOCK"
                );

        clockCard.addView(
                clockLabel
        );

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
                clockText,
                params(8)
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
                params(4)
        );

        marketStatusText =
                createText(
                        "●  CHECKING FOREX MARKET",
                        17,
                        YELLOW,
                        true
                );

        marketStatusText.setGravity(
                Gravity.CENTER
        );

        clockCard.addView(
                marketStatusText,
                params(14)
        );

        nextOpenText =
                createText(
                        "CHECKING MARKET HOURS...",
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

        // =========================================================
        // CONNECTION STATUS
        // =========================================================

        LinearLayout connectionCard =
                createCard();

        connectionStatus =
                createText(
                        "●  CHECKING LIVE DATA...",
                        12,
                        YELLOW,
                        true
                );

        connectionStatus.setGravity(
                Gravity.CENTER
        );

        connectionCard.addView(
                connectionStatus
        );

        root.addView(
                connectionCard,
                params(12)
        );

        // =========================================================
        // FOREX SESSIONS
        // =========================================================

        LinearLayout sessionsCard =
                createCard();

        sessionsCard.addView(
                createSmallLabel(
                        "FOREX SESSIONS • SAST"
                )
        );

        TextView sessionDescription =
                createText(
                        "Live global trading session monitor",
                        11,
                        MUTED,
                        false
                );

        sessionsCard.addView(
                sessionDescription,
                params(4)
        );

        sydneyStatusText =
                createSessionRow(
                        "SYDNEY",
                        "Australia/Sydney"
                );

        tokyoStatusText =
                createSessionRow(
                        "TOKYO",
                        "Asia/Tokyo"
                );

        londonStatusText =
                createSessionRow(
                        "LONDON",
                        "Europe/London"
                );

        newYorkStatusText =
                createSessionRow(
                        "NEW YORK",
                        "America/New_York"
                );

        sessionsCard.addView(
                sydneyStatusText,
                params(10)
        );

        sessionsCard.addView(
                tokyoStatusText,
                params(6)
        );

        sessionsCard.addView(
                londonStatusText,
                params(6)
        );

        sessionsCard.addView(
                newYorkStatusText,
                params(6)
        );

        root.addView(
                sessionsCard,
                params(14)
        );

        // =========================================================
        // MARKET SELECTOR
        // =========================================================

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

        // =========================================================
        // LIVE MARKET
        // =========================================================

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

        // =========================================================
        // AI SIGNAL
        // =========================================================

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

        // =========================================================
        // MARKET ANALYSIS
        // =========================================================

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

        // =========================================================
        // TRADE PLAN
        // =========================================================

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

        // =========================================================
        // MULTI-TIMEFRAME
        // =========================================================

        LinearLayout mtfCard =
                createCard();

        mtfCard.addView(
                createSmallLabel(
                        "MULTI-TIMEFRAME"
                )
        );

        HorizontalScrollView horizontal =
                new HorizontalScrollView(this);

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

            timeframeRow.addView(
                    box
            );
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

        // =========================================================
        // SCAN BUTTON
        // =========================================================

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

        // =========================================================
        // FOOTER
        // =========================================================

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

        // =========================================================
        // MARKET SELECTION
        // =========================================================

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

        // =========================================================
        // TIMEFRAME SELECTION
        // =========================================================

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

    // =============================================================
    // MARKET CLOCK
    // =============================================================

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

        // Update all four trading sessions.
        updateSessionDisplay(
                sydneyStatusText,
                "Australia/Sydney",
                8,
                17
        );

        updateSessionDisplay(
                tokyoStatusText,
                "Asia/Tokyo",
                9,
                18
        );

        updateSessionDisplay(
                londonStatusText,
                "Europe/London",
                8,
                17
        );

        updateSessionDisplay(
                newYorkStatusText,
                "America/New_York",
                8,
                17
        );

        if (lastMarketOpenState
                && !marketOpen) {

            showMarketClosedState();
        }

        lastMarketOpenState =
                marketOpen;

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

    // =============================================================
    // SESSION STATUS
    // =============================================================

    private void updateSessionDisplay(
            TextView view,
            String zoneName,
            int openHour,
            int closeHour
    ) {

        if (view == null) {
            return;
        }

        Instant now =
                Instant.now();

        ZoneId zone =
                ZoneId.of(
                        zoneName
                );

        ZonedDateTime localTime =
                now.atZone(zone);

        DayOfWeek day =
                localTime.getDayOfWeek();

        LocalTime time =
                localTime.toLocalTime();

        boolean weekday =
                day != DayOfWeek.SATURDAY
                        && day != DayOfWeek.SUNDAY;

        boolean open =
                weekday
                        && !time.isBefore(
                        LocalTime.of(
                                openHour,
                                0
                        )
                )
                        && time.isBefore(
                        LocalTime.of(
                                closeHour,
                                0
                        )
                );

        String sessionName =
                getSessionName(
                        zoneName
                );

        String sastTime =
                now.atZone(
                        ZoneId.of(
                                "Africa/Johannesburg"
                        )
                )
                .format(
                        DateTimeFormatter.ofPattern(
                                "HH:mm"
                        )
                );

        if (open) {

            view.setText(
                    "●  "
                            + sessionName
                            + "     OPEN"
                            + "\n"
                            + "    Local "
                            + localTime.format(
                            DateTimeFormatter.ofPattern(
                                    "HH:mm"
                            )
                    )
                            + "     •     SAST "
                            + sastTime
            );

            view.setTextColor(
                    GREEN
            );

        } else {

            view.setText(
                    "●  "
                            + sessionName
                            + "     CLOSED"
                            + "\n"
                            + "    Local "
                            + localTime.format(
                            DateTimeFormatter.ofPattern(
                                    "HH:mm"
                            )
                    )
                            + "     •     SAST "
                            + sastTime
            );

            view.setTextColor(
                    MUTED
            );
        }
    }

    private String getSessionName(
            String zoneName
    ) {

        if ("Australia/Sydney".equals(
                zoneName
        )) {

            return "SYDNEY";
        }

        if ("Asia/Tokyo".equals(
                zoneName
        )) {

            return "TOKYO";
        }

        if ("Europe/London".equals(
                zoneName
        )) {

            return "LONDON";
        }

        if ("America/New_York".equals(
                zoneName
        )) {

            return "NEW YORK";
        }

        return "SESSION";
    }

    // =============================================================
    // MARKET CLOSED STATE
    // =============================================================

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

    // =============================================================
    // DATA CONNECTION
    // =============================================================

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

    // =============================================================
    // REQUEST SIGNAL
    // =============================================================

    private void requestCurrentSignal() {

        if (signalRepository == null) {
            return;
        }

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

    // =============================================================
    // RESET SELECTION
    // =============================================================

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

    // =============================================================
    // UPDATE DASHBOARD
    // =============================================================

    private void updateDashboard(
            Signal signal
    ) {

        if (signal == null) {
            return;
        }

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

    // =============================================================
    // CLEAR TRADE LEVELS
    // =============================================================

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

    // =============================================================
    // PRICE FORMAT
    // =============================================================

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

    // =============================================================
    // UI HELPERS
    // =============================================================

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

    // =============================================================
    // SESSION ROW
    // =============================================================

    private TextView createSessionRow(
            String sessionName,
            String zoneName
    ) {

        TextView view =
                createText(
                        sessionName
                                + "     CHECKING...",
                        13,
                        MUTED,
                        true
                );

        view.setPadding(
                14,
                13,
                14,
                13
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
                12
        );

        view.setBackground(
                background
        );

        return view;
    }

    // =============================================================
    // SPINNER
    // =============================================================

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

    // =============================================================
    // METRIC
    // =============================================================

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

    // =============================================================
    // TRADE ROW
    // =============================================================

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

    // =============================================================
    // TIMEFRAME BOX
    // =============================================================

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

    // =============================================================
    // METRIC ROW
    // =============================================================

    private LinearLayout createMetricRow() {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        return row;
    }

    // =============================================================
    // CARD
    // =============================================================

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

    // =============================================================
    // LAYOUT PARAMS
    // =============================================================

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

    // =============================================================
    // DESTROY
    // =============================================================

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