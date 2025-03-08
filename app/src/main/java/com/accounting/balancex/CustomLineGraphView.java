package com.accounting.balancex;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
//not working right now
public class CustomLineGraphView extends GraphView {

    private LineGraphSeries<DataPoint> series;

    public CustomLineGraphView(Context context) {
        super(context);
        setupGraph();
    }

    public CustomLineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupGraph();
    }

    private void setupGraph() {
        series = new LineGraphSeries<>();

        // Set properties for a smooth line
        series.setDrawDataPoints(true); // Show points
        series.setDataPointsRadius(6f);
        series.setThickness(5); // Line thickness
        series.setColor(Color.BLUE); // Line color

        addSeries(series);

        // Enable scrolling and zooming
        getViewport().setScalable(true);
        getViewport().setScalableY(true);

        // Set axis limits (will auto-adjust as data comes in)
        getViewport().setXAxisBoundsManual(true);
        getViewport().setMinX(0);
        getViewport().setMaxX(10);

        getViewport().setYAxisBoundsManual(true);
        getViewport().setMinY(0);
        getViewport().setMaxY(5000);

        getGridLabelRenderer().setHorizontalLabelsVisible(true);
        getGridLabelRenderer().setVerticalLabelsVisible(true);
    }

    // Function to add new data points dynamically
    public void addDataPoint(double x, double y) {
        series.appendData(new DataPoint(x, y), true, 50);
    }
}
