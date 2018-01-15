package OSVOMD_COMP;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import OSVOMD_COMP.DTW.Distancable;
import static OSVOMD_COMP.Constants.*;

/**klasa reprezentuj�ca jeden punkt podpisu (czas, x, y, press)*/
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
        return sqrt( pow((this.x - other.x), 2)*X_W + pow((this.y - other.y), 2)*Y_W + pow((this.press - other.press), 2)*P_W + (pow((this.time - other.time), 2))*T_W);
    }
}

