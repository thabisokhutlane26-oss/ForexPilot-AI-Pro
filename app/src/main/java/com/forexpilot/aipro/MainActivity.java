
package com.forexpilot.aipro;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.widget.FrameLayout;
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

    private TextView connectionStatus, clockText, dateText;
    private TextView forexStatusText, nextMarketText;
    private TextView sydneySessionText, tokyoSessionText;
    private TextView londonSessionText, newYorkSessionText;
    private TextView marketName, priceText, signalText;
    private TextView trendText, rsiText, atrText, momentumText;
    private TextView entryText, stopLossText, tp1Text, tp2Text, tp3Text;
    private TextView mtf5mText, mtf15mText, mtf30mText;
    private TextView mtf1hText, mtf4hText, mtf1dText, mtfConfluenceText;

    private Spinner marketSpinner, timeframeSpinner;
    private Button scanButton, chartStyleButton, themeButton;

    private FrameLayout pageContainer;
    private View[] appPages;
    private Button[] navigationButtons;

    private int currentPage = 0;

    private String selectedMarket = "EUR/USD";
    private String selectedTimeframe = "15M";

    private boolean lineChartMode = false;
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
                getSharedPreferences("forexpilot_preferences", MODE_PRIVATE);

        lightMode = preferences.getBoolean("light_mode", false);
        lineChartMode = preferences.getBoolean("line_chart_mode", false);

        applyThemeColors();
        buildDashboard();
        updateTimeAndSessions();
        clockHandler.post(clockRunnable);
        connectToMarketData();
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

        getSharedPreferences("forexpilot_preferences", MODE_PRIVATE)
                .edit()
                .putBoolean("light_mode", lightMode)
                .apply();

        applyThemeColors();
        buildDashboard();
        updateTimeAndSessions();

        if (marketDataProvider != null) {
            requestChartCandles();
        }

        if (signalRepository != null && isForexMarketOpen(new Date())) {
            requestCurrentSignal();
        }
    }

    private void buildDashboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        // PROFESSIONAL HEADER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(12));
        header.setBackgroundColor(CARD);

        TextView title = createText("FOREXPILOT AI PRO", 23, WHITE, true);
        header.addView(title);

        header.addView(createText(
                "TRADING INTELLIGENCE  •  SAST", 11, BLUE, true),
                params(3));

        connectionStatus = createText(
                "● CONNECTING TO MARKET", 11, YELLOW, true);
        header.addView(connectionStatus, params(6));
        root.addView(header);

        // PAGE CONTAINER
        pageContainer = new FrameLayout(this);
        LinearLayout.LayoutParams contentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(pageContainer, contentParams);

        appPages = new View[5];
        navigationButtons = new Button[5];

        LinearLayout home = createPage(0);
        LinearLayout charts = createPage(1);
        LinearLayout analysis = createPage(2);
        LinearLayout trade = createPage(3);
        LinearLayout settings = createPage(4);

        // HOME PAGE
        LinearLayout clockCard = createCard();
        clockCard.addView(createSmallLabel("SOUTH AFRICAN MARKET CLOCK"));

        clockText = createText("--:--:-- SAST", 27, WHITE, true);
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

        // CHARTS PAGE
        LinearLayout chartCard = createCard();
        chartCard.addView(createText("PRICE CHART", 19, WHITE, true));
        chartCard.addView(createText(
                "Candlestick and line chart", 12, MUTED, false),
                params(3));

        chartStyleButton = createButton("");
        updateChartStyleButtonText();
        chartStyleButton.setOnClickListener(v -> toggleChartStyle());
        chartCard.addView(chartStyleButton, params(8));

        forexChartView = new ForexChartView(this);
        forexChartView.setLineChartMode(lineChartMode);

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
                        + "\n\nChoose the pair and timeframe on Home. "
                        + "Use the chart-style button to switch views.",
                13, MUTED, false), params(6));
        charts.addView(chartInfo, params(8));

        // ANALYSIS PAGE
        LinearLayout indicatorCard = createCard();
        indicatorCard.addView(createText(
                "TECHNICAL ANALYSIS", 19, WHITE, true));
        indicatorCard.addView(createText(
                "Indicators from available market candles",
                12, MUTED, false), params(3));

        LinearLayout row1 = createMetricRow();
        trendText = createMetric("TREND", "--");
        rsiText = createMetric("RSI", "--");
        row1.addView(trendText, weightParams());
        row1.addView(rsiText, weightParams());
        indicatorCard.addView(row1, params(8));

        LinearLayout row2 = createMetricRow();
        atrText = createMetric("ATR", "--");
        momentumText = createMetric("TIMEFRAME", selectedTimeframe);
        row2.addView(atrText, weightParams());
        row2.addView(momentumText, weightParams());
        indicatorCard.addView(row2, params(8));
        analysis.addView(indicatorCard, params(8));

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

        // TRADE PLAN PAGE
        LinearLayout tradeCard = createCard();
        tradeCard.addView(createText("TRADE PLAN", 21, WHITE, true));
        tradeCard.addView(createText(
                "Trade levels appear after signal confirmation.",
                12, MUTED, false), params(4));

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

        // SETTINGS PAGE
        LinearLayout appearanceCard = createCard();
        appearanceCard.addView(createText(
                "APPEARANCE", 18, WHITE, true));
        appearanceCard.addView(createText(
                "Change the app colour theme.",
                12, MUTED, false), params(4));

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
                "Market scanner • Technical indicators"
                        + "\nMulti-timeframe confirmation"
                        + "\nTrade planning tools"
                        + "\n\nSignals are informational and are not "
                        + "financial advice.",
                13, MUTED, false), params(6));
        settings.addView(aboutCard, params(8));

        // FIXED BOTTOM NAVIGATION
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(3), dp(5), dp(3), dp(5));
        nav.setBackgroundColor(CARD);

        String[] labels = {
                "⌂\nHOME",
                "▥\nCHARTS",
                "◈\nANALYSIS",
                "⇄\nTRADE",
                "⚙\nSETTINGS"
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

        showPage(currentPage);
    }

    private LinearLayout createPage(int index) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), dp(8), dp(12), dp(16));
        scroll.addView(body);

        appPages[index] = scroll;

        FrameLayout.LayoutParams pageParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);

        pageContainer.addView(scroll, pageParams);
        return body;
    }

    private void showPage(int index) {
        if (appPages == null || navigationButtons == null) return;

        currentPage = Math.max(0, Math.min(index, appPages.length - 1));

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

    private void toggleChartStyle() {
        lineChartMode = !lineChartMode;

        getSharedPreferences("forexpilot_preferences", MODE_PRIVATE)
                .edit()
                .putBoolean("line_chart_mode", lineChartMode)
                .apply();

        if (forexChartView != null) {
            forexChartView.setLineChartMode(lineChartMode);
        }

        updateChartStyleButtonText();
    }

    private void updateChartStyleButtonText() {
        if (chartStyleButton != null) {
            chartStyleButton.setText(lineChartMode
                    ? "LINE CHART  •  SWITCH TO CANDLESTICKS"
                    : "CANDLESTICKS  •  SWITCH TO LINE");
        }
    }

    private void resetForNewSelection() {
        mtfScanning = false;
        pendingPrimarySignal = null;
        mtfResults.clear();
        mtfRequestIndex = 0;

        resetMtfDisplay();
        clearTradeLevels();

        if (signalText != null) {
            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);
        }

        if (priceText != null) priceText.setText("PRICE  --");
        if (trendText != null) trendText.setText("TREND\n--");
        if (rsiText != null) rsiText.setText("RSI\n--");
        if (atrText != null) atrText.setText("ATR\n--");

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
            forexChartView.setLineChartMode(lineChartMode);
        }

        requestChartCandles();

        if (signalRepository != null && isForexMarketOpen(new Date())) {
            requestCurrentSignal();
        }
    }

    private void updateTimeAndSessions() {
        Date now = new Date();

        SimpleDateFormat timeFormat =
                new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        timeFormat.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEEE • dd MMMM yyyy", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));

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
                TimeZone.getTimeZone("America/New_York"), Locale.ENGLISH);
        ny.setTime(date);

        int day = ny.get(Calendar.DAY_OF_WEEK);
        int minutes = ny.get(Calendar.HOUR_OF_DAY) * 60
                + ny.get(Calendar.MINUTE);

        if (day == Calendar.SATURDAY) return false;
        if (day == Calendar.SUNDAY) return minutes >= 17 * 60;
        if (day == Calendar.FRIDAY) return minutes < 17 * 60;
        return true;
    }

    private void updateSession(TextView view, String name, String zone,
                               int openHour, int closeHour) {
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
                + "     •     SAST " + sastTime);

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
    }

    private void connectToMarketData() {
        String apiKey = AppConfig.getTwelveDataApiKey();
        marketDataProvider = new TwelveDataProvider(apiKey);

        signalRepository = new SignalRepository(
                marketDataProvider, new SignalRepository.SignalCallback() {

            @Override
            public void onCandles(String symbol, String timeframe,
                                  List<Candle> candles) {
                runOnUiThread(() -> {
                    if (forexChartView == null
                            || !selectedMarket.equals(symbol)
                            || !selectedTimeframe.equals(timeframe)
                            || candles == null || candles.isEmpty()) {
                        return;
                    }

                    forexChartView.setCandles(candles);
                    forexChartView.setLineChartMode(lineChartMode);
                    updateIndicatorDashboard(symbol, candles);
                });
            }

            @Override
            public void onSignal(Signal signal) {
                runOnUiThread(() -> {
                    if (signal == null) return;

                    if (!isForexMarketOpen(new Date())) {
                        updatePrimaryDashboard(signal);

                        connectionStatus.setText(
                                "● HISTORICAL SIGNAL DATA • MARKET CLOSED");
                        connectionStatus.setTextColor(MUTED);
                        return;
                    }

                    pendingPrimarySignal = signal;
                    updatePrimaryDashboard(signal);
                    requestMultiTimeframeAnalysis(signal.getSymbol());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    mtfScanning = false;

                    if (connectionStatus != null) {
                        connectionStatus.setText(
                                "● DATA ERROR • " + selectedMarket);
                        connectionStatus.setTextColor(RED);
                    }

                    if (signalText != null) {
                        signalText.setText("WAIT");
                        signalText.setTextColor(YELLOW);
                    }

                    if (nextMarketText != null && message != null
                            && !message.trim().isEmpty()) {
                        nextMarketText.setText("DATA ERROR • " + message);
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

        marketDataProvider.requestCandles(market, timeframe,
                new MarketDataManager.MarketDataCallback() {

            @Override
            public void onCandlesReceived(List<Candle> candles) {
                runOnUiThread(() -> {
                    if (!market.equals(selectedMarket)
                            || !timeframe.equals(selectedTimeframe)) {
                        return;
                    }

                    if (candles == null || candles.isEmpty()) {
                        connectionStatus.setText("● CHART DATA EMPTY");
                        connectionStatus.setTextColor(RED);
                        return;
                    }

                    forexChartView.setCandles(candles);
                    forexChartView.setLineChartMode(lineChartMode);
                    updateIndicatorDashboard(market, candles);

                    Candle latest = candles.get(candles.size() - 1);

                    if (latest != null
                            && Double.isFinite(latest.getClose())) {
                        priceText.setText("PRICE  "
                                + formatPrice(market, latest.getClose()));
                    }

                    boolean open = isForexMarketOpen(new Date());

                    connectionStatus.setText((open
                            ? "● LIVE CHART • "
                            : "● HISTORICAL CHART • ")
                            + market + " • " + timeframe
                            + " • " + candles.size() + " CANDLES");

                    connectionStatus.setTextColor(open ? GREEN : MUTED);
                });
            }

            @Override
            public void onPriceReceived(double price) {
                runOnUiThread(() -> {
                    if (market.equals(selectedMarket)
                            && Double.isFinite(price)) {
                        priceText.setText(
                                "PRICE  " + formatPrice(market, price));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (!market.equals(selectedMarket)) return;

                    connectionStatus.setText("● CHART DATA ERROR");
                    connectionStatus.setTextColor(RED);

                    nextMarketText.setText("CHART ERROR • "
                            + (message == null
                            ? "UNKNOWN ERROR" : message));
                    nextMarketText.setTextColor(RED);
                });
            }
        });
    }

    private void updateIndicatorDashboard(
            String market, List<Candle> candles) {
        if (candles == null || candles.isEmpty()) return;

        String trend = IndicatorEngine.trend(candles, 20, 50);
        double rsi = IndicatorEngine.rsi(candles, 14);
        double atr = IndicatorEngine.atr(candles, 14);
        double momentum = IndicatorEngine.momentum(candles, 5);

        if (trendText != null) {
            trendText.setText(
                    "TREND\n" + (trend == null ? "--" : trend));

            trendText.setTextColor(
                    "BULLISH".equalsIgnoreCase(trend) ? GREEN
                            : "BEARISH".equalsIgnoreCase(trend) ? RED
                            : WHITE);
        }

        if (rsiText != null) {
            rsiText.setText(Double.isFinite(rsi)
                    ? "RSI\n" + String.format(Locale.US, "%.2f", rsi)
                    : "RSI\n--");

            if (Double.isFinite(rsi)) {
                rsiText.setTextColor(
                        rsi >= 70 ? RED : rsi <= 30 ? GREEN : WHITE);
            }
        }

        if (atrText != null) {
            atrText.setText("ATR\n"
                    + (Double.isFinite(atr)
                    ? formatPrice(market, atr) : "--"));
        }

        if (momentumText != null) {
            momentumText.setText(Double.isFinite(momentum)
                    ? "MOMENTUM\n" + formatPrice(market, momentum)
                    : "TIMEFRAME\n" + selectedTimeframe);
        }
    }

    private void requestCurrentSignal() {
        if (signalRepository == null || mtfScanning) return;

        if (!isForexMarketOpen(new Date())) {
            requestChartCandles();
            return;
        }

        final String market = selectedMarket;
        final String timeframe = selectedTimeframe;

        pendingPrimarySignal = null;
        mtfResults.clear();
        resetMtfDisplay();

        connectionStatus.setText("● SCANNING "
                + market + " • " + timeframe);
        connectionStatus.setTextColor(YELLOW);

        signalText.setText("WAIT");
        signalText.setTextColor(YELLOW);

        clearTradeLevels();
        priceText.setText("PRICE  --");

        signalRepository.requestSignal(market, timeframe);
    }

    private void requestMultiTimeframeAnalysis(String market) {
        if (marketDataProvider == null) {
            finishMtfWithWait("NO DATA PROVIDER");
            return;
        }

        if (!isForexMarketOpen(new Date())) return;

        mtfResults.clear();
        mtfRequestIndex = 0;
        mtfScanning = true;
        resetMtfDisplay();

        mtfConfluenceText.setText("MTF CONFLUENCE • SCANNING 1/6");
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
            return;
        }

        if (mtfRequestIndex >= timeframes.length) {
            finishMtfAnalysis();
            return;
        }

        final String timeframe = timeframes[mtfRequestIndex];

        mtfConfluenceText.setText("MTF CONFLUENCE • SCANNING "
                + (mtfRequestIndex + 1) + "/6 • " + timeframe);

        marketDataProvider.requestCandles(market, timeframe,
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
                    if (Double.isFinite(price)) {
                        priceText.setText(
                                "PRICE  " + formatPrice(market, price));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (!mtfScanning) return;

                    mtfResults.add(
                            new MultiTimeframeEngine.TimeframeResult(
                                    timeframe, Signal.Direction.WAIT,
                                    "DATA ERROR", Double.NaN, Double.NaN));

                    updateMtfBox(timeframe,
                            mtfResults.get(mtfResults.size() - 1));

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
        if (!isForexMarketOpen(new Date())) return;

        MultiTimeframeEngine.ConfluenceResult confluence =
                MultiTimeframeEngine.calculateConfluence(mtfResults);

        mtfConfluenceText.setText("MTF CONFLUENCE • BUY "
                + confluence.getBuyCount() + " • SELL "
                + confluence.getSellCount() + " • WAIT "
                + confluence.getWaitCount() + " • STRENGTH "
                + confluence.getStrength());

        if (pendingPrimarySignal == null) {
            finishMtfWithWait("NO PRIMARY SIGNAL");
            return;
        }

        Signal.Direction primary = pendingPrimarySignal.getDirection();
        Signal.Direction mtf = confluence.getDirection();

        boolean primaryTrade = primary == Signal.Direction.BUY
                || primary == Signal.Direction.SELL;

        boolean mtfTrade = mtf == Signal.Direction.BUY
                || mtf == Signal.Direction.SELL;

        boolean confirmed = primaryTrade && mtfTrade
                && primary == mtf && confluence.getStrength() >= 3;

        if (confirmed) {
            int color = mtf == Signal.Direction.BUY ? GREEN : RED;

            mtfConfluenceText.setTextColor(color);
            connectionStatus.setText("● MTF CONFIRMED • "
                    + mtf.name() + " • STRENGTH "
                    + confluence.getStrength());
            connectionStatus.setTextColor(color);

            if (forexChartView != null) {
                forexChartView.setTradeLevels(pendingPrimarySignal);
            }

            updateDashboard(pendingPrimarySignal);
        } else {
            mtfConfluenceText.setTextColor(YELLOW);
            connectionStatus.setText("● MTF NOT CONFIRMED • WAIT");
            connectionStatus.setTextColor(YELLOW);
            signalText.setText("WAIT");
            signalText.setTextColor(YELLOW);
            clearTradeLevels();
        }
    }

    private void finishMtfWithWait(String reason) {
        mtfScanning = false;

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
    }

    private void resetMtfDisplay() {
        setMtfBox(mtf5mText, "5M", Signal.Direction.WAIT);
        setMtfBox(mtf15mText, "15M", Signal.Direction.WAIT);
        setMtfBox(mtf30mText, "30M", Signal.Direction.WAIT);
        setMtfBox(mtf1hText, "1H", Signal.Direction.WAIT);
        setMtfBox(mtf4hText, "4H", Signal.Direction.WAIT);
        setMtfBox(mtf1dText, "1D", Signal.Direction.WAIT);

        if (mtfConfluenceText != null) {
            mtfConfluenceText.setText("MTF CONFLUENCE • WAIT");
            mtfConfluenceText.setTextColor(YELLOW);
        }
    }

    private void updateMtfBox(
            String timeframe,
            MultiTimeframeEngine.TimeframeResult result) {
        if (result != null) {
            setMtfBox(getMtfTextView(timeframe),
                    timeframe, result.getDirection());
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

    private void setMtfBox(TextView view, String timeframe,
                           Signal.Direction direction) {
        if (view == null) return;

        String label = direction == null ? "WAIT" : direction.name();
        view.setText(timeframe + "\n" + label);

        view.setTextColor(direction == Signal.Direction.BUY ? GREEN
                : direction == Signal.Direction.SELL ? RED : YELLOW);
    }

    private void updatePrimaryDashboard(Signal signal) {
        if (signal == null
                || !selectedMarket.equals(signal.getSymbol())
                || !selectedTimeframe.equals(signal.getTimeframe())) {
            return;
        }

        if (!Double.isNaN(signal.getEntry())) {
            priceText.setText("PRICE  "
                    + formatPrice(signal.getSymbol(), signal.getEntry()));
        }

        trendText.setText("TREND\n" + signal.getTrend());

        rsiText.setText(Double.isNaN(signal.getRsi()) ? "RSI\n--"
                : "RSI\n" + String.format(
                        Locale.US, "%.2f", signal.getRsi()));

        atrText.setText(Double.isNaN(signal.getAtr()) ? "ATR\n--"
                : "ATR\n" + formatPrice(
                        signal.getSymbol(), signal.getAtr()));

        momentumText.setText("TIMEFRAME\n" + signal.getTimeframe());
    }

    private void updateDashboard(Signal signal) {
        if (signal == null) return;

        if (!isForexMarketOpen(new Date())) {
            updatePrimaryDashboard(signal);
            return;
        }

        if (!selectedMarket.equals(signal.getSymbol())
                || !selectedTimeframe.equals(signal.getTimeframe())) {
            return;
        }

        marketName.setText(signal.getSymbol());

        connectionStatus.setText("● LIVE DATA • MTF CONFIRMED • "
                + signal.getSymbol() + " • " + signal.getTimeframe());

        connectionStatus.setTextColor(
                signal.getDirection() == Signal.Direction.BUY ? GREEN
                        : signal.getDirection() == Signal.Direction.SELL
                        ? RED : YELLOW);

        priceText.setText(Double.isNaN(signal.getEntry()) ? "PRICE  --"
                : "PRICE  " + formatPrice(
                        signal.getSymbol(), signal.getEntry()));

        signalText.setText(signal.getDirection().name());
        signalText.setTextColor(
                signal.getDirection() == Signal.Direction.BUY ? GREEN
                        : signal.getDirection() == Signal.Direction.SELL
                        ? RED : YELLOW);

        trendText.setText("TREND\n" + signal.getTrend());

        rsiText.setText(Double.isNaN(signal.getRsi()) ? "RSI\n--"
                : "RSI\n" + String.format(
                        Locale.US, "%.2f", signal.getRsi()));

        atrText.setText(Double.isNaN(signal.getAtr()) ? "ATR\n--"
                : "ATR\n" + formatPrice(
                        signal.getSymbol(), signal.getAtr()));

        momentumText.setText("TIMEFRAME\n" + signal.getTimeframe());

        if (signal.getDirection() == Signal.Direction.BUY
                || signal.getDirection() == Signal.Direction.SELL) {

            entryText.setText("ENTRY                         "
                    + formatPrice(signal.getSymbol(), signal.getEntry()));

            stopLossText.setText("STOP LOSS                  "
                    + formatPrice(signal.getSymbol(), signal.getStopLoss()));

            tp1Text.setText("TAKE PROFIT 1          "
                    + formatPrice(signal.getSymbol(),
                    signal.getTakeProfit1()));

            tp2Text.setText("TAKE PROFIT 2          "
                    + formatPrice(signal.getSymbol(),
                    signal.getTakeProfit2()));

            tp3Text.setText("TAKE PROFIT 3          "
                    + formatPrice(signal.getSymbol(),
                    signal.getTakeProfit3()));

            if (forexChartView != null) {
                forexChartView.setTradeLevels(signal);
            }
        } else {
            clearTradeLevels();
        }
    }

    private void clearTradeLevels() {
        if (entryText != null) entryText.setText("ENTRY                         --");
        if (stopLossText != null) stopLossText.setText("STOP LOSS                  --");
        if (tp1Text != null) tp1Text.setText("TAKE PROFIT 1          --");
        if (tp2Text != null) tp2Text.setText("TAKE PROFIT 2          --");
        if (tp3Text != null) tp3Text.setText("TAKE PROFIT 3          --");

        if (forexChartView != null) {
            forexChartView.clearTradeLevels();
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

    private int dp(float value) {
        return (int) (value
                * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView createText(
            String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private TextView createSmallLabel(String text) {
        return createText(text, 11, MUTED, true);
    }

    private Button createButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(46));

        GradientDrawable background = new GradientDrawable();
        background.setColor(BUTTON_BACKGROUND);
        background.setCornerRadius(dp(12));
        button.setBackground(background);

        return button;
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView createMetric(String label, String value) {
        TextView view = createText(
                label + "\n" + value, 14, WHITE, true);

        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(15), dp(10), dp(15));
        view.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 14));

        return view;
    }

    private TextView createTradeRow(String label, int color) {
        TextView view = createText(
                label + "                         --", 13, color, true);

        view.setPadding(dp(12), dp(14), dp(8), dp(14));
        view.setBackground(createRoundedBackground(
                FIELD_BACKGROUND, CARD_BORDER, 10));

        return view;
    }

    private TextView createTimeframeBox(String text) {
        TextView view = createText(text, 12, WHITE, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(14), dp(16), dp(14));
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
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
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
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

        p.setMargins(dp(4), 0, dp(4), 0);
        return p;
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockRunnable);

        if (signalRepository != null) {
            signalRepository.stop();
        }

        if (marketDataProvider != null) {
            marketDataProvider.stop();
        }

        super.onDestroy();
    }
}
