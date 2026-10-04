package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ChartDrawingView extends View {

    public enum DrawingType {
        HORIZONTAL,
        TREND
    }

    private static class Drawing {

        DrawingType type;

        float x1;
        float y1;

        float x2;
        float y2;

        Drawing(
                DrawingType type,
                float x1,
                float y1,
                float x2,
                float y2
        ) {
            this.type = type;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint previewPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Drawing> drawings =
            new ArrayList<>();

    private DrawingType selectedType =
            DrawingType.HORIZONTAL;

    private boolean drawingMode = false;

    private boolean drawingInProgress = false;

    private float startX;
    private float startY;

    private float currentX;
    private float currentY;

    public ChartDrawingView(Context context) {
        super(context);

        setBackgroundColor(
                Color.TRANSPARENT
        );

        paint.setAntiAlias(true);
        paint.setStrokeWidth(3f);
        paint.setStyle(Paint.Style.STROKE);

        previewPaint.setAntiAlias(true);
        previewPaint.setStrokeWidth(3f);
        previewPaint.setStyle(Paint.Style.STROKE);

        setFocusable(true);
    }

    /**
     * Select horizontal-line drawing mode.
     */
    public void setHorizontalMode() {

        selectedType =
                DrawingType.HORIZONTAL;

        drawingMode = true;

        drawingInProgress = false;

        invalidate();
    }

    /**
     * Select trend-line drawing mode.
     */
    public void setTrendMode() {

        selectedType =
                DrawingType.TREND;

        drawingMode = true;

        drawingInProgress = false;

        invalidate();
    }

    /**
     * Turns drawing mode off.
     */
    public void disableDrawingMode() {

        drawingMode = false;

        drawingInProgress = false;

        invalidate();
    }

    /**
     * Returns whether drawing mode is active.
     */
    public boolean isDrawingMode() {

        return drawingMode;
    }

    /**
     * Removes the most recently created drawing.
     */
    public void undoLastDrawing() {

        if (!drawings.isEmpty()) {

            drawings.remove(
                    drawings.size() - 1
            );

            invalidate();
        }
    }

    /**
     * Removes all drawings.
     */
    public void clearDrawings() {

        drawings.clear();

        drawingInProgress = false;

        invalidate();
    }

    /**
     * Returns number of saved drawings.
     */
    public int getDrawingCount() {

        return drawings.size();
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (!drawingMode) {
            return false;
        }

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                startX = event.getX();
                startY = event.getY();

                currentX = startX;
                currentY = startY;

                drawingInProgress = true;

                invalidate();

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!drawingInProgress) {
                    return true;
                }

                currentX = event.getX();
                currentY = event.getY();

                if (selectedType
                        == DrawingType.HORIZONTAL) {

                    currentY = startY;
                }

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:

                if (!drawingInProgress) {
                    return true;
                }

                currentX = event.getX();
                currentY = event.getY();

                if (selectedType
                        == DrawingType.HORIZONTAL) {

                    currentY = startY;
                }

                drawings.add(
                        new Drawing(
                                selectedType,
                                startX,
                                startY,
                                currentX,
                                currentY
                        )
                );

                drawingInProgress = false;

                drawingMode = false;

                invalidate();

                return true;

            case MotionEvent.ACTION_CANCEL:

                drawingInProgress = false;

                invalidate();

                return true;

            default:

                return true;
        }
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(canvas);

        drawSavedDrawings(canvas);

        if (drawingInProgress) {

            drawPreview(canvas);
        }
    }

    private void drawSavedDrawings(
            Canvas canvas
    ) {

        for (Drawing drawing : drawings) {

            if (drawing.type
                    == DrawingType.HORIZONTAL) {

                paint.setColor(
                        Color.rgb(
                                255,
                                193,
                                7
                        )
                );

                paint.setStrokeWidth(3f);

                canvas.drawLine(
                        0f,
                        drawing.y1,
                        getWidth(),
                        drawing.y1,
                        paint
                );

            } else {

                paint.setColor(
                        Color.rgb(
                                80,
                                170,
                                255
                        )
                );

                paint.setStrokeWidth(3f);

                canvas.drawLine(
                        drawing.x1,
                        drawing.y1,
                        drawing.x2,
                        drawing.y2,
                        paint
                );

                drawTrendEndpoints(
                        canvas,
                        drawing
                );
            }
        }
    }

    private void drawTrendEndpoints(
            Canvas canvas,
            Drawing drawing
    ) {

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                Color.WHITE
        );

        canvas.drawCircle(
                drawing.x1,
                drawing.y1,
                5f,
                paint
        );

        canvas.drawCircle(
                drawing.x2,
                drawing.y2,
                5f,
                paint
        );

        paint.setStyle(
                Paint.Style.STROKE
        );
    }

    private void drawPreview(
            Canvas canvas
    ) {

        previewPaint.setColor(
                Color.rgb(
                        255,
                        255,
                        255
                )
        );

        previewPaint.setStrokeWidth(2.5f);

        if (selectedType
                == DrawingType.HORIZONTAL) {

            canvas.drawLine(
                    0f,
                    startY,
                    getWidth(),
                    startY,
                    previewPaint
            );

        } else {

            canvas.drawLine(
                    startX,
                    startY,
                    currentX,
                    currentY,
                    previewPaint
            );
        }
    }
}