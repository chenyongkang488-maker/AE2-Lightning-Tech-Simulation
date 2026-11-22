package dev.overloadsim.integration.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import dev.overloadsim.construction.StructurePreviewCatalog;
import dev.overloadsim.core.StructureViewState;
import dev.overloadsim.core.StructurePreviewInput;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** One widget per displayed recipe. Renders ordinary block models, never ticks or forms machines. */
final class StructurePreviewWidget implements IRecipeWidget,IJeiInputHandler {
    private static final int X=4,Y=24,W=176,H=66;
    private final StructurePreviewCatalog.Layout recipe;
    private final StructureViewState view=new StructureViewState();
    private final StructurePreviewInput input=new StructurePreviewInput();
    private Map<BlockPos,BlockState> cells;
    private List<Map.Entry<BlockPos,BlockState>> occupied;
    private StructurePreviewCatalog.Bounds bounds;
    private boolean flat;
    private BlockPos hovered;

    StructurePreviewWidget(StructurePreviewCatalog.Layout recipe){this.recipe=recipe;updateCells();}
    private void updateCells(){
        cells=recipe.cells(flat);
        occupied=cells.entrySet().stream().filter(e->!e.getValue().isAir()).sorted(Comparator.comparingInt(e->e.getKey().getY())).toList();
        bounds=new StructurePreviewCatalog.Layout(recipe.id(),recipe.controller(),recipe.kind(),cells).bounds();
        hovered=null;view.allLayers();
    }
    @Override public ScreenPosition getPosition(){return new ScreenPosition(0,0);}
    @Override public ScreenRectangle getArea(){return new ScreenRectangle(0,0,StructurePreviewCategory.WIDTH,StructurePreviewCategory.HEIGHT);}
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    private static boolean viewport(double mx,double my){return inside(mx,my,X,Y,W,H);}
    private double pixels(){return .86*Math.min(W,H)/Math.sqrt(bounds.width()*bounds.width()+bounds.height()*bounds.height()+bounds.depth()*bounds.depth())*view.scale();}

