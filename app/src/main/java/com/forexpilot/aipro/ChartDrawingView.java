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

    private final Paint endpointPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Drawing> drawings =
            new ArrayList<>();

    private DrawingType selectedType =
            DrawingType.HORIZONTAL;

    private int selectedDrawingIndex = -1;

    private boolean drawingMode = false;

    private boolean drawingInProgress = false;

    private boolean movingDrawing = false;

    private float startX;
    private float startY;

    private float currentX;
    private float currentY;

    private float lastMoveX;
    private float lastMoveY;

    private static final float HIT_DISTANCE = 35f;

    private static final float ENDPOINT_RADIUS = 7f;

    public ChartDrawingView(Context context) {

        super(context);

        setBackgroundColor(
                Color.TRANSPARENT
        );

        paint.setAntiAlias(true);

        paint.setStrokeWidth(3f);

        paint.setStyle(
                Paint.Style.STROKE
        );

        previewPaint.setAntiAlias(true);

        previewPaint.setStrokeWidth(3f);

        previewPaint.setStyle(
                Paint.Style.STROKE
        );

        endpointPaint.setAntiAlias(true);

        setFocusable(true);

        setClickable(true);
    }

    public void setHorizontalMode() {

        selectedType =
                DrawingType.HORIZONTAL;

        drawingMode = true;

        drawingInProgress = false;

        movingDrawing = false;

        selectedDrawingIndex = -1;

        invalidate();
    }

    public void setTrendMode() {

        selectedType =
                DrawingType.TREND;

        drawingMode = true;

        drawingInProgress = false;

        movingDrawing = false;

        selectedDrawingIndex = -1;

        invalidate();
    }

    public void disableDrawingMode() {

        drawingMode = false;

        drawingInProgress = false;

        movingDrawing = false;

        invalidate();
    }

    public boolean isDrawingMode() {

        return drawingMode;
    }

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

    public boolean deleteSelectedDrawing() {

        if (
                selectedDrawingIndex < 0
                        || selectedDrawingIndex
                        >= drawings.size()
        ) {
            return false;
        }

        drawings.remove(
                selectedDrawingIndex
        );

        selectedDrawingIndex = -1;

        movingDrawing = false;

        invalidate();

        return true;
    }

    public void undoLastDrawing() {

        if (!drawings.isEmpty()) {

            drawings.remove(
                    drawings.size() - 1
            );

            selectedDrawingIndex = -1;

            movingDrawing = false;

            invalidate();
        }
    }

    public void clearDrawings() {

        drawings.clear();

        selectedDrawingIndex = -1;

        drawingInProgress = false;

        movingDrawing = false;

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

        float x =
                event.getX();

        float y =
                event.getY();

        switch (
                event.getActionMasked()
        ) {

            case MotionEvent.ACTION_DOWN:

                /*
                 * Drawing mode:
                 * start a brand-new drawing.
                 */
                if (drawingMode) {

                    startX = x;

                    startY = y;

                    currentX = x;

                    currentY = y;

                    drawingInProgress = true;

                    movingDrawing = false;

                    invalidate();

                    return true;
                }

                /*
                 * Normal mode:
                 * select an existing drawing.
                 */
                int found =
                        findDrawingAt(
                                x,
                                y
                        );

                selectedDrawingIndex =
                        found;

                if (found >= 0) {

                    movingDrawing = true;

                    lastMoveX = x;

                    lastMoveY = y;

                } else {

                    movingDrawing = false;
                }

                invalidate();

                return true;

            case MotionEvent.ACTION_MOVE:

                /*
                 * Creating a new drawing.
                 */
                if (drawingInProgress) {

                    currentX = x;

                    currentY = y;

                    if (
                            selectedType
                                    == DrawingType.HORIZONTAL
                    ) {

                        currentY =
                                startY;
                    }

                    invalidate();

                    return true;
                }

                /*
                 * Moving an existing drawing.
                 */
                if (
                        movingDrawing
                                && selectedDrawingIndex
                                >= 0
                                && selectedDrawingIndex
                                < drawings.size()
                ) {

                    moveSelectedDrawing(
                            x - lastMoveX,
                            y - lastMoveY
                    );

                    lastMoveX = x;

                    lastMoveY = y;

                    invalidate();

                    return true;
                }

                return true;

            case MotionEvent.ACTION_UP:

                /*
                 * Finish new drawing.
                 */
                if (drawingInProgress) {

                    currentX = x;

                    currentY = y;

                    if (
                            selectedType
                                    == DrawingType.HORIZONTAL
                    ) {

                        currentY =
                                startY;
                    }

                    /*
                     * Do not save an almost-zero
                     * accidental tap as a drawing.
                     */
                    float length =
                            distance(
                                    startX,
                                    startY,
                                    currentX,
                                    currentY
                            );

                    if (
                            selectedType
                                    == DrawingType.HORIZONTAL
                    ) {

                        if (
                                Math.abs(
                                        currentY
                                                - startY
                                ) < 4f
                        ) {

                            drawings.add(
                                    new Drawing(
                                            selectedType,
                                            startX,
                                            startY,
                                            currentX,
                                            currentY
                                    )
                            );
                        }

                    } else {

                        if (length >= 12f) {

                            drawings.add(
                                    new Drawing(
                                            selectedType,
                                            startX,
                                            startY,
                                            currentX,
                                            currentY
                                    )
                            );
                        }
                    }

                    if (!drawings.isEmpty()) {

                        selectedDrawingIndex =
                                drawings.size() - 1;
                    }

                    drawingInProgress = false;

                    drawingMode = false;

                    movingDrawing = false;

                    invalidate();

                    performClick();

                    return true;
                }

                movingDrawing = false;

                performClick();

                return true;

            case MotionEvent.ACTION_CANCEL:

                drawingInProgress = false;

                movingDrawing = false;

                invalidate();

                return true;

            default:

                return true;
        }
    }

    private void moveSelectedDrawing(
            float dx,
            float dy
    ) {

        if (
                selectedDrawingIndex < 0
                        || selectedDrawingIndex
                        >= drawings.size()
        ) {
            return;
        }

        Drawing drawing =
                drawings.get(
                        selectedDrawingIndex
                );

        if (
                drawing.type
                        == DrawingType.HORIZONTAL
        ) {

            drawing.y1 += dy;

            drawing.y2 =
                    drawing.y1;

            /*
             * Keep horizontal lines
             * inside the chart view.
             */
            if (drawing.y1 < 0f) {

                drawing.y1 = 0f;

                drawing.y2 = 0f;
            }

            if (
                    drawing.y1
                            > getHeight()
            ) {

                drawing.y1 =
                        getHeight();

                drawing.y2 =
                        getHeight();
            }

        } else {

            drawing.x1 += dx;

            drawing.y1 += dy;

            drawing.x2 += dx;

            drawing.y2 += dy;

            /*
             * Keep trend-line endpoints
             * reasonably inside the drawing area.
             */
            drawing.x1 =
                    Math.max(
                            0f,
                            Math.min(
                                    getWidth(),
                                    drawing.x1
                            )
                    );

            drawing.x2 =
                    Math.max(
                            0f,
                            Math.min(
                                    getWidth(),
                                    drawing.x2
                            )
                    );

            drawing.y1 =
                    Math.max(
                            0f,
                            Math.min(
                                    getHeight(),
                                    drawing.y1
                            )
                    );

            drawing.y2 =
                    Math.max(
                            0f,
                            Math.min(
                                    getHeight(),
                                    drawing.y2
                            )
                    );
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

            if (
                    drawing.type
                            == DrawingType.HORIZONTAL
            ) {

                if (selected) {

                    paint.setColor(
                            Color.WHITE
                    );

                    paint.setStrokeWidth(
                            6f
                    );

                } else {

                    paint.setColor(
                            Color.rgb(
                                    255,
                                    193,
                                    7
                            )
                    );

                    paint.setStrokeWidth(
                            3f
                    );
                }

                paint.setStyle(
                        Paint.Style.STROKE
                );

                canvas.drawLine(
                        0f,
                        drawing.y1,
                        getWidth(),
                        drawing.y1,
                        paint
                );

                /*
                 * Selection handles.
                 */
                if (selected) {

                    drawHorizontalHandles(
                            canvas,
                            drawing
                    );
                }

            } else {

                if (selected) {

                    paint.setColor(
                            Color.WHITE
                    );

                    paint.setStrokeWidth(
                            6f
                    );

                } else {

                    paint.setColor(
                            Color.rgb(
                                    80,
                                    170,
                                    255
                            )
                    );

                    paint.setStrokeWidth(
                            3f
                    );
                }

                paint.setStyle(
                        Paint.Style.STROKE
                );

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

    private void drawHorizontalHandles(
            Canvas canvas,
            Drawing drawing
    ) {

        endpointPaint.setStyle(
                Paint.Style.FILL
        );

        endpointPaint.setColor(
                Color.WHITE
        );

        canvas.drawCircle(
                14f,
                drawing.y1,
                ENDPOINT_RADIUS,
                endpointPaint
        );

        canvas.drawCircle(
                getWidth() - 14f,
                drawing.y1,
                ENDPOINT_RADIUS,
                endpointPaint
        );
    }

    private void drawTrendEndpoints(
            Canvas canvas,
            Drawing drawing,
            boolean selected
    ) {

        endpointPaint.setStyle(
                Paint.Style.FILL
        );

        if (selected) {

            endpointPaint.setColor(
                    Color.WHITE
            );

        } else {

            endpointPaint.setColor(
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
                selected
                        ? ENDPOINT_RADIUS
                        : 5f,
                endpointPaint
        );

        canvas.drawCircle(
                drawing.x2,
                drawing.y2,
                selected
                        ? ENDPOINT_RADIUS
                        : 5f,
                endpointPaint
        );

        endpointPaint.setStyle(
                Paint.Style.STROKE
        );
    }

    private void drawPreview(
            Canvas canvas
    ) {

        previewPaint.setColor(
                Color.WHITE
        );

        previewPaint.setStrokeWidth(
                2.5f
        );

        previewPaint.setStyle(
                Paint.Style.STROKE
        );

        if (
                selectedType
                        == DrawingType.HORIZONTAL
        ) {

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

            /*
             * Preview endpoint.
             */
            previewPaint.setStyle(
                    Paint.Style.FILL
            );

            canvas.drawCircle(
                    startX,
                    startY,
                    5f,
                    previewPaint
            );

            canvas.drawCircle(
                    currentX,
                    currentY,
                    5f,
                    previewPaint
            );
        }
    }

    private int findDrawingAt(
            float x,
            float y
    ) {

        /*
         * Search newest to oldest so the
         * newest drawing gets priority.
         */
        for (
                int i = drawings.size() - 1;
                i >= 0;
                i--
        ) {

            Drawing drawing =
                    drawings.get(i);

            if (
                    drawing.type
                            == DrawingType.HORIZONTAL
            ) {

                if (
                        Math.abs(
                                y
                                        - drawing.y1
                        )
                                <= HIT_DISTANCE
                ) {

                    return i;
                }

            } else {

                if (
                        distanceToLine(
                                x,
                                y,
                                drawing.x1,
                                drawing.y1,
                                drawing.x2,
                                drawing.y2
                        )
                                <= HIT_DISTANCE
                ) {

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

        float dx =
                x2 - x1;

        float dy =
                y2 - y1;

        if (
                dx == 0f
                        && dy == 0f
        ) {

            return distance(
                    px,
                    py,
                    x1,
                    y1
            );
        }

        float denominator =
                dx * dx
                        + dy * dy;

        if (denominator <= 0f) {

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
                        / denominator;

        t =
                Math.max(
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

        float dx =
                x1 - x2;

        float dy =
                y1 - y2;

        return (float)
                Math.sqrt(
                        dx * dx
                                + dy * dy
                );
    }

    @Override
    public boolean performClick() {

        super.performClick();

        return true;
    }
}