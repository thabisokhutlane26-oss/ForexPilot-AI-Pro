
package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ForexChartView extends View {

    public static final int MODE_CANDLES = 0;
    public static final int MODE_LINE = 1;
    public static final int MODE_BARS = 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Candle> candles = new ArrayList<>();
    private final List<DrawingObject> drawings = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;

    private int chartMode = MODE_CANDLES;

    private int backgroundColor = Color.rgb(10, 15, 25);
    private int gridColor = Color.rgb(35, 45, 60);
    private int borderColor = Color.rgb(65, 75, 90);
    private int textColor = Color.rgb(175, 185, 200);
    private int bullColor = Color.rgb(18, 209, 142);
    private int bearColor = Color.rgb(255, 72, 87);
    private int lineChartColor = Color.rgb(70, 170, 255);
    private int indicatorLineColor = Color.rgb(60, 140, 255);
    private int indicatorBuyColor = Color.rgb(0, 235, 140);
    private int indicatorSellColor = Color.rgb(255, 75, 95);
    private int priceLineColor = Color.rgb(100, 160, 220);
    private int entryColor = Color.rgb(80, 180, 255);
    private int stopColor = Color.rgb(255, 140, 40);
    private int targetColor = Color.rgb(18, 209, 142);
    private int volumeColor = Color.GREEN;
    private int maColor = Color.BLUE;
    private int bollingerColor = Color.CYAN;
    private int macdColor = Color.BLUE;
    private int rsiColor = Color.BLUE;
    private int stochasticKColor = Color.YELLOW;
    private int stochasticDColor = Color.RED;
    private int fractalColor = Color.BLUE;

    private float candleWidth = 18f;
    private float spacing = 6f;
    private float scrollOffset;
    private float lastX;
    private float lastY;
    private boolean moving;
    private boolean showCrosshair;
    private float crosshairX = -1f;
    private float crosshairY = -1f;
    private int selectedCandleIndex = -1;

    private double entry = Double.NaN;
    private double stopLoss = Double.NaN;
    private double tp1 = Double.NaN;
    private double tp2 = Double.NaN;
    private double tp3 = Double.NaN;
    private Signal.Direction signalDirection = Signal.Direction.WAIT;

    private String activeDrawingTool = "";
    private float drawingStartX;
    private float drawingStartY;
    private boolean placingDrawing;
    private DrawingObject selectedDrawing;

    private boolean showBollinger = false;
    private boolean showMovingAverage = false;
    private boolean showMacd = false;
    private boolean showRsi = false;
    private boolean showStochastic = false;
    private boolean showVolumes = false;
    private boolean showFractals = false;

    private int bollingerPeriod = 20;
    private int movingAveragePeriod = 50;
    private int macdFastPeriod = 12;
    private int macdSlowPeriod = 26;
    private int rsiPeriod = 14;
    private int rsiUpperLevel = 70;
    private int rsiLowerLevel = 30;
    private int stochasticKPeriod = 5;
    private int stochasticDPeriod = 3;
    private int stochasticSlowing = 3;

    private static final float LEFT = 18f;
    private static final float RIGHT = 96f;
    private static final float TOP = 28f;
    private static final float BOTTOM = 48f;

    private static class DrawingObject {
        String type;
        float x1, y1, x2, y2;
        int color;
        String note;

        DrawingObject(String type, float x1, float y1,
                      float x2, float y2, int color) {
            this.type = type;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
        }
    }

    public ForexChartView(Context context) {
        super(context);
        paint.setStrokeWidth(2f);
        setBackgroundColor(backgroundColor);
        setFocusable(true);
        setFocusableInTouchMode(true);
        scaleDetector = new ScaleGestureDetector(
                context, new ScaleListener());
    }

    // ---------------------------------------------------------
    // CHART STYLE
    // ---------------------------------------------------------

    public void setChartMode(int mode) {
        if (mode >= MODE_CANDLES && mode <= MODE_BARS) {
            chartMode = mode;
            invalidate();
        }
    }

    public int getChartMode() {
        return chartMode;
    }

    public void setLineChartMode(boolean enabled) {
        setChartMode(enabled ? MODE_LINE : MODE_CANDLES);
    }

    public boolean isLineChartMode() {
        return chartMode == MODE_LINE;
    }

    public void setBarChartMode(boolean enabled) {
        setChartMode(enabled ? MODE_BARS : MODE_CANDLES);
    }

    public boolean isBarChartMode() {
        return chartMode == MODE_BARS;
    }

    public void toggleChartStyle() {
        chartMode = chartMode == MODE_CANDLES
                ? MODE_LINE : MODE_CANDLES;
        invalidate();
    }

    public void zoomIn() {
        candleWidth = Math.min(55f, candleWidth + 4f);
        updateSpacing();
        clampScroll();
        invalidate();
    }

    public void zoomOut() {
        candleWidth = Math.max(5f, candleWidth - 4f);
        updateSpacing();
        clampScroll();
        invalidate();
    }

    public void fitLatest() {
        scrollOffset = 0;
        invalidate();
    }

    private void updateSpacing() {
        spacing = Math.max(2f, Math.min(14f, candleWidth * .30f));
    }

    // ---------------------------------------------------------
    // COLORS
    // ---------------------------------------------------------

    public void setChartColors(int background, int grid,
                               int bull, int bear, int line, int text) {
        backgroundColor = background;
        gridColor = grid;
        bullColor = bull;
        bearColor = bear;
        lineChartColor = line;
        textColor = text;
        setBackgroundColor(background);
        invalidate();
    }

    public void setBackgroundChartColor(int color) {
        backgroundColor = color;
        setBackgroundColor(color);
        invalidate();
    }

    public void setGridColor(int color) {
        gridColor = color;
        invalidate();
    }

    public void setBullColor(int color) {
        bullColor = color;
        invalidate();
    }

    public void setBearColor(int color) {
        bearColor = color;
        invalidate();
    }

    public void setLineColor(int color) {
        lineChartColor = color;
        invalidate();
    }

    public void setTextColor(int color) {
        textColor = color;
        invalidate();
    }

    public void setIndicatorLineColor(int color) {
        indicatorLineColor = color;
        invalidate();
    }

    public void setTradeColors(int entry, int stop, int target) {
        entryColor = entry;
        stopColor = stop;
        targetColor = target;
        invalidate();
    }

    public void setColorRole(String role, int color) {
        if (role == null) return;

        switch (role.toUpperCase(Locale.US)) {
            case "BACKGROUND": backgroundColor = color; setBackgroundColor(color); break;
            case "FOREGROUND":
            case "TEXT": textColor = color; break;
            case "GRID": gridColor = color; break;
            case "BULL":
            case "BAR_UP": bullColor = color; break;
            case "BEAR":
            case "BAR_DOWN": bearColor = color; break;
            case "LINE":
            case "CHART_LINE": lineChartColor = color; break;
            case "VOLUME":
            case "VOLUMES": volumeColor = color; break;
            case "INDICATOR":
            case "MA": maColor = color; break;
            case "BOLLINGER": bollingerColor = color; break;
            case "MACD": macdColor = color; break;
            case "RSI": rsiColor = color; break;
            case "STOCHASTIC_K": stochasticKColor = color; break;
            case "STOCHASTIC_D": stochasticDColor = color; break;
            case "FRACTALS": fractalColor = color; break;
            case "ENTRY": entryColor = color; break;
            case "SL":
            case "STOP": stopColor = color; break;
            case "TP":
            case "TARGET": targetColor = color; break;
            case "BUY": indicatorBuyColor = color; break;
            case "SELL": indicatorSellColor = color; break;
        }
        invalidate();
    }

    // ---------------------------------------------------------
    // CANDLE DATA AND TRADE LEVELS
    // ---------------------------------------------------------

    public void setCandles(List<Candle> newCandles) {
        candles.clear();
        if (newCandles != null) candles.addAll(newCandles);
        scrollOffset = 0;
        selectedCandleIndex = -1;
        showCrosshair = false;
        invalidate();
    }

    public void clearChart() {
        candles.clear();
        scrollOffset = 0;
        selectedCandleIndex = -1;
        showCrosshair = false;
        invalidate();
    }

    public void setTradeLevels(Signal signal) {
        if (signal == null) {
            clearTradeLevels();
            return;
        }

        entry = signal.getEntry();
        stopLoss = signal.getStopLoss();
        tp1 = signal.getTakeProfit1();
        tp2 = signal.getTakeProfit2();
        tp3 = signal.getTakeProfit3();
        signalDirection = signal.getDirection();
        invalidate();
    }

    public void clearTradeLevels() {
        entry = stopLoss = tp1 = tp2 = tp3 = Double.NaN;
        signalDirection = Signal.Direction.WAIT;
        invalidate();
    }

    // ---------------------------------------------------------
    // INDICATOR CONTROLS
    // ---------------------------------------------------------

    public void setIndicatorEnabled(String name, boolean enabled) {
        if (name == null) return;

        switch (name.toUpperCase(Locale.US).replace(" ", "")) {
            case "BOLLINGER":
            case "BOLLINGERBANDS": showBollinger = enabled; break;
            case "MA":
            case "EMA":
            case "MOVINGAVERAGE": showMovingAverage = enabled; break;
            case "MACD": showMacd = enabled; break;
            case "RSI": showRsi = enabled; break;
            case "STOCHASTIC": showStochastic = enabled; break;
            case "VOLUME":
            case "VOLUMES": showVolumes = enabled; break;
            case "FRACTALS": showFractals = enabled; break;
        }
        invalidate();
    }

    public void setIndicatorPeriod(String name, int period) {
        if (name == null || period < 1) return;

        switch (name.toUpperCase(Locale.US).replace(" ", "")) {
            case "BB":
            case "BOLLINGER":
            case "BOLLINGERBANDS": bollingerPeriod = period; break;
            case "MA":
            case "EMA":
            case "MOVINGAVERAGE": movingAveragePeriod = period; break;
            case "RSI": rsiPeriod = period; break;
            case "STOCHASTIC":
            case "STOCHASTIC_K": stochasticKPeriod = period; break;
            case "STOCHASTIC_D": stochasticDPeriod = period; break;
            case "STOCHASTIC_SLOWING": stochasticSlowing = period; break;
            case "MACD_FAST": macdFastPeriod = period; break;
            case "MACD_SLOW": macdSlowPeriod = period; break;
        }
        invalidate();
    }

    public void setRsiLevels(int upper, int lower) {
        rsiUpperLevel = Math.max(51, Math.min(99, upper));
        rsiLowerLevel = Math.max(1, Math.min(49, lower));
        invalidate();
    }

    public void setStochasticColors(int kColor, int dColor) {
        stochasticKColor = kColor;
        stochasticDColor = dColor;
        invalidate();
    }

    // ---------------------------------------------------------
    // DRAWING TOOLS
    // ---------------------------------------------------------

    public void setDrawingTool(String tool) {
        activeDrawingTool = tool == null ? "" : tool.trim();
        placingDrawing = false;
        selectedDrawing = null;
        invalidate();
    }

    public String getDrawingTool() {
        return activeDrawingTool;
    }

    public void cancelDrawingTool() {
        activeDrawingTool = "";
        placingDrawing = false;
        selectedDrawing = null;
        invalidate();
    }

    public void addDrawing(String type, float x1, float y1,
                           float x2, float y2) {
        drawings.add(new DrawingObject(
                type, x1, y1, x2, y2, Color.YELLOW));
        invalidate();
    }

    public void setDrawingColor(int color) {
        if (selectedDrawing != null) {
            selectedDrawing.color = color;
            invalidate();
        }
    }

    public void removeSelectedDrawing() {
        if (selectedDrawing != null) {
            drawings.remove(selectedDrawing);
            selectedDrawing = null;
            invalidate();
        }
    }

    public void setSelectedDrawingNote(String note) {
        if (selectedDrawing != null) {
            selectedDrawing.note = note;
            invalidate();
        }
    }

    public void clearDrawings() {
        // Drawing objects are cleared only when explicitly requested
        // by the calling code. No Delete All UI button is provided.
        drawings.clear();
        selectedDrawing = null;
        invalidate();
    }

    // ---------------------------------------------------------
    // MAIN RENDERING
    // ---------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);
        drawGrid(canvas);

        if (candles.isEmpty()) {
            drawMessage(canvas, "WAITING FOR LIVE CANDLE DATA");
            drawObjects(canvas);
            return;
        }

        int start = calculateStartIndex();
        int end = Math.min(candles.size(), start + calculateVisibleCount());

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = start; i < end; i++) {
            Candle c = candles.get(i);
            if (!valid(c)) continue;
            min = Math.min(min, c.getLow());
            max = Math.max(max, c.getHigh());
        }

        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            drawMessage(canvas, "INVALID CANDLE DATA");
            return;
        }

        if (max <= min) {
            double pad = Math.max(Math.abs(max) * .001, .0001);
            min -= pad;
            max += pad;
        }

        double tMin = validMinTrade();
        double tMax = validMaxTrade();
        if (Double.isFinite(tMin)) min = Math.min(min, tMin);
        if (Double.isFinite(tMax)) max = Math.max(max, tMax);

        double pad = Math.max((max - min) * .08, .00001);
        min -= pad;
        max += pad;

        double range = max - min;
        float top = TOP;
        float chartHeight = getHeight() - TOP - BOTTOM;

        if (chartMode == MODE_LINE) {
            drawCloseLine(canvas, start, end, min, range, top, chartHeight);
        } else if (chartMode == MODE_BARS) {
            drawBars(canvas, start, end, min, range, top, chartHeight);
        } else {
            drawCandles(canvas, start, end, min, range, top, chartHeight);
        }

        if (showMovingAverage) {
            drawMovingAverage(canvas, start, end, min, range,
                    top, chartHeight, movingAveragePeriod, maColor, 2.5f);
        }

        if (showBollinger) {
            drawBollinger(canvas, start, end, min, range, top, chartHeight);
        }

        if (showMacd) {
            drawMacd(canvas, start, end, min, range, top, chartHeight);
        }

        if (showRsi) {
            drawRsi(canvas, start, end, min, range, top, chartHeight);
        }

        if (showStochastic) {
            drawStochastic(canvas, start, end, min, range, top, chartHeight);
        }

        if (showVolumes) drawVolumes(canvas, start, end);
        if (showFractals) drawFractals(canvas, start, end, min, range, top, chartHeight);

        drawLatestPrice(canvas, end - 1, start, min, range, top, chartHeight);
        drawTradeLines(canvas, min, max, range, top, chartHeight);
        drawPriceScale(canvas, min, max);
        drawTimeScale(canvas, start, end);
        drawObjects(canvas);
        drawSignalMarker(canvas, start, min, range, top, chartHeight);

        if (showCrosshair) drawCrosshair(canvas);
    }

    private void drawGrid(Canvas canvas) {
        float right = getWidth() - RIGHT;
        float bottom = getHeight() - BOTTOM;
        float h = bottom - TOP;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(gridColor);

        for (int i = 1; i < 6; i++) {
            float y = TOP + h * i / 6f;
            canvas.drawLine(LEFT, y, right, y, paint);
        }
        for (int i = 1; i < 8; i++) {
            float x = LEFT + (right - LEFT) * i / 8f;
            canvas.drawLine(x, TOP, x, bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCandles(Canvas canvas, int start, int end,
                             double min, double range,
                             float top, float height) {
        for (int i = start; i < end; i++) {
            Candle c = candles.get(i);
            if (!valid(c)) continue;

            float x = candleX(i, start);
            if (!onScreen(x)) continue;

            float oy = priceY(c.getOpen(), min, range, top, height);
            float cy = priceY(c.getClose(), min, range, top, height);
            float hy = priceY(c.getHigh(), min, range, top, height);
            float ly = priceY(c.getLow(), min, range, top, height);

            paint.setColor(c.getClose() >= c.getOpen() ? bullColor : bearColor);
            paint.setStrokeWidth(Math.max(1f, candleWidth * .1f));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x, hy, x, ly, paint);

            paint.setStyle(Paint.Style.FILL);
            float bw = Math.max(2f, candleWidth * .72f);
            float y1 = Math.min(oy, cy);
            float y2 = Math.max(oy, cy);
            if (y2 - y1 < 2f) y2 = y1 + 2f;

            canvas.drawRect(x - bw / 2f, y1, x + bw / 2f, y2, paint);

            if (i == selectedCandleIndex) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                canvas.drawRect(x - bw / 2f - 4, y1 - 4,
                        x + bw / 2f + 4, y2 + 4, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    private void drawBars(Canvas canvas, int start, int end,
                          double min, double range,
                          float top, float height) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);

        for (int i = start; i < end; i++) {
            Candle c = candles.get(i);
            if (!valid(c)) continue;
            float x = candleX(i, start);
            if (!onScreen(x)) continue;

            paint.setColor(c.getClose() >= c.getOpen() ? bullColor : bearColor);
            float oy = priceY(c.getOpen(), min, range, top, height);
            float cy = priceY(c.getClose(), min, range, top, height);
            float hy = priceY(c.getHigh(), min, range, top, height);
            float ly = priceY(c.getLow(), min, range, top, height);
            float tick = Math.max(3f, candleWidth * .25f);

            canvas.drawLine(x, hy, x, ly, paint);
            canvas.drawLine(x - tick, oy, x, oy, paint);
            canvas.drawLine(x, cy, x + tick, cy, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCloseLine(Canvas canvas, int start, int end,
                               double min, double range,
                               float top, float height) {
        Path path = new Path();
        boolean begun = false;

        for (int i = start; i < end; i++) {
            Candle c = candles.get(i);
            if (!valid(c)) {
                begun = false;
                continue;
            }

            float x = candleX(i, start);
            float y = priceY(c.getClose(), min, range, top, height);
            if (!begun) {
                path.moveTo(x, y);
                begun = true;
            } else {
                path.lineTo(x, y);
            }
        }

        paint.setColor(lineChartColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    // ---------------------------------------------------------
    // INDICATOR CALCULATIONS AND RENDERING
    // ---------------------------------------------------------

    private void drawMovingAverage(Canvas canvas, int start, int end,
                                   double min, double range, float top,
                                   float height, int period, int color,
                                   float stroke) {
        drawCalculatedLine(canvas, start, end, min, range, top, height,
                color, stroke, i -> movingAverage(i, period));
    }

    private interface ValueFunction {
        double value(int index);
    }

    private void drawCalculatedLine(Canvas canvas, int start, int end,
                                    double min, double range, float top,
                                    float height, int color, float stroke,
                                    ValueFunction function) {
        Path path = new Path();
        boolean begun = false;

        for (int i = start; i < end; i++) {
            double value = function.value(i);
            if (!Double.isFinite(value)) {
                begun = false;
                continue;
            }

            float x = candleX(i, start);
            float y = priceY(value, min, range, top, height);

            if (!begun) {
                path.moveTo(x, y);
                begun = true;
            } else {
                path.lineTo(x, y);
            }
        }

        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private double movingAverage(int index, int period) {
        if (period < 1 || index < period - 1) return Double.NaN;
        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += candles.get(i).getClose();
        }
        return sum / period;
    }

    private double standardDeviation(int index, int period) {
        double avg = movingAverage(index, period);
        if (!Double.isFinite(avg)) return Double.NaN;

        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            double difference = candles.get(i).getClose() - avg;
            sum += difference * difference;
        }
        return Math.sqrt(sum / period);
    }

    private void drawBollinger(Canvas canvas, int start, int end,
                               double min, double range, float top,
                               float height) {
        drawCalculatedLine(canvas, start, end, min, range, top, height,
                bollingerColor, 2f, i -> movingAverage(i, bollingerPeriod));

        drawCalculatedLine(canvas, start, end, min, range, top, height,
                bollingerColor, 1.5f, i -> {
                    double ma = movingAverage(i, bollingerPeriod);
                    double sd = standardDeviation(i, bollingerPeriod);
                    return Double.isFinite(ma) && Double.isFinite(sd)
                            ? ma + 2 * sd : Double.NaN;
                });

        drawCalculatedLine(canvas, start, end, min, range, top, height,
                bollingerColor, 1.5f, i -> {
                    double ma = movingAverage(i, bollingerPeriod);
                    double sd = standardDeviation(i, bollingerPeriod);
                    return Double.isFinite(ma) && Double.isFinite(sd)
                            ? ma - 2 * sd : Double.NaN;
                });
    }

    private double ema(int index, int period) {
        if (period < 1 || index < period - 1) return Double.NaN;
        double value = 0;
        for (int i = index - period + 1; i <= index; i++) {
            value += candles.get(i).getClose();
        }
        value /= period;

        double multiplier = 2.0 / (period + 1);
        for (int i = index - period + 2; i <= index; i++) {
            value = (candles.get(i).getClose() - value) * multiplier + value;
        }
        return value;
    }

    private double macd(int index) {
        double fast = ema(index, macdFastPeriod);
        double slow = ema(index, macdSlowPeriod);
        if (!Double.isFinite(fast) || !Double.isFinite(slow)) {
            return Double.NaN;
        }
        return fast - slow;
    }

    private void drawMacd(Canvas canvas, int start, int end,
                          double min, double range, float top,
                          float height) {
        drawCalculatedLine(canvas, start, end, min, range, top, height,
                macdColor, 2f, this::macd);
    }

    private double rsi(int index) {
        if (rsiPeriod < 1 || index < rsiPeriod) return Double.NaN;
        double gains = 0;
        double losses = 0;

        for (int i = index - rsiPeriod + 1; i <= index; i++) {
            double change = candles.get(i).getClose()
                    - candles.get(i - 1).getClose();
            if (change > 0) gains += change;
            else losses -= change;
        }

        if (losses == 0) return 100;
        double rs = (gains / rsiPeriod) / (losses / rsiPeriod);
        return 100 - 100 / (1 + rs);
    }

    private void drawRsi(Canvas canvas, int start, int end,
                         double min, double range, float top,
                         float height) {
        drawCalculatedLine(canvas, start, end, min, range, top, height,
                rsiColor, 2f, i -> {
                    double value = rsi(i);
                    if (!Double.isFinite(value)) return Double.NaN;
                    return min + (maxSafe(range) * value / 100.0);
                });
    }

    private double stochastic(int index) {
        if (index < stochasticKPeriod - 1) return Double.NaN;
        double highest = -Double.MAX_VALUE;
        double lowest = Double.MAX_VALUE;

        for (int i = index - stochasticKPeriod + 1; i <= index; i++) {
            highest = Math.max(highest, candles.get(i).getHigh());
            lowest = Math.min(lowest, candles.get(i).getLow());
        }

        if (highest == lowest) return 50;
        return 100 * (candles.get(index).getClose() - lowest)
                / (highest - lowest);
    }

    private double stochasticD(int index) {
        if (index < stochasticDPeriod - 1) return Double.NaN;
        double sum = 0;
        for (int i = index - stochasticDPeriod + 1; i <= index; i++) {
            double value = stochastic(i);
            if (!Double.isFinite(value)) return Double.NaN;
            sum += value;
        }
        return sum / stochasticDPeriod;
    }

    private void drawStochastic(Canvas canvas, int start, int end,
                                double min, double range, float top,
                                float height) {
        drawCalculatedLine(canvas, start, end, min, range, top, height,
                stochasticKColor, 2f, i -> {
                    double value = stochastic(i);
                    return Double.isFinite(value)
                            ? min + maxSafe(range) * value / 100.0
                            : Double.NaN;
                });

        drawCalculatedLine(canvas, start, end, min, range, top, height,
                stochasticDColor, 2f, i -> {
                    double value = stochasticD(i);
                    return Double.isFinite(value)
                            ? min + maxSafe(range) * value / 100.0
                            : Double.NaN;
                });
    }

    private void drawVolumes(Canvas canvas, int start, int end) {
        double maxVolume = 0;
        for (int i = start; i < end; i++) {
            maxVolume = Math.max(maxVolume, candles.get(i).getVolume());
        }
        if (maxVolume <= 0) return;

        float bottom = getHeight() - BOTTOM;
        float area = Math.max(30f, getHeight() * .15f);

        for (int i = start; i < end; i++) {
            Candle c = candles.get(i);
            float x = candleX(i, start);
            if (!onScreen(x)) continue;

            float h = (float) (Math.max(0, c.getVolume()) / maxVolume * area);
            paint.setColor(volumeColor);
            canvas.drawRect(x - candleWidth * .25f, bottom - h,
                    x + candleWidth * .25f, bottom, paint);
        }
    }

    private void drawFractals(Canvas canvas, int start, int end,
                              double min, double range, float top,
                              float height) {
        for (int i = Math.max(2, start); i < Math.min(end, candles.size() - 2); i++) {
            Candle c = candles.get(i);
            boolean high = true;
            boolean low = true;

            for (int j = i - 2; j <= i + 2; j++) {
                if (j == i) continue;
                high &= c.getHigh() > candles.get(j).getHigh();
                low &= c.getLow() < candles.get(j).getLow();
            }

            float x = candleX(i, start);
            if (high) {
                float y = priceY(c.getHigh(), min, range, top, height) - 8;
                paint.setColor(fractalColor);
                canvas.drawCircle(x, y, 4, paint);
            }
            if (low) {
                float y = priceY(c.getLow(), min, range, top, height) + 8;
                paint.setColor(fractalColor);
                canvas.drawCircle(x, y, 4, paint);
            }
        }
    }

    private double maxSafe(double value) {
        return Double.isFinite(value) && value > 0 ? value : 1;
    }

    // ---------------------------------------------------------
    // PRICE, TRADE LEVELS, OBJECTS AND MARKERS
    // ---------------------------------------------------------

    private void drawLatestPrice(Canvas canvas, int index, int start,
                                 double min, double range, float top,
                                 float height) {
        if (index < start || index >= candles.size()) return;
        Candle c = candles.get(index);
        if (!valid(c)) return;

        float y = priceY(c.getClose(), min, range, top, height);
        paint.setColor(priceLineColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        for (float x = LEFT; x < getWidth() - RIGHT; x += 12f) {
            canvas.drawLine(x, y, Math.min(x + 6f, getWidth() - RIGHT),
                    y, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(30, 50, 70));
        canvas.drawRect(getWidth() - RIGHT + 2, y - 13,
                getWidth() - 2, y + 13, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(14);
        canvas.drawText(formatPrice(c.getClose()),
                getWidth() - RIGHT + 6, y + 5, paint);
    }

    private void drawTradeLines(Canvas canvas, double min, double max,
                                double range, float top, float height) {
        drawTradeLine(canvas, entry, "ENTRY", entryColor, min, max, range, top, height);
        drawTradeLine(canvas, stopLoss, "SL", stopColor, min, max, range, top, height);
        drawTradeLine(canvas, tp1, "TP1", targetColor, min, max, range, top, height);
        drawTradeLine(canvas, tp2, "TP2", targetColor, min, max, range, top, height);
        drawTradeLine(canvas, tp3, "TP3", targetColor, min, max, range, top, height);
    }

    private void drawTradeLine(Canvas canvas, double price, String label,
                               int color, double min, double max,
                               double range, float top, float height) {
        if (!Double.isFinite(price) || price < min || price > max) return;

        float y = priceY(price, min, range, top, height);
        paint.setColor(color);
        paint.setStrokeWidth(1.5f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(LEFT, y, getWidth() - RIGHT, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(14);
        canvas.drawText(label, getWidth() - RIGHT + 6, y - 4, paint);
    }

    private void drawSignalMarker(Canvas canvas, int start,
                                  double min, double range,
                                  float top, float height) {
        if (signalDirection == Signal.Direction.WAIT || candles.isEmpty()) return;
        int index = candles.size() - 1;
        if (index < start) return;

        Candle c = candles.get(index);
        if (!valid(c)) return;

        boolean buy = signalDirection == Signal.Direction.BUY;
        float x = candleX(index, start);
        float y = priceY(buy ? c.getLow() : c.getHigh(),
                min, range, top, height);
        paint.setColor(buy ? indicatorBuyColor : indicatorSellColor);
        canvas.drawCircle(x, y + (buy ? 20 : -20), 9, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12);
        canvas.drawText(buy ? "B" : "S", x, y + (buy ? 24 : -16), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawObjects(Canvas canvas) {
        for (DrawingObject d : drawings) {
            paint.setColor(d.color);
            paint.setStrokeWidth(selectedDrawing == d ? 3f : 2f);
            paint.setStyle(Paint.Style.STROKE);

            String type = d.type.toUpperCase(Locale.US);
            switch (type) {
                case "VERTICAL":
                case "VERTICAL LINE":
                    canvas.drawLine(d.x1, TOP, d.x1,
                            getHeight() - BOTTOM, paint);
                    break;
                case "HORIZONTAL":
                case "HORIZONTAL LINE":
                    canvas.drawLine(LEFT, d.y1,
                            getWidth() - RIGHT, d.y1, paint);
                    break;
                case "RECTANGLE":
                case "RECT":
                    canvas.drawRect(new RectF(d.x1, d.y1, d.x2, d.y2), paint);
                    break;
                case "ELLIPSE":
                case "CIRCLE":
                    canvas.drawOval(new RectF(d.x1, d.y1, d.x2, d.y2), paint);
                    break;
                case "TRIANGLE": {
                    Path p = new Path();
                    p.moveTo((d.x1 + d.x2) / 2f, d.y1);
                    p.lineTo(d.x1, d.y2);
                    p.lineTo(d.x2, d.y2);
                    p.close();
                    canvas.drawPath(p, paint);
                    break;
                }
                case "ARROWED":
                case "ARROW": {
                    canvas.drawLine(d.x1, d.y1, d.x2, d.y2, paint);
                    float angle = (float) Math.atan2(d.y2 - d.y1, d.x2 - d.x1);
                    float size = 12f;
                    canvas.drawLine(d.x2, d.y2,
                            d.x2 - size * (float) Math.cos(angle - .5),
                            d.y2 - size * (float) Math.sin(angle - .5), paint);
                    canvas.drawLine(d.x2, d.y2,
                            d.x2 - size * (float) Math.cos(angle + .5),
                            d.y2 - size * (float) Math.sin(angle + .5), paint);
                    break;
                }
                case "FIBONACCI RETRACEMENT":
                case "FIBONACCI": {
                    float[] levels = {0f, .236f, .382f, .5f, .618f, .786f, 1f};
                    for (float level : levels) {
                        float y = d.y1 + (d.y2 - d.y1) * level;
                        canvas.drawLine(Math.min(d.x1, d.x2), y,
                                Math.max(d.x1, d.x2), y, paint);
                    }
                    break;
                }
                case "CHANNEL":
                case "EQUidistant":
                case "EQUI DISTANT":
                    canvas.drawLine(d.x1, d.y1, d.x2, d.y2, paint);
                    canvas.drawLine(d.x1, d.y1 - 25, d.x2, d.y2 - 25, paint);
                    canvas.drawLine(d.x1, d.y1 + 25, d.x2, d.y2 + 25, paint);
                    break;
                case "GANN FAN":
                case "FAN":
                    canvas.drawLine(d.x1, d.y1, d.x2, d.y2, paint);
                    canvas.drawLine(d.x1, d.y1, d.x2, d.y1, paint);
                    canvas.drawLine(d.x1, d.y1, d.x1, d.y2, paint);
                    canvas.drawLine(d.x1, d.y1, d.x2,
                            d.y1 + (d.y2 - d.y1) * .5f, paint);
                    break;
                case "NOTE":
                case "NOTES":
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(d.note == null ? "Note" : d.note,
                            d.x1, d.y1, paint);
                    break;
                default:
                    canvas.drawLine(d.x1, d.y1, d.x2, d.y2, paint);
                    break;
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    // ---------------------------------------------------------
    // SCALES AND CROSSHAIR
    // ---------------------------------------------------------

    private void drawPriceScale(Canvas canvas, double min, double max) {
        paint.setColor(textColor);
        paint.setTextSize(13);
        float height = getHeight() - TOP - BOTTOM;

        for (int i = 0; i <= 6; i++) {
            double price = min + (max - min) * i / 6.0;
            float y = TOP + height * (1 - i / 6f);
            canvas.drawText(formatPrice(price),
                    getWidth() - RIGHT + 7, y + 5, paint);
        }
    }

    private void drawTimeScale(Canvas canvas, int start, int end) {
        if (end <= start) return;

        float bottom = getHeight() - BOTTOM;
        int step = Math.max(1, (end - start) / 5);
        paint.setColor(textColor);
        paint.setTextSize(11);
        paint.setTextAlign(Paint.Align.CENTER);

        for (int i = start; i < end; i += step) {
            float x = candleX(i, start);
            if (x < LEFT || x > getWidth() - RIGHT) continue;
            canvas.drawText(formatTime(candles.get(i).getTimestamp()),
                    x, bottom + 21, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCrosshair(Canvas canvas) {
        if (crosshairX < LEFT || crosshairX > getWidth() - RIGHT
                || crosshairY < TOP || crosshairY > getHeight() - BOTTOM) return;

        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(LEFT, crosshairY, getWidth() - RIGHT, crosshairY, paint);
        canvas.drawLine(crosshairX, TOP, crosshairX,
                getHeight() - BOTTOM, paint);
        paint.setStyle(Paint.Style.FILL);

        int index = selectedCandleIndex;
        if (index >= 0 && index < candles.size()) {
            Candle c = candles.get(index);
            paint.setColor(Color.WHITE);
            paint.setTextSize(12);
            canvas.drawText("O " + formatPrice(c.getOpen())
                            + " H " + formatPrice(c.getHigh())
                            + " L " + formatPrice(c.getLow())
                            + " C " + formatPrice(c.getClose()),
                    LEFT + 4, TOP + 16, paint);
        }
    }

    private void drawMessage(Canvas canvas, String message) {
        paint.setColor(textColor);
        paint.setTextSize(18);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private int calculateVisibleCount() {
        float slot = candleWidth + spacing;
        return Math.max(1, (int) ((getWidth() - LEFT - RIGHT) / slot));
    }

    private int calculateStartIndex() {
        int visible = calculateVisibleCount();
        int maxStart = Math.max(0, candles.size() - visible);
        return Math.max(0, Math.min(maxStart,
                maxStart - Math.round(scrollOffset)));
    }

    private void clampScroll() {
        int visible = calculateVisibleCount();
        float max = Math.max(0, candles.size() - visible);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset));
    }

    private float candleX(int index, int start) {
        float slot = candleWidth + spacing;
        return LEFT + (index - start) * slot + slot / 2f;
    }

    private boolean onScreen(float x) {
        return x >= LEFT - candleWidth && x <= getWidth() - RIGHT + candleWidth;
    }

    private float priceY(double price, double min, double range,
                         float top, float height) {
        if (!Double.isFinite(range) || range == 0) return top + height / 2;
        return (float) (top + height * (1 - (price - min) / range));
    }

    private boolean valid(Candle c) {
        return c != null && Double.isFinite(c.getOpen())
                && Double.isFinite(c.getHigh())
                && Double.isFinite(c.getLow())
                && Double.isFinite(c.getClose())
                && c.getHigh() >= c.getLow();
    }

    private double validMinTrade() {
        double v = Double.MAX_VALUE;
        if (Double.isFinite(entry)) v = Math.min(v, entry);
        if (Double.isFinite(stopLoss)) v = Math.min(v, stopLoss);
        if (Double.isFinite(tp1)) v = Math.min(v, tp1);
        if (Double.isFinite(tp2)) v = Math.min(v, tp2);
        if (Double.isFinite(tp3)) v = Math.min(v, tp3);
        return v == Double.MAX_VALUE ? Double.NaN : v;
    }

    private double validMaxTrade() {
        double v = -Double.MAX_VALUE;
        if (Double.isFinite(entry)) v = Math.max(v, entry);
        if (Double.isFinite(stopLoss)) v = Math.max(v, stopLoss);
        if (Double.isFinite(tp1)) v = Math.max(v, tp1);
        if (Double.isFinite(tp2)) v = Math.max(v, tp2);
        if (Double.isFinite(tp3)) v = Math.max(v, tp3);
        return v == -Double.MAX_VALUE ? Double.NaN : v;
    }

    private String formatPrice(double price) {
        if (!Double.isFinite(price)) return "--";
        if (price >= 1000) return String.format(Locale.US, "%.2f", price);
        if (price >= 100) return String.format(Locale.US, "%.3f", price);
        if (price >= 10) return String.format(Locale.US, "%.4f", price);
        return String.format(Locale.US, "%.5f", price);
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "--";
        SimpleDateFormat format =
                new SimpleDateFormat("dd MMM HH:mm", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));
        return format.format(new Date(timestamp));
    }

    private int getCandleIndexFromX(float x) {
        float slot = candleWidth + spacing;
        if (slot <= 0) return -1;
        int index = calculateStartIndex() + (int) ((x - LEFT) / slot);
        return index >= 0 && index < candles.size() ? index : -1;
    }

    // ---------------------------------------------------------
    // TOUCH / DRAWING PLACEMENT / ZOOM
    // ---------------------------------------------------------

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            if (!Float.isFinite(factor) || factor <= 0) return true;
            candleWidth = Math.max(5f, Math.min(55f, candleWidth * factor));
            updateSpacing();
            clampScroll();
            invalidate();
            return true;
        }
    }

    private void setParentScrollEnabled(boolean enabled) {
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(!enabled);
            parent = parent.getParent();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setParentScrollEnabled(false);
                lastX = x;
                lastY = y;
                moving = true;

                if (!activeDrawingTool.isEmpty()) {
                    drawingStartX = x;
                    drawingStartY = y;
                    placingDrawing = true;
                    return true;
                }

                showCrosshair = true;
                crosshairX = x;
                crosshairY = y;
                selectedCandleIndex = getCandleIndexFromX(x);
                invalidate();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                showCrosshair = false;
                placingDrawing = false;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() > 1) {
                    showCrosshair = false;
                    placingDrawing = false;
                    invalidate();
                    return true;
                }

                if (!moving) return true;

                if (placingDrawing) {
                    invalidate();
                    return true;
                }

                float dx = x - lastX;
                float dy = y - lastY;

                if (Math.abs(dx) > Math.abs(dy)) {
                    float slot = candleWidth + spacing;
                    if (slot > 0) {
                        scrollOffset -= dx / slot;
                        clampScroll();
                    }
                } else {
                    crosshairY = y;
                }

                crosshairX = x;
                selectedCandleIndex = getCandleIndexFromX(x);
                lastX = x;
                lastY = y;
                showCrosshair = true;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (placingDrawing) {
                    String type = activeDrawingTool;
                    drawings.add(new DrawingObject(type,
                            drawingStartX, drawingStartY, x, y, Color.YELLOW));
                    selectedDrawing = drawings.get(drawings.size() - 1);
                    placingDrawing = false;
                    activeDrawingTool = "";
                }

                moving = false;
                setParentScrollEnabled(true);
                performClick();
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                moving = false;
                placingDrawing = false;
                setParentScrollEnabled(true);
                invalidate();
                return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
