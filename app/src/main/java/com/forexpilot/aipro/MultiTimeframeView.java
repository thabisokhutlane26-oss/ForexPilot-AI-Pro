package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MultiTimeframeView extends View {

    private final Paint backgroundPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint titlePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint timeframePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint signalPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint detailPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<MultiTimeframeEngine.TimeframeResult>
            results = new ArrayList<>();

    private static final String[] TIMEFRAMES = {
            "5M",
            "15M",
            "30M",
            "1H",
            "4H",
            "1D"
    };

    public MultiTimeframeView(Context context) {
        super(context);

        backgroundPaint.setStyle(
                Paint.Style.FILL
        );

        titlePaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        timeframePaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        signalPaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        setBackgroundColor(
                Color.TRANSPARENT
        );

        setFocusable(true);
    }

    /**
     * Supplies the real MTF results.
     */
    public void setResults(
            List<MultiTimeframeEngine.TimeframeResult>
                    newResults
    ) {

        results.clear();

        if (newResults != null) {

            results.addAll(
                    newResults
            );
        }

        invalidate();
    }

    /**
     * Clears all MTF results.
     */
    public void clearResults() {

        results.clear();

        invalidate();
    }

    public int getResultCount() {

        return results.size();
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

        /*
         * Panel background.
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

        drawTitle(
                canvas
        );

        if (results.isEmpty()) {

            drawWaiting(
                    canvas,
                    width,
                    height
            );

            return;
        }

        drawTimeframes(
                canvas,
                width,
                height
        );

        drawConfluence(
                canvas,
                width,
                height
        );
    }

    private void drawTitle(
            Canvas canvas
    ) {

        titlePaint.setColor(
                Color.rgb(
                        230,
                        238,
                        248
                )
        );

        titlePaint.setTextSize(21f);

        canvas.drawText(
                "MULTI-TIMEFRAME INTELLIGENCE",
                20f,
                31f,
                titlePaint
        );
    }

    private void drawTimeframes(
            Canvas canvas,
            float width,
            float height
    ) {

        float startY = 60f;

        float rowHeight =
                Math.max(
                        32f,
                        (height - 115f) / 6f
                );

        for (
                int i = 0;
                i < TIMEFRAMES.length;
                i++
        ) {

            String timeframe =
                    TIMEFRAMES[i];

            MultiTimeframeEngine.TimeframeResult
                    result =
                    findResult(
                            timeframe
                    );

            float y =
                    startY
                            + i * rowHeight;

            drawRow(
                    canvas,
                    timeframe,
                    result,
                    y
            );
        }
    }

    private void drawRow(
            Canvas canvas,
            String timeframe,
            MultiTimeframeEngine.TimeframeResult
                    result,
            float y
    ) {

        timeframePaint.setColor(
                Color.rgb(
                        175,
                        188,
                        205
                )
        );

        timeframePaint.setTextSize(18f);

        canvas.drawText(
                timeframe,
                22f,
                y,
                timeframePaint
        );

        String directionText =
                "WAIT";

        int directionColor =
                Color.rgb(
                        145,
                        158,
                        175
                );

        if (result != null) {

            Signal.Direction direction =
                    result.getDirection();

            if (direction
                    == Signal.Direction.BUY) {

                directionText =
                        "BUY";

                directionColor =
                        Color.rgb(
                                40,
                                220,
                                125
                        );

            } else if (
                    direction
                            == Signal.Direction.SELL) {

                directionText =
                        "SELL";

                directionColor =
                        Color.rgb(
                                255,
                                70,
                                80
                        );
            }
        }

        signalPaint.setColor(
                directionColor
        );

        signalPaint.setTextSize(18f);

        canvas.drawText(
                directionText,
                105f,
                y,
                signalPaint
        );

        if (result != null) {

            String status =
                    result.getDirection()
                            == Signal.Direction.WAIT
                            ? "WAIT"
                            : "CONFIRMED";

            detailPaint.setColor(
                    Color.rgb(
                            110,
                            125,
                            145
                    )
            );

            detailPaint.setTextSize(14f);

            canvas.drawText(
                    status,
                    175f,
                    y,
                    detailPaint
            );
        }
    }

    private void drawConfluence(
            Canvas canvas,
            float width,
            float height
    ) {

        if (results.size() < 6) {

            return;
        }

        MultiTimeframeEngine.ConfluenceResult
                confluence =
                MultiTimeframeEngine
                        .calculateConfluence(
                                results
                        );

        if (confluence == null) {

            return;
        }

        int buy =
                confluence.getBuyCount();

        int sell =
                confluence.getSellCount();

        int wait =
                confluence.getWaitCount();

        int strength =
                confluence.getStrength();

        Signal.Direction direction =
                confluence.getDirection();

        String directionText =
                "WAIT";

        int directionColor =
                Color.rgb(
                        145,
                        158,
                        175
                );

        if (direction
                == Signal.Direction.BUY) {

            directionText =
                    "BUY";

            directionColor =
                    Color.rgb(
                            40,
                            220,
                            125
                    );

        } else if (
                direction
                        == Signal.Direction.SELL) {

            directionText =
                    "SELL";

            directionColor =
                    Color.rgb(
                            255,
                            70,
                            80
                    );
        }

        float baseY =
                height - 72f;

        detailPaint.setColor(
                Color.rgb(
                        165,
                        178,
                        195
                )
        );

        detailPaint.setTextSize(15f);

        canvas.drawText(
                "BUY " + buy
                        + "   SELL " + sell
                        + "   WAIT " + wait,
                20f,
                baseY,
                detailPaint
        );

        signalPaint.setColor(
                directionColor
        );

        signalPaint.setTextSize(20f);

        canvas.drawText(
                "CONFLUENCE  "
                        + directionText,
                20f,
                baseY + 27f,
                signalPaint
        );

        detailPaint.setColor(
                Color.rgb(
                        165,
                        178,
                        195
                )
        );

        detailPaint.setTextSize(15f);

        canvas.drawText(
                "STRENGTH  "
                        + strength
                        + "/6",
                190f,
                baseY + 27f,
                detailPaint
        );
    }

    private void drawWaiting(
            Canvas canvas,
            float width,
            float height
    ) {

        detailPaint.setColor(
                Color.rgb(
                        145,
                        158,
                        175
                )
        );

        detailPaint.setTextSize(18f);

        canvas.drawText(
                "WAITING FOR MTF DATA",
                20f,
                height / 2f,
                detailPaint
        );
    }

    private MultiTimeframeEngine.TimeframeResult
    findResult(
            String timeframe
    ) {

        for (
                MultiTimeframeEngine.TimeframeResult
                        result : results
        ) {

            if (result == null) {
                continue;
            }

            if (timeframe.equalsIgnoreCase(
                    result.getTimeframe()
            )) {

                return result;
            }
        }

        return null;
    }
}