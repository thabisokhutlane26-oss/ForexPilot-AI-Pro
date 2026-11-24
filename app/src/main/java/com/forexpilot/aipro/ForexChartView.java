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

/** Clean price-only chart with pinch zoom and two-axis panning. */
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

    private float horizontalZoom = 1f;
    private float verticalZoom = 1f;
    private float horizontalOffset;
    private float verticalOffset;
    private float lastX, lastY;
    private boolean dragging;
    private boolean scaling;
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
                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        scaling = true;
                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        float oldHorizontal = horizontalZoom;
                        float oldVertical = verticalZoom;

                        horizontalZoom = clamp(horizontalZoom * factor,
                                MIN_ZOOM, MAX_ZOOM);
                        verticalZoom = clamp(verticalZoom * factor,
                                MIN_ZOOM, MAX_ZOOM);

                        float focusX = detector.getFocusX() - plot.left;
                        float focusY = detector.getFocusY() - plot.top;

                        horizontalOffset =
                                (horizontalOffset + focusX)
                                        * (horizontalZoom / oldHorizontal) - focusX;
                        verticalOffset =
                                (verticalOffset + focusY)
                                        * (verticalZoom / oldVertical) - focusY;
                        invalidate();
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        scaling = false;
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
        horizontalOffset = 0f;
        verticalOffset = 0f;
        horizontalZoom = 1f;
        verticalZoom = 1f;
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
        horizontalOffset = 0f;
        verticalOffset = 0f;
        horizontalZoom = 1f;
        verticalZoom = 1f;
        tradeSignal = null;
        invalidate();
    }

    public void setChartMode(int mode) {
        chartMode = mode == MODE_LINE ? MODE_LINE
                : mode == MODE_BARS ? MODE_BARS : MODE_CANDLES;
        invalidate();
    }

    public void setChartMode(String mode) {
        if (mode == null) return;
        String value = mode.toUpperCase(Locale.US);
        setChartMode(value.contains("LINE") ? MODE_LINE
                : (value.contains("BAR") || value.contains("OHLC"))
                ? MODE_BARS : MODE_CANDLES);
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

    public void setTradeLevels(Signal signal) {
        tradeSignal = signal;
        invalidate();
    }

    public void clearTradeLevels() {
        tradeSignal = null;
        invalidate();
    }

    // Compatibility methods: the chart intentionally has no indicators or drawing tools.
    @Deprecated public void setDrawingTool(String ignored) { }
    @Deprecated public void setIndicatorEnabled(String ignored, boolean enabled) { }
    @Deprecated public void setIndicatorPeriod(String ignored, int period) { }
    @Deprecated public void setRsiLevels(double upper, double lower) { }
    @Deprecated public void setStochasticColors(int first, int second) { }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        plot.set(LEFT_PAD, TOP_PAD,
                Math.max(LEFT_PAD + 10, getWidth() - RIGHT_PAD),
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
        int capacity = Math.max(2, (int) (plot.width() / step));
        int count = candles.size();
        int visibleCount = Math.max(2,
                Math.min(count, (int) (capacity / horizontalZoom)));
        int maxStart = Math.max(0, count - visibleCount);
        int candleShift = Math.round(horizontalOffset / step);

        visibleStart = clamp(maxStart - candleShift, 0, Math.max(0, count - 1));
        visibleEnd = Math.min(count, visibleStart + visibleCount);

        if (visibleEnd - visibleStart < 2 && count >= 2) {
            visibleEnd = count;
            visibleStart = Math.max(0, count - 2);
        }

        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle candle = candles.get(i);
            low = Math.min(low, candle.getLow());
            high = Math.max(high, candle.getHigh());
        }

        if (low == Double.MAX_VALUE) {
            low = 0d;
            high = 1d;
        }

        double span = Math.max(high - low,
                Math.max(Math.abs(high) * 0.00001, 0.000001));
        double padding = span * 0.08;
        double baseMin = low - padding;
        double baseMax = high + padding;
        double baseRange = Math.max(0.0000001, baseMax - baseMin);

        priceRange = baseRange / verticalZoom;
        double center = (baseMin + baseMax) / 2d;

        // Separate name prevents the duplicate local-variable compilation failure.
        double priceShift =
                (verticalOffset / Math.max(1f, plot.height())) * priceRange;
        visibleMin = center - priceRange / 2d + priceShift;
        visibleMax = center + priceRange / 2d + priceShift;
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(gridColor);
        for (int i = 0; i <= 6; i++) {
            float y = plot.top + plot.height() * i / 6f;
            float x = plot.left + plot.width() * i / 6f;
            canvas.drawLine(plot.left, y, plot.right, y, paint);
            canvas.drawLine(x, plot.top, x, plot.bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCandles(Canvas canvas) {
        float step = getStep();
        float body = Math.max(2f, Math.min(18f, step * 0.68f));
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle candle = candles.get(i);
            float x = candleX(i, step);
            float open = priceY(candle.getOpen());
            float close = priceY(candle.getClose());

            paint.setColor(candle.getClose() >= candle.getOpen()
                    ? bullColor : bearColor);
            paint.setStrokeWidth(1.5f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x, priceY(candle.getHigh()),
                    x, priceY(candle.getLow()), paint);

            paint.setStyle(Paint.Style.FILL);
            float top = Math.min(open, close);
            float bottom = Math.max(open, close);
            if (bottom - top < 1.5f) bottom = top + 1.5f;
            canvas.drawRect(x - body / 2f, top,
                    x + body / 2f, bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBars(Canvas canvas) {
        float step = getStep();
        float tick = Math.max(3f, Math.min(7f, step * 0.28f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        for (int i = visibleStart; i < visibleEnd; i++) {
            Candle candle = candles.get(i);
            float x = candleX(i, step);
            paint.setColor(candle.getClose() >= candle.getOpen()
                    ? bullColor : bearColor);
            canvas.drawLine(x, priceY(candle.getHigh()),
                    x, priceY(candle.getLow()), paint);
            canvas.drawLine(x - tick, priceY(candle.getOpen()),
                    x, priceY(candle.getOpen()), paint);
            canvas.drawLine(x, priceY(candle.getClose()),
                    x + tick, priceY(candle.getClose()), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLineChart(Canvas canvas) {
        Path path = new Path();
        boolean started = false;
        float step = getStep();
        for (int i = visibleStart; i < visibleEnd; i++) {
            float x = candleX(i, step);
            float y = priceY(candles.get(i).getClose());
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        paint.setColor(lineColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTradeLevels(Canvas canvas) {
        if (tradeSignal == null) return;
        drawLevel(canvas, tradeSignal.getEntry(), entryColor);
        drawLevel(canvas, tradeSignal.getStopLoss(), stopColor);
        drawLevel(canvas, tradeSignal.getTakeProfit1(), targetColor);
        drawLevel(canvas, tradeSignal.getTakeProfit2(), targetColor);
        drawLevel(canvas, tradeSignal.getTakeProfit3(), targetColor);
    }

    private void drawLevel(Canvas canvas, double price, int color) {
        if (!Double.isFinite(price) || price <= 0) return;
        float y = priceY(price);
        if (y < plot.top || y > plot.bottom) return;

        paint.setColor(color);
        paint.setStrokeWidth(1.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{7f, 5f}, 0));
        canvas.drawLine(plot.left, y, plot.right, y, paint);
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPriceScale(Canvas canvas) {
        paint.setColor(textColor);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i <= 6; i++) {
            double price = visibleMax - priceRange * i / 6d;
            float y = plot.top + plot.height() * i / 6f;
            canvas.drawText(formatPrice(price), plot.right + 7f,
                    y + sp(3), paint);
        }
    }

    private void drawLatestPrice(Canvas canvas) {
        Candle last = candles.get(candles.size() - 1);
        float y = priceY(last.getClose());
        if (y < plot.top || y > plot.bottom) return;

        int color = last.getClose() >= last.getOpen() ? bullColor : bearColor;
        paint.setColor(color);
        paint.setStrokeWidth(1f);
        canvas.drawLine(plot.left, y, plot.right, y, paint);

        RectF label = new RectF(plot.right + 3f, y - sp(9),
                getWidth() - 2f, y + sp(9));
        canvas.drawRect(label, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(formatPrice(last.getClose()),
                label.centerX(), y + sp(3.5f), paint);
    }

    private void drawMessage(Canvas canvas, String message) {
        paint.setColor(textColor);
        paint.setTextSize(sp(13));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f,
                getHeight() / 2f, paint);
    }

    private float candleX(int index, float step) {
        return plot.right - step * 0.5f
                - (visibleEnd - 1 - index) * step;
    }

    private float priceY(double price) {
        return (float) (plot.bottom
                - ((price - visibleMin) / priceRange) * plot.height());
    }

    private float getStep() {
        return Math.max(2.5f, (candleWidth + candleGap) * horizontalZoom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                dragging = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && !scaling
                        && dragging && event.getPointerCount() == 1) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    horizontalOffset += dx;
                    verticalOffset += dy;
                    lastX = event.getX();
                    lastY = event.getY();
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
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
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

    private boolean isValid(Candle candle) {
        return candle != null
                && Double.isFinite(candle.getOpen())
                && Double.isFinite(candle.getHigh())
                && Double.isFinite(candle.getLow())
                && Double.isFinite(candle.getClose())
                && candle.getHigh() >= candle.getLow();
    }

    private String formatPrice(double price) {
        double absolute = Math.abs(price);
        if (absolute >= 1000) return String.format(Locale.US, "%.2f", price);
        if (absolute >= 10) return String.format(Locale.US, "%.4f", price);
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
