package dev.overloadsim.tool;

import dev.overloadsim.SimulationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import org.joml.Vector3f;

public final class CoilMining {
    private CoilMining(){}
    /** Internal server-break transaction shared with the transformed game-mode mixin. */
    public static final class BreakAttempt {
        public final net.minecraft.world.item.ItemStack tool;
        public boolean removed;
        public BreakAttempt(net.minecraft.world.item.ItemStack tool){this.tool=tool;}
    }
    public static boolean ready(net.minecraft.world.item.ItemStack stack){return CoilModules.miningReady(stack)&&CoilEnergy.read(stack)>=SimulationConfig.COIL_MINING_FE.get();}
    /** Vanilla server destruction handles drops, break events, block entities and permissions. */
    public static boolean breakUnbreakable(ServerPlayer player,BlockPos pos){
        var stack=player.getMainHandItem();var level=player.serverLevel();
        if(!ready(stack)||!CoilModules.has(stack,CoilModuleItem.Type.ULTIMATE)||!level.isLoaded(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(player,pos)||!player.canInteractWithBlock(pos,1)||player.isSpectator()||level.getBlockState(pos).getDestroySpeed(level,pos)>=0)return false;
        return player.gameMode.destroyBlock(pos);
    }
    public static boolean modifyTerrain(UseOnContext context){
        var stack=context.getItemInHand();var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||!ready(stack)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return false;
        var state=level.getBlockState(pos);BlockState changed=null;
        for(var action:new net.neoforged.neoforge.common.ItemAbility[]{ItemAbilities.AXE_STRIP,ItemAbilities.AXE_SCRAPE,ItemAbilities.AXE_WAX_OFF,ItemAbilities.HOE_TILL,ItemAbilities.SHOVEL_FLATTEN}){
            changed=state.getToolModifiedState(context,action,false);if(changed!=null)break;
        }
        boolean carving=changed==null&&state.is(net.minecraft.world.level.block.Blocks.PUMPKIN);
        if(carving){var facing=context.getClickedFace();if(facing.getAxis()==net.minecraft.core.Direction.Axis.Y)facing=player.getDirection().getOpposite();changed=net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING,facing);}
        if(changed==null)return false;
        if(player instanceof ServerPlayer sp){
            if(!level.setBlockAndUpdate(pos,changed))return false;
        }
        return true;
    }
    /** Run effects only after the standard placement wrapper has accepted the change. */
    public static void afterTerrain(net.minecraft.world.item.ItemStack stack,ServerPlayer player,BlockPos pos,BlockState before){
        var level=player.serverLevel();var after=level.getBlockState(pos);if(after.equals(before))return;
        CoilEnergy.INSTANCE.tryConsume(stack,player,SimulationConfig.COIL_MINING_FE.get());
        if(before.is(net.minecraft.world.level.block.Blocks.PUMPKIN)&&after.is(net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN))
            net.minecraft.world.level.block.Block.popResource(level,pos,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PUMPKIN_SEEDS,4));
        spark(player,pos.getCenter());
    }
    public static void spark(ServerPlayer player,Vec3 end){
        var start=player.getEyePosition().add(player.getLookAngle().scale(.5)).add(0,-.25,0);
        var dust=new DustParticleOptions(new Vector3f(1f,.48f,.78f),.65f);
        for(int i=0;i<=12;i++){var point=start.lerp(end,i/12d).add((i%3-1)*.035,(i%4-2)*.025,0);player.serverLevel().sendParticles(dust,point.x,point.y,point.z,1,0,0,0,0);}
    }
}
