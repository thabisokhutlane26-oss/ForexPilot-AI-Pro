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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
  
    private ForexChartView forexChartView;

    private SignalRepository signalRepository;
    private MarketDataProvider marketDataProvider;

    private TextView connectionStatus;

    private TextView clockText;
    private TextView dateText;
    private TextView forexStatusText;
    private TextView nextMarketText;

    private TextView sydneySessionText;
    private TextView tokyoSessionText;
    private TextView londonSessionText;
    private TextView newYorkSessionText;

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

    private TextView mtf5mText;
    private TextView mtf15mText;
    private TextView mtf30mText;
    private TextView mtf1hText;
    private TextView mtf4hText;
    private TextView mtf1dText;

    private TextView mtfConfluenceText;

    private Spinner marketSpinner;
    private Spinner timeframeSpinner;
    private Button scanButton;

    private String selectedMarket = "EUR/USD";
    private String selectedTimeframe = "15M";

    private boolean lastForexOpen = false;

    private boolean mtfScanning = false;
    private int mtfRequestIndex = 0;

    /*
     * Stores the primary timeframe signal until the
     * six-timeframe confluence has been completed.
     */
    private Signal pendingPrimarySignal;

    private final List<MultiTimeframeEngine.TimeframeResult> mtfResults =
            new ArrayList<>();

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

                    updateTimeAndSessions();

                    clockHandler.postDelayed(
                            this,
                            1000
                    );
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildDashboard();

        updateTimeAndSessions();

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

        connectionStatus =
                createText(
                        "●  CONNECTING",
                        13,
                        YELLOW,
                        true
                );

        connectionStatus.setGravity(
                Gravity.CENTER
        );

        header.addView(
                connectionStatus,
                params(14)
        );

        root.addView(
                header,
                params(0)
        );

        // =========================================================
        // CLOCK + FOREX MARKET STATUS
        // =========================================================

        LinearLayout marketClockCard =
                createCard();

        marketClockCard.addView(
                createSmallLabel(
                        "MARKET CLOCK"
                )
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

        marketClockCard.addView(
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

        marketClockCard.addView(
                dateText,
                params(4)
        );

        forexStatusText =
                createText(
                        "●  CHECKING FOREX MARKET",
                        17,
                        YELLOW,
                        true
                );

        forexStatusText.setGravity(
                Gravity.CENTER
        );

        marketClockCard.addView(
                forexStatusText,
                params(14)
        );

        nextMarketText =
                createText(
                        "CHECKING MARKET HOURS...",
                        11,
                        MUTED,
                        false
                );

        nextMarketText.setGravity(
                Gravity.CENTER
        );

        marketClockCard.addView(
                nextMarketText,
                params(5)
        );

        root.addView(
                marketClockCard,
                params(18)
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

        TextView sessionInfo =
                createText(
                        "Global session monitor",
                        11,
                        MUTED,
                        false
                );

        sessionsCard.addView(
                sessionInfo,
                params(4)
        );

        sydneySessionText =
                createSessionRow(
                        "SYDNEY"
                );

        tokyoSessionText =
                createSessionRow(
                        "TOKYO"
                );

        londonSessionText =
                createSessionRow(
                        "LONDON"
                );

        newYorkSessionText =
                createSessionRow(
                        "NEW YORK"
                );

        sessionsCard.addView(
                sydneySessionText,
                params(10)
        );

        sessionsCard.addView(
                tokyoSessionText,
                params(6)
        );

        sessionsCard.addView(
                londonSessionText,
                params(6)
        );

        sessionsCard.addView(
                newYorkSessionText,
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
        // LIVE MARKET CARD
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
        // SIGNAL CARD
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
                        "MULTI-TIMEFRAME • REAL-TIME CONFLUENCE"
                )
        );

        TextView mtfInfo =
                createText(
                        "6 timeframe technical analysis",
                        11,
                        MUTED,
                        false
                );

        mtfCard.addView(
                mtfInfo,
                params(4)
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

        mtf5mText =
                createTimeframeBox(
                        "5M\nWAIT"
                );

        mtf15mText =
                createTimeframeBox(
                        "15M\nWAIT"
                );

        mtf30mText =
                createTimeframeBox(
                        "30M\nWAIT"
                );

        mtf1hText =
                createTimeframeBox(
                        "1H\nWAIT"
                );

        mtf4hText =
                createTimeframeBox(
                        "4H\nWAIT"
                );

        mtf1dText =
                createTimeframeBox(
                        "1D\nWAIT"
                );

        timeframeRow.addView(
                mtf5mText
        );

        timeframeRow.addView(
                mtf15mText
        );

        timeframeRow.addView(
                mtf30mText
        );

        timeframeRow.addView(
                mtf1hText
        );

        timeframeRow.addView(
                mtf4hText
        );

        timeframeRow.addView(
                mtf1dText
        );

        horizontal.addView(
                timeframeRow
        );

        mtfCard.addView(
                horizontal,
                params(10)
        );

        // =========================================================
        // REAL MTF CONFLUENCE RESULT
        // =========================================================

        mtfConfluenceText =
                createText(
                        "CONFLUENCE\nWAITING FOR ANALYSIS",
                        14,
                        YELLOW,
                        true
                );

        mtfConfluenceText.setGravity(
                Gravity.CENTER
        );

        mtfConfluenceText.setPadding(
                14,
                16,
                14,
                16
        );

        GradientDrawable confluenceBackground =
                new GradientDrawable();

        confluenceBackground.setColor(
                Color.rgb(10, 16, 25)
        );

        confluenceBackground.setStroke(
                1,
                CARD_BORDER
        );

        confluenceBackground.setCornerRadius(
                14
        );

        mtfConfluenceText.setBackground(
                confluenceBackground
        );

        mtfCard.addView(
                mtfConfluenceText,
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
    // CLOCK + FOREX STATUS + SESSIONS
    // =============================================================

    private void updateTimeAndSessions() {

        Calendar now =
                Calendar.getInstance();

        SimpleDateFormat timeFormat =
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.ENGLISH
                );

        timeFormat.setTimeZone(
                TimeZone.getTimeZone(
                        "Africa/Johannesburg"
                )
        );

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "EEEE • dd MMMM yyyy",
                        Locale.ENGLISH
                );

        dateFormat.setTimeZone(
                TimeZone.getTimeZone(
                        "Africa/Johannesburg"
                )
        );

        if (clockText != null) {

            clockText.setText(
                    timeFormat.format(
                            now.getTime()
                    )
                            + " SAST"
            );
        }

        if (dateText != null) {

            dateText.setText(
                    dateFormat.format(
                            now.getTime()
                    )
                            .toUpperCase(
                                    Locale.ENGLISH
                            )
            );
        }

        boolean forexOpen =
                isForexMarketOpen(
                        now.getTime()
                );

        if (forexStatusText != null) {

            if (forexOpen) {

                forexStatusText.setText(
                        "●  FOREX MARKET OPEN"
                );

                forexStatusText.setTextColor(
                        GREEN
                );

            } else {

                forexStatusText.setText(
                        "●  FOREX MARKET CLOSED"
                );

                forexStatusText.setTextColor(
                        RED
                );
            }
        }

        if (nextMarketText != null) {

            if (forexOpen) {

                nextMarketText.setText(
                        "SIGNAL SCANNER • ACTIVE"
                );

                nextMarketText.setTextColor(
                        GREEN
                );

            } else {

                nextMarketText.setText(
                        "MARKET CLOSED • NO NEW SIGNALS"
                );

                nextMarketText.setTextColor(
                        MUTED
                );
            }
        }

        if (scanButton != null) {

            scanButton.setEnabled(
                    forexOpen
            );

            if (forexOpen) {

                scanButton.setAlpha(
                        1.0f
                );

            } else {

                scanButton.setAlpha(
                        0.45f
                );
            }
        }

        if (!forexOpen) {

            mtfScanning = false;

            showMarketClosedState();

        } else if (!lastForexOpen) {

            if (signalRepository != null) {
                requestCurrentSignal();
            }
        }

        lastForexOpen =
                forexOpen;

        updateSession(
                sydneySessionText,
                "SYDNEY",
                "Australia/Sydney",
                8,
                17
        );

        updateSession(
                tokyoSessionText,
                "TOKYO",
                "Asia/Tokyo",
                9,
                18
        );

        updateSession(
                londonSessionText,
                "LONDON",
                "Europe/London",
                8,
                17
        );

        updateSession(
                newYorkSessionText,
                "NEW YORK",
                "America/New_York",
                8,
                17
        );
    }

    private void showMarketClosedState() {

        pendingPrimarySignal = null;
        mtfResults.clear();

        if (signalText != null) {

            signalText.setText(
                    "WAIT"
            );

            signalText.setTextColor(
                    YELLOW
            );
        }

        if (connectionStatus != null) {

            connectionStatus.setText(
                    "●  MARKET CLOSED • SIGNALS LOCKED"
            );

            connectionStatus.setTextColor(
                    MUTED
            );
        }

        if (priceText != null) {

            priceText.setText(
                    "PRICE  --"
            );
        }

        if (trendText != null) {

            trendText.setText(
                    "TREND\nMARKET CLOSED"
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

        resetMtfBoxes(
                "MARKET CLOSED"
        );

        if (mtfConfluenceText != null) {

            mtfConfluenceText.setText(
                    "CONFLUENCE\nMARKET CLOSED"
            );

            mtfConfluenceText.setTextColor(
                    MUTED
            );
        }
    }

    private boolean isForexMarketOpen(
            Date date
    ) {

        Calendar newYork =
                Calendar.getInstance(
                        TimeZone.getTimeZone(
                                "America/New_York"
                        ),
                        Locale.ENGLISH
                );

        newYork.setTime(
                date
        );

        int day =
                newYork.get(
                        Calendar.DAY_OF_WEEK
                );

        int hour =
                newYork.get(
                        Calendar.HOUR_OF_DAY
                );

        int minute =
                newYork.get(
                        Calendar.MINUTE
                );

        int totalMinutes =
                (hour * 60) + minute;

        if (day == Calendar.SATURDAY) {
            return false;
        }

        if (day == Calendar.SUNDAY) {

            return totalMinutes >= 17 * 60;
        }

        if (day == Calendar.FRIDAY) {

            return totalMinutes < 17 * 60;
        }

        return true;
    }

    private void updateSession(
            TextView view,
            String name,
            String timeZoneId,
            int openHour,
            int closeHour
    ) {

        if (view == null) {
            return;
        }

        Calendar local =
                Calendar.getInstance(
                        TimeZone.getTimeZone(
                                timeZoneId
                        ),
                        Locale.ENGLISH
                );

        Date now =
                new Date();

        local.setTime(
                now
        );

        int day =
                local.get(
                        Calendar.DAY_OF_WEEK
                );

        int hour =
                local.get(
                        Calendar.HOUR_OF_DAY
                );

        int minute =
                local.get(
                        Calendar.MINUTE
                );

        boolean weekend =
                day == Calendar.SATURDAY
                        || day == Calendar.SUNDAY;

        int totalMinutes =
                (hour * 60) + minute;

        boolean open =
                !weekend
                        && totalMinutes >= openHour * 60
                        && totalMinutes < closeHour * 60;

        SimpleDateFormat localTimeFormat =
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.ENGLISH
                );

        localTimeFormat.setTimeZone(
                TimeZone.getTimeZone(
                        timeZoneId
                )
        );

        SimpleDateFormat sastTimeFormat =
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.ENGLISH
                );

        sastTimeFormat.setTimeZone(
                TimeZone.getTimeZone(
                        "Africa/Johannesburg"
                )
        );

        String localTime =
                localTimeFormat.format(
                        now
                );

        String sastTime =
                sastTimeFormat.format(
                        now
                );

        if (open) {

            view.setText(
                    "●  "
                            + name
                            + "     OPEN"
                            + "\n"
                            + "    Local "
                            + localTime
                            + "     •     SAST "
                            + sastTime
            );

            view.setTextColor(
                    GREEN
            );

        } else {

            view.setText(
                    "●  "
                            + name
                            + "     CLOSED"
                            + "\n"
                            + "    Local "
                            + localTime
                            + "     •     SAST "
                            + sastTime
            );

            view.setTextColor(
                    MUTED
            );
        }
    }

    // =============================================================
    // DATA CONNECTION
    // =============================================================

    private void connectToMarketData() {

        String apiKey =
                AppConfig.getTwelveDataApiKey();

        marketDataProvider =
                new TwelveDataProvider(
                        apiKey
                );

        signalRepository =
                new SignalRepository(
                        marketDataProvider,
                        new SignalRepository.SignalCallback() {

                            @Override
                            public void onSignal(
                                    Signal signal
                            ) {

                                runOnUiThread(
                                        () -> {

                                            if (!isForexMarketOpen(
                                                    new Date()
                                            )) {

                                                showMarketClosedState();

                                                return;
                                            }

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

                                            /*
                                             * Do NOT immediately display the
                                             * primary BUY/SELL.
                                             *
                                             * Store it until all six
                                             * timeframes have been analysed.
                                             */
                                            pendingPrimarySignal =
                                                    signal;

                                            if (mtfScanning) {

                                                connectionStatus.setText(
                                                        "●  PRIMARY SIGNAL RECEIVED • CHECKING MTF"
                                                );

                                                connectionStatus.setTextColor(
                                                        YELLOW
                                                );

                                            } else {

                                                applyFinalMtfDecision();
                                            }
                                        }
                                );
                            }

                            @Override
                            public void onError(
                                    String message
                            ) {

                                runOnUiThread(
                                        () -> {

                                            if (!isForexMarketOpen(
                                                    new Date()
                                            )) {

                                                showMarketClosedState();

                                                return;
                                            }

                                            pendingPrimarySignal =
                                                    null;

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

        if (isForexMarketOpen(
                new Date()
        )) {

            requestCurrentSignal();

        } else {

            showMarketClosedState();
        }
    }

    // =============================================================
    // PRIMARY SIGNAL
    // =============================================================

    private void requestCurrentSignal() {

        if (signalRepository == null) {
            return;
        }

        if (!isForexMarketOpen(
                new Date()
        )) {

            showMarketClosedState();

            return;
        }

        final String marketToScan =
                selectedMarket;

        final String timeframeToScan =
                selectedTimeframe;

        pendingPrimarySignal =
                null;

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

        requestMultiTimeframeAnalysis(
                marketToScan
        );
    }

    // =============================================================
    // MULTI-TIMEFRAME ANALYSIS
    // =============================================================

    private void requestMultiTimeframeAnalysis(
            String market
    ) {

        if (marketDataProvider == null) {
            return;
        }

        if (!isForexMarketOpen(
                new Date()
        )) {

            resetMtfBoxes(
                    "MARKET CLOSED"
            );

            if (mtfConfluenceText != null) {

                mtfConfluenceText.setText(
                        "CONFLUENCE\nMARKET CLOSED"
                );

                mtfConfluenceText.setTextColor(
                        MUTED
                );
            }

            return;
        }

        mtfScanning = true;
        mtfRequestIndex = 0;

        mtfResults.clear();

        if (mtfConfluenceText != null) {

            mtfConfluenceText.setText(
                    "CONFLUENCE\nSCANNING 6 TIMEFRAMES..."
            );

            mtfConfluenceText.setTextColor(
                    YELLOW
            );
        }

        resetMtfBoxes(
                "LOADING"
        );

        requestNextMtfTimeframe(
                market
        );
    }

    private void requestNextMtfTimeframe(
            String market
    ) {

        if (!mtfScanning) {
            return;
        }

        if (!isForexMarketOpen(
                new Date()
        )) {

            mtfScanning = false;

            resetMtfBoxes(
                    "MARKET CLOSED"
            );

            return;
        }

        if (!market.equals(
                selectedMarket
        )) {

            mtfScanning = false;

            return;
        }

        if (mtfRequestIndex >= timeframes.length) {

            mtfScanning = false;

            updateMtfConfluenceStatus();

            /*
             * Now that all six timeframes have been analysed,
             * allow the MTF engine to make the final decision.
             */
            applyFinalMtfDecision();

            return;
        }

        final int currentIndex =
                mtfRequestIndex;

        final String timeframe =
                timeframes[currentIndex];

        updateMtfBox(
                timeframe,
                "LOADING",
                YELLOW
        );

        marketDataProvider.requestCandles(
                market,
                timeframe,
                new MarketDataManager.MarketDataCallback() {

                    @Override
                    public void onCandlesReceived(
                            List<Candle> candles
                    ) {

                        runOnUiThread(
                                () -> {

                                    if (!mtfScanning) {
                                        return;
                                    }

                                    if (!isForexMarketOpen(
                                            new Date()
                                    )) {

                                        mtfScanning = false;

                                        resetMtfBoxes(
                                                "MARKET CLOSED"
                                        );

                                        return;
                                    }

                                    if (!market.equals(
                                            selectedMarket
                                    )) {

                                        mtfScanning = false;

                                        return;
                                    }

                                    MultiTimeframeEngine.TimeframeResult result =
                                            MultiTimeframeEngine.analyzeTimeframe(
                                                    market,
                                                    timeframe,
                                                    candles
                                            );

                                    displayMtfResult(
                                            result
                                    );

                                    mtfRequestIndex++;

                                    requestNextMtfTimeframe(
                                            market
                                    );
                                }
                        );
                    }

                    @Override
                    public void onPriceReceived(
                            double price
                    ) {
                        // Primary signal controls the main price display.
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    if (!mtfScanning) {
                                        return;
                                    }

                                    if (!isForexMarketOpen(
                                            new Date()
                                    )) {

                                        mtfScanning = false;

                                        resetMtfBoxes(
                                                "MARKET CLOSED"
                                        );

                                        return;
                                    }

                                    if (!market.equals(
                                            selectedMarket
                                    )) {

                                        mtfScanning = false;

                                        return;
                                    }

                                    /*
                                     * An unavailable timeframe is explicitly
                                     * recorded as WAIT. This prevents the app
                                     * from pretending that missing data agrees
                                     * with the trade.
                                     */
                                    MultiTimeframeEngine.TimeframeResult waitResult =
                                            new MultiTimeframeEngine.TimeframeResult(
                                                    timeframe,
                                                    Signal.Direction.WAIT,
                                                    "DATA ERROR",
                                                    Double.NaN,
                                                    Double.NaN
                                            );

                                    displayMtfResult(
                                            waitResult
                                    );

                                    mtfRequestIndex++;

                                    requestNextMtfTimeframe(
                                            market
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void displayMtfResult(
            MultiTimeframeEngine.TimeframeResult result
    ) {

        if (result == null) {
            return;
        }

        mtfResults.add(
                result
        );

        String timeframe =
                result.getTimeframe();

        Signal.Direction direction =
                result.getDirection();

        if (direction == Signal.Direction.BUY) {

            updateMtfBox(
                    timeframe,
                    "BUY",
                    GREEN
            );

        } else if (
                direction == Signal.Direction.SELL
        ) {

            updateMtfBox(
                    timeframe,
                    "SELL",
                    RED
            );

        } else {

            updateMtfBox(
                    timeframe,
                    "WAIT",
                    YELLOW
            );
        }
    }

    private void updateMtfBox(
            String timeframe,
            String status,
            int color
    ) {

        TextView view =
                getMtfView(
                        timeframe
                );

        if (view == null) {
            return;
        }

        view.setText(
                timeframe
                        + "\n"
                        + status
        );

        view.setTextColor(
                color
        );
    }

    private TextView getMtfView(
            String timeframe
    ) {

        if (timeframe == null) {
            return null;
        }

        switch (
                timeframe.toUpperCase(
                        Locale.ENGLISH
                )
        ) {

            case "5M":
                return mtf5mText;

            case "15M":
                return mtf15mText;

            case "30M":
                return mtf30mText;

            case "1H":
                return mtf1hText;

            case "4H":
                return mtf4hText;

            case "1D":
                return mtf1dText;

            default:
                return null;
        }
    }

    private void resetMtfBoxes(
            String status
    ) {

        int color =
                status.equals(
                        "MARKET CLOSED"
                )
                        ? MUTED
                        : YELLOW;

        updateMtfBox(
                "5M",
                status,
                color
        );

        updateMtfBox(
                "15M",
                status,
                color
        );

        updateMtfBox(
                "30M",
                status,
                color
        );

        updateMtfBox(
                "1H",
                status,
                color
        );

        updateMtfBox(
                "4H",
                status,
                color
        );

        updateMtfBox(
                "1D",
                status,
                color
        );
    }

    private void updateMtfConfluenceStatus() {

        if (!isForexMarketOpen(
                new Date()
        )) {
            return;
        }

        if (mtfConfluenceText == null) {
            return;
        }

        MultiTimeframeEngine.ConfluenceResult confluence =
                MultiTimeframeEngine.calculateConfluence(
                        mtfResults
                );

        int buyCount =
                confluence.getBuyCount();

        int sellCount =
                confluence.getSellCount();

        int waitCount =
                confluence.getWaitCount();

        int strength =
                confluence.getStrength();

        Signal.Direction direction =
                confluence.getDirection();

        if (direction == Signal.Direction.BUY) {

            mtfConfluenceText.setText(
                    "BUY CONFLUENCE\n"
                            + buyCount
                            + "/6 TIMEFRAMES BUY\n"
                            + "SELL "
                            + sellCount
                            + "  •  WAIT "
                            + waitCount
                            + "  •  STRENGTH "
                            + strength
                            + "/5"
            );

            mtfConfluenceText.setTextColor(
                    GREEN
            );

        } else if (
                direction == Signal.Direction.SELL
        ) {

            mtfConfluenceText.setText(
                    "SELL CONFLUENCE\n"
                            + sellCount
                            + "/6 TIMEFRAMES SELL\n"
                            + "BUY "
                            + buyCount
                            + "  •  WAIT "
                            + waitCount
                            + "  •  STRENGTH "
                            + strength
                            + "/5"
            );

            mtfConfluenceText.setTextColor(
                    RED
            );

        } else {

            mtfConfluenceText.setText(
                    "NO CLEAR CONFLUENCE\n"
                            + "BUY "
                            + buyCount
                            + "  •  SELL "
                            + sellCount
                            + "  •  WAIT "
                            + waitCount
                            + "\nWAIT FOR STRONGER ALIGNMENT"
            );

            mtfConfluenceText.setTextColor(
                    YELLOW
            );
        }
    }

    // =============================================================
    // FINAL MTF DECISION
    // =============================================================

    private void applyFinalMtfDecision() {

        if (!isForexMarketOpen(
                new Date()
        )) {

            showMarketClosedState();

            return;
        }

        if (mtfResults.size() < timeframes.length) {

            showMtfWaitState(
                    "WAITING FOR ALL 6 TIMEFRAMES"
            );

            return;
        }

        MultiTimeframeEngine.ConfluenceResult confluence =
                MultiTimeframeEngine.calculateConfluence(
                        mtfResults
                );

        Signal.Direction mtfDirection =
                confluence.getDirection();

        if (pendingPrimarySignal == null) {

            showMtfWaitState(
                    "WAITING FOR PRIMARY SIGNAL"
            );

            return;
        }

        Signal.Direction primaryDirection =
                pendingPrimarySignal.getDirection();

        /*
         * PRIMARY SIGNAL MUST AGREE WITH MTF CONFLUENCE.
         *
         * BUY + BUY  = allowed
         * SELL + SELL = allowed
         *
         * BUY + SELL  = WAIT
         * SELL + BUY  = WAIT
         * WAIT        = WAIT
         */
        if (primaryDirection
                == Signal.Direction.BUY
                && mtfDirection
                == Signal.Direction.BUY) {

            updateDashboard(
                    pendingPrimarySignal
            );

            connectionStatus.setText(
                    "●  MTF CONFIRMED BUY • "
                            + confluence.getBuyCount()
                            + "/6"
            );

            connectionStatus.setTextColor(
                    GREEN
            );

            return;
        }

        if (primaryDirection
                == Signal.Direction.SELL
                && mtfDirection
                == Signal.Direction.SELL) {

            updateDashboard(
                    pendingPrimarySignal
            );

            connectionStatus.setText(
                    "●  MTF CONFIRMED SELL • "
                            + confluence.getSellCount()
                            + "/6"
            );

            connectionStatus.setTextColor(
                    RED
            );

            return;
        }

        /*
         * Any disagreement becomes WAIT.
         */
        showMtfWaitState(
                "PRIMARY SIGNAL NOT CONFIRMED BY MTF"
        );
    }

    private void showMtfWaitState(
            String reason
    ) {

        if (signalText != null) {

            signalText.setText(
                    "WAIT"
            );

            signalText.setTextColor(
                    YELLOW
            );
        }

        clearTradeLevels();

        if (pendingPrimarySignal != null) {

            if (!Double.isNaN(
                    pendingPrimarySignal.getEntry()
            )) {

                priceText.setText(
                        "PRICE  "
                                + formatPrice(
                                pendingPrimarySignal.getSymbol(),
                                pendingPrimarySignal.getEntry()
                        )
                );

            } else {

                priceText.setText(
                        "PRICE  --"
                );
            }

            trendText.setText(
                    "TREND\n"
                            + pendingPrimarySignal.getTrend()
            );

            if (!Double.isNaN(
                    pendingPrimarySignal.getRsi()
            )) {

                rsiText.setText(
                        "RSI\n"
                                + String.format(
                                Locale.US,
                                "%.2f",
                                pendingPrimarySignal.getRsi()
                        )
                );

            } else {

                rsiText.setText(
                        "RSI\n--"
                );
            }

            if (!Double.isNaN(
                    pendingPrimarySignal.getAtr()
            )) {

                atrText.setText(
                        "ATR\n"
                                + formatPrice(
                                pendingPrimarySignal.getSymbol(),
                                pendingPrimarySignal.getAtr()
                        )
                );

            } else {

                atrText.setText(
                        "ATR\n--"
                );
            }

            momentumText.setText(
                    "TIMEFRAME\n"
                            + pendingPrimarySignal.getTimeframe()
            );

        } else {

            priceText.setText(
                    "PRICE  --"
            );

            trendText.setText(
                    "TREND\n--"
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

        connectionStatus.setText(
                "●  MTF WAIT • "
                        + reason
        );

        connectionStatus.setTextColor(
                YELLOW
        );
    }

    // =============================================================
    // SELECTION
    // =============================================================

    private void resetForNewSelection() {

        mtfScanning = false;

        pendingPrimarySignal = null;

        mtfResults.clear();

        if (mtfConfluenceText != null) {

            mtfConfluenceText.setText(
                    "CONFLUENCE\nWAITING FOR ANALYSIS"
            );

            mtfConfluenceText.setTextColor(
                    YELLOW
            );
        }

        if (marketName != null) {
            marketName.setText(
                    selectedMarket
            );
        }

        if (!isForexMarketOpen(
                new Date()
        )) {

            showMarketClosedState();

            return;
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

        resetMtfBoxes(
                "WAIT"
        );

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

        requestCurrentSignal();
    }

    // =============================================================
    // DASHBOARD
    // =============================================================

    private void updateDashboard(
            Signal signal
    ) {

        if (signal == null) {
            return;
        }

        if (!isForexMarketOpen(
                new Date()
        )) {

            showMarketClosedState();

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
    // FORMATTING
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

    private TextView createSessionRow(
            String name
    ) {

        TextView view =
                createText(
                        "●  "
                                + name
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

        mtfScanning = false;

        pendingPrimarySignal = null;

        if (signalRepository != null) {
            signalRepository.stop();
        }

        if (marketDataProvider != null) {
            marketDataProvider.stop();
        }

        super.onDestroy();
    }
}