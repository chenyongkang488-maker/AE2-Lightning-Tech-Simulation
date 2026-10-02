package dev.overloadsim.command;
import java.util.*;
import dev.overloadsim.data.MobSimulationData;
import dev.overloadsim.api.*;
import dev.overloadsim.data.*;
import dev.overloadsim.binding.SimulationEntityEligibility;
import com.google.gson.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Pure inspection contract; never rolls loot or invokes output/template providers. */
public final class SimulationDiagnostics {
    public record Report(String category,String target,List<String> details,String error){
        public Report { details=List.copyOf(details); }
    }
    public static Report explain(ServerLevel level,ItemStack stack){
        var data=CrystalDataAccess.read(stack);if(data.isEmpty())return new Report("crystal",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),List.of(),"no_crystal_data");
        var crystal=data.get();var details=new ArrayList<String>();details.add("profile="+crystal.profile());details.add("format="+crystal.format());
        if(crystal.entityType().isPresent()){
            var entity=crystal.entityType().get();var mob=describeMob(level,entity,MobSimulationData.resolve(entity));details.addAll(mob.details());
            if(!mob.error().isEmpty())return new Report("crystal",entity.toString(),details,mob.error());
        }
        var production=SimulationResolvers.production(level,crystal);
        if(SimulationData.profile(crystal.profile()).isEmpty())return new Report("crystal",crystal.profile().toString(),details,"missing_profile");
        if(production.isEmpty()){
            var mineral=MineralSimulationData.resolveProfile(level,crystal.profile());
            var recipes=SimulationData.recipes(level,SimulationRecipe.Kind.PRODUCTION).stream().filter(r->r.value().data().profile().equals(crystal.profile())).toList();
            return new Report("crystal",crystal.profile().toString(),details,!recipes.isEmpty()?"ambiguous_production_recipe":mineral.error().equals("missing_mineral_profile")?"missing_production":mineral.error());
        }
        var p=production.get();details.add("production="+p.id());
        if(p.mineral().isPresent())details.addAll(mineralDetails(p.mineral().get()));
        else if(p.config().provider().isPresent()){
            var provider=p.config().provider().get();details.add("production_provider="+provider);if(!hasProvider(provider))return new Report("crystal",crystal.profile().toString(),details,"missing_provider");
        }else if(!p.config().entityLoot())for(var output:p.config().outputs())details.add("output="+output.item()+" x"+output.count()+" chance="+output.chance());
        details.add("random=not_evaluated");return new Report("crystal",crystal.profile().toString(),details,"");
    }
    public static Report explainBlock(ServerLevel level,BlockState state){
        var resolution=MineralSimulationData.resolveBinding(level,state);return new Report("binding",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),resolution.mineral().map(SimulationDiagnostics::mineralDetails).orElse(List.of()),resolution.error());
    }
    private static List<String> mineralDetails(ResolvedMineral m){
        var result=new ArrayList<String>();result.add("profile="+m.profile());result.add("material="+m.material());result.add("binding="+m.binding());result.add("same_block="+m.sameBlock());
        m.item().ifPresent(id->result.add("item="+id));result.add("quantity="+m.min()+".."+m.max());m.ore().ifPresent(id->result.add("block_loot="+id));m.smelting().ifPresent(s->result.add("smelting="+s.input()+" -> "+s.result()+" x"+s.count()));return List.copyOf(result);
    }
    private static boolean hasProvider(ResourceLocation id){return SimulationExtensions.output(id)!=null||SimulationExtensions.outputV2(id)!=null;}
    public static Report describeMob(ServerLevel level,ResourceLocation entity,MobSimulationData.Resolution resolution){
        var details=new ArrayList<String>();var type=BuiltInRegistries.ENTITY_TYPE.getOptional(entity);
        if(type.isEmpty())return new Report("mob",entity.toString(),details,"missing_entity");
        details.add("spawn_egg="+SimulationEntityEligibility.hasEgg(type.get()));details.add("rule="+resolution.id().map(Object::toString).orElse("default"));
        if(!resolution.enabled())return new Report("mob",entity.toString(),details,resolution.error());
        var rule=resolution.rule().orElseThrow();details.add("priority="+rule.priority());details.add("context="+rule.context());details.add("looting=0");
        if(rule.provider().isPresent()){
            var provider=rule.provider().get();details.add("provider="+provider);return new Report("mob",entity.toString(),details,hasProvider(provider)?"":"missing_provider");
        }
        if(!rule.outputs().isEmpty()){
            for(var output:rule.outputs()){
                details.add("independent_output="+output.item()+" x"+output.min()+".."+output.max()+" chance="+output.chance());
                if(BuiltInRegistries.ITEM.getOptional(output.item()).isEmpty())return new Report("mob",entity.toString(),details,"missing_output");
            }
            details.add("random=not_evaluated");return new Report("mob",entity.toString(),details,"");
        }
        if(rule.template().isPresent()){
            var template=rule.template().get();details.add("template="+template);if(SimulationExtensions.entityTemplate(template)==null)return new Report("mob",entity.toString(),details,"missing_template");
        }
        var table=rule.lootTable().orElse(type.get().getDefaultLootTable().location());details.add("loot_table="+table);details.add("loot_status="+tableStatus(level,table));
        details.add("random=not_evaluated; empty draws can be normal; GLMs may add drops");return new Report("mob",entity.toString(),details,"");
    }
    private static String tableStatus(ServerLevel level,ResourceLocation table){
        var resource=level.getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(table.getNamespace(),"loot_table/"+table.getPath()+".json"));
        if(resource.isEmpty())return table.equals(ResourceLocation.parse("minecraft:empty"))?"empty_base_table":"dynamic_or_missing_table";
        try(var reader=resource.get().openAsReader()){
            var json=JsonParser.parseReader(reader).getAsJsonObject();var pools=json.getAsJsonArray("pools");return pools==null||pools.isEmpty()?"empty_base_table":"configured_base_table";
        }catch(Exception error){return "unreadable_base_table";}
    }
    public static List<Report> audit(ServerLevel level){
        var reports=new ArrayList<Report>();MineralSimulationData.audit(level).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->reports.add(new Report("mineral_profile",e.getKey().toString(),e.getValue().mineral().map(SimulationDiagnostics::mineralDetails).orElse(List.of()),e.getValue().error())));
        for(var block:BuiltInRegistries.BLOCK){var report=explainBlock(level,block.defaultBlockState());if(!report.error().equals("no_mineral_tags"))reports.add(report);}
        for(var type:BuiltInRegistries.ENTITY_TYPE){var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);reports.add(describeMob(level,id,MobSimulationData.resolve(id)));}
        reports.sort(Comparator.comparing(Report::category).thenComparing(Report::target));return List.copyOf(reports);
    }
    public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("overload_sim")
            .then(Commands.literal("explain").executes(ctx->{var source=ctx.getSource();send(source,explain(source.getLevel(),source.getPlayerOrException().getMainHandItem()));return 1;}))
            .then(Commands.literal("explain_block").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(ctx->{var source=ctx.getSource();var pos=BlockPosArgument.getLoadedBlockPos(ctx,"pos");send(source,explainBlock(source.getLevel(),source.getLevel().getBlockState(pos)));return 1;})))
            .then(Commands.literal("audit").requires(source->source.hasPermission(2)).executes(ctx->{
                var source=ctx.getSource();var reports=audit(source.getLevel());var path=source.getServer().getWorldPath(LevelResource.ROOT).resolve("overload_sim-audit.json");
                try{java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(reports));}
                catch(java.io.IOException error){source.sendFailure(Component.translatable("commands.overload_sim.audit_failed",error.getMessage()));return 0;}
                source.sendSuccess(()->Component.translatable("commands.overload_sim.audit",reports.size(),path.toAbsolutePath().toString()),false);return reports.size();
            })));
    }
    private static void send(net.minecraft.commands.CommandSourceStack source,Report report){
        source.sendSuccess(()->Component.literal(report.category()+": "+report.target()),false);
        if(!report.error().isEmpty())source.sendFailure(Component.literal("reason="+report.error()));
        for(var line:report.details())source.sendSuccess(()->Component.literal(line),false);
    }
    private SimulationDiagnostics(){}
}