    @Override public void drawWidget(GuiGraphics graphics,double mx,double my){
        var mc=Minecraft.getInstance();
        graphics.fill(X,Y,X+W,Y+H,0xFFF0EDF5);
        graphics.fill(X,Y,X+W,Y+1,0xFFBBB4C8);
        graphics.fill(X,Y+H-1,X+W,Y+H,0xFFBBB4C8);
        hovered=viewport(mx,my)?pick(mx,my):null;
        graphics.flush();
        // GuiGraphics scissors are absolute GUI coordinates; JEI translates the recipe pose.
        var topLeft=graphics.pose().last().pose().transformPosition(new Vector3f(X,Y,0));
        var bottomRight=graphics.pose().last().pose().transformPosition(new Vector3f(X+W,Y+H,0));
        graphics.enableScissor((int)Math.floor(Math.min(topLeft.x,bottomRight.x)),(int)Math.floor(Math.min(topLeft.y,bottomRight.y)),
                (int)Math.ceil(Math.max(topLeft.x,bottomRight.x)),(int)Math.ceil(Math.max(topLeft.y,bottomRight.y)));
        var pose=graphics.pose();pose.pushPose();
        try{
            pose.translate(X+W/2d,Y+H/2d,150);
            float scale=(float)pixels();pose.scale(scale,-scale,scale);
            pose.mulPose(Axis.XP.rotationDegrees((float)view.pitch()));
            pose.mulPose(Axis.YP.rotationDegrees((float)view.yaw()));
            pose.translate(-bounds.centerX(),-bounds.centerY(),-bounds.centerZ());
            RenderSystem.enableDepthTest();Lighting.setupFor3DItems();
            var buffers=graphics.bufferSource();
            for(var cell:occupied){
                if(!view.visible(cell.getKey().getY(),bounds.minY()))continue;
                pose.pushPose();
                try{
                    var pos=cell.getKey();pose.translate(pos.getX(),pos.getY(),pos.getZ());
                    mc.getBlockRenderer().renderSingleBlock(cell.getValue(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,ModelData.EMPTY,null);
                }finally{pose.popPose();}
            }
            buffers.endBatch();
            if(hovered!=null){
                var lines=buffers.getBuffer(RenderType.lines());
                LevelRenderer.renderLineBox(pose,lines,new AABB(hovered).inflate(.006),.3f,.8f,1f,1f);
                buffers.endBatch(RenderType.lines());
            }
        }finally{
            pose.popPose();graphics.disableScissor();Lighting.setupFor3DItems();RenderSystem.enableDepthTest();
        }
        var font=mc.font;
        graphics.drawString(font,Component.translatable("jei.overload_sim.structure.dimensions",bounds.width(),bounds.height(),bounds.depth()),4,14,0x69667D,false);
        button(graphics,4,94,26,Component.literal("◀"),mx,my,true);
        var layer=view.layerIndex()<0?Component.translatable("jei.overload_sim.structure.all_layers"):
                Component.translatable("jei.overload_sim.structure.layer",view.layerIndex()+1,bounds.height());
        button(graphics,32,94,86,layer,mx,my,true);
        button(graphics,120,94,26,Component.literal("▶"),mx,my,true);
        button(graphics,148,94,32,Component.translatable("jei.overload_sim.structure.reset"),mx,my,true);
        if(recipe.ring())button(graphics,4,115,82,Component.translatable("jei.overload_sim.structure."+(flat?"flat":"upright")),mx,my,true);
        graphics.drawString(font,Component.translatable("jei.overload_sim.structure.drag"),recipe.ring()?92:4,120,0x69667D,false);
        graphics.drawString(font,Component.translatable("jei.overload_sim.structure.scroll"),4,134,0x69667D,false);
    }
    private static void button(GuiGraphics g,int x,int y,int width,Component label,double mx,double my,boolean active){
        g.fill(x,y,x+width,y+18,inside(mx,my,x,y,width,18)?0xFFCEBCD9:0xFFDDD5E6);
        var font=Minecraft.getInstance().font;
        g.drawString(font,font.plainSubstrByWidth(label.getString(),width-4),x+3,y+5,0x413F54,false);
    }
    private BlockPos pick(double mx,double my){
        var inverse=new Matrix4f().rotateX((float)Math.toRadians(view.pitch())).rotateY((float)Math.toRadians(view.yaw())).invert();
        var origin=inverse.transformPosition(new Vector3f((float)((mx-X-W/2d)/pixels()),(float)(-(my-Y-H/2d)/pixels()),100));
        var direction=inverse.transformDirection(new Vector3f(0,0,-1));
        var start=new Vec3(origin.x+bounds.centerX(),origin.y+bounds.centerY(),origin.z+bounds.centerZ());
        var end=start.add(direction.x*200,direction.y*200,direction.z*200);
        BlockPos found=null;double distance=Double.POSITIVE_INFINITY;
        for(var cell:occupied){
            if(!view.visible(cell.getKey().getY(),bounds.minY()))continue;
            var hit=new AABB(cell.getKey()).clip(start,end);
            if(hit.isPresent()&&start.distanceToSqr(hit.get())<distance){distance=start.distanceToSqr(hit.get());found=cell.getKey();}
        }
        return found;
    }
    @Override public void getTooltip(ITooltipBuilder tooltip,double mx,double my){
        if(inside(mx,my,32,94,86,18)){
            tooltip.add(Component.translatable("jei.overload_sim.structure.layer_hint"));return;
        }
        if(hovered==null||!viewport(mx,my))return;
        var block=cells.get(hovered);
        tooltip.add(block.getBlock().getName());
        tooltip.add(Component.translatable("jei.overload_sim.structure.position",hovered.getX(),hovered.getY(),hovered.getZ()));
        var alternatives=recipe.alternatives(hovered,flat);
        if(alternatives.size()>1){
            tooltip.add(Component.translatable("jei.overload_sim.structure.alternatives"));
            alternatives.stream().skip(1).forEach(state->tooltip.add(state.getBlock().getName()));
            tooltip.add(Component.translatable("jei.overload_sim.structure.optional_hint"));
        }
    }
    @Override public boolean handleInput(double mx,double my,IJeiUserInput userInput){
        if(userInput.getKey().getType()!=InputConstants.Type.MOUSE)return false;
        var result=input.click(mx,my,userInput.getKey().getValue(),userInput.isSimulate(),recipe.ring());
        switch(result.action()){
            case PREVIOUS_LAYER->view.stepLayer(-1,bounds.height());
            case ALL_LAYERS->view.allLayers();
            case NEXT_LAYER->view.stepLayer(1,bounds.height());
            case RESET->view.reset();
            case TOGGLE_FLAT->{flat=!flat;updateCells();}
            default->{}
        }
        return result.consumed();
    }
    @Override public boolean handleMouseDragged(double mx,double my,InputConstants.Key key,double dx,double dy){
        if(key.getType()!=InputConstants.Type.MOUSE||!input.drag(mx,my,key.getValue()))return false;
        view.rotate(dx,dy);return true;
    }
    @Override public void handleMouseMoved(double mx,double my){input.move(mx,my);}
    @Override public boolean handleMouseScrolled(double mx,double my,double horizontal,double vertical){
        if(!viewport(mx,my))return false;
        view.zoom(vertical);return true;
    }
}
