package dev.overloadsim.laser;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Creative demonstration writes only an empty, already-loaded region. */
public final class LaserDemo {
    private record Machine(BlockPos pos,LaserKind kind,Direction facing){}
    private LaserDemo(){}
    public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("overload_sim").then(Commands.literal("laser").requires(s->s.hasPermission(2))
            .then(Commands.literal("demo").executes(c->demo(c.getSource())))
            .then(Commands.literal("build").then(Commands.argument("kind",StringArgumentType.word()).suggests((c,b)->{for(var k:LaserKind.values())b.suggest(k.name().toLowerCase(Locale.ROOT));return b.buildFuture();}).executes(c->single(c.getSource(),StringArgumentType.getString(c,"kind")))))));
    }
    private static ServerPlayer player(CommandSourceStack source){
        var player=source.getPlayer();if(player==null||!player.isCreative()){source.sendFailure(Component.literal("此演示指令需要创造模式玩家。"));return null;}return player;
    }
    private static int single(CommandSourceStack source,String name){
        var player=player(source);if(player==null)return 0;
        LaserKind kind;
        try{kind=LaserKind.valueOf(name.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException bad){source.sendFailure(Component.literal("未知设备；按 Tab 查看设备名。"));return 0;}
        var forward=player.getDirection();var pos=player.blockPosition().above(4).relative(forward,8);
        return place(source,List.of(new Machine(pos,kind,forward.getOpposite())),false);
    }
    private static int demo(CommandSourceStack source){
        var player=player(source);if(player==null)return 0;
        var forward=player.getDirection();var facing=forward.getOpposite();
        var origin=player.blockPosition().above(4).relative(forward,8);
        var machines=new ArrayList<Machine>();machines.add(new Machine(origin,LaserKind.EMITTER,facing));
        int along=6,remaining=4;
        machines.add(new Machine(origin.relative(forward,along).below(3),LaserKind.ENHANCEMENT,facing));
        for(int i=0;i<16;i++){
            if(remaining<=2){along+=2;machines.add(new Machine(origin.relative(forward,along).below(3),LaserKind.CONSTRAINT,facing));remaining=6;}
            along+=2;remaining-=2;machines.add(new Machine(origin.relative(forward,along).below(3),LaserKind.BURST,facing));
        }
        along+=2;machines.add(new Machine(origin.relative(forward,along).below(3),LaserKind.FOCUS,facing));
        var core=origin.relative(forward,along+7);machines.add(new Machine(core,LaserKind.CORE,facing));
        machines.add(new Machine(core.relative(forward,6).below(3),LaserKind.RECEIVER,facing));
        return place(source,machines,true);
    }
    private static int place(CommandSourceStack source,List<Machine> machines,boolean demo){
        var level=source.getLevel();var plan=new LinkedHashMap<BlockPos,BlockState>();
        for(var m:machines)for(var entry:LaserStructureGeometry.template(m.kind(),m.facing()).entrySet()){
            var block=switch(entry.getValue()){
                case CONTROLLER->LaserContent.machine(m.kind());case FRAME->LaserContent.FRAME.get();case GLASS->LaserContent.GLASS.get();case PORT->LaserContent.PORT.get();case SOURCE->LaserContent.SOURCE.get();case AIR->Blocks.AIR;
            };
            var state=block.defaultBlockState();if(entry.getValue()==LaserStructureGeometry.Part.CONTROLLER)state=state.setValue(LaserMachineBlock.FACING,m.facing());
            var pos=m.pos().offset(entry.getKey());var previous=plan.putIfAbsent(pos,state);
            if(previous!=null&&!previous.equals(state)){source.sendFailure(Component.literal("演示模板内部重叠，已取消生成。"));return 0;}
        }
        for(var pos:plan.keySet())if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos).isAir()){
            source.sendFailure(Component.literal("需要一片已加载的空地（完整演示长约 80 格）；当前位置受阻："+pos.toShortString()+"。请在超平坦世界高处面向空地重试。"));return 0;
        }
        for(var entry:plan.entrySet())if(!entry.getValue().isAir())level.setBlockAndUpdate(entry.getKey(),entry.getValue());
        for(var m:machines)if(level.getBlockEntity(m.pos()) instanceof LaserMachineBlockEntity host){
            host.checkStructure();
            if(host.kind()!=LaserKind.RECEIVER)host.energy().load(host.energy().capacity());
            if(host.kind()==LaserKind.ENHANCEMENT)host.setBoost(9_000_000);
            if(host.kind()==LaserKind.BURST)host.setExtremeLightning(6_400);
            if(host.kind()==LaserKind.RECEIVER)host.fuelInput().setStackInSlot(0,new ItemStack(Items.NETHERITE_BLOCK,64));
            host.sync();host.saveChanges();
        }
        source.sendSuccess(()->Component.literal((demo?"完整激光粒子演示链":"多方块设备")+"已生成于 "+machines.getFirst().pos().toShortString()+"。已预充有限 FE / 极高压；右键控制器查看，耗尽后接入过载网络补充。"),false);
        return 1;
    }
}
