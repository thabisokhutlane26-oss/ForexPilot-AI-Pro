package com.forexpilot.aipro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private int selectedDrawingIndex = -1;

    private boolean drawingMode = false;
    private boolean drawingInProgress = false;

    private float startX;
    private float startY;

    private float currentX;
    private float currentY;

    private static final float HIT_DISTANCE = 35f;

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
        setClickable(true);
    }

    public void setHorizontalMode() {

        selectedType =
                DrawingType.HORIZONTAL;

        drawingMode = true;
        drawingInProgress = false;
        selectedDrawingIndex = -1;

        invalidate();
    }

    public void setTrendMode() {

        selectedType =
                DrawingType.TREND;

        drawingMode = true;
        drawingInProgress = false;
        selectedDrawingIndex = -1;

        invalidate();
    }

    public void disableDrawingMode() {

        drawingMode = false;
        drawingInProgress = false;

        invalidate();
    }

    public boolean isDrawingMode() {

        return drawingMode;
    }

    /**
     * Selects a drawing by touching near it.
     */
    public boolean selectDrawing(
            float x,
            float y
    ) {

        int foundIndex =
                findDrawingAt(
                        x,
                        y
                );

        selectedDrawingIndex =
                foundIndex;

        invalidate();

        return foundIndex >= 0;
    }

    /**
     * Deletes the currently selected drawing.
     */
    public boolean deleteSelectedDrawing() {

        if (selectedDrawingIndex < 0
                || selectedDrawingIndex
                >= drawings.size()) {

            return false;
        }

        drawings.remove(
                selectedDrawingIndex
        );

        selectedDrawingIndex = -1;

        invalidate();

        return true;
    }

    /**
     * Removes the latest drawing.
     */
    public void undoLastDrawing() {

        if (!drawings.isEmpty()) {

            drawings.remove(
                    drawings.size() - 1
            );

            selectedDrawingIndex = -1;

            invalidate();
        }
    }

    /**
     * Removes every drawing.
     */
    public void clearDrawings() {

        drawings.clear();

        selectedDrawingIndex = -1;
        drawingInProgress = false;

        invalidate();
    }

    public int getDrawingCount() {

        return drawings.size();
    }

    public int getSelectedDrawingIndex() {

        return selectedDrawingIndex;
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                /*
                 * Drawing mode creates a new drawing.
                 */
                if (drawingMode) {

                    startX = x;
                    startY = y;

                    currentX = x;
                    currentY = y;

                    drawingInProgress = true;

                    invalidate();

                    return true;
                }

                /*
                 * Normal mode selects an existing drawing.
                 */
                selectDrawing(x, y);

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!drawingInProgress) {
                    return true;
                }

                currentX = x;
                currentY = y;

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

                currentX = x;
                currentY = y;

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

                selectedDrawingIndex =
                        drawings.size() - 1;

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

        for (
                int i = 0;
                i < drawings.size();
                i++
        ) {

            Drawing drawing =
                    drawings.get(i);

            boolean selected =
                    i == selectedDrawingIndex;

            if (drawing.type
                    == DrawingType.HORIZONTAL) {

                if (selected) {

                    paint.setColor(
                            Color.WHITE
                    );

                    paint.setStrokeWidth(6f);

                } else {

                    paint.setColor(
                            Color.rgb(
                                    255,
                                    193,
                                    7
                            )
                    );

                    paint.setStrokeWidth(3f);
                }

                canvas.drawLine(
                        0f,
                        drawing.y1,
                        getWidth(),
                        drawing.y1,
                        paint
                );

            } else {

                if (selected) {

                    paint.setColor(
                            Color.WHITE
                    );

                    paint.setStrokeWidth(6f);

                } else {

                    paint.setColor(
                            Color.rgb(
                                    80,
                                    170,
                                    255
                            )
                    );

                    paint.setStrokeWidth(3f);
                }

                canvas.drawLine(
                        drawing.x1,
                        drawing.y1,
                        drawing.x2,
                        drawing.y2,
                        paint
                );

                drawTrendEndpoints(
                        canvas,
                        drawing,
                        selected
                );
            }
        }
    }

    private void drawTrendEndpoints(
            Canvas canvas,
            Drawing drawing,
            boolean selected
    ) {

        paint.setStyle(
                Paint.Style.FILL
        );

        if (selected) {

            paint.setColor(
                    Color.WHITE
            );

        } else {

            paint.setColor(
                    Color.rgb(
                            220,
                            230,
                            240
                    )
            );
        }

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
                Color.WHITE
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

    private int findDrawingAt(
            float x,
            float y
    ) {

        /*
         * Search from newest to oldest so that
         * the most recent drawing gets priority.
         */
        for (
                int i = drawings.size() - 1;
                i >= 0;
                i--
        ) {

            Drawing drawing =
                    drawings.get(i);

            if (drawing.type
                    == DrawingType.HORIZONTAL) {

                if (Math.abs(y - drawing.y1)
                        <= HIT_DISTANCE) {

                    return i;
                }

            } else {

                if (distanceToLine(
                        x,
                        y,
                        drawing.x1,
                        drawing.y1,
                        drawing.x2,
                        drawing.y2
                ) <= HIT_DISTANCE) {

                    return i;
                }
            }
        }

        return -1;
    }

    private float distanceToLine(
            float px,
            float py,
            float x1,
            float y1,
            float x2,
            float y2
    ) {

        float dx = x2 - x1;
        float dy = y2 - y1;

        if (dx == 0f && dy == 0f) {

            return distance(
                    px,
                    py,
                    x1,
                    y1
            );
        }

        float t =
                (
                        (px - x1) * dx
                                + (py - y1) * dy
                )
                        / (
                        dx * dx
                                + dy * dy
                );

        t = Math.max(
                0f,
                Math.min(
                        1f,
                        t
                )
        );

        float nearestX =
                x1 + t * dx;

        float nearestY =
                y1 + t * dy;

        return distance(
                px,
                py,
                nearestX,
                nearestY
        );
    }

    private float distance(
            float x1,
            float y1,
            float x2,
            float y2
    ) {

        float dx = x1 - x2;
        float dy = y1 - y2;

        return (float)
                Math.sqrt(
                        dx * dx
                                + dy * dy
                );
    }
}