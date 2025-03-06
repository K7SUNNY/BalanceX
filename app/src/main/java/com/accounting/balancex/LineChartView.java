package com.accounting.balancex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

public class LineChartView extends View {
    private Paint linePaint, pointPaint, textPaint;
    private List<Float> dataPoints;

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Line paint (Blue)
        linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(6);
        linePaint.setStyle(Paint.Style.STROKE);

        // Point paint (Red)
        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStrokeWidth(12);
        pointPaint.setStyle(Paint.Style.FILL);

        // Text paint (Black)
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(36);
    }

    // Method to set transaction data
    public void setData(List<Float> dataPoints) {
        this.dataPoints = dataPoints;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints == null || dataPoints.size() < 2) return;

        int width = getWidth();
        int height = getHeight();
        float spacing = width / (dataPoints.size() - 1);

        // Draw the line chart
        for (int i = 0; i < dataPoints.size() - 1; i++) {
            float startX = i * spacing;
            float startY = height - (dataPoints.get(i) * 10); // Scaling factor
            float stopX = (i + 1) * spacing;
            float stopY = height - (dataPoints.get(i + 1) * 10);

            canvas.drawLine(startX, startY, stopX, stopY, linePaint);
            canvas.drawCircle(startX, startY, 8, pointPaint);
            canvas.drawText(String.valueOf(dataPoints.get(i)), startX, startY - 20, textPaint);
        }
    }
}
