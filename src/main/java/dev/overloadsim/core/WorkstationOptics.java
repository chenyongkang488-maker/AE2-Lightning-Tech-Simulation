package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Four wall-mounted crystal tips focus on the central material holder, in local chamber coordinates. */
public final class WorkstationOptics {
    public record Point(double x,double y,double z) {}
    public record Ray(Point start,Point end) {}
    private static final List<Ray> RAYS=build();
    private WorkstationOptics() {}
    public static List<Ray> rays() {return RAYS;}
    private static List<Ray> build() {
        var result=new ArrayList<Ray>();
        var focus=new Point(0,-1.92,0);
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1})
            result.add(new Ray(new Point(sx*1.125,sy*1.125,1.84375),focus));
        return List.copyOf(result);
    }
}
