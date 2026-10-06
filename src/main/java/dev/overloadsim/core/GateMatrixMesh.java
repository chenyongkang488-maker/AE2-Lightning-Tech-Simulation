package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Distinct outer matrix collars, authored on the Minecraft 1/16-block grid. */
public final class GateMatrixMesh {
    private static final List<IndustrialMachineMesh.Surface> CONSTRAINT=build(false),FOCUS=build(true);
    private GateMatrixMesh(){}
    public static List<IndustrialMachineMesh.Surface> outer(boolean focus){return focus?FOCUS:CONSTRAINT;}
    private static List<IndustrialMachineMesh.Surface> build(boolean focus){
        var p=new ArrayList<IndustrialMachineMesh.Surface>();
        if(focus){
            annulus(p,44,false,3.75,3.25,-.5,-.3125);
            annulus(p,44,false,3.75,3.25,.3125,.5);
            annulus(p,47,true,3.5625,3.375,-.5625,-.5);
            annulus(p,47,true,3.5625,3.375,.5,.5625);
        }else{
            annulus(p,44,false,3.875,3.25,-.3125,.3125);
            annulus(p,7,false,3.75,3.3125,-.375,-.3125);
            annulus(p,7,false,3.75,3.3125,.3125,.375);
        }
        // Glyph cartridges and clamps are separated blocks rather than a smooth torus.
        for(int i=0;i<16;i++){
            double a=i*Math.PI/8,x=snap(Math.cos(a)*3.5),y=snap(Math.sin(a)*3.5);
            for(int sign:new int[]{-1,1}){
                double z=sign*(focus?.625:.4375);
                box(p,focus?47:46,true,x-.1875,y-.1875,z-.0625,x+.1875,y+.1875,z+.0625);
            }
            if(i%2==0){
                double cx=snap(Math.cos(a)*3.75),cy=snap(Math.sin(a)*3.75);
                box(p,44,false,cx-.25,cy-.25,-.6875,cx+.25,cy+.25,.6875);
                for(int sign:new int[]{-1,1})box(p,focus?49:48,true,cx-.125,cy-.125,sign*.75-.0625,cx+.125,cy+.125,sign*.75+.0625);
            }
        }
        return List.copyOf(p);
    }
    private static double snap(double value){return Math.rint(value*16)/16;}
    private static void annulus(List<IndustrialMachineMesh.Surface> p,int tile,boolean glow,double outer,double inner,double z0,double z1){
        int radius=(int)Math.ceil(outer*4);
        for(int y=-radius;y<radius;y++){
            int start=Integer.MIN_VALUE;
            for(int x=-radius;x<=radius;x++){
                double r2=Math.pow((x+.5)/4,2)+Math.pow((y+.5)/4,2);
                boolean filled=x<radius&&r2<=outer*outer&&r2>=inner*inner;
                if(filled&&start==Integer.MIN_VALUE)start=x;
                if(!filled&&start!=Integer.MIN_VALUE){box(p,tile,glow,start/4d,y/4d,z0,x/4d,(y+1)/4d,z1);start=Integer.MIN_VALUE;}
            }
        }
    }
    private static void box(List<IndustrialMachineMesh.Surface> p,int tile,boolean glow,double x0,double y0,double z0,double x1,double y1,double z1){
        var faces=new ArrayList<IndustrialMachineMesh.Surface>();
        IndustrialMachineMesh.box(faces,tile,x0,y0,z0,x1,y1,z1);
        for(var face:faces)p.add(new IndustrialMachineMesh.Surface(face.quad(),tile,glow));
    }
}
