package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.Locale;

public class TradeLevelView extends View {

    private final Paint linePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint textPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint markerPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private Signal signal;

    private double visibleHigh =
            Double.NaN;

    private double visibleLow =
            Double.NaN;

    private boolean showLabels = true;

    public TradeLevelView(Context context) {
        super(context);

        setBackgroundColor(
                android.graphics.Color.TRANSPARENT
        );

        linePaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        markerPaint.setAntiAlias(true);

        textPaint.setTextSize(22f);

        setFocusable(true);
    }

    /**
     * Sets the real signal whose trade levels
     * should be displayed.
     */
    public void setSignal(Signal signal) {

        this.signal = signal;

        invalidate();
    }

    /**
     * Clears all trade levels.
     */
    public void clearSignal() {

        this.signal = null;

        invalidate();
    }

    /**
     * Sets the visible price range used to
     * convert prices into screen coordinates.
     */
    public void setPriceRange(
            double high,
            double low
    ) {

        if (!isValidPrice(high)
                || !isValidPrice(low)
                || high <= low) {

            visibleHigh = Double.NaN;
            visibleLow = Double.NaN;

        } else {

            visibleHigh = high;
            visibleLow = low;
        }

        invalidate();
    }

    public void setShowLabels(
            boolean show
    ) {

        showLabels = show;

        invalidate();
    }

    public boolean hasSignal() {

        return signal != null
                && signal.getDirection()
                != Signal.Direction.WAIT;
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(canvas);

        if (!hasSignal()) {
            return;
        }

        if (!hasValidRange()) {
            return;
        }

        drawTradeLevels(canvas);

        drawSignalMarker(canvas);
    }

    private void drawTradeLevels(
            Canvas canvas
    ) {

        Signal.Direction direction =
                signal.getDirection();

        double entry =
                signal.getEntry();

        double stopLoss =
                signal.getStopLoss();

        double tp1 =
                signal.getTakeProfit1();

        double tp2 =
                signal.getTakeProfit2();

        double tp3 =
                signal.getTakeProfit3();

        if (isValidPrice(entry)) {

            drawLevel(
                    canvas,
                    entry,
                    getEntryColor(direction),
                    "ENTRY"
            );
        }

        if (isValidPrice(stopLoss)) {

            drawLevel(
                    canvas,
                    stopLoss,
                    android.graphics.Color.rgb(
                            255,
                            70,
                            80
                    ),
                    "SL"
            );
        }

        if (isValidPrice(tp1)) {

            drawLevel(
                    canvas,
                    tp1,
                    android.graphics.Color.rgb(
                            40,
                            220,
                            125
                    ),
                    "TP1"
            );
        }

        if (isValidPrice(tp2)) {

            drawLevel(
                    canvas,
                    tp2,
                    android.graphics.Color.rgb(
                            40,
                            220,
                            125
                    ),
                    "TP2"
            );
        }

        if (isValidPrice(tp3)) {

            drawLevel(
                    canvas,
                    tp3,
                    android.graphics.Color.rgb(
                            40,
                            220,
                            125
                    ),
                    "TP3"
            );
        }
    }

    private void drawLevel(
            Canvas canvas,
            double price,
            int color,
            String label
    ) {

        float y =
                priceToY(price);

        if (Float.isNaN(y)) {
            return;
        }

        linePaint.setColor(color);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setStyle(
                Paint.Style.STROKE
        );

        canvas.drawLine(
                0f,
                y,
                getWidth(),
                y,
                linePaint
        );

        if (!showLabels) {
            return;
        }

        textPaint.setColor(color);
        textPaint.setTextSize(18f);
        textPaint.setStyle(
                Paint.Style.FILL
        );

        String priceText =
                String.format(
                        Locale.US,
                        "%s  %.5f",
                        label,
                        price
                );

        float textWidth =
                textPaint.measureText(
                        priceText
                );

        float x =
                Math.max(
                        8f,
                        getWidth()
                                - textWidth
                                - 8f
                );

        float baseline =
                Math.max(
                        20f,
                        y - 6f
                );

        canvas.drawText(
                priceText,
                x,
                baseline,
                textPaint
        );
    }

    private void drawSignalMarker(
            Canvas canvas
    ) {

        double entry =
                signal.getEntry();

        float y =
                priceToY(entry);

        if (Float.isNaN(y)) {
            return;
        }

        boolean buy =
                signal.getDirection()
                        == Signal.Direction.BUY;

        int color =
                buy
                        ? android.graphics.Color.rgb(
                                40,
                                220,
                                125
                        )
                        : android.graphics.Color.rgb(
                                255,
                                70,
                                80
                        );

        markerPaint.setColor(color);
        markerPaint.setStyle(
                Paint.Style.FILL
        );

        Path marker =
                new Path();

        float centerX =
                24f;

        if (buy) {

            marker.moveTo(
                    centerX,
                    y - 18f
            );

            marker.lineTo(
                    centerX - 12f,
                    y
            );

            marker.lineTo(
                    centerX,
                    y + 18f
            );

        } else {

            marker.moveTo(
                    centerX,
                    y + 18f
            );

            marker.lineTo(
                    centerX - 12f,
                    y
            );

            marker.lineTo(
                    centerX,
                    y - 18f
            );
        }

        marker.close();

        canvas.drawPath(
                marker,
                markerPaint
        );

        textPaint.setColor(color);
        textPaint.setTextSize(18f);

        canvas.drawText(
                buy ? "BUY" : "SELL",
                42f,
                y + 6f,
                textPaint
        );
    }

    private float priceToY(
            double price
    ) {

        if (!hasValidRange()
                || !isValidPrice(price)) {

            return Float.NaN;
        }

        double normalized =
                (
                        price - visibleLow
                )
                        / (
                        visibleHigh
                                - visibleLow
                );

        normalized =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                normalized
                        )
                );

        float top = 8f;

        float bottom =
                getHeight() - 8f;

        return (float)
                (
                        bottom
                                - normalized
                                * (
                                bottom - top
                        )
                );
    }

    private boolean hasValidRange() {

        return isValidPrice(visibleHigh)
                && isValidPrice(visibleLow)
                && visibleHigh > visibleLow;
    }

    private boolean isValidPrice(
            double price
    ) {

        return !Double.isNaN(price)
                && !Double.isInfinite(price)
                && price > 0;
    }

    private int getEntryColor(
            Signal.Direction direction
    ) {

        if (direction
                == Signal.Direction.BUY) {

            return android.graphics.Color.rgb(
                    40,
                    220,
                    125
            );
        }

        if (direction
                == Signal.Direction.SELL) {

            return android.graphics.Color.rgb(
                    255,
                    70,
                    80
            );
        }

        return android.graphics.Color.rgb(
                145,
                158,
                175
        );
    }
}