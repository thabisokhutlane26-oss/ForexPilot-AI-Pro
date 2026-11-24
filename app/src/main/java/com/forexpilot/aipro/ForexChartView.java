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

/** Price-only chart with free two-axis panning and pinch zoom. */
public class ForexChartView extends View {
    public static final int MODE_CANDLES = 0;
    public static final int MODE_LINE = 1;
    public static final int MODE_BARS = 2;

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
    private int entryColor = 0xFFFFC04D;
    private int stopColor = 0xFFEF5350;
    private int targetColor = 0xFF26C281;

    // Horizontal view: candles visible. Vertical view: price scale.
    private float horizontalZoom = 1f;
    private float verticalZoom = 1f;
    private float horizontalOffset;
    private float verticalOffset;
    private float lastX, lastY;
    private boolean dragging;
    private boolean scaling;
    private float pinchFocusX, pinchFocusY;
    private float candleWidth = 9f;
    private float candleGap = 4f;

    private double visibleMin, visibleMax, priceRange = 1d;
    private int visibleStart, visibleEnd;
    private Signal tradeSignal;

    private static final float LEFT_PAD = 12f;
    private static final float RIGHT_PAD = 78f;
    private static final float TOP_PAD = 18f;
    private static final float BOTTOM_PAD = 28f;
    private static final float MIN_ZOOM = 0.35f;
    private static final float MAX_ZOOM = 12f;

