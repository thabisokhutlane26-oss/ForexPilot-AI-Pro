package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Candle> candles =
            new ArrayList<>();

    private final ScaleGestureDetector scaleDetector;

    private float lastX;
    private float lastY;

    private boolean moving = false;

    private float candleWidth = 18f;
    private float spacing = 6f;

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

    private static final float MIN_CANDLE_WIDTH = 5f;
    private static final float MAX_CANDLE_WIDTH = 55f;

    private static final float MIN_SPACING = 2f;
    private static final float MAX_SPACING = 14f;

    private static final float CHART_LEFT = 18f;
    private static final float CHART_RIGHT = 96f;
    private static final float CHART_TOP = 28f;
    private static final float CHART_BOTTOM = 48f;

    private static final int BACKGROUND =
            Color.rgb(10, 15, 25);

    private static final int GRID =
            Color.rgb(35, 45, 60);

    private static final int BORDER =
            Color.rgb(65, 75, 90);

    private static final int TEXT =
            Color.rgb(175, 185, 200);

    private static final int BULL =
            Color.rgb(0, 220, 130);

    private static final int BEAR =
            Color.rgb(255, 70, 90);

    public ForexChartView(Context context) {

        super(context);

        paint.setStrokeWidth(2f);

        setBackgroundColor(BACKGROUND);

        setFocusable(true);
        setFocusableInTouchMode(true);

        scaleDetector =
                new ScaleGestureDetector(
                        context,
                        new ScaleListener()
                );
    }

    public void setCandles(
            List<Candle> newCandles
    ) {

        candles.clear();

        if (newCandles != null) {
            candles.addAll(newCandles);
        }

        scrollOffset = 0f;

        crosshairX = -1f;
        crosshairY = -1f;
        showCrosshair = false;

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

        candleWidth += 4f;

        if (candleWidth > MAX_CANDLE_WIDTH) {
            candleWidth = MAX_CANDLE_WIDTH;
        }

        updateSpacing();
        clampScroll();

        invalidate();
    }

    public void zoomOut() {

        candleWidth -= 4f;

        if (candleWidth < MIN_CANDLE_WIDTH) {
            candleWidth = MIN_CANDLE_WIDTH;
        }

        updateSpacing();
        clampScroll();

        invalidate();
    }

    public void fitLatest() {

        scrollOffset = 0f;

        invalidate();
    }

    private void updateSpacing() {

        spacing =
                Math.max(
                        MIN_SPACING,
                        Math.min(
                                MAX_SPACING,
                                candleWidth * 0.30f
                        )
                );
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

        canvas.drawColor(BACKGROUND);

        float right =
                getWidth() - CHART_RIGHT;

        float bottom =
                getHeight() - CHART_BOTTOM;

        float chartHeight =
                bottom - CHART_TOP;

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(1f);

        paint.setColor(GRID);

        for (int i = 1; i < 6; i++) {

            float y =
                    CHART_TOP
                            + chartHeight
                            * i
                            / 6f;

            canvas.drawLine(
                    CHART_LEFT,
                    y,
                    right,
                    y,
                    paint
            );
        }

        float chartWidth =
                right - CHART_LEFT;

        for (int i = 1; i < 8; i++) {

            float x =
                    CHART_LEFT
                            + chartWidth
                            * i
                            / 8f;

            canvas.drawLine(
                    x,
                    CHART_TOP,
                    x,
                    bottom,
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

        int visibleCount =
                calculateVisibleCount();

        int endIndex =
                Math.min(
                        candles.size(),
                        startIndex + visibleCount
                );

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
                    "INVALID CANDLE DATA"
            );

            return;
        }

        double tradeMin =
                validMinTradeLevel();

        double tradeMax =
                validMaxTradeLevel();

        if (Double.isFinite(tradeMin)) {
            minPrice =
                    Math.min(
                            minPrice,
                            tradeMin
                    );
        }

        if (Double.isFinite(tradeMax)) {
            maxPrice =
                    Math.max(
                            maxPrice,
                            tradeMax
                    );
        }

        double rawRange =
                maxPrice - minPrice;

        double padding =
                rawRange * 0.08;

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
                    x < CHART_LEFT - candleWidth
                            || x > width - CHART_RIGHT
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

            int candleColor =
                    bullish
                            ? BULL
                            : BEAR;

            paint.setColor(candleColor);

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    Math.max(
                            1f,
                            Math.min(
                                    2.5f,
                                    candleWidth * 0.10f
                            )
                    )
            );

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

            float minimumBody =
                    Math.max(
                            2f,
                            Math.min(
                                    5f,
                                    candleWidth * 0.25f
                            )
                    );

            if (
                    bodyBottom - bodyTop
                            < minimumBody
            ) {

                float center =
                        (bodyTop + bodyBottom)
                                / 2f;

                bodyTop =
                        center
                                - minimumBody / 2f;

                bodyBottom =
                        center
                                + minimumBody / 2f;
            }

            float bodyWidth =
                    Math.max(
                            2f,
                            candleWidth * 0.72f
                    );

            RectF body =
                    new RectF(
                            x - bodyWidth / 2f,
                            bodyTop,
                            x + bodyWidth / 2f,
                            bodyBottom
                    );

            canvas.drawRect(
                    body,
                    paint
            );

            if (candleWidth >= 12f) {

                paint.setStyle(
                        Paint.Style.STROKE
                );

                paint.setStrokeWidth(1f);

                canvas.drawRect(
                        body,
                        paint
                );

                paint.setStyle(
                        Paint.Style.FILL
                );
            }
        }

        drawLatestPriceLine(
                canvas,
                endIndex - 1,
                startIndex,
                minPrice,
                priceRange,
                chartTop,
                chartHeight
        );

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

        drawTimeScale(
                canvas,
                startIndex,
                endIndex
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

    private void drawLatestPriceLine(
            Canvas canvas,
            int latestIndex,
            int startIndex,
            double minPrice,
            double priceRange,
            float chartTop,
            float chartHeight
    ) {

        if (
                latestIndex < startIndex
                        || latestIndex >= candles.size()
        ) {
            return;
        }

        Candle latest =
                candles.get(latestIndex);

        if (!isValidCandle(latest)) {
            return;
        }

        double latestPrice =
                latest.getClose();

        float y =
                priceToY(
                        latestPrice,
                        minPrice,
                        priceRange,
                        chartTop,
                        chartHeight
                );

        if (
                y < CHART_TOP
                        || y > getHeight() - CHART_BOTTOM
        ) {
            return;
        }

        paint.setColor(
                Color.rgb(100, 160, 220)
        );

        paint.setStrokeWidth(1f);

        paint.setStyle(
                Paint.Style.STROKE
        );

        float x1 = CHART_LEFT;
        float x2 = getWidth() - CHART_RIGHT;

        for (
                float x = x1;
                x < x2;
                x += 12f
        ) {

            canvas.drawLine(
                    x,
                    y,
                    Math.min(x + 6f, x2),
                    y,
                    paint
            );
        }

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                Color.rgb(30, 50, 70)
        );

        RectF box =
                new RectF(
                        getWidth() - CHART_RIGHT + 2f,
                        y - 13f,
                        getWidth() - 2f,
                        y + 13f
                );

        canvas.drawRect(
                box,
                paint
        );

        paint.setColor(Color.WHITE);

        paint.setTextSize(15f);

        canvas.drawText(
                formatPrice(latestPrice),
                getWidth() - CHART_RIGHT + 7f,
                y + 5f,
                paint
        );
    }

    private void drawChartBorder(
            Canvas canvas
    ) {

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(1f);

        paint.setColor(BORDER);

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
                BEAR,
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
                BULL,
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
                BULL,
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
                BULL,
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

        paint.setTextSize(17f);

        canvas.drawText(
                label,
                getWidth() - CHART_RIGHT + 7f,
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

        double markerPrice =
                buy
                        ? lastCandle.getLow()
                        : lastCandle.getHigh();

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
                buy ? BULL : BEAR
        );

        float radius = 10f;

        float markerY =
                buy
                        ? y + 20f
                        : y - 20f;

        canvas.drawCircle(
                x,
                markerY,
                radius,
                paint
        );

        paint.setColor(Color.WHITE);

        paint.setTextSize(14f);

        paint.setTextAlign(
                Paint.Align.CENTER
        );

        canvas.drawText(
                buy ? "B" : "S",
                x,
                markerY + 5f,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );
    }

    private void drawCrosshair(
            Canvas canvas
    ) {

        float right =
                getWidth() - CHART_RIGHT;

        float bottom =
                getHeight() - CHART_BOTTOM;

        if (
                crosshairX < CHART_LEFT
                        || crosshairX > right
                        || crosshairY < CHART_TOP
                        || crosshairY > bottom
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
                right,
                crosshairY,
                paint
        );

        canvas.drawLine(
                crosshairX,
                CHART_TOP,
                crosshairX,
                bottom,
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
                        right + 2f,
                        crosshairY - 16f,
                        getWidth() - 2f,
                        crosshairY + 16f
                );

        canvas.drawRect(
                priceBox,
                paint
        );

        paint.setColor(Color.WHITE);

        paint.setTextSize(15f);

        canvas.drawText(
                formatPrice(
                        getPriceFromCrosshair()
                ),
                right + 7f,
                crosshairY + 5f,
                paint
        );

        int index =
                getCandleIndexFromX(
                        crosshairX
                );

        if (
                index >= 0
                        && index < candles.size()
        ) {

            Candle candle =
                    candles.get(index);

            if (candle != null) {

                paint.setColor(
                        Color.rgb(25, 35, 48)
                );

                String time =
                        formatCandleTime(
                                candle.getTimestamp()
                        );

                float boxWidth =
                        110f;

                float left =
                        Math.max(
                                CHART_LEFT,
                                Math.min(
                                        crosshairX - boxWidth / 2f,
                                        right - boxWidth
                                )
                        );

                RectF timeBox =
                        new RectF(
                                left,
                                bottom + 3f,
                                left + boxWidth,
                                bottom + 29f
                        );

                canvas.drawRect(
                        timeBox,
                        paint
                );

                paint.setColor(Color.WHITE);

                paint.setTextSize(13f);

                paint.setTextAlign(
                        Paint.Align.CENTER
                );

                canvas.drawText(
                        time,
                        left + boxWidth / 2f,
                        bottom + 21f,
                        paint
                );

                paint.setTextAlign(
                        Paint.Align.LEFT
                );
            }
        }
    }

    private double getPriceFromCrosshair() {

        if (candles.isEmpty()) {
            return Double.NaN;
        }

        int startIndex =
                calculateStartIndex();

        int visibleCount =
                calculateVisibleCount();

        int endIndex =
                Math.min(
                        candles.size(),
                        startIndex + visibleCount
                );

        double min =
                Double.MAX_VALUE;

        double max =
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

        if (chartHeight <= 0) {
            return Double.NaN;
        }

        double normalized =
                (
                        crosshairY
                                - CHART_TOP
                )
                        / chartHeight;

        return max
                - normalized * (max - min);
    }

    private int calculateVisibleCount() {

        float slot =
                candleWidth + spacing;

        if (slot <= 0) {
            return 1;
        }

        float chartWidth =
                getWidth()
                        - CHART_LEFT
                        - CHART_RIGHT;

        return Math.max(
                1,
                (int) Math.floor(
                        chartWidth / slot
                )
        );
    }

    private int calculateStartIndex() {

        int visible =
                calculateVisibleCount();

        int maxStart =
                Math.max(
                        0,
                        candles.size() - visible
                );

        int start =
                maxStart
                        - Math.round(
                        scrollOffset
                );

        if (start < 0) {
            start = 0;
        }

        if (start > maxStart) {
            start = maxStart;
        }

        return start;
    }

    private void clampScroll() {

        int visible =
                calculateVisibleCount();

        float maxScroll =
                Math.max(
                        0,
                        candles.size() - visible
                );

        if (scrollOffset < 0) {
            scrollOffset = 0;
        }

        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private float getCandleX(
            int index,
            int startIndex
    ) {

        float slot =
                candleWidth + spacing;

        return CHART_LEFT
                + (
                index - startIndex
        )
                * slot
                + slot / 2f;
    }

    private int getCandleIndexFromX(
            float x
    ) {

        int start =
                calculateStartIndex();

        float slot =
                candleWidth + spacing;

        if (slot <= 0) {
            return start;
        }

        int index =
                start
                        + (int)
                        Math.floor(
                                (
                                        x - CHART_LEFT
                                ) / slot
                        );

        if (
                index < 0
                        || index >= candles.size()
        ) {
            return -1;
        }

        return index;
    }

    private float priceToY(
            double price,
            double minPrice,
            double range,
            float top,
            float chartHeight
    ) {

        double normalized =
                (
                        price - minPrice
                ) / range;

        return (float)
                (
                        top
                                + chartHeight
                                * (
                                1.0
                                        - normalized
                        )
                );
    }

    private void drawPriceScale(
            Canvas canvas,
            double minPrice,
            double maxPrice
    ) {

        paint.setColor(TEXT);

        paint.setTextSize(17f);

        paint.setStyle(
                Paint.Style.FILL
        );

        int steps = 6;

        float chartHeight =
                getHeight()
                        - CHART_TOP
                        - CHART_BOTTOM;

        for (
                int i = 0;
                i <= steps;
                i++
        ) {

            double price =
                    minPrice
                            + (
                            maxPrice
                                    - minPrice
                    )
                            * i
                            / steps;

            float y =
                    CHART_TOP
                            + chartHeight
                            * (
                            1f
                                    - i
                                    / (float) steps
                    );

            paint.setColor(
                    Color.rgb(90, 100, 115)
            );

            canvas.drawRect(
                    getWidth() - CHART_RIGHT,
                    y - 1f,
                    getWidth() - CHART_RIGHT + 5f,
                    y + 1f,
                    paint
            );

            paint.setColor(TEXT);

            String text =
                    formatPrice(price);

            canvas.drawText(
                    text,
                    getWidth() - CHART_RIGHT + 8f,
                    y + 5f,
                    paint
            );
        }
    }

    private void drawTimeScale(
            Canvas canvas,
            int startIndex,
            int endIndex
    ) {

        if (endIndex <= startIndex) {
            return;
        }

        float bottom =
                getHeight() - CHART_BOTTOM;

        int count =
                endIndex - startIndex;

        int labels =
                Math.min(
                        6,
                        Math.max(
                                2,
                                count / 12
                        )
                );

        int step =
                Math.max(
                        1,
                        count / labels
                );

        paint.setTextSize(12f);

        paint.setColor(TEXT);

        for (
                int i = startIndex;
                i < endIndex;
                i += step
        ) {

            Candle candle =
                    candles.get(i);

            if (candle == null) {
                continue;
            }

            float x =
                    getCandleX(
                            i,
                            startIndex
                    );

            if (
                    x < CHART_LEFT
                            || x > getWidth() - CHART_RIGHT
            ) {
                continue;
            }

            String text =
                    formatCandleTime(
                            candle.getTimestamp()
                    );

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            canvas.drawText(
                    text,
                    x,
                    bottom + 22f,
                    paint
            );
        }

        paint.setTextAlign(
                Paint.Align.LEFT
        );
    }

    private String formatCandleTime(
            long timestamp
    ) {

        if (timestamp <= 0) {
            return "--";
        }

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "dd MMM HH:mm",
                        Locale.US
                );

        format.setTimeZone(
                TimeZone.getTimeZone(
                        "Africa/Johannesburg"
                )
        );

        return format.format(
                new Date(timestamp)
        );
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

        paint.setTextSize(23f);

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
        )
                && candle.getHigh()
                >= candle.getLow();
    }

    private double validMinTradeLevel() {

        double value =
                Double.MAX_VALUE;

        if (Double.isFinite(entry)) {
            value =
                    Math.min(
                            value,
                            entry
                    );
        }

        if (Double.isFinite(stopLoss)) {
            value =
                    Math.min(
                            value,
                            stopLoss
                    );
        }

        if (Double.isFinite(tp1)) {
            value =
                    Math.min(
                            value,
                            tp1
                    );
        }

        if (Double.isFinite(tp2)) {
            value =
                    Math.min(
                            value,
                            tp2
                    );
        }

        if (Double.isFinite(tp3)) {
            value =
                    Math.min(
                            value,
                            tp3
                    );
        }

        return value == Double.MAX_VALUE
                ? Double.NaN
                : value;
    }

    private double validMaxTradeLevel() {

        double value =
                -Double.MAX_VALUE;

        if (Double.isFinite(entry)) {
            value =
                    Math.max(
                            value,
                            entry
                    );
        }

        if (Double.isFinite(stopLoss)) {
            value =
                    Math.max(
                            value,
                            stopLoss
                    );
        }

        if (Double.isFinite(tp1)) {
            value =
                    Math.max(
                            value,
                            tp1
                    );
        }

        if (Double.isFinite(tp2)) {
            value =
                    Math.max(
                            value,
                            tp2
                    );
        }

        if (Double.isFinite(tp3)) {
            value =
                    Math.max(
                            value,
                            tp3
                    );
        }

        return value == -Double.MAX_VALUE
                ? Double.NaN
                : value;
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(
                ScaleGestureDetector detector
        ) {

            float factor =
                    detector.getScaleFactor();

            if (
                    !Float.isFinite(factor)
                            || factor <= 0
            ) {
                return true;
            }

            float focusX =
                    detector.getFocusX();

            float oldSlot =
                    candleWidth + spacing;

            int oldStart =
                    calculateStartIndex();

            int focusIndex =
                    oldStart
                            + (int) Math.floor(
                            (
                                    focusX
                                            - CHART_LEFT
                            ) / oldSlot
                    );

            candleWidth *= factor;

            if (
                    candleWidth
                            < MIN_CANDLE_WIDTH
            ) {
                candleWidth =
                        MIN_CANDLE_WIDTH;
            }

            if (
                    candleWidth
                            > MAX_CANDLE_WIDTH
            ) {
                candleWidth =
                        MAX_CANDLE_WIDTH;
            }

            updateSpacing();

            float newSlot =
                    candleWidth + spacing;

            int newVisible =
                    calculateVisibleCount();

            int newMaxStart =
                    Math.max(
                            0,
                            candles.size()
                                    - newVisible
                    );

            int desiredStart =
                    focusIndex
                            - (int) Math.floor(
                            (
                                    focusX
                                            - CHART_LEFT
                            ) / newSlot
                    );

            desiredStart =
                    Math.max(
                            0,
                            Math.min(
                                    desiredStart,
                                    newMaxStart
                            )
                    );

            scrollOffset =
                    newMaxStart
                            - desiredStart;

            clampScroll();

            invalidate();

            return true;
        }
    }

    private void setParentScrollEnabled(
            boolean enabled
    ) {

        ViewParent parent =
                getParent();

        while (parent != null) {

            parent.requestDisallowInterceptTouchEvent(
                    !enabled
            );

            parent =
                    parent.getParent();
        }
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                setParentScrollEnabled(false);

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

            case MotionEvent.ACTION_POINTER_DOWN:

                setParentScrollEnabled(false);

                showCrosshair = false;

                invalidate();

                return true;

            case MotionEvent.ACTION_MOVE:

                setParentScrollEnabled(false);

                if (
                        event.getPointerCount()
                                >= 2
                ) {

                    showCrosshair = false;

                    invalidate();

                    return true;
                }

                if (!moving) {
                    return true;
                }

                float dx =
                        event.getX()
                                - lastX;

                float dy =
                        event.getY()
                                - lastY;

                if (
                        Math.abs(dx)
                                > Math.abs(dy)
                ) {

                    float slot =
                            candleWidth
                                    + spacing;

                    if (slot > 0) {

                        /*
                         * Dragging LEFT moves backward
                         * through candle history.
                         *
                         * Dragging RIGHT returns toward
                         * the latest candles.
                         */
                        scrollOffset -=
                                dx / slot;

                        clampScroll();
                    }

                } else {

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

            case MotionEvent.ACTION_POINTER_UP:

                setParentScrollEnabled(false);

                int pointerIndex =
                        event.getActionIndex();

                int remainingPointer = -1;

                for (
                        int i = 0;
                        i < event.getPointerCount();
                        i++
                ) {

                    if (i != pointerIndex) {
                        remainingPointer = i;
                        break;
                    }
                }

                if (remainingPointer >= 0) {

                    lastX =
                            event.getX(
                                    remainingPointer
                            );

                    lastY =
                            event.getY(
                                    remainingPointer
                            );
                }

                return true;

            case MotionEvent.ACTION_UP:

                moving = false;

                setParentScrollEnabled(true);

                performClick();

                return true;

            case MotionEvent.ACTION_CANCEL:

                moving = false;

                setParentScrollEnabled(true);

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