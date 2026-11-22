package dev.overloadsim.core;

/** JEI probes a press before executing its release. Only the matching release emits an action. */
public final class StructurePreviewInput {
    public enum Action { NONE, ROTATE, PREVIOUS_LAYER, ALL_LAYERS, NEXT_LAYER, RESET, TOGGLE_FLAT }
    public record Result(boolean consumed,Action action){}
    private Action pressed=Action.NONE;
    private boolean dragging;
    public static boolean viewport(double x,double y){return inside(x,y,4,24,176,66);}
    private static boolean inside(double x,double y,int left,int top,int width,int height){return x>=left&&y>=top&&x<left+width&&y<top+height;}
    private static Action hit(double x,double y,int button,boolean ring){
        if(button==1&&viewport(x,y))return Action.RESET;
        if(button!=0)return Action.NONE;
        if(inside(x,y,4,94,26,18))return Action.PREVIOUS_LAYER;
        if(inside(x,y,32,94,86,18))return Action.ALL_LAYERS;
        if(inside(x,y,120,94,26,18))return Action.NEXT_LAYER;
        if(inside(x,y,148,94,32,18))return Action.RESET;
        if(ring&&inside(x,y,4,115,82,18))return Action.TOGGLE_FLAT;
        return viewport(x,y)?Action.ROTATE:Action.NONE;
    }
    public Result click(double x,double y,int button,boolean simulate,boolean ring){
        var hit=hit(x,y,button,ring);
        if(simulate){pressed=hit;dragging=hit==Action.ROTATE;return new Result(hit!=Action.NONE,Action.NONE);}
        var previous=pressed;pressed=Action.NONE;dragging=false;
        return new Result(previous!=Action.NONE,previous==hit&&hit!=Action.ROTATE?hit:Action.NONE);
    }
    public boolean drag(double x,double y,int button){
        if(!viewport(x,y))dragging=false;
        return dragging&&button==0;
    }
    public void move(double x,double y){if(!viewport(x,y))dragging=false;}
}
