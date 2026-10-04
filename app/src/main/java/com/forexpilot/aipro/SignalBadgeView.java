package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import java.util.Locale;

public class SignalBadgeView extends View {

    private final Paint backgroundPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint borderPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint titlePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint detailPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private Signal signal;

    public SignalBadgeView(Context context) {
        super(context);

        setBackgroundColor(Color.TRANSPARENT);

        backgroundPaint.setStyle(
                Paint.Style.FILL
        );

        borderPaint.setStyle(
                Paint.Style.STROKE
        );

        borderPaint.setStrokeWidth(3f);

        titlePaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        detailPaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                )
        );

        setFocusable(true);
    }

    /**
     * Displays a real signal.
     */
    public void setSignal(Signal signal) {

        this.signal = signal;

        invalidate();
    }

    /**
     * Clears the displayed signal.
     */
    public void clearSignal() {

        signal = null;

        invalidate();
    }

    public boolean hasSignal() {

        return signal != null;
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        int accentColor =
                getAccentColor();

        /*
         * Background.
         */
        backgroundPaint.setColor(
                Color.rgb(
                        14,
                        20,
                        31
                )
        );

        canvas.drawRoundRect(
                8f,
                8f,
                width - 8f,
                height - 8f,
                18f,
                18f,
                backgroundPaint
        );

        /*
         * Accent border.
         */
        borderPaint.setColor(
                accentColor
        );

        canvas.drawRoundRect(
                8f,
                8f,
                width - 8f,
                height - 8f,
                18f,
                18f,
                borderPaint
        );

        if (signal == null) {

            drawWaiting(
                    canvas,
                    width,
                    height
            );

            return;
        }

        Signal.Direction direction =
                signal.getDirection();

        String directionText;

        if (direction
                == Signal.Direction.BUY) {

            directionText = "BUY";

        } else if (
                direction
                        == Signal.Direction.SELL) {

            directionText = "SELL";

        } else {

            directionText = "WAIT";
        }

        /*
         * Main signal.
         */
        titlePaint.setColor(
                accentColor
        );

        titlePaint.setTextSize(
                Math.min(
                        42f,
                        height * 0.48f
                )
        );

        canvas.drawText(
                directionText,
                24f,
                height * 0.55f,
                titlePaint
        );

        /*
         * Timeframe.
         */
        detailPaint.setColor(
                Color.rgb(
                        180,
                        190,
                        205
                )
        );

        detailPaint.setTextSize(18f);

        String timeframe =
                signal.getTimeframe();

        if (timeframe == null
                || timeframe.trim().isEmpty()) {

            timeframe = "UNKNOWN";
        }

        canvas.drawText(
                "TIMEFRAME  " + timeframe,
                24f,
                height - 18f,
                detailPaint
        );

        /*
         * Entry price.
         */
        double entry =
                signal.getEntry();

        if (isValidPrice(entry)) {

            String entryText =
                    String.format(
                            Locale.US,
                            "ENTRY  %.5f",
                            entry
                    );

            float textWidth =
                    detailPaint.measureText(
                            entryText
                    );

            canvas.drawText(
                    entryText,
                    Math.max(
                            24f,
                            width
                                    - textWidth
                                    - 24f
                    ),
                    32f,
                    detailPaint
            );
        }
    }

    private void drawWaiting(
            Canvas canvas,
            float width,
            float height
    ) {

        titlePaint.setColor(
                Color.rgb(
                        145,
                        158,
                        175
                )
        );

        titlePaint.setTextSize(30f);

        canvas.drawText(
                "WAIT",
                24f,
                height * 0.55f,
                titlePaint
        );

        detailPaint.setColor(
                Color.rgb(
                        120,
                        132,
                        150
                )
        );

        detailPaint.setTextSize(17f);

        canvas.drawText(
                "NO CONFIRMED SIGNAL",
                24f,
                height - 18f,
                detailPaint
        );
    }

    private int getAccentColor() {

        if (signal == null) {

            return Color.rgb(
                    145,
                    158,
                    175
            );
        }

        Signal.Direction direction =
                signal.getDirection();

        if (direction
                == Signal.Direction.BUY) {

            return Color.rgb(
                    40,
                    220,
                    125
            );
        }

        if (direction
                == Signal.Direction.SELL) {

            return Color.rgb(
                    255,
                    70,
                    80
            );
        }

        return Color.rgb(
                145,
                158,
                175
        );
    }

    private boolean isValidPrice(
            double price
    ) {

        return !Double.isNaN(price)
                && !Double.isInfinite(price)
                && price > 0;
    }
}