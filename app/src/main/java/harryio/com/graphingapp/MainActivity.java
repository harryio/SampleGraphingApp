package harryio.com.graphingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class MainActivity extends AppCompatActivity implements GraphingActivityInterface {
    private static final String TAG = MainActivity.class.getSimpleName();

    LineChartView mChart;
    int maxNumberOfPoints = 1000;
    int maxNumberOfPointsOnScreen = 32;
    List<TextView> yAxisTitles = new ArrayList<>();
    Axis xAxis, yAxisLeft, yAxisRight;
    boolean manualAxisScaling = false;
    boolean lockedRight = true;
    //Set some initial values for the left and right axis ranges
    int leftYMin = 0, leftYMax = 99, rightYMin = 100, rightYMax = 149;
    //Find scale of left axis w.r.t right axis
    float scale = (leftYMax -leftYMin) / (rightYMax - rightYMin);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChart = (LineChartView) findViewById(R.id.lineChart);
        mChart.setViewportCalculationEnabled(false);
        mChart.setMaxZoom((float) 1000.0);

        (findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        (findViewById(R.id.add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start adding values from the background thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int index = addStream("Line");
                        //Add second line to the graph which will operate in different range
                        int index2 = addStream("Line 2");

                        int i = 0;
                        while (i < 4) {
                            //Add random values
                            addPoint(index, i, (float) (100 * Math.random()));
                            //Add random point between the range of right axis, to the line
                            addPoint(index2, i, (float) (new Random().nextInt(rightYMax - rightYMin)
                                    + rightYMin));
                            i++;
                            try {
                                //Set update value to 1 second
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

        (findViewById(R.id.clear)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start clearing values from the background thread
                int size = mChart.getLineChartData().getLines().size();
                for (int i = 0; i < size; ++i) {
                    clearPoints(i);
                }
            }
        });

        (findViewById(R.id.lock_right)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockedRight = !lockedRight;
            }
        });

        final Button scalingButton = (Button) findViewById(R.id.scaling);
        scalingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manualAxisScaling = !manualAxisScaling;
                if (manualAxisScaling)
                    scalingButton.setText("Auto Scaling");
                else
                    scalingButton.setText("Manual Scaling");
            }
        });
        mChart.setInteractive(true);
        mChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        mChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);

        LineChartData lineChartData = new LineChartData(new ArrayList<Line>());
        xAxis = new Axis().setName("Axis X").setHasLines(true);
        lineChartData.setAxisXBottom(xAxis);
        yAxisLeft = new Axis().setName("Left Y").setHasLines(true);
        yAxisRight = new Axis().setName("Right Y").setHasLines(true).setFormatter(new ValueFormatter());
        lineChartData.setAxisYLeft(yAxisLeft);
        lineChartData.setAxisYRight(yAxisRight);
        mChart.setLineChartData(lineChartData);

    }

    @Override
    public void refresh() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Force chart to draw current data again
                mChart.setLineChartData(mChart.getLineChartData());
            }
        });
    }

    @Override
    public void setNPointOnScreen(int maxPoints) {
        maxNumberOfPointsOnScreen = maxPoints;
    }

    @Override
    public int addStream(final String title) {
        //Create new line with some default values
        Line line = new Line();
        line.setHasLines(true);
        line.setHasPoints(false);
        Random random = new Random();
        final int argb = Color.rgb(random.nextInt(256),
                random.nextInt(256), random.nextInt(256));
        line.setColor(argb);
        List<Line> lines = new ArrayList<>(mChart.getLineChartData().getLines());
        lines.add(line);
        mChart.getLineChartData().setLines(lines);
        return lines.size() - 1;
    }

    @Override
    public void addPoint(final int series_n, float x, float y) {
        final float pointX, pointY;

        final LineChartData lineChartData = mChart.getLineChartData();
        if (y >= rightYMin && y <= rightYMax) {
            //This point belongs to the line plotted against right axis
            pointX = x;
            //Scale y value of the point in the range of left axis
            pointY = (y - rightYMin) * scale + leftYMin;

            Log.i(TAG, "Initial Value of point: " + y);
            Log.i(TAG, "Scaled Value of point: " + pointY);
        } else {
            //This point belongs to the line plotted against left axis
            pointX = x;
            pointY = y;
        }

        try {
            //Set new data on the graph
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<Line> lines = new ArrayList<>(lineChartData.getLines());
                    final Line line = lines.get(series_n);
                    //Get list of previous points on the line
                    final List<PointValue> values = line.getValues();
                    values.add(new PointValue(pointX, pointY));
                    if(values.size() > maxNumberOfPoints) {
                        values.remove(0);
                    }
                    mChart.setLineChartData(lineChartData);
                    setViewport();
                }
            });
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            Log.e(TAG, "No series found at index " + series_n);
        }
    }

    @Override
    public void clearPoints(final int series_n) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LineChartData lineChartData = mChart.getLineChartData();
                try {
                    //Get line for which points are to be cleared
                    Line line = lineChartData.getLines().get(series_n);
                    //Set empty list of points replacing old values list
                    line.setValues(new ArrayList<PointValue>(0));
                    //Set new data on the graph
                    mChart.setLineChartData(lineChartData);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    Log.e(TAG, "No series found at index " + series_n);
                }
            }
        });
    }

    @Override
    public void setNAxes(int n_axes) {
    }

    @Override
    public void setXAxisTitle(String title) {
        xAxis.setName(title);
    }

    @Override
    public void setYAxisTitle(int series_n, String title) {
        try {
            yAxisTitles.get(series_n).setText(title);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            Log.e(TAG, "No series found at index " + series_n);
        }
    }

    private void setViewport() {
        Viewport maximumViewport = new Viewport();
        Viewport currentViewport = new Viewport(mChart.getCurrentViewport());

        LineChartData lineChartData = mChart.getLineChartData();

        List<Line> lines = lineChartData.getLines();
        final Line line = lines.get(0);
        final List<PointValue> values = line.getValues();
        final List<PointValue> pointsInView;

        // Compute maximum viewport

        float min = values.get(0).getY();
        float max = values.get(0).getY();
        for (PointValue p : values) {
            float ty = p.getY();
            if (ty < min) {
                min = ty;
            }
            if (ty > max) {
                max = ty;
            }
        }
        maximumViewport.left = values.get(0).getX();
        maximumViewport.right = values.get(values.size()-1).getX();
        maximumViewport.top = max;
        maximumViewport.bottom = min;

        mChart.setMaximumViewport(maximumViewport);

        // Grab only the most recent N points and base the current viewport off of them

        if (values.size() >= maxNumberOfPointsOnScreen) {
            pointsInView = values.subList(values.size() - maxNumberOfPointsOnScreen, values.size());
        } else {
            pointsInView = values;
        }

        if (!manualAxisScaling && lockedRight) {
            // Figure out the max and min of the points in view
            min = pointsInView.get(0).getY();
            max = pointsInView.get(0).getY();

            for (PointValue p : pointsInView) {
                float ty = p.getY();
                if (ty < min) {
                    min = ty;
                }
                if (ty > max) {
                    max = ty;
                }
            }
            currentViewport.top = max;
            currentViewport.bottom = min;
        }

        if (lockedRight) {
            currentViewport.left = pointsInView.get(0).getX();
            currentViewport.right = pointsInView.get(pointsInView.size() - 1).getX();
        }

        mChart.setCurrentViewport(currentViewport);
    }

    @Override
    public void setLeftYAxisRange(int minY, int maxY) {
        leftYMin = minY;
        leftYMax = maxY;

        //Recalculate scale
        calculateScale();
    }

    @Override
    public void setRightYAxisRange(int miny, int maxY) {
        rightYMin = miny;
        rightYMax = maxY;

        //Recalculate scale
        calculateScale();
    }

    private void calculateScale() {
        scale = (leftYMax - leftYMin) / (rightYMax = rightYMin);
    }

    private class ValueFormatter extends SimpleAxisValueFormatter {
        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            //Scale back to the original value so that it can be shown on
            float scaledValue = (value - leftYMin) / scale + rightYMin;
            return super.formatValueForAutoGeneratedAxis(formattedValue, scaledValue, 0);
        }
    }
}