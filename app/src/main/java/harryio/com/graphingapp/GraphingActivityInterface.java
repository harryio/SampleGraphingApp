package harryio.com.graphingapp;

public interface GraphingActivityInterface {
    void refresh();

    void setNPointOnScreen(int maxPoints);

    int addStream();

    void addPoint(int series_n, float x, float y);

    void clearPoints(int series_n);

    void setNAxes(int n_axes);

    void setXAxisTitle(String title);

    void setYAxisTitle(int series_n, String title);
}
