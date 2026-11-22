package dev.overloadsim.core;

/** Per-open-preview interaction state. No Minecraft, JEI, world or shared mutable state. */
public final class StructureViewState {
    private double yaw=135,pitch=25,scale=1;
    private int layerIndex=-1;
    public double yaw(){return yaw;}
    public double pitch(){return pitch;}
    public double scale(){return scale;}
    public int layerIndex(){return layerIndex;}
    public void rotate(double dx,double dy){
        if(!Double.isFinite(dx)||!Double.isFinite(dy))return;
        yaw=(yaw+dx*.7)%360;
        pitch=Math.clamp(pitch+dy*.7,-85,85);
    }
    public void zoom(double steps){
        if(Double.isFinite(steps))scale=Math.clamp(scale*Math.pow(1.12,Math.clamp(steps,-100,100)),.5,2.5);
    }
    public void stepLayer(int delta,int height){
        if(height<1)throw new IllegalArgumentException("height");
        layerIndex=Math.floorMod(layerIndex+1+delta,height+1)-1;
    }
    public boolean visible(int y,int minY){return layerIndex<0||y-minY==layerIndex;}
    public void allLayers(){layerIndex=-1;}
    public void reset(){yaw=135;pitch=25;scale=1;layerIndex=-1;}
}
