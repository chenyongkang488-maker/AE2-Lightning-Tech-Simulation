package dev.overloadsim.output;

/** Fixed colors follow source slots, including empty slots, rather than resource identity. */
public final class OutputLinkColors {
    private static final int[] COLORS=new int[36];
    static {
        for(int slot=0;slot<COLORS.length;slot++) {
            double hue=(.89+slot*.6180339887498949)%1;
            double saturation=.62, value=.98;
            double h=hue*6, chroma=value*saturation;
            double x=chroma*(1-Math.abs(h%2-1)), m=value-chroma;
            double[] rgb=switch((int)h) {
                case 0->new double[]{chroma,x,0};case 1->new double[]{x,chroma,0};
                case 2->new double[]{0,chroma,x};case 3->new double[]{0,x,chroma};
                case 4->new double[]{x,0,chroma};default->new double[]{chroma,0,x};
            };
            COLORS[slot]=((int)Math.round((rgb[0]+m)*255)<<16)
                    |((int)Math.round((rgb[1]+m)*255)<<8)|(int)Math.round((rgb[2]+m)*255);
        }
    }
    private OutputLinkColors() {}
    public static int color(int slot,int alpha) {
        if(slot<0||slot>=COLORS.length)throw new IllegalArgumentException("Invalid output source slot");
        return (alpha&255)<<24|COLORS[slot];
    }
}
