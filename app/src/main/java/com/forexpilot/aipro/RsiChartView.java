package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class RsiChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Double> rsiValues =
            new ArrayList<>();

    private static final float LEFT = 20f;
    private static final float RIGHT = 20f;
    private static final float TOP = 20f;
    private static final float BOTTOM = 25f;

    private static final double RSI_MIN = 0.0;
    private static final double RSI_MAX = 100.0;

    public RsiChartView(Context context) {
        super(context);

        setBackgroundColor(
                Color.rgb(10, 15, 25)
        );

        paint.setStrokeWidth(2f);

        setFocusable(true);
    }

    /**
     * Updates the RSI panel with real RSI values.
     */
    public void setRsiValues(
            List<Double> values
    ) {

        rsiValues.clear();

        if (values != null) {

            for (Double value : values) {

                if (value != null
                        && Double.isFinite(value)) {

                    double safeValue =
                            Math.max(
                                    RSI_MIN,
                                    Math.min(
                                            RSI_MAX,
                                            value
                                    )
                            );

                    rsiValues.add(
                            safeValue
                    );
                }
            }
        }

        invalidate();
    }

    /**
     * Calculates the RSI series directly from candles.
     *
     * This allows the component to work independently
     * before MainActivity integration.
     */
    public void setCandles(
            List<Candle> candles,
            int period
    ) {

        rsiValues.clear();

        if (candles == null
                || candles.size() <= period
                || period <= 0) {

            invalidate();
            return;
        }

        for (
                int end = period;
                end < candles.size();
                end++
        ) {

            List<Candle> section =
                    candles.subList(
                            0,
                            end + 1
                    );

            double rsi =
                    IndicatorEngine.rsi(
                            section,
                            period
                    );

            if (Double.isFinite(rsi)) {

                rsiValues.add(
                        Math.max(
                                RSI_MIN,
                                Math.min(
                                        RSI_MAX,
                                        rsi
                                )
                        )
                );
            }
        }

        invalidate();
    }

    public void clearRsi() {

        rsiValues.clear();

        invalidate();
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(canvas);

        drawBackground(canvas);

        drawReferenceLines(canvas);

        drawLabels(canvas);

        if (rsiValues.isEmpty()) {

            drawWaitingMessage(canvas);

            return;
        }

        drawRsiLine(canvas);
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
                Color.rgb(45, 55, 70)
        );

        canvas.drawRect(
                LEFT,
                TOP,
                getWidth() - RIGHT,
                getHeight() - BOTTOM,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );
    }

    private void drawReferenceLines(
            Canvas canvas
    ) {

        float chartHeight =
                getHeight()
                        - TOP
                        - BOTTOM;

        float y70 =
                rsiToY(70);

        float y50 =
                rsiToY(50);

        float y30 =
                rsiToY(30);

        drawReferenceLine(
                canvas,
                y70,
                Color.rgb(255, 80, 90)
        );

        drawReferenceLine(
                canvas,
                y50,
                Color.rgb(80, 100, 125)
        );

        drawReferenceLine(
                canvas,
                y30,
                Color.rgb(40, 220, 125)
        );
    }

    private void drawReferenceLine(
            Canvas canvas,
            float y,
            int color
    ) {

        paint.setColor(color);

        paint.setStrokeWidth(1.5f);

        paint.setStyle(
                Paint.Style.STROKE
        );

        canvas.drawLine(
                LEFT,
                y,
                getWidth() - RIGHT,
                y,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );
    }

    private void drawLabels(
            Canvas canvas
    ) {

        paint.setTextSize(18f);

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                Color.rgb(255, 100, 110)
        );

        canvas.drawText(
                "70",
                4f,
                rsiToY(70) + 6f,
                paint
        );

        paint.setColor(
                Color.rgb(145, 158, 175)
        );

        canvas.drawText(
                "50",
                4f,
                rsiToY(50) + 6f,
                paint
        );

        paint.setColor(
                Color.rgb(40, 220, 125)
        );

        canvas.drawText(
                "30",
                4f,
                rsiToY(30) + 6f,
                paint
        );

        paint.setColor(
                Color.rgb(145, 158, 175)
        );

        paint.setTextSize(16f);

        canvas.drawText(
                "RSI 14",
                LEFT + 8f,
                TOP + 18f,
                paint
        );
    }

    private void drawRsiLine(
            Canvas canvas
    ) {

        if (rsiValues.size() < 2) {
            return;
        }

        float chartWidth =
                getWidth()
                        - LEFT
                        - RIGHT;

        float step =
                chartWidth
                        / (rsiValues.size() - 1);

        Path path =
                new Path();

        for (
                int i = 0;
                i < rsiValues.size();
                i++
        ) {

            double value =
                    rsiValues.get(i);

            float x =
                    LEFT
                            + i * step;

            float y =
                    rsiToY(value);

            if (i == 0) {

                path.moveTo(
                        x,
                        y
                );

            } else {

                path.lineTo(
                        x,
                        y
                );
            }
        }

        paint.setColor(
                Color.rgb(70, 150, 255)
        );

        paint.setStrokeWidth(3f);

        paint.setStyle(
                Paint.Style.STROKE
        );

        canvas.drawPath(
                path,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        /*
         * Highlight the latest RSI value.
         */
        double latest =
                rsiValues.get(
                        rsiValues.size() - 1
                );

        float latestX =
                LEFT + chartWidth;

        float latestY =
                rsiToY(latest);

        paint.setColor(
                Color.WHITE
        );

        canvas.drawCircle(
                latestX,
                latestY,
                5f,
                paint
        );

        paint.setTextSize(17f);

        String latestText =
                String.format(
                        java.util.Locale.US,
                        "%.2f",
                        latest
                );

        canvas.drawText(
                latestText,
                latestX - 35f,
                latestY - 10f,
                paint
        );
    }

    private float rsiToY(
            double rsi
    ) {

        double normalized =
                (
                        rsi - RSI_MIN
                )
                        / (
                        RSI_MAX - RSI_MIN
                );

        float chartHeight =
                getHeight()
                        - TOP
                        - BOTTOM;

        return (float)
                (
                        TOP
                                + chartHeight
                                * (
                                1.0
                                        - normalized
                        )
                );
    }

    private void drawWaitingMessage(
            Canvas canvas
    ) {

        paint.setColor(
                Color.rgb(145, 158, 175)
        );

        paint.setTextSize(20f);

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setTextAlign(
                Paint.Align.CENTER
        );

        canvas.drawText(
                "WAITING FOR RSI DATA",
                getWidth() / 2f,
                getHeight() / 2f,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );
    }
}