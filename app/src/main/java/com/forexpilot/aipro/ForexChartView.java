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
import java.util.Locale;

public class ForexChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Candle> candles = new ArrayList<>();

    private float lastX;
    private float lastY;

    private boolean moving = false;

    private float candleWidth = 18f;
    private float spacing = 5f;

    private float scrollOffset = 0f;

    private float crosshairX = -1f;
    private float crosshairY = -1f;

    private boolean showCrosshair = false;

    private double entry = Double.NaN;
    private double stopLoss = Double.NaN;
    private double tp1 = Double.NaN;
    private double tp2 = Double.NaN;
    private double tp3 = Double.NaN;

    private Signal.Direction signalDirection =
            Signal.Direction.WAIT;

    private static final float MIN_CANDLE_WIDTH = 6f;
    private static final float MAX_CANDLE_WIDTH = 45f;

    private static final float CHART_LEFT = 16f;
    private static final float CHART_RIGHT = 105f;
    private static final float CHART_TOP = 28f;
    private static final float CHART_BOTTOM = 30f;

    public ForexChartView(Context context) {
        super(context);

        paint.setStrokeWidth(2f);

        setBackgroundColor(
                Color.rgb(10, 15, 25)
        );

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setCandles(List<Candle> newCandles) {

        candles.clear();

        if (newCandles != null) {
            candles.addAll(newCandles);
        }

        scrollOffset = 0f;

        invalidate();
    }

    public void clearChart() {

        candles.clear();

        scrollOffset = 0f;

        crosshairX = -1f;
        crosshairY = -1f;

        showCrosshair = false;

        invalidate();
    }

    public void setTradeLevels(
            Signal signal
    ) {

        if (signal == null) {
            clearTradeLevels();
            return;
        }

        entry = signal.getEntry();
        stopLoss = signal.getStopLoss();
        tp1 = signal.getTakeProfit1();
        tp2 = signal.getTakeProfit2();
        tp3 = signal.getTakeProfit3();

        signalDirection =
                signal.getDirection();

        invalidate();
    }

    public void clearTradeLevels() {

        entry = Double.NaN;
        stopLoss = Double.NaN;
        tp1 = Double.NaN;
        tp2 = Double.NaN;
        tp3 = Double.NaN;

        signalDirection =
                Signal.Direction.WAIT;

        invalidate();
    }

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

    public void fitLatest() {

        scrollOffset = 0f;

        invalidate();
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

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

        if (showCrosshair) {
            drawCrosshair(canvas);
        }
    }

    private void drawBackground(
            Canvas canvas
    ) {

        canvas.drawColor(
                Color.rgb(10, 15, 25)
        );

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(1f);

        paint.setColor(
                Color.rgb(35, 45, 60)
        );

        float chartWidth =
                getWidth() - CHART_RIGHT;

        float chartHeight =
                getHeight()
                        - CHART_TOP
                        - CHART_BOTTOM;

        float horizontalStep =
                chartHeight / 5f;

        for (int i = 1; i < 5; i++) {

            float y =
                    CHART_TOP
                            + horizontalStep * i;

            canvas.drawLine(
                    CHART_LEFT,
                    y,
                    chartWidth,
                    y,
                    paint
            );
        }

        float verticalStep =
                chartWidth / 6f;

        for (int i = 1; i < 6; i++) {

            float x =
                    CHART_LEFT
                            + verticalStep * i;

            canvas.drawLine(
                    x,
                    CHART_TOP,
                    x,
                    getHeight()
                            - CHART_BOTTOM,
                    paint
            );
        }

        paint.setStyle(
                Paint.Style.FILL
        );
    }

    private void drawChart(
            Canvas canvas
    ) {

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        int startIndex =
                calculateStartIndex();

        int endIndex =
                candles.size();

        if (startIndex >= endIndex) {
            return;
        }

        double minPrice =
                Double.MAX_VALUE;

        double maxPrice =
                -Double.MAX_VALUE;

        for (
                int i = startIndex;
                i < endIndex;
                i++
        ) {

            Candle candle =
                    candles.get(i);

            if (!isValidCandle(candle)) {
                continue;
            }

            minPrice =
                    Math.min(
                            minPrice,
                            candle.getLow()
                    );

            maxPrice =
                    Math.max(
                            maxPrice,
                            candle.getHigh()
                    );
        }

        if (
                !Double.isFinite(minPrice)
                        || !Double.isFinite(maxPrice)
                        || maxPrice <= minPrice
        ) {

            drawMessage(
                    canvas,
                    "INVALID LIVE CANDLE DATA"
            );

            return;
        }

        /*
         * Include trade levels in the visible
         * price range when they are valid.
         */
        minPrice =
                Math.min(
                        minPrice,
                        validMinTradeLevel()
                );

        maxPrice =
                Math.max(
                        maxPrice,
                        validMaxTradeLevel()
                );

        double padding =
                (maxPrice - minPrice) * 0.08;

        if (padding <= 0) {
            padding = 0.0001;
        }

        minPrice -= padding;
        maxPrice += padding;

        double priceRange =
                maxPrice - minPrice;

        float chartTop =
                CHART_TOP;

        float chartBottom =
                height - CHART_BOTTOM;

        float chartHeight =
                chartBottom - chartTop;

        for (
                int i = startIndex;
                i < endIndex;
                i++
        ) {

            Candle candle =
                    candles.get(i);

            if (!isValidCandle(candle)) {
                continue;
            }

            float x =
                    getCandleX(
                            i,
                            startIndex
                    );

            if (
                    x < -candleWidth
                            || x > width + candleWidth
            ) {
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

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(2f);

            canvas.drawLine(
                    x,
                    highY,
                    x,
                    lowY,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

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

            if (
                    bodyBottom - bodyTop
                            < 2f
            ) {
                bodyBottom =
                        bodyTop + 2f;
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

        drawTradeLevels(
                canvas,
                minPrice,
                maxPrice,
                priceRange,
                chartTop,
                chartHeight
        );

        drawPriceScale(
                canvas,
                minPrice,
                maxPrice
        );

        drawChartBorder(canvas);

        drawSignalMarker(
                canvas,
                startIndex,
                minPrice,
                priceRange,
                chartTop,
                chartHeight
        );
    }

    private void drawChartBorder(
            Canvas canvas
    ) {

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(1f);

        paint.setColor(
                Color.rgb(55, 65, 80)
        );

        canvas.drawRect(
                CHART_LEFT,
                CHART_TOP,
                getWidth() - CHART_RIGHT,
                getHeight() - CHART_BOTTOM,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );
    }

    private void drawTradeLevels(
            Canvas canvas,
            double minPrice,
            double maxPrice,
            double priceRange,
            float chartTop,
            float chartHeight
    ) {

        drawTradeLine(
                canvas,
                entry,
                "ENTRY",
                Color.rgb(80, 180, 255),
                minPrice,
                maxPrice,
                priceRange,
                chartTop,
                chartHeight
        );

        drawTradeLine(
                canvas,
                stopLoss,
                "SL",
                Color.rgb(255, 70, 90),
                minPrice,
                maxPrice,
                priceRange,
                chartTop,
                chartHeight
        );

        drawTradeLine(
                canvas,
                tp1,
                "TP1",
                Color.rgb(0, 220, 130),
                minPrice,
                maxPrice,
                priceRange,
                chartTop,
                chartHeight
        );

        drawTradeLine(
                canvas,
                tp2,
                "TP2",
                Color.rgb(0, 220, 130),
                minPrice,
                maxPrice,
                priceRange,
                chartTop,
                chartHeight
        );

        drawTradeLine(
                canvas,
                tp3,
                "TP3",
                Color.rgb(0, 220, 130),
                minPrice,
                maxPrice,
                priceRange,
                chartTop,
                chartHeight
        );
    }

    private void drawTradeLine(
            Canvas canvas,
            double price,
            String label,
            int color,
            double minPrice,
            double maxPrice,
            double range,
            float chartTop,
            float chartHeight
    ) {

        if (!Double.isFinite(price)) {
            return;
        }

        if (
                price < minPrice
                        || price > maxPrice
        ) {
            return;
        }

        float y =
                priceToY(
                        price,
                        minPrice,
                        range,
                        chartTop,
                        chartHeight
                );

        paint.setColor(color);

        paint.setStrokeWidth(1.5f);

        paint.setStyle(
                Paint.Style.STROKE
        );

        canvas.drawLine(
                CHART_LEFT,
                y,
                getWidth() - CHART_RIGHT,
                y,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setTextSize(20f);

        canvas.drawText(
                label,
                getWidth() - CHART_RIGHT + 8f,
                y - 4f,
                paint
        );
    }

    private void drawSignalMarker(
            Canvas canvas,
            int startIndex,
            double minPrice,
            double range,
            float chartTop,
            float chartHeight
    ) {

        if (
                signalDirection
                        == Signal.Direction.WAIT
        ) {
            return;
        }

        if (candles.isEmpty()) {
            return;
        }

        int lastIndex =
                candles.size() - 1;

        if (lastIndex < startIndex) {
            return;
        }

        Candle lastCandle =
                candles.get(lastIndex);

        if (!isValidCandle(lastCandle)) {
            return;
        }

        float x =
                getCandleX(
                        lastIndex,
                        startIndex
                );

        if (
                x < CHART_LEFT
                        || x > getWidth() - CHART_RIGHT
        ) {
            return;
        }

        boolean buy =
                signalDirection
                        == Signal.Direction.BUY;

        double markerPrice;

        if (buy) {
            markerPrice =
                    lastCandle.getLow();
        } else {
            markerPrice =
                    lastCandle.getHigh();
        }

        float y =
                priceToY(
                        markerPrice,
                        minPrice,
                        range,
                        chartTop,
                        chartHeight
                );

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                buy
                        ? Color.rgb(0, 220, 130)
                        : Color.rgb(255, 70, 90)
        );

        float radius = 10f;

        canvas.drawCircle(
                x,
                buy ? y + 18f : y - 18f,
                radius,
                paint
        );

        paint.setColor(Color.WHITE);

        paint.setTextSize(16f);

        paint.setTextAlign(
                Paint.Align.CENTER
        );

        canvas.drawText(
                buy ? "B" : "S",
                x,
                buy ? y + 23f : y - 13f,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );
    }

    private void drawCrosshair(
            Canvas canvas
    ) {

        if (
                crosshairX < CHART_LEFT
                        || crosshairX
                        > getWidth() - CHART_RIGHT
                        || crosshairY < CHART_TOP
                        || crosshairY
                        > getHeight() - CHART_BOTTOM
        ) {
            return;
        }

        paint.setColor(
                Color.rgb(180, 190, 205)
        );

        paint.setStrokeWidth(1f);

        paint.setStyle(
                Paint.Style.STROKE
        );

        canvas.drawLine(
                CHART_LEFT,
                crosshairY,
                getWidth() - CHART_RIGHT,
                crosshairY,
                paint
        );

        canvas.drawLine(
                crosshairX,
                CHART_TOP,
                crosshairX,
                getHeight() - CHART_BOTTOM,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                Color.rgb(25, 35, 48)
        );

        RectF priceBox =
                new RectF(
                        getWidth() - CHART_RIGHT + 3f,
                        crosshairY - 16f,
                        getWidth() - 3f,
                        crosshairY + 16f
                );

        canvas.drawRect(
                priceBox,
                paint
        );

        paint.setColor(Color.WHITE);

        paint.setTextSize(17f);

        String text =
                formatPrice(
                        getPriceFromCrosshair()
                );

        canvas.drawText(
                text,
                getWidth() - CHART_RIGHT + 9f,
                crosshairY + 6f,
                paint
        );
    }

    private double getPriceFromCrosshair() {

        if (candles.isEmpty()) {
            return Double.NaN;
        }

        /*
         * Crosshair price is estimated from
         * the currently visible candles.
         */
        int startIndex =
                calculateStartIndex();

        double min =
                Double.MAX_VALUE;

        double max =
                -Double.MAX_VALUE;

        for (
                int i = startIndex;
                i < candles.size();
                i++
        ) {

            Candle candle =
                    candles.get(i);

            if (!isValidCandle(candle)) {
                continue;
            }

            min =
                    Math.min(
                            min,
                            candle.getLow()
                    );

            max =
                    Math.max(
                            max,
                            candle.getHigh()
                    );
        }

        if (
                !Double.isFinite(min)
                        || !Double.isFinite(max)
                        || max <= min
        ) {
            return Double.NaN;
        }

        double padding =
                (max - min) * 0.08;

        min -= padding;
        max += padding;

        float chartHeight =
                getHeight()
                        - CHART_TOP
                        - CHART_BOTTOM;

        double normalized =
                (crosshairY - CHART_TOP)
                        / chartHeight;

        return max
                - normalized * (max - min);
    }

    private int calculateStartIndex() {

        float candleSlot =
                candleWidth + spacing;

        if (candleSlot <= 0) {
            return 0;
        }

        int chartWidth =
                (int) (
                        getWidth()
                                - CHART_LEFT
                                - CHART_RIGHT
                );

        int visible =
                (int)
                        (chartWidth / candleSlot)
                                + 3;

        int start =
                candles.size()
                        - visible;

        start +=
                (int) scrollOffset;

        if (start < 0) {
            start = 0;
        }

        if (
                start >= candles.size()
        ) {
            start =
                    Math.max(
                            0,
                            candles.size() - 1
                    );
        }

        return start;
    }

    private float getCandleX(
            int index,
            int startIndex
    ) {

        float slot =
                candleWidth + spacing;

        return CHART_LEFT
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
                (
                        top
                                + chartHeight
                                * (1.0 - normalized)
                );
    }

    private void drawPriceScale(
            Canvas canvas,
            double minPrice,
            double maxPrice
    ) {

        paint.setColor(
                Color.rgb(170, 180, 195)
        );

        paint.setTextSize(20f);

        paint.setStyle(
                Paint.Style.FILL
        );

        int steps = 5;

        for (
                int i = 0;
                i <= steps;
                i++
        ) {

            double price =
                    minPrice
                            + (maxPrice - minPrice)
                            * i
                            / steps;

            float y =
                    CHART_TOP
                            + (
                            getHeight()
                                    - CHART_TOP
                                    - CHART_BOTTOM
                    )
                            * (
                            1f
                                    - i
                                    / (float) steps
                    );

            String text =
                    formatPrice(price);

            canvas.drawText(
                    text,
                    getWidth()
                            - CHART_RIGHT
                            + 8f,
                    y + 6f,
                    paint
            );
        }
    }

    private String formatPrice(
            double price
    ) {

        if (!Double.isFinite(price)) {
            return "--";
        }

        if (price >= 1000) {

            return String.format(
                    Locale.US,
                    "%.2f",
                    price
            );
        }

        if (price >= 100) {

            return String.format(
                    Locale.US,
                    "%.3f",
                    price
            );
        }

        if (price >= 10) {

            return String.format(
                    Locale.US,
                    "%.4f",
                    price
            );
        }

        return String.format(
                Locale.US,
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

        paint.setTextSize(25f);

        paint.setStyle(
                Paint.Style.FILL
        );

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

    private boolean isValidCandle(
            Candle candle
    ) {

        if (candle == null) {
            return false;
        }

        return Double.isFinite(
                candle.getOpen()
        )
                && Double.isFinite(
                candle.getHigh()
        )
                && Double.isFinite(
                candle.getLow()
        )
                && Double.isFinite(
                candle.getClose()
        );
    }

    private double validMinTradeLevel() {

        double value =
                Double.MAX_VALUE;

        if (Double.isFinite(entry)) {
            value = Math.min(value, entry);
        }

        if (Double.isFinite(stopLoss)) {
            value = Math.min(value, stopLoss);
        }

        if (Double.isFinite(tp1)) {
            value = Math.min(value, tp1);
        }

        if (Double.isFinite(tp2)) {
            value = Math.min(value, tp2);
        }

        if (Double.isFinite(tp3)) {
            value = Math.min(value, tp3);
        }

        if (value == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        return value;
    }

    private double validMaxTradeLevel() {

        double value =
                -Double.MAX_VALUE;

        if (Double.isFinite(entry)) {
            value = Math.max(value, entry);
        }

        if (Double.isFinite(stopLoss)) {
            value = Math.max(value, stopLoss);
        }

        if (Double.isFinite(tp1)) {
            value = Math.max(value, tp1);
        }

        if (Double.isFinite(tp2)) {
            value = Math.max(value, tp2);
        }

        if (Double.isFinite(tp3)) {
            value = Math.max(value, tp3);
        }

        if (value == -Double.MAX_VALUE) {
            return -Double.MAX_VALUE;
        }

        return value;
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                lastX =
                        event.getX();

                lastY =
                        event.getY();

                moving = true;

                showCrosshair = true;

                crosshairX =
                        event.getX();

                crosshairY =
                        event.getY();

                invalidate();

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!moving) {
                    return true;
                }

                float dx =
                        event.getX()
                                - lastX;

                float dy =
                        event.getY()
                                - lastY;

                /*
                 * Horizontal movement scrolls
                 * through historical candles.
                 */
                if (
                        Math.abs(dx)
                                > Math.abs(dy)
                ) {

                    float slot =
                            candleWidth
                                    + spacing;

                    if (slot > 0) {

                        scrollOffset -=
                                dx / slot;

                        float maxScroll =
                                Math.max(
                                        0,
                                        candles.size() - 1
                                );

                        if (scrollOffset < 0) {
                            scrollOffset = 0;
                        }

                        if (
                                scrollOffset
                                        > maxScroll
                        ) {
                            scrollOffset =
                                    maxScroll;
                        }
                    }

                } else {

                    /*
                     * Vertical movement moves
                     * the crosshair.
                     */
                    crosshairY =
                            event.getY();
                }

                crosshairX =
                        event.getX();

                lastX =
                        event.getX();

                lastY =
                        event.getY();

                showCrosshair = true;

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:

                moving = false;

                /*
                 * Keep the crosshair visible
                 * after release so the user can
                 * inspect the selected price.
                 */
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