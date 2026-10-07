package dev.overloadsim.core;

/** Exact local coordinates: x right, y up, z inward; controller at (3,0,0). */
public final class WorkstationGeometry {
    public enum Part { CONTROLLER, FRAME, GLASS, RECEIVER, AIR }
    private WorkstationGeometry() {}
    public static Part part(int x,int y,int z) {
        if(x<0||x>6||y<0||y>6||z<0||z>6)throw new IllegalArgumentException("outside workstation");
        if(x==3&&y==0&&z==0)return Part.CONTROLLER;
        if(x==3&&y==3&&z==6)return Part.RECEIVER;
        if(y==0||y==6||z==6)return Part.FRAME;
        int boundaries=(x==0||x==6?1:0)+(z==0||z==6?1:0);
        return boundaries>=2?Part.FRAME:boundaries==1?Part.GLASS:Part.AIR;
    }
}