    public ForexChartView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(backgroundColor);
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScaleBegin(ScaleGestureDetector d) {
                        scaling = true;
                        pinchFocusX = d.getFocusX();
                        pinchFocusY = d.getFocusY();
                        return true;
                    }
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        float factor = d.getScaleFactor();
                        float oldH = horizontalZoom;
                        float oldV = verticalZoom;
                        horizontalZoom = clamp(horizontalZoom * factor, MIN_ZOOM, MAX_ZOOM);
                        verticalZoom = clamp(verticalZoom * factor, MIN_ZOOM, MAX_ZOOM);
                        float plotX = Math.max(1f, plot.width());
                        float plotY = Math.max(1f, plot.height());
                        float focusX = d.getFocusX() - plot.left;
                        float focusY = d.getFocusY() - plot.top;
                        horizontalOffset = (horizontalOffset + focusX) * (horizontalZoom / oldH) - focusX;
                        verticalOffset = (verticalOffset + focusY) * (verticalZoom / oldV) - focusY;
                        invalidate();
                        return true;
                    }
                    @Override public void onScaleEnd(ScaleGestureDetector d) {
                        scaling = false;
                    }
                });
    }

    public void setCandles(List<Candle> data) {
        candles.clear();
        if (data != null) for (Candle c : data) if (isValid(c)) candles.add(c);
        // Keep the newest candles in view when a new market/timeframe is loaded.
        horizontalOffset = 0f;
        verticalOffset = 0f;
        invalidate();
    }

    public void addCandle(Candle candle) {
        if (isValid(candle)) {
            candles.add(candle);
            if (candles.size() > 2000) candles.remove(0);
            invalidate();
        }
    }

    public void clearChart() {
        candles.clear();
        horizontalOffset = verticalOffset = 0f;
        horizontalZoom = verticalZoom = 1f;
        tradeSignal = null;
        invalidate();
    }

    public void setChartMode(int mode) {
        chartMode = mode == MODE_LINE ? MODE_LINE : mode == MODE_BARS ? MODE_BARS : MODE_CANDLES;
        invalidate();
    }

    public void setChartMode(String mode) {
        if (mode == null) return;
        String s = mode.toUpperCase(Locale.US);
        setChartMode(s.contains("LINE") ? MODE_LINE : (s.contains("BAR") || s.contains("OHLC")) ? MODE_BARS : MODE_CANDLES);
    }

    public int getChartMode() { return chartMode; }

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

    public void setColorRole(String role, int color) {
        if (role == null) return;
        String key = role.toUpperCase(Locale.US);
        if (key.contains("BACKGROUND") || key.equals("BG")) backgroundColor = color;
        else if (key.contains("GRID")) gridColor = color;
        else if (key.contains("FOREGROUND") || key.contains("TEXT")) textColor = color;
        else if (key.contains("BULL") || key.contains("UP")) bullColor = color;
        else if (key.contains("BEAR") || key.contains("DOWN")) bearColor = color;
        else if (key.contains("LINE")) lineColor = color;
        else if (key.equals("SL") || key.contains("STOP")) stopColor = color;
        else if (key.equals("TP") || key.contains("TAKE")) targetColor = color;
        invalidate();
    }

    // Trade levels are price references, not indicators or drawing objects.
    public void setTradeLevels(Signal signal) { tradeSignal = signal; invalidate(); }
    public void clearTradeLevels() { tradeSignal = null; invalidate(); }

    // Kept as no-op compatibility methods for older MainActivity versions.
    @Deprecated public void setDrawingTool(String ignored) { }
    @Deprecated public void setIndicatorEnabled(String ignored, boolean enabled) { }
    @Deprecated public void setIndicatorPeriod(String ignored, int period) { }
    @Deprecated public void setRsiLevels(double upper, double lower) { }
    @Deprecated public void setStochasticColors(int first, int second) { }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        plot.set(LEFT_PAD, TOP_PAD, Math.max(LEFT_PAD + 10, getWidth() - RIGHT_PAD),
                Math.max(TOP_PAD + 10, getHeight() - BOTTOM_PAD));
        drawGrid(canvas);
        if (candles.isEmpty()) {
            drawMessage(canvas, "Waiting for market data");
            return;
        }
        calculateVisibleRange();
        if (visibleEnd <= visibleStart || priceRange <= 0) {
            drawMessage(canvas, "Waiting for market data");
            return;
        }
        canvas.save();
        canvas.clipRect(plot);
        if (chartMode == MODE_LINE) drawLineChart(canvas);
        else if (chartMode == MODE_BARS) drawBars(canvas);
        else drawCandles(canvas);
        drawTradeLevels(canvas);
        canvas.restore();
        drawPriceScale(canvas);
        drawLatestPrice(canvas);
    }

    private void calculateVisibleRange() {
        float step = getStep();
        int capacity = Math.max(2, (int)(plot.width() / step));
        int count = candles.size();
        int visibleCount = Math.max(2, Math.min(count, (int)(capacity / horizontalZoom)));
        int maxStart = Math.max(0, count - visibleCount);
        int shift = Math.round(horizontalOffset / step);
        visibleStart = clamp(maxStart - shift, 0, Math.max(0, count - 1));
        visibleEnd = Math.min(count, visibleStart + visibleCount);
        if (visibleEnd - visibleStart < 2 && count >= 2) {
            visibleEnd = count;
            visibleStart = Math.max(0, count - 2);
        }
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle c = candles.get(i);
            low = Math.min(low, c.getLow());
            high = Math.max(high, c.getHigh());
        }
        if (low == Double.MAX_VALUE) { low = 0; high = 1; }
        double span = Math.max(high - low, Math.max(Math.abs(high) * 0.00001, 0.000001));
        double padding = span * 0.08;
        double baseMin = low - padding, baseMax = high + padding;
        double baseRange = Math.max(0.0000001, baseMax - baseMin);
        priceRange = baseRange / verticalZoom;
        double center = (baseMin + baseMax) / 2d;
        double shift = (verticalOffset / Math.max(1f, plot.height())) * priceRange;
        visibleMin = center - priceRange / 2d + shift;
        visibleMax = center + priceRange / 2d + shift;
    }

    private void drawGrid(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(gridColor);
        for (int i = 0; i <= 6; i++) {
            float y = plot.top + plot.height() * i / 6f;
            float x = plot.left + plot.width() * i / 6f;
            c.drawLine(plot.left, y, plot.right, y, paint);
            c.drawLine(x, plot.top, x, plot.bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCandles(Canvas c) {
        float step = getStep(), body = Math.max(2f, Math.min(18f, step * 0.68f));
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle v = candles.get(i);
            float x = candleX(i, step), o = priceY(v.getOpen()), cl = priceY(v.getClose());
            paint.setColor(v.getClose() >= v.getOpen() ? bullColor : bearColor);
            paint.setStrokeWidth(1.5f);
            paint.setStyle(Paint.Style.STROKE);
            c.drawLine(x, priceY(v.getHigh()), x, priceY(v.getLow()), paint);
            paint.setStyle(Paint.Style.FILL);
            float top = Math.min(o, cl), bottom = Math.max(o, cl);
            if (bottom - top < 1.5f) bottom = top + 1.5f;
            c.drawRect(x - body / 2f, top, x + body / 2f, bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBars(Canvas c) {
        float step = getStep(), tick = Math.max(3f, Math.min(7f, step * 0.28f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle v = candles.get(i);
            float x = candleX(i, step);
            paint.setColor(v.getClose() >= v.getOpen() ? bullColor : bearColor);
            c.drawLine(x, priceY(v.getHigh()), x, priceY(v.getLow()), paint);
            c.drawLine(x - tick, priceY(v.getOpen()), x, priceY(v.getOpen()), paint);
            c.drawLine(x, priceY(v.getClose()), x + tick, priceY(v.getClose()), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLineChart(Canvas c) {
        Path path = new Path();
        boolean started = false;
        float step = getStep();
        for (int i = visibleStart; i < visibleEnd; i++) {
            float x = candleX(i, step), y = priceY(candles.get(i).getClose());
            if (!started) { path.moveTo(x, y); started = true; }
            else path.lineTo(x, y);
        }
        paint.setColor(lineColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTradeLevels(Canvas c) {
        if (tradeSignal == null) return;
        drawLevel(c, tradeSignal.getEntry(), entryColor);
        drawLevel(c, tradeSignal.getStopLoss(), stopColor);
        drawLevel(c, tradeSignal.getTakeProfit1(), targetColor);
        drawLevel(c, tradeSignal.getTakeProfit2(), targetColor);
        drawLevel(c, tradeSignal.getTakeProfit3(), targetColor);
    }

    private void drawLevel(Canvas c, double price, int color) {
        if (!Double.isFinite(price) || price <= 0) return;
        float y = priceY(price);
        if (y < plot.top || y > plot.bottom) return;
        paint.setColor(color);
        paint.setStrokeWidth(1.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{7f, 5f}, 0));
        c.drawLine(plot.left, y, plot.right, y, paint);
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPriceScale(Canvas c) {
        paint.setColor(textColor);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i <= 6; i++) {
            double p = visibleMax - priceRange * i / 6d;
            float y = plot.top + plot.height() * i / 6f;
            c.drawText(formatPrice(p), plot.right + 7f, y + sp(3), paint);
        }
    }

    private void drawLatestPrice(Canvas c) {
        Candle last = candles.get(candles.size() - 1);
        float y = priceY(last.getClose());
        if (y < plot.top || y > plot.bottom) return;
        int color = last.getClose() >= last.getOpen() ? bullColor : bearColor;
        paint.setColor(color);
        paint.setStrokeWidth(1f);
        c.drawLine(plot.left, y, plot.right, y, paint);
        RectF label = new RectF(plot.right + 3f, y - sp(9), getWidth() - 2f, y + sp(9));
        c.drawRect(label, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(formatPrice(last.getClose()), label.centerX(), y + sp(3.5f), paint);
    }

    private void drawMessage(Canvas c, String message) {
        paint.setColor(textColor);
        paint.setTextSize(sp(13));
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
    }

    private float candleX(int index, float step) {
        return plot.right - step * 0.5f - (visibleEnd - 1 - index) * step;
    }
    private float priceY(double price) {
        return (float)(plot.bottom - ((price - visibleMin) / priceRange) * plot.height());
    }
    private float getStep() { return Math.max(2.5f, (candleWidth + candleGap) * horizontalZoom); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX(); lastY = e.getY(); dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && !scaling && dragging && e.getPointerCount() == 1) {
                    float dx = e.getX() - lastX;
                    float dy = e.getY() - lastY;
                    // Dragging left/right moves through candle history; up/down moves the price scale.
                    horizontalOffset += dx;
                    verticalOffset += dy;
                    lastX = e.getX(); lastY = e.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                dragging = false;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                scaling = false;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private boolean isValid(Candle c) {
        return c != null && Double.isFinite(c.getOpen()) && Double.isFinite(c.getHigh())
                && Double.isFinite(c.getLow()) && Double.isFinite(c.getClose())
                && c.getHigh() >= c.getLow();
    }
    private String formatPrice(double p) {
        double a = Math.abs(p);
        if (a >= 1000) return String.format(Locale.US, "%.2f", p);
        if (a >= 10) return String.format(Locale.US, "%.4f", p);
        return String.format(Locale.US, "%.5f", p);
    }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
