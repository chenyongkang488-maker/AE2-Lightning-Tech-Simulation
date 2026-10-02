package dev.overloadsim.binding;
import java.util.*;
import java.util.function.Supplier;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.*;
import dev.overloadsim.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

public final class CrystalBinding {
    private CrystalBinding(){}
    public static ItemStack bindStructure(ServerLevel level,BlockPos center,ItemStack stack,boolean natural){return bindStructure(level,center,stack,natural,()->stack);}
    public static ItemStack bindStructure(ServerLevel level,BlockPos center,ItemStack stack,boolean natural,Supplier<ItemStack> current){
        if(!stack.is(ModContent.BLANK.get()))return stack;
        var original=stack.copy();
        // Select the explicit physical match before its policy. Otherwise an automatic tag
        // mapping could bypass natural-only restrictions, missing profiles or author conditions.
        var matches=SimulationData.recipes(level,SimulationRecipe.Kind.BINDING).stream().filter(h->structure(level,center,h.value().data().world(),false).isPresent()).toList();
        var chosen=SimulationData.select(matches);if(chosen.isEmpty()){
            if(!matches.isEmpty())return stack;
            var crop=CropBinding.bind(level,center,stack,current);return crop!=stack?crop:bindMineral(level,center,stack,natural,current);
        }
        var recipe=chosen.get().value();if(!natural&&!recipe.data().allowArtificial()||SimulationData.profile(level,recipe.data().profile()).isEmpty())return stack;
        var matched=structure(level,center,recipe.data().world());if(matched.isEmpty())return stack;var snapshots=matched.get();
        var data=new CrystalData(recipe.data().profile(),Optional.empty(),0,1);
        if(NeoForge.EVENT_BUS.post(new SimulationEvents.BeforeBinding(level,center,data)).isCanceled())return stack;
        if(current.get()!=stack||!ItemStack.matches(original,current.get()))return stack;
        if(structure(level,center,recipe.data().world()).filter(snapshots::equals).isEmpty())return stack;
        if(!snapshots.entrySet().stream().allMatch(e->level.hasChunkAt(e.getKey()) && level.getBlockState(e.getKey()).equals(e.getValue()) && level.getBlockEntity(e.getKey())==null))return stack;
        for(var pos:snapshots.keySet())level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        return CrystalDataAccess.bound(data);
    }
    private static ItemStack bindMineral(ServerLevel level,BlockPos center,ItemStack stack,boolean natural,Supplier<ItemStack> current){
        var original=stack.copy();var snapshots=new LinkedHashMap<BlockPos,BlockState>();dev.overloadsim.api.ResolvedMineral mineral=null;BlockState first=null;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(x==0&&z==0)continue;var pos=center.offset(x,0,z);if(!level.hasChunkAt(pos)||level.getBlockEntity(pos)!=null)return stack;var state=level.getBlockState(pos);
            var resolved=MineralSimulationData.resolveBinding(level,state);if(!resolved.valid())return stack;var value=resolved.mineral().orElseThrow();
            if(mineral==null){mineral=value;first=state;}else if(!mineral.equals(value)||mineral.sameBlock()&&first.getBlock()!=state.getBlock())return stack;
            snapshots.put(pos,state);
        }
        if(mineral==null)return stack;var data=new CrystalData(mineral.profile(),Optional.empty(),0,1);
        if(NeoForge.EVENT_BUS.post(new SimulationEvents.BeforeBinding(level,center,data)).isCanceled()||current.get()!=stack||!ItemStack.matches(original,current.get()))return stack;
        for(var entry:snapshots.entrySet())if(!level.hasChunkAt(entry.getKey())||level.getBlockEntity(entry.getKey())!=null||!level.getBlockState(entry.getKey()).equals(entry.getValue())||!MineralSimulationData.resolveBinding(level,entry.getValue()).mineral().filter(mineral::equals).isPresent())return stack;
        for(var pos:snapshots.keySet())level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());return CrystalDataAccess.bound(data);
    }
    private static Optional<Map<BlockPos,BlockState>> structure(ServerLevel level,BlockPos center,SimulationRecipe.WorldRule rule){
        return structure(level,center,rule,true);
    }
    private static Optional<Map<BlockPos,BlockState>> structure(ServerLevel level,BlockPos center,SimulationRecipe.WorldRule rule,boolean checkCondition){
        if(rule.material().isEmpty()||!Set.of("mineral","crop","tree").contains(rule.mode()))return Optional.empty();
        if(checkCondition&&rule.condition().isPresent()&&!SimulationExtensions.binding(rule.condition().get(),level,center))return Optional.empty();
        var states=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(x==0&&z==0)continue;var ground=center.offset(x,0,z);var material=rule.mode().equals("mineral")?ground:ground.above();
            if(!level.hasChunkAt(ground)||!level.hasChunkAt(material))return Optional.empty();
            if(!rule.mode().equals("mineral")&&(rule.soil().isEmpty()||!rule.soil().get().matches(level.getBlockState(ground))))return Optional.empty();
            var state=level.getBlockState(material);if(rule.mode().equals("mineral")&&state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ModContent.id("simulation_mineral_blacklist"))))return Optional.empty();if(!rule.material().get().matches(state)||level.getBlockEntity(material)!=null)return Optional.empty();states.put(material,state);
        }
        return Optional.of(states);
    }
    public static ItemStack cultivate(ServerLevel level,BlockPos pos,ItemStack stack,boolean natural){
        MineralSimulationData.ensure(level);
        if(!stack.is(ModContent.BOUND.get()))return stack;var data=CrystalDataAccess.read(stack);if(data.isEmpty()||SimulationData.profile(data.get().profile()).isEmpty())return stack;
        var matches=SimulationData.recipes(level,SimulationRecipe.Kind.CULTIVATION).stream().filter(h->h.value().data().profile().equals(data.get().profile())||h.value().data().profile().equals(ModContent.id("any"))).toList();
        var chosen=SimulationData.select(matches);if(chosen.isEmpty())return stack;
        if(!natural&&!chosen.get().value().data().allowArtificial())return stack;
        var original=stack.copy();
        if(NeoForge.EVENT_BUS.post(new SimulationEvents.BeforeCultivation(level,pos,data.get())).isCanceled()||!ItemStack.matches(original,stack))return stack;
        var next=data.get().advance(chosen.get().value().data().cultivation().increment());
        var result=next.strikes()>=chosen.get().value().data().cultivation().required()?CrystalDataAccess.perfect(next):CrystalDataAccess.bound(next);
        return result;
    }
}
