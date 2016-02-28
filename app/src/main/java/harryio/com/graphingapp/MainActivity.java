package harryio.com.graphingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    int totalPoints, maxNumberOfPoints = 30;
    Handler handler;
    List<Line> lines = new ArrayList<>();
    List<TextView> yAxisTitles = new ArrayList<>();
    Axis xAxis;
    LinearLayout legend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChart = (LineChartView) findViewById(R.id.lineChart);
        legend = (LinearLayout) findViewById(R.id.legend);

        mChart.setInteractive(true);
        mChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        mChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);

        LineChartData lineChartData = new LineChartData(lines);
        xAxis = new Axis().setName("Axis X");
        lineChartData.setAxisXBottom(xAxis);
        lineChartData.setAxisYLeft(new Axis().setName("Axis Y"));
        mChart.setLineChartData(lineChartData);

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void refresh() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                mChart.setLineChartData(mChart.getLineChartData());
            }
        });
    }

    @Override
    public void setNPointOnScreen(int maxPoints) {
        maxNumberOfPoints = maxPoints;
    }

    @Override
    public int addStream(final String title) {
        Line line = new Line();
        line.setHasLines(true);
        line.setHasPoints(true);
        Random random = new Random();
        final int argb = Color.argb(random.nextInt(256), random.nextInt(256),
                random.nextInt(256), random.nextInt(256));
        line.setColor(argb);
        lines.add(line);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = getLayoutInflater().inflate(R.layout.legend_item, legend, false);
                legend.addView(view);
                TextView textView = (TextView) view.findViewById(R.id.title);
                textView.setText(title);
                yAxisTitles.add(textView);
                (view.findViewById(R.id.line)).setBackgroundColor(argb);
            }
        });
        return lines.size() - 1;
    }

    @Override
    public void addPoint(int series_n, float x, float y) {
        final LineChartData lineChartData = mChart.getLineChartData();
        try {
            final Line line = lineChartData.getLines().get(series_n);
            final List<PointValue> values = line.getValues();
            values.add(new PointValue(x, y));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    line.setValues(new ArrayList<>(values));
                    mChart.setLineChartData(lineChartData);
                    totalPoints++;
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
        handler.post(new Runnable() {
            @Override
            public void run() {
                LineChartData lineChartData = mChart.getLineChartData();
                try {
                    Line line = lineChartData.getLines().get(series_n);
                    line.setValues(new ArrayList<PointValue>(0));
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
        if (totalPoints > maxNumberOfPoints) {
            final Viewport viewport = new Viewport(mChart.getMaximumViewport());
            viewport.left = totalPoints - maxNumberOfPoints;
            mChart.setCurrentViewport(viewport);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh();
                break;

            case R.id.action_add_stream:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int index = addStream("Line " + lines.size());
                        for (int i = 0; i < 10; i++) {
                            addPoint(index, i, (float) (10 * Math.random()));
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                break;

            case R.id.action_clear_points:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int size = lines.size();
                        for (int i = 0; i < size; ++i) {
                            clearPoints(i);
                        }
                    }
                }).start();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}