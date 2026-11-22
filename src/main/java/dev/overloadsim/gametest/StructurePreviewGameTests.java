package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.construction.StructurePreviewCatalog;
import dev.overloadsim.laser.*;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.workstation.WorkstationContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class StructurePreviewGameTests {
    @GameTest(template="empty")
    public static void catalogHasAllEightNonChamberControllersAndExactRequiredMaterials(GameTestHelper h){
        var layouts=StructurePreviewCatalog.layouts();
        h.assertTrue(layouts.size()==8&&layouts.stream().map(StructurePreviewCatalog.Layout::controller).distinct().count()==8,"eight independent controller entries");
        h.assertTrue(layouts.stream().noneMatch(l->l.controller()==MultiblockContent.CONTROLLER.get()||l.controller()==LaserContent.machine(LaserKind.CORE)),"simulation chamber and single-block core excluded");
        for(var layout:layouts){
            var counts=new java.util.HashMap<net.minecraft.world.level.block.Block,Integer>();
            layout.materials().forEach(m->counts.put(m.block(),m.count()));
            h.assertTrue(counts.get(layout.controller())==1,"controller included exactly once");
            if(layout.kind()==null){
                h.assertTrue(layout.controller()==WorkstationContent.CONTROLLER.get()&&counts.size()==4&&counts.get(LaserContent.FRAME.get())==141&&counts.get(LaserContent.GLASS.get())==75&&counts.get(LaserContent.PORT.get())==1,"workstation BOM is141 frame75 glass1 port1 controller");
            }else switch(layout.kind()){
                case EMITTER->h.assertTrue(counts.size()==4&&counts.get(LaserContent.FRAME.get())==36&&counts.get(LaserContent.SOURCE.get())==1&&counts.get(LaserContent.PORT.get())==1,"emitter BOM matches physical reactor");
                case DEFLECTOR->h.assertTrue(counts.size()==3&&counts.get(LaserContent.FRAME.get())==91&&counts.get(LaserContent.PORT.get())==6,"shape2 has91 frames and6 independent ports");
                case RECEIVER->h.assertTrue(counts.size()==4&&counts.get(LaserContent.FRAME.get())==138&&counts.get(LaserContent.GLASS.get())==56&&counts.get(LaserContent.PORT.get())==1,"receiver includes complete two-layer pedestal");
                default->h.assertTrue(counts.size()==2&&counts.get(LaserContent.FRAME.get())==15,"ring BOM counts15 frames plus controller");
            }
            h.assertTrue(counts.values().stream().mapToInt(Integer::intValue).sum()==layout.cells().values().stream().filter(s->!s.isAir()).count(),"counts include exactly all non-air cells, independent of slice");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void flatRingTemplatesAndNewDeflectorAnchorMatchConstruction(GameTestHelper h){
        for(var layout:StructurePreviewCatalog.layouts()){
            if(layout.ring()){
                var flat=layout.cells(true);
                h.assertTrue(flat.entrySet().stream().filter(e->!e.getValue().isAir()).allMatch(e->e.getKey().getY()==0),"flat ring occupies one horizontal layer");
                h.assertTrue(flat.get(BlockPos.ZERO).is(layout.controller())&&flat.values().stream().filter(s->s.is(LaserContent.FRAME.get())).count()==15,"flat ring retains controller and BOM");
                h.assertTrue(flat.get(BlockPos.ZERO).getValue(LaserMachineBlock.FACING)==net.minecraft.core.Direction.DOWN,"flat placement uses actual six-direction controller state");
            }
            if(layout.kind()==LaserKind.DEFLECTOR){
                var center=new BlockPos(-2,2,-2);
                for(var side:net.minecraft.core.Direction.values())h.assertTrue(layout.cells().get(center.relative(side,3)).is(LaserContent.PORT.get()),"JEI uses six-port shape2 instead of historical default overload");
            }
            var bounds=layout.bounds();
            if(layout.kind()==LaserKind.RECEIVER)h.assertTrue(bounds.width()==7&&bounds.height()==9&&bounds.depth()==7,"receiver bounds include pedestal");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void optionalReplacementsDoNotInflateTheRequiredBillOfMaterials(GameTestHelper h){
        var receiver=StructurePreviewCatalog.layouts().stream().filter(l->l.kind()==LaserKind.RECEIVER).findFirst().orElseThrow();
        var center=new BlockPos(0,4,3);
        var side=center.east(3);
        h.assertTrue(receiver.alternatives(side,false).stream().anyMatch(s->s.is(LaserContent.PORT.get())),"legal extra receiver aperture exposed as optional replacement");
        h.assertTrue(receiver.materials().stream().filter(m->m.block()==LaserContent.PORT.get()).findFirst().orElseThrow().count()==1,"optional ports not charged as mandatory");
        h.assertTrue(receiver.alternatives(BlockPos.ZERO,false).size()==1,"controller is never an optional port or interface");
        var station=StructurePreviewCatalog.layouts().stream().filter(l->l.kind()==null).findFirst().orElseThrow();
        h.assertTrue(station.alternatives(new BlockPos(0,6,0),false).size()<=1,"workstation advertises no unsupported network substitutions");
        h.succeed();
    }
    @GameTest(template="laser_empty",timeoutTicks=150)
    public static void everyPreviewFormsAndMatchesActualConstructionPlans(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(12,4,12));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"preview-test"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setPos(origin.getCenter());
        for(var layout:StructurePreviewCatalog.layouts()){
            h.getLevel().setBlockAndUpdate(origin,layout.cells().get(BlockPos.ZERO));
            if(layout.kind()!=null){
                var host=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(origin);
                var plan=LaserAutoConstruction.plan(host);
                h.assertTrue(plan.cells().size()==layout.cells().size()&&layout.cells().entrySet().stream().allMatch(e->e.getValue().equals(plan.cells().get(origin.offset(e.getKey())))),"preview cells exactly match actual automatic-construction plan");
                var result=LaserAutoConstruction.build(player,host);
                h.assertTrue(result.successful()&&host.formed(),"JEI template actually forms "+layout.kind());
                host.setEnabled(false);
            }else{
                var host=(dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity)h.getLevel().getBlockEntity(origin);
                var result=host.build(player);host.checkStructure();
                h.assertTrue(result.successful()&&host.formed(),"JEI workstation template actually forms");
            }
            for(var cell:layout.cells().keySet())h.getLevel().setBlockAndUpdate(origin.offset(cell),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }h.succeed();
    }
}
