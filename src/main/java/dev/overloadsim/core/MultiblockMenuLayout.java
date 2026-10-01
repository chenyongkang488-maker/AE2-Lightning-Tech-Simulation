package dev.overloadsim.core;
/** Pixel coordinates only; server slot IDs never change when the structure does. */
public record MultiblockMenuLayout(int side) {
    public record Point(int x,int y){}
    public static MultiblockMenuLayout of(int capacity,boolean recovery){
        int n=(int)Math.sqrt(Math.max(0,capacity));
        return new MultiblockMenuLayout(recovery||n<3||n>7||n*n!=capacity?7:n);
    }
    public int width(){return outputLeft()+154;}
    public int height(){return 238;}
    public int upperHeight(){return 158;}
    public int inventoryPanelWidth(){return 194;}
    public int inventoryPanelLeft(){return (width()-inventoryPanelWidth())/2;}
    public int progressLeft(){return 18+side*18;}
    public int outputLeft(){return 34+side*18;}
    public Point input(int index){return new Point(10+index%side*18,32+index/side*18);}
    public Point output(int index){return new Point(outputLeft()+index%8*18,32+index/8*18);}
    public Point inventory(int index){return new Point(inventoryPanelLeft()+16+index%9*18,162+index/9*18);}
    public Point hotbar(int index){return new Point(inventoryPanelLeft()+16+index*18,220);}
}
