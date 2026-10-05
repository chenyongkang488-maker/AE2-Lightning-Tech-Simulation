package dev.overloadsim.mixin;
import com.moakiee.ae2lt.client.hub.DeviceHubScreen;
import com.moakiee.ae2lt.menu.hub.DeviceHubMenu;
import dev.overloadsim.compat.CoilHubAccess;
import dev.overloadsim.tool.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Reuse the complete upstream hub; customize only the active coil weapon panel. */
@Mixin(value=DeviceHubScreen.class,remap=false)
public abstract class CoilHubScreenMixin extends AbstractContainerScreen<DeviceHubMenu> {
    protected CoilHubScreenMixin(DeviceHubMenu m,Inventory inv,Component title){super(m,inv,title);}
    @Shadow private int configScrollOffset;
    @Shadow private void drawCheckbox(GuiGraphics g,int x,int y,boolean checked){throw new AssertionError();}
    @Shadow private void drawConfigValueButton(GuiGraphics g,int x,int y,String value,boolean editable,int mx,int my){throw new AssertionError();}
    @Shadow private void renderConfigScrollBar(GuiGraphics g,int count,int mx,int my){throw new AssertionError();}
    @Unique private boolean overloadSim$coil(){var p=Minecraft.getInstance().player;return p!=null&&menu.getSelectedTab()==4&&CoilModules.isCoil(CoilHubAccess.weapon(p));}
    @Unique private int overloadSim$value(int i){var values=menu.getModuleConfigValues();if(i>=values.size())return 0;try{return Integer.parseInt(values.get(i));}catch(NumberFormatException ignored){return 0;}}
    @Unique private boolean overloadSim$editable(int i){var values=menu.getModuleConfigEditable();return i<values.size()&&values.get(i);}
    @Inject(method="railgunStack",at=@At("HEAD"),cancellable=true)
    private static void weapon(Player p,CallbackInfoReturnable<ItemStack> cir){var s=CoilHubAccess.weapon(p);if(CoilModules.isCoil(s))cir.setReturnValue(s);}
    // AE2LT 2.1.0 inlines six settings; 2.1.1 computes the railgun count dynamically.
    // Handle only our seven-row panel so both versions retain their native gun scrolling.
    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    private void coilSettingsScroll(double mx,double my,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        if(!overloadSim$coil()||mx<leftPos+8||mx>leftPos+175||my<topPos+144||my>topPos+223)return;
        configScrollOffset=Math.clamp(configScrollOffset-(int)Math.signum(vertical),0,4);
        cir.setReturnValue(true);
    }
    @Inject(method="renderRailgunSettings",at=@At("HEAD"),cancellable=true)
    private void settings(GuiGraphics g,int mx,int my,CallbackInfo ci){
        if(!overloadSim$coil())return;ci.cancel();configScrollOffset=Math.clamp(configScrollOffset,0,4);
        g.drawString(font,Component.translatable("ae2lt.device_hub.settings"),leftPos+12,topPos+144,0xff6e748c,false);
        for(int row=0;row<3;row++){
            int i=row+configScrollOffset,y=topPos+160+row*16,v=overloadSim$value(i);
            String key=switch(i){case 0->"natural_mode";case 1->"efficiency";case 2->"efficiency_level";case 3->"fortune_level";case 4->"silk";case 5->"wrench";default->"wrench_mode";};
            g.drawString(font,Component.translatable("gui.overload_sim.coil."+key,v),leftPos+19,y+1,overloadSim$editable(i)?0xffffffff:0xffc0c2ce,false);
            if(i==2||i==3||i==6){
                String value=i==6?Component.translatable("gui.overload_sim.coil.wrench_mode."+v).getString():"− "+v+" +";
                drawConfigValueButton(g,leftPos+124,y-1,value,overloadSim$editable(i),mx,my);
            }else drawCheckbox(g,leftPos+142,y,v==1&&overloadSim$editable(i));
        }
        renderConfigScrollBar(g,7,mx,my);
        if(!CoilWrench.mekanism()&&mx>=leftPos+124&&mx<=leftPos+164&&my>=topPos+160+(6-configScrollOffset)*16&&my<=topPos+172+(6-configScrollOffset)*16&&configScrollOffset>=4)
            g.renderTooltip(font,Component.translatable("gui.overload_sim.coil.no_mekanism"),mx,my);
    }
    @Inject(method="mouseClickedRailgunSettings",at=@At("HEAD"),cancellable=true)
    private void click(double mx,double my,CallbackInfoReturnable<Boolean> cir){
        if(!overloadSim$coil())return;cir.setReturnValue(false);
        for(int row=0;row<3;row++){
            int i=row+configScrollOffset,y=topPos+160+row*16;boolean choice=i==2||i==3||i==6;int x=leftPos+(choice?124:142);
            if(mx<x||mx>x+(choice?40:22)||my<y-1||my>y+12)continue;
            if(overloadSim$editable(i)){
                int old=overloadSim$value(i),next=i==6?(old+1)%8:choice?Math.clamp(old+(mx<leftPos+144?-1:1),0,i==2?10:5):1-old;
                PacketDistributor.sendToServer(new CoilPackets.Configure(menu.containerId,i,next));
            }cir.setReturnValue(true);return;
        }
    }
    @Inject(method="renderTabTooltips",at=@At("HEAD"),cancellable=true)
    private void tooltip(GuiGraphics g,int mx,int my,int mask,CallbackInfo ci){
        var p=Minecraft.getInstance().player;if(p!=null&&CoilModules.isCoil(CoilHubAccess.weapon(p))&&mx>=leftPos+145&&mx<leftPos+176&&my>=topPos&&my<topPos+25){
            g.renderTooltip(font,CoilHubAccess.weapon(p).getHoverName(),mx,my);ci.cancel();
        }
    }
}
