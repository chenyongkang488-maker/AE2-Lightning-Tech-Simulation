package dev.overloadsim.core;
public final class SimulationShellTopology {
    public enum Role{NONE,ROOF,ROOF_EDGE,ROOF_CORNER,HORIZONTAL,VERTICAL,CORNER,FLOOR,GLASS}
    public static Role role(int n,int x,int y,int z){
        if(n<3||n>7||x<0||y<0||z<0||x>=n||y>=n||z>=n)return Role.NONE;
        boolean xb=x==0||x==n-1,zb=z==0||z==n-1;
        if(y==n-1)return xb&&zb?Role.ROOF_CORNER:xb||zb?Role.ROOF_EDGE:Role.ROOF;
        if(y==0)return xb&&zb?Role.CORNER:xb||zb?Role.HORIZONTAL:Role.FLOOR;
        return xb&&zb?Role.VERTICAL:xb||zb?Role.GLASS:Role.NONE;
    }
    public static String outsideFace(int n,int x,int y,int z){
        if(role(n,x,y,z)!=Role.HORIZONTAL)return "";
        if(z==0)return "NORTH";if(z==n-1)return "SOUTH";return x==0?"WEST":"EAST";
    }
}
