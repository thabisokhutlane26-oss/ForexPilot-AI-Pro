package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ForexChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Candle> candles = new ArrayList<>();

    private float lastX;
    private float lastY;

    private boolean moving = false;

    private float candleWidth = 18f;
    private float spacing = 5f;

    private float scrollOffset = 0f;

    private static final float MIN_CANDLE_WIDTH = 6f;
    private static final float MAX_CANDLE_WIDTH = 45f;

    public ForexChartView(Context context) {
        super(context);

        paint.setStrokeWidth(2f);

        setBackgroundColor(Color.rgb(10, 15, 25));

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * Supply real Twelve Data candles to the chart.
     */
    public void setCandles(List<Candle> newCandles) {

        candles.clear();

        if (newCandles != null) {
            candles.addAll(newCandles);
        }

        scrollOffset = 0f;

        invalidate();
    }

    /**
     * Clear chart data.
     */
    public void clearChart() {

        candles.clear();

        scrollOffset = 0f;

        invalidate();
    }

    /**
     * Zoom the chart.
     */
    public void zoomIn() {

        candleWidth += 3f;

        if (candleWidth > MAX_CANDLE_WIDTH) {
            candleWidth = MAX_CANDLE_WIDTH;
        }

        invalidate();
    }

    public void zoomOut() {

        candleWidth -= 3f;

        if (candleWidth < MIN_CANDLE_WIDTH) {
            candleWidth = MIN_CANDLE_WIDTH;
        }

        invalidate();
    }

    /**
     * Return the chart to the latest candles.
     */
    public void fitLatest() {

        scrollOffset = 0f;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        drawBackground(canvas);

        if (candles.isEmpty()) {

            drawMessage(
                    canvas,
                    "WAITING FOR LIVE CANDLE DATA"
            );

            return;
        }

        drawChart(canvas);
    }

    private void drawBackground(Canvas canvas) {

        canvas.drawColor(Color.rgb(10, 15, 25));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.rgb(35, 45, 60));

        float horizontalStep = getHeight() / 5f;

        for (int i = 1; i < 5; i++) {

            float y = horizontalStep * i;

            canvas.drawLine(
                    0,
                    y,
                    getWidth(),
                    y,
                    paint
            );
        }

        float verticalStep = getWidth() / 6f;

        for (int i = 1; i < 6; i++) {

            float x = verticalStep * i;

            canvas.drawLine(
                    x,
                    0,
                    x,
                    getHeight(),
                    paint
            );
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawChart(Canvas canvas) {

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        int startIndex = calculateStartIndex();
        int endIndex = candles.size();

        if (startIndex >= endIndex) {
            return;
        }

        double minPrice = Double.MAX_VALUE;
        double maxPrice = -Double.MAX_VALUE;

        for (int i = startIndex; i < endIndex; i++) {

            Candle candle = candles.get(i);

            minPrice = Math.min(
                    minPrice,
                    candle.getLow()
            );

            maxPrice = Math.max(
                    maxPrice,
                    candle.getHigh()
            );
        }

        if (!Double.isFinite(minPrice)
                || !Double.isFinite(maxPrice)
                || maxPrice <= minPrice) {

            drawMessage(
                    canvas,
                    "INVALID LIVE CANDLE DATA"
            );

            return;
        }

        double padding = (maxPrice - minPrice) * 0.08;

        if (padding <= 0) {
            padding = 0.0001;
        }

        minPrice -= padding;
        maxPrice += padding;

        double priceRange = maxPrice - minPrice;

        float chartTop = 20f;
        float chartBottom = height - 20f;
        float chartHeight = chartBottom - chartTop;

        for (int i = startIndex; i < endIndex; i++) {

            Candle candle = candles.get(i);

            float x =
                    getCandleX(
                            i,
                            startIndex
                    );

            if (x < -candleWidth
                    || x > width + candleWidth) {
                continue;
            }

            float openY =
                    priceToY(
                            candle.getOpen(),
                            minPrice,
                            priceRange,
                            chartTop,
                            chartHeight
                    );

            float closeY =
                    priceToY(
                            candle.getClose(),
                            minPrice,
                            priceRange,
                            chartTop,
                            chartHeight
                    );

            float highY =
                    priceToY(
                            candle.getHigh(),
                            minPrice,
                            priceRange,
                            chartTop,
                            chartHeight
                    );

            float lowY =
                    priceToY(
                            candle.getLow(),
                            minPrice,
                            priceRange,
                            chartTop,
                            chartHeight
                    );

            boolean bullish =
                    candle.getClose()
                            >= candle.getOpen();

            if (bullish) {
                paint.setColor(
                        Color.rgb(0, 220, 130)
                );
            } else {
                paint.setColor(
                        Color.rgb(255, 70, 90)
                );
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);

            // Wick
            canvas.drawLine(
                    x,
                    highY,
                    x,
                    lowY,
                    paint
            );

            // Body
            paint.setStyle(Paint.Style.FILL);

            float bodyTop =
                    Math.min(
                            openY,
                            closeY
                    );

            float bodyBottom =
                    Math.max(
                            openY,
                            closeY
                    );

            if (bodyBottom - bodyTop < 2f) {
                bodyBottom = bodyTop + 2f;
            }

            RectF body =
                    new RectF(
                            x - candleWidth / 2f,
                            bodyTop,
                            x + candleWidth / 2f,
                            bodyBottom
                    );

            canvas.drawRect(
                    body,
                    paint
            );
        }

        drawPriceScale(
                canvas,
                minPrice,
                maxPrice
        );
    }

    private int calculateStartIndex() {

        float candleSlot =
                candleWidth + spacing;

        if (candleSlot <= 0) {
            return 0;
        }

        int visible =
                (int)
                        (getWidth()
                                / candleSlot)
                        + 3;

        int start =
                candles.size()
                        - visible;

        start +=
                (int)
                        scrollOffset;

        if (start < 0) {
            start = 0;
        }

        if (start >= candles.size()) {
            start = candles.size() - 1;
        }

        return start;
    }

    private float getCandleX(
            int index,
            int startIndex
    ) {

        float slot =
                candleWidth + spacing;

        return 20f
                + (index - startIndex)
                * slot;
    }

    private float priceToY(
            double price,
            double minPrice,
            double range,
            float top,
            float chartHeight
    ) {

        double normalized =
                (price - minPrice)
                        / range;

        return (float)
                (top
                        + chartHeight
                        * (1.0 - normalized));
    }

    private void drawPriceScale(
            Canvas canvas,
            double minPrice,
            double maxPrice
    ) {

        paint.setColor(
                Color.rgb(170, 180, 195)
        );

        paint.setTextSize(24f);
        paint.setStyle(Paint.Style.FILL);

        int steps = 5;

        for (int i = 0; i <= steps; i++) {

            double price =
                    minPrice
                            + (maxPrice - minPrice)
                            * i
                            / steps;

            float y =
                    20f
                            + (getHeight() - 40f)
                            * (1f - i / (float) steps);

            String text =
                    formatPrice(price);

            canvas.drawText(
                    text,
                    getWidth() - 125f,
                    y,
                    paint
            );
        }
    }

    private String formatPrice(
            double price
    ) {

        if (price >= 1000) {
            return String.format(
                    "%.2f",
                    price
            );
        }

        if (price >= 100) {
            return String.format(
                    "%.3f",
                    price
            );
        }

        if (price >= 10) {
            return String.format(
                    "%.4f",
                    price
            );
        }

        return String.format(
                "%.5f",
                price
        );
    }

    private void drawMessage(
            Canvas canvas,
            String message
    ) {

        paint.setColor(
                Color.rgb(150, 165, 185)
        );

        paint.setTextSize(28f);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(
                Paint.Align.CENTER
        );

        canvas.drawText(
                message,
                getWidth() / 2f,
                getHeight() / 2f,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                lastX = event.getX();
                lastY = event.getY();

                moving = true;

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!moving) {
                    return true;
                }

                float dx =
                        event.getX() - lastX;

                float dy =
                        event.getY() - lastY;

                /*
                 * Horizontal movement controls
                 * historical candle scrolling.
                 */
                if (Math.abs(dx)
                        > Math.abs(dy)) {

                    float slot =
                            candleWidth
                                    + spacing;

                    if (slot > 0) {

                        scrollOffset -=
                                dx / slot;

                        float maxScroll =
                                Math.max(
                                        0,
                                        candles.size()
                                                - 1
                                );

                        if (scrollOffset < 0) {
                            scrollOffset = 0;
                        }

                        if (scrollOffset
                                > maxScroll) {

                            scrollOffset =
                                    maxScroll;
                        }
                    }
                }

                lastX = event.getX();
                lastY = event.getY();

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                moving = false;

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
}