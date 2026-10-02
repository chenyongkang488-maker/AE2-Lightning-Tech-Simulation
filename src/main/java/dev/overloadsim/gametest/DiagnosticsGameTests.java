package dev.overloadsim.gametest;
import java.util.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.command.SimulationDiagnostics;
import dev.overloadsim.data.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class DiagnosticsGameTests {
    @GameTest(template="empty")
    public static void explainIsPureAndIncludesPlayerLootContext(GameTestHelper h){
        var crystal=CrystalDataAccess.perfect(new CrystalData(ModContent.id("mob"),Optional.of(ResourceLocation.parse("minecraft:phantom")),0,1));var before=crystal.copy();
        h.getLevel().random.setSeed(851);var report=SimulationDiagnostics.explain(h.getLevel(),crystal);
        h.assertTrue(report.error().isEmpty()&&report.details().contains("context=player")&&report.details().contains("loot_table=minecraft:entities/phantom"),"explain resolves player context and actual table");
        h.assertTrue(net.minecraft.world.item.ItemStack.matches(before,crystal)&&h.getLevel().random.nextLong()==RandomSource.create(851).nextLong(),"inspection changes neither crystal nor loot RNG");h.succeed();
    }
    @GameTest(template="empty")
    public static void diagnosticsDistinguishMissingAndAmbiguousMappings(GameTestHelper h){
        var missing=SimulationDiagnostics.explain(h.getLevel(),CrystalDataAccess.perfect(new CrystalData(ModContent.id("removed_profile"),Optional.empty(),0,1)));
        var ambiguous=SimulationDiagnostics.explainBlock(h.getLevel(),BuiltInRegistries.BLOCK.get(ModContent.id("test_ambiguous_block")).defaultBlockState());
        var valid=SimulationDiagnostics.explainBlock(h.getLevel(),BuiltInRegistries.BLOCK.get(ModContent.id("test_raw_block")).defaultBlockState());
        h.assertTrue(missing.error().equals("missing_profile")&&ambiguous.error().startsWith("ambiguous_")&&valid.error().isEmpty(),"missing/ambiguous/valid are separate diagnostics: "+missing.error()+", "+ambiguous.error()+", "+valid.error());
        h.assertTrue(valid.details().contains("material=c:testium")&&SimulationDiagnostics.audit(h.getLevel()).stream().anyMatch(r->r.target().equals("minecraft:phantom")),"audit includes current bindings and mob types");h.succeed();
    }
    @GameTest(template="empty")
    public static void diagnosticsDoNotInvokeProvidersOrTemplates(GameTestHelper h){
        var provider=ModContent.id("diagnostic_no_roll");int[] calls={0};SimulationExtensions.registerOutputV2(provider,c->{calls[0]++;throw new AssertionError("diagnostic rolled output");});
        var rule=new MobSimulationData.Rule("minecraft:pig",100,false,List.of(),Optional.empty(),Optional.of(provider),"player",Optional.of(ModContent.id("missing_template")));
        var report=SimulationDiagnostics.describeMob(h.getLevel(),ResourceLocation.parse("minecraft:pig"),new MobSimulationData.Resolution(Optional.of(rule),Optional.of(ModContent.id("test_rule")),""));
        h.assertTrue(calls[0]==0&&report.error().isEmpty()&&report.details().contains("provider="+provider),"provider mode is inspected without rolling or requiring unused template");
        var missing=SimulationDiagnostics.describeMob(h.getLevel(),ResourceLocation.parse("removed:mob"),MobSimulationData.resolve(ResourceLocation.parse("removed:mob")));
        h.assertTrue(missing.error().equals("missing_entity"),"removed mob diagnosed safely");h.succeed();
    }
    @GameTest(template="empty")
    public static void diagnosticCommandsAreRegistered(GameTestHelper h){
        var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("overload_sim");
        h.assertTrue(root!=null&&root.getChild("explain")!=null&&root.getChild("audit")!=null&&root.getChild("explain_block")!=null,"author diagnostic commands registered");h.succeed();
    }
}
