package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Price-only chart: candlesticks, line and OHLC bars.
 * Supports pinch zoom and horizontal scrolling.
 */
public class ForexChartView extends View {

    // Integer modes retained for compatibility with MainActivity.
    public static final int MODE_CANDLES = 0;
    public static final int MODE_LINE = 1;
    public static final int MODE_BARS = 2;

    private static final int DRAW_CANDLES = 0;
    private static final int DRAW_LINE = 1;
    private static final int DRAW_BARS = 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Candle> candles = new ArrayList<>();
    private final RectF plot = new RectF();
    private final ScaleGestureDetector scaleDetector;

    private int chartMode = MODE_CANDLES;
    private int backgroundColor = 0xFF101820;
    private int gridColor = 0xFF263541;
    private int textColor = 0xFF9EADB8;
    private int bullColor = 0xFF26C281;
    private int bearColor = 0xFFEF5350;
    private int lineColor = 0xFF42A5F5;

    private float zoom = 1f;
    private float scrollOffset = 0f;
    private float lastTouchX;
    private boolean dragging;
    private float candleWidth = 9f;
    private float candleGap = 4f;

    private double visibleMin;
    private double visibleMax;
    private double priceRange = 1d;
    private int visibleStart;
    private int visibleEnd;

    private static final float LEFT_PAD = 12f;
    private static final float RIGHT_PAD = 76f;
    private static final float TOP_PAD = 18f;
    private static final float BOTTOM_PAD = 28f;
    private static final float MIN_ZOOM = 0.55f;
    private static final float MAX_ZOOM = 5f;

