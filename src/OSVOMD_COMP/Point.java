package OSVOMD_COMP;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import OSVOMD_COMP.DTW.Distancable;
import static OSVOMD_COMP.Constants.DTW_TIME_WEIGHT;

/**klasa reprezentuj¹ca jeden punkt podpisu (czas, x, y, press)*/
class Point implements Distancable<Point>
{
    double x;
    double y;
    double press;
    long time;

    Point(long time, double x, double y, double press)
    {
        this.x = x;
        this.y = y;
        this.press = press;
        this.time = time;
    }

    @Override
    public String toString()
    {
        return time + "\t" + x + "\t" + y + "\t" + press;
    }

    @Override
    public double distance(Point other)
    {
        return sqrt( pow((this.x - other.x), 2) + pow((this.y - other.y), 2) + pow((this.press - other.press), 2) + (pow((this.time - other.time), 2))*DTW_TIME_WEIGHT);
    }
}

