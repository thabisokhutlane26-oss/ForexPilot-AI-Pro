package com.forexpilot.aipro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private static final String PREFS = "forexpilot_preferences";
    private static final String CHART_PREFS = "forexpilot_chart_settings";

    private static final int NOTIFICATION_PERMISSION_REQUEST = 4101;
    private static final long CHART_REFRESH_INTERVAL_MS = 60_000L;

    private ForexChartView forexChartView;
    private SignalRepository signalRepository;
    private MarketDataProvider marketDataProvider;

    private BackgroundScanner backgroundScanner;
    private SignalAlertCoordinator signalAlertCoordinator;
    private SignalNotificationManager signalNotificationManager;

    private final Handler chartRefreshHandler = new Handler();

    private final Runnable chartRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (marketDataProvider != null
                    && isForexMarketOpen(new Date())) {
                requestChartCandles();
            }

            chartRefreshHandler.postDelayed(
                    this, CHART_REFRESH_INTERVAL_MS);
        }
    };

    // FULL-SCREEN LANDSCAPE CHART
    private FrameLayout fullscreenChartOverlay;
    private ViewGroup chartOriginalParent;
    private ViewGroup.LayoutParams chartOriginalLayoutParams;
    private int chartOriginalIndex = -1;
    private int previousSystemUiVisibility;
    private int previousRequestedOrientation;
    private boolean fullscreenChart = false;

    private TextView connectionStatus, clockText, dateText;
    private TextView forexStatusText, nextMarketText;
    private TextView sydneySessionText, tokyoSessionText;
    private TextView londonSessionText, newYorkSessionText;
    private TextView marketName, priceText, signalText;
    private TextView trendText, momentumText;
    private TextView entryText, stopLossText, tp1Text, tp2Text, tp3Text;
    private TextView mtf5mText, mtf15mText, mtf30mText;
    private TextView mtf1hText, mtf4hText, mtf1dText;
    private TextView mtfConfluenceText;

    private TextView homeTradeStyleText, homeConfidenceText;
    private TextView homeReviewTimeText, homeStyleReasonText;
    private TextView tradeStyleText, confidenceText;
    private TextView reviewTimeText, styleReasonText;

    private TradeStyleEngine.TradeStylePlan currentTradeStylePlan;

    private Spinner marketSpinner, timeframeSpinner;
    private Button scanButton, themeButton;

    private Button candlesButton, lineButton, barsButton;
    private Button colorsButton;

    private FrameLayout pageContainer;
    private View[] appPages;
    private Button[] navigationButtons;

    private int currentPage = 0;
    private String selectedMarket = "EUR/USD";
    private String selectedTimeframe = "15M";

    private int chartMode = ForexChartView.MODE_CANDLES;
    private boolean lightMode = false;
    private boolean lastForexOpen = false;
    private boolean mtfScanning = false;

    private Signal pendingPrimarySignal;

    private final List<MultiTimeframeEngine.TimeframeResult> mtfResults =
            new ArrayList<>();

    private int mtfRequestIndex = 0;

    private final String[] markets = {
            "XAU/USD", "EUR/USD", "GBP/USD", "USD/JPY", "NZD/USD"
    };

    private final String[] timeframes = {
            "5M", "15M", "30M", "1H", "4H", "1D"
    };

    private final String[] pageNames = {
            "HOME", "CHARTS", "ANALYSIS", "TRADE PLAN", "SETTINGS"
    };

    private int BACKGROUND, CARD, CARD_BORDER;
    private int WHITE, MUTED, GREEN, RED, YELLOW, BLUE;
    private int FIELD_BACKGROUND, BUTTON_BACKGROUND;

    private SharedPreferences chartPreferences;

    private final String[] colorNames = {
            "Foreground", "Grid", "Bar up", "Bar down",
            "Bull", "Bear", "Chart line", "Volumes",
            "Bid", "Ask", "Last", "Stop Loss",
            "Take Profit", "Buy", "Sell"
    };

    private final String[] colorRoles = {
            "FOREGROUND", "GRID", "BAR_UP", "BAR_DOWN",
            "BULL", "BEAR", "CHART_LINE", "VOLUMES",
            "BID", "ASK", "LAST", "SL", "TP", "BUY", "SELL"
    };

    private final int[] defaultChartColors = {
            Color.WHITE, Color.rgb(30, 120, 80),
            Color.BLACK, Color.BLACK,
            Color.rgb(18, 209, 142), Color.rgb(255, 72, 87),
            Color.rgb(30, 190, 110), Color.rgb(30, 190, 110),
            Color.LTGRAY, Color.RED, Color.GREEN,
            Color.rgb(255, 145, 30), Color.GREEN,
            Color.BLUE, Color.RED
    };

    private final Handler clockHandler = new Handler();

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimeAndSessions();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        chartPreferences =
                getSharedPreferences(CHART_PREFS, MODE_PRIVATE);

        lightMode = preferences.getBoolean("light_mode", false);

        chartMode = chartPreferences.getInt(
                "chart_mode",
                preferences.getBoolean("line_chart_mode", false)
                        ? ForexChartView.MODE_LINE
                        : ForexChartView.MODE_CANDLES);

        applyThemeColors();
        buildDashboard();
        requestNotificationPermission();
        updateTimeAndSessions();
        clockHandler.post(clockRunnable);
        connectToMarketData();

        chartRefreshHandler.postDelayed(
                chartRefreshRunnable, CHART_REFRESH_INTERVAL_MS);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    private void applyThemeColors() {
        GREEN = Color.rgb(20, 190, 100);
        RED = Color.rgb(230, 65, 75);
        YELLOW = Color.rgb(225, 175, 45);
        BLUE = Color.rgb(45, 120, 230);

        if (lightMode) {
            BACKGROUND = Color.rgb(239, 243, 249);
            CARD = Color.WHITE;
            CARD_BORDER = Color.rgb(215, 224, 235);
            WHITE = Color.rgb(25, 35, 50);
            MUTED = Color.rgb(100, 115, 135);
            FIELD_BACKGROUND = Color.rgb(246, 248, 252);
            BUTTON_BACKGROUND = Color.rgb(35, 100, 210);
        } else {
            BACKGROUND = Color.rgb(7, 11, 18);
            CARD = Color.rgb(15, 22, 33);
            CARD_BORDER = Color.rgb(35, 47, 64);
            WHITE = Color.rgb(245, 248, 252);
            MUTED = Color.rgb(145, 158, 175);
            FIELD_BACKGROUND = Color.rgb(10, 16, 25);
            BUTTON_BACKGROUND = Color.rgb(25, 95, 180);
        }
    }

    private void toggleTheme() {
        lightMode = !lightMode;

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean("light_mode", lightMode).apply();

        applyThemeColors();
        buildDashboard();
        updateTimeAndSessions();
        applyAllChartSettings();

        if (marketDataProvider != null) {
            requestChartCandles();
        }

        if (signalRepository != null && isForexMarketOpen(new Date())) {
            requestCurrentSignal();
        } else {
            resetTradeStyleDisplay();
        }
    }

    private void buildDashboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(12));
        header.setBackgroundColor(CARD);

        header.addView(createText("FOREXPILOT AI PRO", 23, WHITE, true));
        header.addView(createText(
                "TRADING INTELLIGENCE  •  SAST", 11, BLUE, true),
                params(3));

        connectionStatus = createText(
                "● CONNECTING TO MARKET", 11, YELLOW, true);
        header.addView(connectionStatus, params(6));
        root.addView(header);

        pageContainer = new FrameLayout(this);
        root.addView(pageContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        appPages = new View[5];
        navigationButtons = new Button[5];

        LinearLayout home = createPage(0);
        LinearLayout charts = createPage(1);
        LinearLayout analysis = createPage(2);
        LinearLayout trade = createPage(3);
        LinearLayout settings = createPage(4);

        // HOME
        LinearLayout clockCard = createCard();
        clockCard.addView(createSmallLabel("SOUTH AFRICAN MARKET CLOCK"));

        clockText = createText(" --:--:-- SAST", 27, WHITE, true);
        clockCard.addView(clockText, params(6));

        dateText = createText("--", 12, MUTED, false);
        clockCard.addView(dateText, params(3));

        forexStatusText = createText(
                "● CHECKING MARKET", 14, YELLOW, true);
        clockCard.addView(forexStatusText, params(8));

        nextMarketText = createText(
                "Checking market status...", 11, MUTED, false);
        clockCard.addView(nextMarketText, params(4));
        home.addView(clockCard, params(8));

        LinearLayout selectorCard = createCard();
        selectorCard.addView(createSmallLabel("CURRENCY PAIR"));

        marketSpinner = createSpinner(markets);
        marketSpinner.setSelection(indexOf(markets, selectedMarket));
        selectorCard.addView(marketSpinner, params(5));

        selectorCard.addView(createSmallLabel("TIMEFRAME"), params(8));
        timeframeSpinner = createSpinner(timeframes);
        timeframeSpinner.setSelection(indexOf(timeframes, selectedTimeframe));
        selectorCard.addView(timeframeSpinner, params(5));
        home.addView(selectorCard, params(8));

        LinearLayout priceCard = createCard();
        priceCard.addView(createSmallLabel("SELECTED MARKET"));

        marketName = createText(selectedMarket, 25, WHITE, true);
        marketName.setGravity(Gravity.CENTER);
        priceCard.addView(marketName, params(5));

        priceText = createText("PRICE  --", 20, BLUE, true);
        priceText.setGravity(Gravity.CENTER);
        priceCard.addView(priceText, params(5));
        home.addView(priceCard, params(8));

        LinearLayout signalCard = createCard();
        signalCard.addView(createSmallLabel("AI SIGNAL"));

        signalText = createText("WAIT", 38, YELLOW, true);
        signalText.setGravity(Gravity.CENTER);
        signalCard.addView(signalText, params(5));

        TextView signalDescription = createText(
                "BUY / SELL signals require confirmation",
                12, MUTED, false);
        signalDescription.setGravity(Gravity.CENTER);
        signalCard.addView(signalDescription, params(4));

        homeTradeStyleText = createText(
                "TRADE STYLE • WAIT", 17, YELLOW, true);
        homeTradeStyleText.setGravity(Gravity.CENTER);
        signalCard.addView(homeTradeStyleText, params(10));

        homeConfidenceText = createText(
                "RULE-BASED CONFIDENCE • --", 13, MUTED, true);
        homeConfidenceText.setGravity(Gravity.CENTER);
        signalCard.addView(homeConfidenceText, params(5));

        homeReviewTimeText = createText(
                "SUGGESTED REVIEW • --", 13, MUTED, true);
        homeReviewTimeText.setGravity(Gravity.CENTER);
        signalCard.addView(homeReviewTimeText, params(5));

        homeStyleReasonText = createText(
                "Waiting for confirmed timeframe agreement.",
                12, MUTED, false);
        homeStyleReasonText.setGravity(Gravity.CENTER);
        signalCard.addView(homeStyleReasonText, params(5));
        home.addView(signalCard, params(8));

        scanButton = createButton("SCAN MARKET");
        scanButton.setTextSize(15);
        scanButton.setOnClickListener(v -> requestCurrentSignal());
        home.addView(scanButton, params(8));

        LinearLayout quickCard = createCard();
        quickCard.addView(createSmallLabel("QUICK ACCESS"));

        Button openChart = createButton("OPEN CHARTS");
        openChart.setOnClickListener(v -> showPage(1));
        quickCard.addView(openChart, params(7));

        Button openAnalysis = createButton("VIEW ANALYSIS");
        openAnalysis.setOnClickListener(v -> showPage(2));
        quickCard.addView(openAnalysis, params(7));

        Button openTrade = createButton("VIEW TRADE PLAN");
        openTrade.setOnClickListener(v -> showPage(3));
        quickCard.addView(openTrade, params(7));
        home.addView(quickCard, params(8));

        // CHARTS
        LinearLayout chartCard = createCard();
        chartCard.addView(createText("PRICE CHART", 19, WHITE, true));
        chartCard.addView(createText(
                "Candlesticks • Line • Bars", 12, MUTED, false),
                params(3));

        Button fullscreenButton =
                createSmallActionButton("FULL SCREEN  ⛶");
        fullscreenButton.setOnClickListener(
                v -> enterFullscreenChart());
        chartCard.addView(fullscreenButton, params(8));

        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);

        candlesButton = createSmallActionButton("CANDLES");
        lineButton = createSmallActionButton("LINE");
        barsButton = createSmallActionButton("BARS");

        candlesButton.setOnClickListener(
                v -> selectChartMode(ForexChartView.MODE_CANDLES));
        lineButton.setOnClickListener(
                v -> selectChartMode(ForexChartView.MODE_LINE));
        barsButton.setOnClickListener(
                v -> selectChartMode(ForexChartView.MODE_BARS));

        styleRow.addView(candlesButton, weightParams());
        styleRow.addView(lineButton, weightParams());
        styleRow.addView(barsButton, weightParams());
        chartCard.addView(styleRow, params(8));

        LinearLayout toolRow = new LinearLayout(this);
        toolRow.setOrientation(LinearLayout.HORIZONTAL);

        colorsButton = createSmallActionButton("COLORS");
        colorsButton.setOnClickListener(v -> showChartColors());

        toolRow.addView(colorsButton, weightParams());
        chartCard.addView(toolRow, params(6));

        forexChartView = new ForexChartView(this);
        forexChartView.setChartMode(chartMode);

        int chartHeight =
                getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE ? 300 : 420;

        chartCard.addView(forexChartView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(chartHeight)));

        charts.addView(chartCard, params(8));

        LinearLayout chartInfo = createCard();
        chartInfo.addView(createSmallLabel("CURRENT CHART"));
        chartInfo.addView(createText(
                "PAIR: " + selectedMarket
                        + "\nTIMEFRAME: " + selectedTimeframe
                        + "\n\nChoose a chart style or customize chart colors.",
                13, MUTED, false), params(6));
        charts.addView(chartInfo, params(8));

        // MARKET ANALYSIS
        LinearLayout analysisCard = createCard();
        analysisCard.addView(createText(
                "MARKET ANALYSIS", 19, WHITE, true));
        analysisCard.addView(createText(
                "Trend and momentum from available market candles",
                12, MUTED, false), params(3));

        LinearLayout row1 = createMetricRow();
        trendText = createMetric("TREND", "--");
        momentumText = createMetric("TIMEFRAME", selectedTimeframe);
        row1.addView(trendText, weightParams());
        row1.addView(momentumText, weightParams());
        analysisCard.addView(row1, params(8));
        analysis.addView(analysisCard, params(8));

        LinearLayout mtfCard = createCard();
        mtfCard.addView(createText(
                "MULTI-TIMEFRAME CONFIRMATION", 16, WHITE, true));

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);

        LinearLayout timeframeRow = new LinearLayout(this);
        timeframeRow.setOrientation(LinearLayout.HORIZONTAL);

        mtf5mText = createTimeframeBox("5M\nWAIT");
        mtf15mText = createTimeframeBox("15M\nWAIT");
        mtf30mText = createTimeframeBox("30M\nWAIT");
        mtf1hText = createTimeframeBox("1H\nWAIT");
        mtf4hText = createTimeframeBox("4H\nWAIT");
        mtf1dText = createTimeframeBox("1D\nWAIT");

        timeframeRow.addView(mtf5mText);
        timeframeRow.addView(mtf15mText);
        timeframeRow.addView(mtf30mText);
        timeframeRow.addView(mtf1hText);
        timeframeRow.addView(mtf4hText);
        timeframeRow.addView(mtf1dText);

        horizontal.addView(timeframeRow);
        mtfCard.addView(horizontal, params(8));

        mtfConfluenceText = createText(
                "MTF CONFLUENCE • WAIT", 13, YELLOW, true);
        mtfCard.addView(mtfConfluenceText, params(10));
        analysis.addView(mtfCard, params(8));

        // TRADE PLAN
        LinearLayout tradeCard = createCard();
        tradeCard.addView(createText("TRADE PLAN", 21, WHITE, true));
        tradeCard.addView(createText(
                "Trade levels appear after signal confirmation.",
                12, MUTED, false), params(4));

        tradeStyleText = createTradeRow("TRADE STYLE", YELLOW);
        confidenceText = createTradeRow("RULE-BASED CONFIDENCE", MUTED);
        reviewTimeText = createTradeRow("SUGGESTED REVIEW", MUTED);

        styleReasonText = createText(
                "Waiting for confirmed timeframe agreement.",
                12, MUTED, false);
        styleReasonText.setPadding(dp(12), dp(8), dp(12), dp(8));

        tradeCard.addView(tradeStyleText, params(10));
        tradeCard.addView(confidenceText, params(6));
        tradeCard.addView(reviewTimeText, params(6));
        tradeCard.addView(styleReasonText, params(6));

        entryText = createTradeRow("ENTRY", BLUE);
        stopLossText = createTradeRow("STOP LOSS", RED);
        tp1Text = createTradeRow("TAKE PROFIT 1", GREEN);
        tp2Text = createTradeRow("TAKE PROFIT 2", GREEN);
        tp3Text = createTradeRow("TAKE PROFIT 3", GREEN);

        tradeCard.addView(entryText, params(10));
        tradeCard.addView(stopLossText, params(6));
        tradeCard.addView(tp1Text, params(6));
        tradeCard.addView(tp2Text, params(6));
        tradeCard.addView(tp3Text, params(6));

        Button tradeScan = createButton("SCAN FOR A TRADE");
        tradeScan.setOnClickListener(v -> requestCurrentSignal());
        tradeCard.addView(tradeScan, params(12));
        trade.addView(tradeCard, params(8));

        // SETTINGS
        LinearLayout appearanceCard = createCard();
        appearanceCard.addView(createText(
                "APPEARANCE", 18, WHITE, true));
        appearanceCard.addView(createText(
                "Change the app color theme.", 12, MUTED, false),
                params(4));

        themeButton = createButton("");
        updateThemeButtonText();
        themeButton.setOnClickListener(v -> toggleTheme());
        appearanceCard.addView(themeButton, params(8));
        settings.addView(appearanceCard, params(8));

        LinearLayout settingsCard = createCard();
        settingsCard.addView(createText(
                "MARKET SETTINGS", 18, WHITE, true));
        settingsCard.addView(createText(
                "Default market: " + selectedMarket
                        + "\nSelected timeframe: " + selectedTimeframe
                        + "\n\nData provider: Twelve Data"
                        + "\n\nMarket data availability depends on your "
                        + "configured provider and API access.",
                13, MUTED, false), params(8));
        settings.addView(settingsCard, params(8));

        LinearLayout sessionsCard = createCard();
        sessionsCard.addView(createText(
                "FOREX SESSIONS • SAST", 17, WHITE, true));

        sydneySessionText = createSessionRow("SYDNEY");
        tokyoSessionText = createSessionRow("TOKYO");
        londonSessionText = createSessionRow("LONDON");
        newYorkSessionText = createSessionRow("NEW YORK");

        sessionsCard.addView(sydneySessionText, params(8));
        sessionsCard.addView(tokyoSessionText, params(6));
        sessionsCard.addView(londonSessionText, params(6));
        sessionsCard.addView(newYorkSessionText, params(6));
        settings.addView(sessionsCard, params(8));

        LinearLayout aboutCard = createCard();
        aboutCard.addView(createText(
                "ABOUT FOREXPILOT AI PRO", 16, WHITE, true));
        aboutCard.addView(createText(
                "Market scanner"
                        + "\nMulti-timeframe confirmation"
                        + "\nAutomatic trade-style selection"
                        + "\nTrade planning tools"
                        + "\n\nSignals are informational and are not "
                        + "financial advice.",
                13, MUTED, false), params(6));
        settings.addView(aboutCard, params(8));

        // BOTTOM NAVIGATION
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(3), dp(5), dp(3), dp(5));
        nav.setBackgroundColor(CARD);

        String[] labels = {
                "⌂\nHOME", "▥\nCHARTS", "◈\nANALYSIS",
                "⇄\nTRADE", "⚙\nSETTINGS"
        };

        for (int i = 0; i < labels.length; i++) {
            final int page = i;
            Button button = new Button(this);
            navigationButtons[i] = button;
            button.setText(labels[i]);
            button.setTextSize(9);
            button.setAllCaps(false);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setPadding(dp(1), dp(2), dp(1), dp(2));
            button.setTextColor(WHITE);
            button.setOnClickListener(v -> showPage(page));

            LinearLayout.LayoutParams navParams =
                    new LinearLayout.LayoutParams(0, dp(57), 1f);
            navParams.setMargins(dp(2), 0, dp(2), 0);
            nav.addView(button, navParams);
        }

        root.addView(nav);
        setContentView(root);

        marketSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view, int position, long id) {
                String value = markets[position];
                if (value.equals(selectedMarket)) return;
                selectedMarket = value;
                marketName.setText(value);
                resetForNewSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        timeframeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view, int position, long id) {
                String value = timeframes[position];
                if (value.equals(selectedTimeframe)) return;
                selectedTimeframe = value;
                momentumText.setText("TIMEFRAME\n" + value);
                resetForNewSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        resetTradeStyleDisplay();
        showPage(currentPage);
        updateChartModeButtons();
        applyAllChartSettings();
    }


    // CHART STYLE CONTROLS

    private void selectChartMode(int mode) {
        chartMode = mode;

        chartPreferences.edit()
                .putInt("chart_mode", chartMode)
                .apply();

        if (forexChartView != null) {
            forexChartView.setChartMode(chartMode);
        }

        updateChartModeButtons();
    }

    private void updateChartModeButtons() {
        if (candlesButton == null) return;

        setActionButtonSelected(
                candlesButton,
                chartMode == ForexChartView.MODE_CANDLES);

        setActionButtonSelected(
                lineButton,
                chartMode == ForexChartView.MODE_LINE);

        setActionButtonSelected(
                barsButton,
                chartMode == ForexChartView.MODE_BARS);
    }

    private void setActionButtonSelected(
            Button button, boolean selected) {

        if (button == null) return;

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(9));
        background.setColor(
                selected ? BUTTON_BACKGROUND : FIELD_BACKGROUND);
        background.setStroke(
                dp(1), selected ? BLUE : CARD_BORDER);

        button.setBackground(background);
        button.setTextColor(selected ? Color.WHITE : MUTED);
    }

    private Button createSmallActionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(10);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(2), dp(5), dp(2), dp(5));
        button.setTextColor(WHITE);
        button.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 9));
        return button;
    }

    // CHART SETTINGS

    private void applyAllChartSettings() {
        if (forexChartView == null) return;

        forexChartView.setChartMode(chartMode);

        applySavedChartColors();
        updateChartModeButtons();
    }

    // CHART COLOR SETTINGS

    private void showChartColors() {
        String[] options = new String[colorNames.length];

        for (int i = 0; i < colorNames.length; i++) {
            options[i] = colorNames[i];
        }

        new AlertDialog.Builder(this)
                .setTitle("CHART COLORS")
                .setItems(options, (dialog, which) ->
                        showColorChoices(which))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showColorChoices(int index) {
        final int[] palette = {
                Color.WHITE,
                Color.BLACK,
                Color.RED,
                Color.GREEN,
                Color.BLUE,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.LTGRAY,
                Color.DKGRAY,
                Color.rgb(18, 209, 142),
                Color.rgb(255, 72, 87),
                Color.rgb(255, 145, 30),
                Color.rgb(120, 80, 220),
                Color.rgb(255, 120, 180),
                Color.rgb(40, 170, 210),
                Color.rgb(150, 200, 80),
                Color.rgb(210, 170, 90)
        };

        String[] names = {
                "White", "Black", "Red", "Green", "Blue",
                "Yellow", "Cyan", "Magenta", "Silver",
                "Dark gray", "Bullish green", "Bearish red",
                "Orange", "Purple", "Pink", "Sky blue",
                "Lime", "Gold"
        };

        new AlertDialog.Builder(this)
                .setTitle("COLOR • " + colorNames[index])
                .setItems(names, (dialog, which) -> {
                    int color = palette[which];

                    chartPreferences.edit()
                            .putInt(
                                    "color_" + colorRoles[index],
                                    color)
                            .apply();

                    if (forexChartView != null) {
                        forexChartView.setColorRole(
                                colorRoles[index], color);
                    }

                    Toast.makeText(
                            this,
                            colorNames[index] + " color saved",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void applySavedChartColors() {
        if (forexChartView == null) return;

        for (int i = 0; i < colorRoles.length; i++) {
            int color = chartPreferences.getInt(
                    "color_" + colorRoles[i],
                    defaultChartColors[i]);

            forexChartView.setColorRole(
                    colorRoles[i], color);
        }
    }


    // GENERAL PAGE HELPERS

    private LinearLayout createPage(int index) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), dp(8), dp(12), dp(16));
        scroll.addView(body);

        appPages[index] = scroll;

        pageContainer.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        return body;
    }

    private void showPage(int index) {
        if (appPages == null || navigationButtons == null) return;

        currentPage = Math.max(
                0, Math.min(index, appPages.length - 1));

        for (int i = 0; i < appPages.length; i++) {
            appPages[i].setVisibility(
                    i == currentPage ? View.VISIBLE : View.GONE);

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(10));
            background.setColor(
                    i == currentPage ? BUTTON_BACKGROUND : CARD);

            navigationButtons[i].setBackground(background);
            navigationButtons[i].setTextColor(
                    i == currentPage ? Color.WHITE : MUTED);
        }
    }

    private void updateThemeButtonText() {
        if (themeButton != null) {
            themeButton.setText(lightMode
                    ? "SWITCH TO DARK MODE  ☾"
                    : "SWITCH TO LIGHT MODE  ☼");
        }
    }

    private int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) return i;
        }
        return 0;
    }

    // AUTOMATIC TRADE STYLE

    private void updateTradeStyleDisplay(
            TradeStyleEngine.TradeStylePlan plan) {

        if (plan == null || !plan.isTradeAllowed()) {
            resetTradeStyleDisplay();
            return;
        }

        currentTradeStylePlan = plan;

        String styleName;

        switch (plan.getStyle()) {
            case SCALP:
                styleName = "SCALP";
                break;
            case DAY_TRADE:
                styleName = "DAY TRADE";
                break;
            case SWING:
                styleName = "SWING";
                break;
            default:
                resetTradeStyleDisplay();
                return;
        }

        int directionColor =
                plan.getDirection() == Signal.Direction.BUY
                        ? GREEN : RED;

        String styleLabel = "TRADE STYLE • " + styleName
                + " • " + plan.getDirection().name();

        String confidenceLabel = "RULE-BASED CONFIDENCE • "
                + plan.getConfidence() + "/100";

        String reviewLabel = "SUGGESTED REVIEW • "
                + plan.getReviewTime();

        if (homeTradeStyleText != null) {
            homeTradeStyleText.setText(styleLabel);
            homeTradeStyleText.setTextColor(directionColor);
        }

        if (homeConfidenceText != null) {
            homeConfidenceText.setText(confidenceLabel);
            homeConfidenceText.setTextColor(WHITE);
        }

        if (homeReviewTimeText != null) {
            homeReviewTimeText.setText(reviewLabel);
            homeReviewTimeText.setTextColor(BLUE);
        }

        if (homeStyleReasonText != null) {
            homeStyleReasonText.setText(plan.getReason());
            homeStyleReasonText.setTextColor(MUTED);
        }

        if (tradeStyleText != null) {
            tradeStyleText.setText(styleLabel);
            tradeStyleText.setTextColor(directionColor);
        }

        if (confidenceText != null) {
            confidenceText.setText(confidenceLabel);
            confidenceText.setTextColor(WHITE);
        }

        if (reviewTimeText != null) {
            reviewTimeText.setText(reviewLabel);
            reviewTimeText.setTextColor(BLUE);
        }

        if (styleReasonText != null) {
            styleReasonText.setText(plan.getReason()
                    + "\n\nReview time is a reminder to reassess the market, "
                    + "not a guaranteed exit time.");
            styleReasonText.setTextColor(MUTED);
        }
    }

    private void resetTradeStyleDisplay() {
        currentTradeStylePlan = null;

        if (homeTradeStyleText != null) {
            homeTradeStyleText.setText("TRADE STYLE • WAIT");
            homeTradeStyleText.setTextColor(YELLOW);
        }

        if (homeConfidenceText != null) {
            homeConfidenceText.setText("RULE-BASED CONFIDENCE • --");
            homeConfidenceText.setTextColor(MUTED);
        }

        if (homeReviewTimeText != null) {
            homeReviewTimeText.setText("SUGGESTED REVIEW • --");
            homeReviewTimeText.setTextColor(MUTED);
        }

        if (homeStyleReasonText != null) {
            homeStyleReasonText.setText(
                    "Waiting for confirmed timeframe agreement.");
            homeStyleReasonText.setTextColor(MUTED);
        }

        if (tradeStyleText != null) {
            tradeStyleText.setText("TRADE STYLE • WAIT");
            tradeStyleText.setTextColor(YELLOW);
        }

        if (confidenceText != null) {
            confidenceText.setText("RULE-BASED CONFIDENCE • --");
            confidenceText.setTextColor(MUTED);
        }

        if (reviewTimeText != null) {
            reviewTimeText.setText("SUGGESTED REVIEW • --");
            reviewTimeText.setTextColor(MUTED);
        }

        if (styleReasonText != null) {
            styleReasonText.setText(
                    "Waiting for confirmed timeframe agreement.");
            styleReasonText.setTextColor(MUTED);
        }
    }

    private void resetForNewSelection() {
        mtfScanning = false;
        pendingPrimarySignal = null;
        mtfResults.clear();
        mtfRequestIndex = 0;

        resetMtfDisplay();
        resetTradeStyleDisplay();
        clearTradeLevels();

        if (signalText != null) {
            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);
        }

        if (priceText != null) {
            priceText.setText("PRICE  --");
        }

        if (trendText != null) {
            trendText.setText("TREND\n--");
        }

        if (momentumText != null) {
            momentumText.setText("TIMEFRAME\n" + selectedTimeframe);
        }

        if (connectionStatus != null) {
            connectionStatus.setText("● LOADING • "
                    + selectedMarket + " • " + selectedTimeframe);
            connectionStatus.setTextColor(YELLOW);
        }

        if (forexChartView != null) {
            forexChartView.clearChart();
            forexChartView.clearTradeLevels();
            forexChartView.setChartMode(chartMode);
            applyAllChartSettings();
        }

        if (signalAlertCoordinator != null) {
            signalAlertCoordinator.setMarket(selectedMarket);
        }

        requestChartCandles();

        if (signalRepository != null && isForexMarketOpen(new Date())) {
            requestCurrentSignal();
        }
    }

    // SOUTH AFRICAN TIME AND MARKET SESSIONS

    private void updateTimeAndSessions() {
        Date now = new Date();

        SimpleDateFormat timeFormat =
                new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

        timeFormat.setTimeZone(
                TimeZone.getTimeZone("Africa/Johannesburg"));

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "EEEE • dd MMMM yyyy", Locale.ENGLISH);

        dateFormat.setTimeZone(
                TimeZone.getTimeZone("Africa/Johannesburg"));

        if (clockText != null) {
            clockText.setText(timeFormat.format(now) + " SAST");
        }

        if (dateText != null) {
            dateText.setText(
                    dateFormat.format(now).toUpperCase(Locale.ENGLISH));
        }

        boolean open = isForexMarketOpen(now);

        if (forexStatusText != null) {
            forexStatusText.setText(open
                    ? "● FOREX MARKET OPEN"
                    : "● FOREX MARKET CLOSED");

            forexStatusText.setTextColor(open ? GREEN : RED);
        }

        if (nextMarketText != null) {
            if (open) {
                nextMarketText.setText(mtfScanning
                        ? "SCANNER • MULTI-TIMEFRAME ANALYSIS"
                        : "SCANNER • ACTIVE");
                nextMarketText.setTextColor(GREEN);
            } else {
                nextMarketText.setText(
                        "MARKET CLOSED • HISTORICAL DATA AVAILABLE");
                nextMarketText.setTextColor(MUTED);
            }
        }

        if (scanButton != null) {
            scanButton.setEnabled(open && !mtfScanning);
            scanButton.setAlpha(open ? 1f : 0.45f);
        }

        if (open && !lastForexOpen && signalRepository != null) {
            requestCurrentSignal();
        }

        if (!open && lastForexOpen) {
            resetTradeStyleDisplay();
        }

        if (signalAlertCoordinator != null) {
            if (open) {
                signalAlertCoordinator.setMarket(selectedMarket);
                signalAlertCoordinator.start();
            } else if (lastForexOpen) {
                signalAlertCoordinator.stop();
            }
        }

        lastForexOpen = open;

        updateSession(sydneySessionText, "SYDNEY",
                "Australia/Sydney", 8, 17);

        updateSession(tokyoSessionText, "TOKYO",
                "Asia/Tokyo", 9, 18);

        updateSession(londonSessionText, "LONDON",
                "Europe/London", 8, 17);

        updateSession(newYorkSessionText, "NEW YORK",
                "America/New_York", 8, 17);
    }

    private boolean isForexMarketOpen(Date date) {
        Calendar ny = Calendar.getInstance(
                TimeZone.getTimeZone("America/New_York"),
                Locale.ENGLISH);

        ny.setTime(date);

        int day = ny.get(Calendar.DAY_OF_WEEK);
        int minutes = ny.get(Calendar.HOUR_OF_DAY) * 60
                + ny.get(Calendar.MINUTE);

        if (day == Calendar.SATURDAY) return false;
        if (day == Calendar.SUNDAY) return minutes >= 17 * 60;
        if (day == Calendar.FRIDAY) return minutes < 17 * 60;

        return true;
    }

    private void updateSession(
            TextView view,
            String name,
            String zone,
            int openHour,
            int closeHour) {

        if (view == null) return;

        Calendar local = Calendar.getInstance(
                TimeZone.getTimeZone(zone), Locale.ENGLISH);

        local.setTime(new Date());

        int day = local.get(Calendar.DAY_OF_WEEK);
        int minutes = local.get(Calendar.HOUR_OF_DAY) * 60
                + local.get(Calendar.MINUTE);

        boolean weekend = day == Calendar.SATURDAY
                || day == Calendar.SUNDAY;

        boolean open = !weekend
                && minutes >= openHour * 60
                && minutes < closeHour * 60;

        SimpleDateFormat format =
                new SimpleDateFormat("HH:mm", Locale.ENGLISH);

        format.setTimeZone(TimeZone.getTimeZone(zone));
        String localTime = format.format(new Date());

        format.setTimeZone(
                TimeZone.getTimeZone("Africa/Johannesburg"));

        String sastTime = format.format(new Date());

        view.setText("●  " + name + "     "
                + (open ? "OPEN" : "CLOSED")
                + "\n    Local " + localTime
                + "  •  SAST " + sastTime);

        view.setTextColor(open ? GREEN : MUTED);
    }


    private void showMarketClosedState() {
        mtfScanning = false;
        pendingPrimarySignal = null;
        mtfResults.clear();

        if (signalText != null) {
            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);
        }

        if (connectionStatus != null) {
            connectionStatus.setText(
                    "● MARKET CLOSED • HISTORICAL DATA");
            connectionStatus.setTextColor(MUTED);
        }

        if (trendText != null) {
            trendText.setText("TREND\nHISTORICAL");
        }

        clearTradeLevels();
        resetMtfDisplay();
        resetTradeStyleDisplay();
    }

    // MARKET DATA CONNECTION

    private void connectToMarketData() {
        String apiKey = AppConfig.getTwelveDataApiKey();
        marketDataProvider = new TwelveDataProvider(apiKey);

        signalRepository = new SignalRepository(
                marketDataProvider,
                new SignalRepository.SignalCallback() {

            @Override
            public void onCandles(
                    String symbol,
                    String timeframe,
                    List<Candle> candles) {

                runOnUiThread(() -> {
                    if (forexChartView == null
                            || !selectedMarket.equals(symbol)
                            || !selectedTimeframe.equals(timeframe)
                            || candles == null
                            || candles.isEmpty()) {
                        return;
                    }

                    forexChartView.setCandles(candles);
                    applyAllChartSettings();
                    updateIndicatorDashboard(symbol, candles);
                });
            }

            @Override
            public void onSignal(Signal signal) {
                runOnUiThread(() -> {
                    if (signal == null) return;

                    if (!isForexMarketOpen(new Date())) {
                        updatePrimaryDashboard(signal);

                        resetTradeStyleDisplay();

                        if (connectionStatus != null) {
                            connectionStatus.setText(
                                    "● HISTORICAL SIGNAL DATA • MARKET CLOSED");
                            connectionStatus.setTextColor(MUTED);
                        }
                        return;
                    }

                    pendingPrimarySignal = signal;
                    resetTradeStyleDisplay();
                    updatePrimaryDashboard(signal);
                    requestMultiTimeframeAnalysis(signal.getSymbol());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    mtfScanning = false;
                    pendingPrimarySignal = null;

                    resetTradeStyleDisplay();
                    clearTradeLevels();

                    if (connectionStatus != null) {
                        connectionStatus.setText(
                                "● DATA ERROR • " + selectedMarket);
                        connectionStatus.setTextColor(RED);
                    }

                    if (signalText != null) {
                        signalText.setText("WAIT");
                        signalText.setTextColor(YELLOW);
                    }

                    if (nextMarketText != null
                            && message != null
                            && !message.trim().isEmpty()) {
                        nextMarketText.setText(
                                "DATA ERROR • " + message);
                    }

                    if (mtfConfluenceText != null) {
                        mtfConfluenceText.setText(
                                "MTF CONFLUENCE • DATA ERROR");
                        mtfConfluenceText.setTextColor(RED);
                    }

                    resetMtfDisplay();
                });
            }
        });

        // BACKGROUND SIGNAL ALERTS
        signalNotificationManager =
                new SignalNotificationManager(this);

        backgroundScanner = new BackgroundScanner(
                (market, timeframe, callback) -> {

            if (marketDataProvider == null) {
                callback.onError("NO MARKET DATA PROVIDER");
                return;
            }

            marketDataProvider.requestCandles(
                    market,
                    timeframe,
                    new MarketDataManager.MarketDataCallback() {

                @Override
                public void onCandlesReceived(List<Candle> candles) {
                    if (candles == null || candles.isEmpty()) {
                        callback.onError("NO CANDLE DATA");
                        return;
                    }

                    callback.onSignal(
                            SignalEngine.analyze(
                                    market, timeframe, candles));
                }

                @Override
                public void onPriceReceived(double price) {
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        });

        signalAlertCoordinator = new SignalAlertCoordinator(
                backgroundScanner,
                signalNotificationManager);

        signalAlertCoordinator.setMarket(selectedMarket);

        if (isForexMarketOpen(new Date())) {
            signalAlertCoordinator.start();
        }

        requestChartCandles();

        if (isForexMarketOpen(new Date())) {
            requestCurrentSignal();
        } else {
            showMarketClosedState();
        }
    }

    private void requestChartCandles() {
        if (marketDataProvider == null) return;

        final String market = selectedMarket;
        final String timeframe = selectedTimeframe;

        if (connectionStatus != null) {
            connectionStatus.setText("● LOADING CHART • "
                    + market + " • " + timeframe);
            connectionStatus.setTextColor(YELLOW);
        }

        marketDataProvider.requestCandles(
                market,
                timeframe,
                new MarketDataManager.MarketDataCallback() {

            @Override
            public void onCandlesReceived(List<Candle> candles) {
                runOnUiThread(() -> {
                    if (!market.equals(selectedMarket)
                            || !timeframe.equals(selectedTimeframe)) {
                        return;
                    }

                    if (candles == null || candles.isEmpty()) {
                        if (connectionStatus != null) {
                            connectionStatus.setText(
                                    "● CHART DATA EMPTY");
                            connectionStatus.setTextColor(RED);
                        }
                        return;
                    }

                    if (forexChartView != null) {
                        forexChartView.setCandles(candles);
                        applyAllChartSettings();
                    }

                    updateIndicatorDashboard(market, candles);

                    Candle latest = candles.get(candles.size() - 1);

                    if (latest != null
                            && Double.isFinite(latest.getClose())
                            && priceText != null) {
                        priceText.setText("PRICE  "
                                + formatPrice(
                                        market, latest.getClose()));
                    }

                    boolean open =
                            isForexMarketOpen(new Date());

                    if (connectionStatus != null) {
                        connectionStatus.setText((open
                                ? "● LIVE CHART • "
                                : "● HISTORICAL CHART • ")
                                + market + " • " + timeframe
                                + " • " + candles.size() + " CANDLES");

                        connectionStatus.setTextColor(
                                open ? GREEN : MUTED);
                    }
                });
            }

            @Override
            public void onPriceReceived(double price) {
                runOnUiThread(() -> {
                    if (market.equals(selectedMarket)
                            && Double.isFinite(price)
                            && priceText != null) {
                        priceText.setText(
                                "PRICE  " + formatPrice(market, price));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (!market.equals(selectedMarket)) return;

                    if (connectionStatus != null) {
                        connectionStatus.setText(
                                "● CHART DATA ERROR");
                        connectionStatus.setTextColor(RED);
                    }

                    if (nextMarketText != null) {
                        nextMarketText.setText("CHART ERROR • "
                                + (message == null
                                ? "UNKNOWN ERROR" : message));
                        nextMarketText.setTextColor(RED);
                    }
                });
            }
        });
    }

    // TREND AND MOMENTUM SUMMARY

    private void updateIndicatorDashboard(
            String market, List<Candle> candles) {

        if (candles == null || candles.isEmpty()) return;

        String trend = IndicatorEngine.trend(candles, 20, 50);
        double momentum = IndicatorEngine.momentum(candles, 5);

        if (trendText != null) {
            trendText.setText("TREND\n"
                    + (trend == null ? "--" : trend));

            trendText.setTextColor(
                    "BULLISH".equalsIgnoreCase(trend) ? GREEN
                            : "BEARISH".equalsIgnoreCase(trend)
                            ? RED : WHITE);
        }

        if (momentumText != null) {
            momentumText.setText(Double.isFinite(momentum)
                    ? "MOMENTUM\n" + formatPrice(market, momentum)
                    : "TIMEFRAME\n" + selectedTimeframe);
        }
    }

    // SIGNAL SCANNER

    private void requestCurrentSignal() {
        if (signalRepository == null || mtfScanning) return;

        if (!isForexMarketOpen(new Date())) {
            requestChartCandles();
            resetTradeStyleDisplay();
            return;
        }

        final String market = selectedMarket;
        final String timeframe = selectedTimeframe;

        pendingPrimarySignal = null;
        mtfResults.clear();
        resetMtfDisplay();
        resetTradeStyleDisplay();

        if (connectionStatus != null) {
            connectionStatus.setText("● SCANNING "
                    + market + " • " + timeframe);
            connectionStatus.setTextColor(YELLOW);
        }

        if (signalText != null) {
            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);
        }

        clearTradeLevels();

        if (priceText != null) {
            priceText.setText("PRICE  --");
        }

        signalRepository.requestSignal(market, timeframe);
    }

    private void requestMultiTimeframeAnalysis(String market) {
        if (marketDataProvider == null) {
            finishMtfWithWait("NO DATA PROVIDER");
            return;
        }

        if (!isForexMarketOpen(new Date())) {
            resetTradeStyleDisplay();
            return;
        }

        mtfResults.clear();
        mtfRequestIndex = 0;
        mtfScanning = true;

        resetMtfDisplay();
        resetTradeStyleDisplay();

        mtfConfluenceText.setText(
                "MTF CONFLUENCE • SCANNING 1/6");
        mtfConfluenceText.setTextColor(YELLOW);

        connectionStatus.setText(
                "● PRIMARY SIGNAL FOUND • CHECKING TIMEFRAMES");
        connectionStatus.setTextColor(YELLOW);

        requestNextMtfTimeframe(market);
    }

    private void requestNextMtfTimeframe(String market) {
        if (!mtfScanning) return;

        if (!isForexMarketOpen(new Date())) {
            mtfScanning = false;
            resetTradeStyleDisplay();
            return;
        }

        if (mtfRequestIndex >= timeframes.length) {
            finishMtfAnalysis();
            return;
        }

        final String timeframe = timeframes[mtfRequestIndex];

        mtfConfluenceText.setText(
                "MTF CONFLUENCE • SCANNING "
                        + (mtfRequestIndex + 1)
                        + "/6 • " + timeframe);

        marketDataProvider.requestCandles(
                market,
                timeframe,
                new MarketDataManager.MarketDataCallback() {

            @Override
            public void onCandlesReceived(List<Candle> candles) {
                runOnUiThread(() -> {
                    if (!mtfScanning) return;

                    MultiTimeframeEngine.TimeframeResult result =
                            MultiTimeframeEngine.analyzeTimeframe(
                                    market, timeframe, candles);

                    mtfResults.add(result);
                    updateMtfBox(timeframe, result);
                    mtfRequestIndex++;

                    if (mtfRequestIndex < timeframes.length) {
                        requestNextMtfTimeframe(market);
                    } else {
                        finishMtfAnalysis();
                    }
                });
            }

            @Override
            public void onPriceReceived(double price) {
                runOnUiThread(() -> {
                    if (Double.isFinite(price)
                            && market.equals(selectedMarket)
                            && priceText != null) {
                        priceText.setText(
                                "PRICE  " + formatPrice(market, price));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (!mtfScanning) return;

                    MultiTimeframeEngine.TimeframeResult result =
                            new MultiTimeframeEngine.TimeframeResult(
                                    timeframe,
                                    Signal.Direction.WAIT,
                                    "DATA ERROR",
                                    Double.NaN,
                                    Double.NaN);

                    mtfResults.add(result);
                    updateMtfBox(timeframe, result);
                    mtfRequestIndex++;

                    if (mtfRequestIndex < timeframes.length) {
                        requestNextMtfTimeframe(market);
                    } else {
                        finishMtfAnalysis();
                    }
                });
            }
        });
    }

    private void finishMtfAnalysis() {
        mtfScanning = false;

        if (!isForexMarketOpen(new Date())) {
            resetTradeStyleDisplay();
            return;
        }

        MultiTimeframeEngine.ConfluenceResult confluence =
                MultiTimeframeEngine.calculateConfluence(mtfResults);

        mtfConfluenceText.setText("MTF CONFLUENCE • BUY "
                + confluence.getBuyCount()
                + " • SELL " + confluence.getSellCount()
                + " • WAIT " + confluence.getWaitCount()
                + " • STRENGTH " + confluence.getStrength());

        if (pendingPrimarySignal == null) {
            finishMtfWithWait("NO PRIMARY SIGNAL");
            return;
        }

        Signal.Direction primary =
                pendingPrimarySignal.getDirection();

        Signal.Direction mtf = confluence.getDirection();

        boolean primaryTrade =
                primary == Signal.Direction.BUY
                || primary == Signal.Direction.SELL;

        boolean mtfTrade =
                mtf == Signal.Direction.BUY
                || mtf == Signal.Direction.SELL;

        boolean confirmed = primaryTrade
                && mtfTrade
                && primary == mtf
                && confluence.getStrength() >= 3;

        if (confirmed) {
            TradeStyleEngine.TradeStylePlan plan =
                    TradeStyleEngine.selectBestStyle(
                            pendingPrimarySignal, mtfResults);

            if (plan == null || !plan.isTradeAllowed()) {
                finishMtfWithWait("NO SUITABLE STYLE • "
                        + (plan == null
                        ? "INSUFFICIENT CONFIRMATION"
                        : plan.getReason()));
                return;
            }

            int color = mtf == Signal.Direction.BUY
                    ? GREEN : RED;

            mtfConfluenceText.setTextColor(color);

            connectionStatus.setText(
                    "● MTF CONFIRMED • " + mtf.name()
                            + " • STRENGTH "
                            + confluence.getStrength());
            connectionStatus.setTextColor(color);

            updateTradeStyleDisplay(plan);

            if (forexChartView != null) {
                forexChartView.setTradeLevels(
                        pendingPrimarySignal);
            }

            updateDashboard(pendingPrimarySignal);

        } else {
            mtfConfluenceText.setTextColor(YELLOW);

            connectionStatus.setText(
                    "● MTF NOT CONFIRMED • WAIT");
            connectionStatus.setTextColor(YELLOW);

            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);

            resetTradeStyleDisplay();
            clearTradeLevels();
        }
    }

    private void finishMtfWithWait(String reason) {
        mtfScanning = false;
        pendingPrimarySignal = null;

        resetTradeStyleDisplay();

        if (signalText != null) {
            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);
        }

        clearTradeLevels();

        if (forexChartView != null) {
            forexChartView.clearTradeLevels();
        }

        if (mtfConfluenceText != null) {
            mtfConfluenceText.setText(
                    "MTF CONFLUENCE • WAIT • " + reason);
            mtfConfluenceText.setTextColor(YELLOW);
        }

        if (connectionStatus != null) {
            connectionStatus.setText("● WAIT • " + reason);
            connectionStatus.setTextColor(YELLOW);
        }
    }

    private void resetMtfDisplay() {
        setMtfBox(mtf5mText, "5M", Signal.Direction.WAIT);
        setMtfBox(mtf15mText, "15M", Signal.Direction.WAIT);
        setMtfBox(mtf30mText, "30M", Signal.Direction.WAIT);
        setMtfBox(mtf1hText, "1H", Signal.Direction.WAIT);
        setMtfBox(mtf4hText, "4H", Signal.Direction.WAIT);
        setMtfBox(mtf1dText, "1D", Signal.Direction.WAIT);

        if (mtfConfluenceText != null) {
            mtfConfluenceText.setText(
                    "MTF CONFLUENCE • WAIT");
            mtfConfluenceText.setTextColor(YELLOW);
        }
    }

    private void updateMtfBox(
            String timeframe,
            MultiTimeframeEngine.TimeframeResult result) {

        if (result != null) {
            setMtfBox(
                    getMtfTextView(timeframe),
                    timeframe,
                    result.getDirection());
        }
    }

    private TextView getMtfTextView(String timeframe) {
        if ("5M".equals(timeframe)) return mtf5mText;
        if ("15M".equals(timeframe)) return mtf15mText;
        if ("30M".equals(timeframe)) return mtf30mText;
        if ("1H".equals(timeframe)) return mtf1hText;
        if ("4H".equals(timeframe)) return mtf4hText;
        return mtf1dText;
    }

    private void setMtfBox(
            TextView view,
            String timeframe,
            Signal.Direction direction) {

        if (view == null) return;

        String label = direction == null
                ? "WAIT" : direction.name();

        view.setText(timeframe + "\n" + label);

        view.setTextColor(
                direction == Signal.Direction.BUY ? GREEN
                : direction == Signal.Direction.SELL ? RED
                : YELLOW);
    }


    private TextView createSessionRow(String name) {
        TextView view = createText(
                "● " + name + "     CHECKING...", 12, MUTED, true);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 12));
        return view;
    }

    private Spinner createSpinner(String[] values) {
        Spinner spinner = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values) {

            private TextView styleText(View view) {
                TextView text = (TextView) view;
                text.setTextColor(
                        lightMode ? Color.BLACK : Color.WHITE);
                text.setBackgroundColor(
                        lightMode ? Color.WHITE : FIELD_BACKGROUND);
                text.setPadding(
                        dp(12), dp(10), dp(12), dp(10));
                return text;
            }

            @Override
            public View getView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent) {
                return styleText(super.getView(
                        position, convertView, parent));
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent) {
                return styleText(super.getDropDownView(
                        position, convertView, parent));
            }
        };

        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView createMetric(String label, String value) {
        TextView view = createText(
                label + "\n" + value, 14, WHITE, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(
                dp(10), dp(15), dp(10), dp(15));
        view.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 14));
        return view;
    }

    private TextView createTradeRow(String label, int color) {
        TextView view = createText(
                label + "                         --",
                13, color, true);
        view.setPadding(
                dp(12), dp(14), dp(8), dp(14));
        view.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 10));
        return view;
    }

    private TextView createTimeframeBox(String text) {
        TextView view = createText(text, 12, WHITE, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(
                dp(16), dp(14), dp(16), dp(14));
        view.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 12));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, dp(7), 0);
        view.setLayoutParams(p);
        return view;
    }

    private LinearLayout createMetricRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(14), dp(14), dp(14), dp(14));
        card.setBackground(createRoundedBackground(
                CARD, CARD_BORDER, 16));
        return card;
    }

    private GradientDrawable createRoundedBackground(
            int color, int strokeColor, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));

        if (strokeColor != 0) {
            background.setStroke(dp(1), strokeColor);
        }

        return background;
    }

    private LinearLayout.LayoutParams params(int topMargin) {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topMargin);
        return p;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1);
        p.setMargins(dp(4), 0, dp(4), 0);
        return p;
    }

    // FULL-SCREEN LANDSCAPE CHART

    private void enterFullscreenChart() {
        if (fullscreenChart || forexChartView == null) {
            return;
        }

        if (!(forexChartView.getParent() instanceof ViewGroup)) {
            return;
        }

        chartOriginalParent =
                (ViewGroup) forexChartView.getParent();

        chartOriginalIndex =
                chartOriginalParent.indexOfChild(forexChartView);

        chartOriginalLayoutParams =
                forexChartView.getLayoutParams();

        chartOriginalParent.removeView(forexChartView);

        View decorView = getWindow().getDecorView();

        previousSystemUiVisibility =
                decorView.getSystemUiVisibility();

        previousRequestedOrientation =
                getRequestedOrientation();

        fullscreenChartOverlay = new FrameLayout(this);
        fullscreenChartOverlay.setBackgroundColor(Color.BLACK);
        fullscreenChartOverlay.setFocusableInTouchMode(true);

        FrameLayout.LayoutParams chartParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);

        fullscreenChartOverlay.addView(
                forexChartView, chartParams);

        Button exitButton =
                createSmallActionButton("EXIT FULL SCREEN  ✕");

        exitButton.setOnClickListener(
                v -> exitFullscreenChart());

        FrameLayout.LayoutParams exitParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.END);

        exitParams.setMargins(
                dp(8), dp(8), dp(12), dp(8));

        fullscreenChartOverlay.addView(
                exitButton, exitParams);

        FrameLayout content =
                findViewById(android.R.id.content);

        content.addView(
                fullscreenChartOverlay,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        int immersiveFlags =
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        decorView.setSystemUiVisibility(immersiveFlags);
        fullscreenChart = true;

        setRequestedOrientation(
                android.content.pm.ActivityInfo
                        .SCREEN_ORIENTATION_LANDSCAPE);

        fullscreenChartOverlay.requestFocus();
    }

    private void exitFullscreenChart() {
        if (!fullscreenChart) {
            return;
        }

        if (forexChartView != null
                && forexChartView.getParent() instanceof ViewGroup) {
            ((ViewGroup) forexChartView.getParent())
                    .removeView(forexChartView);
        }

        if (fullscreenChartOverlay != null) {
            FrameLayout content =
                    findViewById(android.R.id.content);

            content.removeView(fullscreenChartOverlay);
            fullscreenChartOverlay = null;
        }

        if (chartOriginalParent != null
                && forexChartView != null) {

            int index = Math.max(
                    0,
                    Math.min(
                            chartOriginalIndex,
                            chartOriginalParent.getChildCount()));

            chartOriginalParent.addView(
                    forexChartView,
                    index,
                    chartOriginalLayoutParams);
        }

        fullscreenChart = false;

        getWindow().getDecorView()
                .setSystemUiVisibility(
                        previousSystemUiVisibility);

        setRequestedOrientation(
                previousRequestedOrientation);

        if (forexChartView != null) {
            forexChartView.requestLayout();
        }

        chartOriginalParent = null;
        chartOriginalLayoutParams = null;
        chartOriginalIndex = -1;
    }

    @Override
    public void onBackPressed() {
        if (fullscreenChart) {
            exitFullscreenChart();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockRunnable);
        chartRefreshHandler.removeCallbacks(chartRefreshRunnable);

        if (signalAlertCoordinator != null) {
            signalAlertCoordinator.stop();
        }

        if (backgroundScanner != null) {
            backgroundScanner.stop();
        }

        if (signalRepository != null) {
            signalRepository.stop();
        }

        if (marketDataProvider != null) {
            marketDataProvider.stop();
        }

        super.onDestroy();
    }
}