    public ForexChartView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(backgroundColor);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float oldZoom = zoom;
                        zoom = clamp(zoom * detector.getScaleFactor(),
                                MIN_ZOOM, MAX_ZOOM);
                        float focusX = detector.getFocusX();
                        float ratio = oldZoom == 0f ? 1f : zoom / oldZoom;
                        scrollOffset = (scrollOffset + focusX) * ratio - focusX;
                        clampScroll();
                        invalidate();
                        return true;
                    }
                });
    }

    public void setCandles(List<Candle> data) {
        candles.clear();
        if (data != null) {
            for (Candle candle : data) {
                if (isValid(candle)) candles.add(candle);
            }
        }
        clampScroll();
        invalidate();
    }

    public void addCandle(Candle candle) {
        if (isValid(candle)) {
            candles.add(candle);
            clampScroll();
            invalidate();
        }
    }

    public void clearChart() {
        candles.clear();
        scrollOffset = 0f;
        zoom = 1f;
        invalidate();
    }

    // MainActivity currently passes the integer constants above.
    public void setChartMode(int mode) {
        if (mode == MODE_LINE) chartMode = MODE_LINE;
        else if (mode == MODE_BARS) chartMode = MODE_BARS;
        else chartMode = MODE_CANDLES;
        invalidate();
    }

    // String overload retained for any older callers.
    public void setChartMode(String mode) {
        if (mode == null) return;
        String value = mode.trim().toUpperCase(Locale.US);
        if (value.contains("LINE")) setChartMode(MODE_LINE);
        else if (value.contains("BAR") || value.contains("OHLC")) setChartMode(MODE_BARS);
        else setChartMode(MODE_CANDLES);
    }

    public int getChartMode() {
        return chartMode;
    }

    public void setChartColors(int background, int grid, int text,
                               int bullish, int bearish, int line) {
        backgroundColor = background;
        gridColor = grid;
        textColor = text;
        bullColor = bullish;
        bearColor = bearish;
        lineColor = line;
        invalidate();
    }

    @Deprecated public void setDrawingTool(String ignored) { }
    @Deprecated public void setIndicatorEnabled(String ignored, boolean enabled) { }
    @Deprecated public void setIndicatorPeriod(String ignored, int period) { }
    @Deprecated public void setRsiLevels(double upper, double lower) { }
    @Deprecated public void setStochasticColors(int first, int second) { }
    @Deprecated public void setTradeLevels(Signal ignored) { }
    @Deprecated public void clearTradeLevels() { }

    @Deprecated
    public void setColorRole(String role, int color) {
        if (role == null) return;
        String key = role.toUpperCase(Locale.US);
        if (key.contains("BACKGROUND") || key.equals("BG")) backgroundColor = color;
        else if (key.contains("GRID")) gridColor = color;
        else if (key.contains("TEXT")) textColor = color;
        else if (key.contains("BULL") || key.contains("UP")) bullColor = color;
        else if (key.contains("BEAR") || key.contains("DOWN")) bearColor = color;
        else if (key.contains("LINE")) lineColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        plot.set(LEFT_PAD, TOP_PAD,
                Math.max(LEFT_PAD + 10f, width - RIGHT_PAD),
                Math.max(TOP_PAD + 10f, height - BOTTOM_PAD));
        drawGrid(canvas);

        if (candles.isEmpty()) {
            drawMessage(canvas, "Waiting for market data");
            return;
        }

        calculateVisibleRange();
        if (visibleEnd <= visibleStart || priceRange <= 0d) {
            drawMessage(canvas, "Waiting for market data");
            return;
        }

        canvas.save();
        canvas.clipRect(plot);
        if (chartMode == MODE_LINE) drawLineChart(canvas);
        else if (chartMode == MODE_BARS) drawBarChart(canvas);
        else drawCandlesticks(canvas);
        canvas.restore();

        drawPriceScale(canvas);
        drawLatestPrice(canvas);
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(gridColor);
        for (int i = 0; i <= 6; i++) {
            float y = plot.top + plot.height() * i / 6f;
            canvas.drawLine(plot.left, y, plot.right, y, paint);
            float x = plot.left + plot.width() * i / 6f;
            canvas.drawLine(x, plot.top, x, plot.bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void calculateVisibleRange() {
        float step = getStep();
        int capacity = Math.max(2, (int) (plot.width() / step));
        int count = candles.size();
        int visibleCount = Math.max(2, Math.min(count, (int) (capacity / zoom)));
        int maxStart = Math.max(0, count - visibleCount);
        int offsetCandles = Math.round(scrollOffset / step);
        visibleStart = clamp(maxStart - offsetCandles, 0, Math.max(0, count - 1));
        visibleEnd = Math.min(count, visibleStart + visibleCount);

        if (visibleEnd - visibleStart < 2 && count >= 2) {
            visibleEnd = count;
            visibleStart = Math.max(0, visibleEnd - 2);
        }

        visibleMin = Double.MAX_VALUE;
        visibleMax = -Double.MAX_VALUE;
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle c = candles.get(i);
            visibleMin = Math.min(visibleMin, c.getLow());
            visibleMax = Math.max(visibleMax, c.getHigh());
        }
        if (visibleMin == Double.MAX_VALUE || visibleMax == -Double.MAX_VALUE) {
            visibleMin = 0d;
            visibleMax = 1d;
        }
        double spread = visibleMax - visibleMin;
        if (spread <= 0d) spread = Math.max(Math.abs(visibleMax) * 0.001d, 0.00001d);
        double padding = spread * 0.08d;
        visibleMin -= padding;
        visibleMax += padding;
        priceRange = Math.max(0.0000001d, visibleMax - visibleMin);
    }

    private void drawCandlesticks(Canvas canvas) {
        float step = getStep();
        float bodyWidth = Math.max(2f, Math.min(18f, step * 0.68f));
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle c = candles.get(i);
            float x = candleX(i, step);
            float high = priceY(c.getHigh());
            float low = priceY(c.getLow());
            float open = priceY(c.getOpen());
            float close = priceY(c.getClose());
            paint.setColor(c.getClose() >= c.getOpen() ? bullColor : bearColor);
            paint.setStrokeWidth(1.5f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x, high, x, low, paint);
            paint.setStyle(Paint.Style.FILL);
            float top = Math.min(open, close);
            float bottom = Math.max(open, close);
            if (bottom - top < 1.5f) bottom = top + 1.5f;
            canvas.drawRect(x - bodyWidth / 2f, top,
                    x + bodyWidth / 2f, bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBarChart(Canvas canvas) {
        float step = getStep();
        float tick = Math.max(3f, Math.min(7f, step * 0.28f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle c = candles.get(i);
            float x = candleX(i, step);
            paint.setColor(c.getClose() >= c.getOpen() ? bullColor : bearColor);
            canvas.drawLine(x, priceY(c.getHigh()), x, priceY(c.getLow()), paint);
            canvas.drawLine(x - tick, priceY(c.getOpen()), x, priceY(c.getOpen()), paint);
            canvas.drawLine(x, priceY(c.getClose()), x + tick, priceY(c.getClose()), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLineChart(Canvas canvas) {
        Path path = new Path();
        boolean started = false;
        float step = getStep();
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle c = candles.get(i);
            float x = candleX(i, step);
            float y = priceY(c.getClose());
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else path.lineTo(x, y);
        }
        paint.setColor(lineColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPriceScale(Canvas canvas) {
        paint.setColor(textColor);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i <= 6; i++) {
            double price = visibleMax - priceRange * i / 6d;
            float y = plot.top + plot.height() * i / 6f;
            canvas.drawText(formatPrice(price), plot.right + 7f, y + sp(3), paint);
        }
    }

    private void drawLatestPrice(Canvas canvas) {
        if (candles.isEmpty()) return;
        Candle latest = candles.get(candles.size() - 1);
        float y = clamp(priceY(latest.getClose()), plot.top + 2f, plot.bottom - 2f);
        int color = latest.getClose() >= latest.getOpen() ? bullColor : bearColor;
        paint.setColor(color);
        paint.setStrokeWidth(1f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(plot.left, y, plot.right, y, paint);
        paint.setStyle(Paint.Style.FILL);
        RectF label = new RectF(plot.right + 3f, y - sp(9), getWidth() - 2f, y + sp(9));
        canvas.drawRect(label, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(formatPrice(latest.getClose()), label.centerX(), y + sp(3.5f), paint);
    }

    private void drawMessage(Canvas canvas, String message) {
        paint.setColor(textColor);
        paint.setTextSize(sp(13));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
    }

    private float candleX(int index, float step) {
        return plot.right - step * 0.5f - (visibleEnd - 1 - index) * step;
    }

    private float priceY(double price) {
        return (float) (plot.bottom - ((price - visibleMin) / priceRange) * plot.height());
    }

    private float getStep() {
        return Math.max(3f, (candleWidth + candleGap) * zoom);
    }

    private void clampScroll() {
        float step = Math.max(3f, (candleWidth + candleGap) * zoom);
        float max = Math.max(0f, candles.size() * step
                - Math.max(1f, getWidth() - LEFT_PAD - RIGHT_PAD));
        scrollOffset = clamp(scrollOffset, 0f, max);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                dragging = true;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging && event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    float x = event.getX();
                    scrollOffset += lastTouchX - x;
                    lastTouchX = x;
                    clampScroll();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() <= 2) dragging = false;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                performClick();
                return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private boolean isValid(Candle c) {
        return c != null && Double.isFinite(c.getOpen())
                && Double.isFinite(c.getHigh()) && Double.isFinite(c.getLow())
                && Double.isFinite(c.getClose()) && c.getHigh() >= c.getLow();
    }

    private String formatPrice(double price) {
        double abs = Math.abs(price);
        if (abs >= 1000d) return String.format(Locale.US, "%.2f", price);
        if (abs >= 10d) return String.format(Locale.US, "%.4f", price);
        return String.format(Locale.US, "%.5f", price);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
