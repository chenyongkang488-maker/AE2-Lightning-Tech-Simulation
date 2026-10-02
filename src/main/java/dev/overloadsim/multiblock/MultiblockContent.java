package dev.overloadsim.multiblock;
import dev.overloadsim.*;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
public final class MultiblockContent {
    public static final DeferredBlock<SimulationControllerBlock> CONTROLLER=ModContent.BLOCKS.register("simulation_controller",SimulationControllerBlock::new);
    public static final DeferredItem<BlockItem> CONTROLLER_ITEM=ModContent.ITEMS.registerSimpleBlockItem(CONTROLLER);
    private static DeferredBlock<SimulationPartBlock> part(String id,SimulationPartBlock.Kind kind){var b=ModContent.BLOCKS.register(id,()->new SimulationPartBlock(kind));ModContent.ITEMS.registerSimpleBlockItem(b);return b;}
    public static final DeferredBlock<SimulationPartBlock> FRAME=part("simulation_frame",SimulationPartBlock.Kind.FRAME);
    public static final DeferredBlock<SimulationPartBlock> T1=part("simulation_efficiency_t1",SimulationPartBlock.Kind.T1);
    public static final DeferredBlock<SimulationPartBlock> T2=part("simulation_efficiency_t2",SimulationPartBlock.Kind.T2);
    public static final DeferredBlock<SimulationPartBlock> T3=part("simulation_efficiency_t3",SimulationPartBlock.Kind.T3);
    public static final DeferredBlock<SimulationPartBlock> FORTUNE=part("simulation_fortune_module",SimulationPartBlock.Kind.FORTUNE);
    public static final DeferredBlock<SimulationPartBlock> OVERLOAD=part("simulation_overload_module",SimulationPartBlock.Kind.OVERLOAD);
    public static final DeferredBlock<SimulationPartBlock> SMELTING=part("simulation_smelting_module",SimulationPartBlock.Kind.SMELTING);
    public static final DeferredBlock<SimulationGlassBlock> GLASS=ModContent.BLOCKS.register("formed_simulation_glass",SimulationGlassBlock::new);
    public static final DeferredBlock<SimulationLightBlock> LIGHT=ModContent.BLOCKS.register("simulation_light",SimulationLightBlock::new);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,OverloadSimulation.ID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,OverloadSimulation.ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<MultiblockSimulationMenu>> MENU=MENUS.register("simulation_controller",MultiblockSimulationMenu::createType);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SimulationControllerBlockEntity>> CONTROLLER_ENTITY=ENTITIES.register("simulation_controller",()->BlockEntityType.Builder.of(SimulationControllerBlockEntity::new,CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SimulationMemberBlockEntity>> MEMBER_ENTITY=ENTITIES.register("simulation_member",()->BlockEntityType.Builder.of(SimulationMemberBlockEntity::new,FRAME.get(),T1.get(),T2.get(),T3.get(),FORTUNE.get(),OVERLOAD.get(),SMELTING.get(),GLASS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SimulationLightBlockEntity>> LIGHT_ENTITY=ENTITIES.register("simulation_light",()->BlockEntityType.Builder.of(SimulationLightBlockEntity::new,LIGHT.get()).build(null));
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);}
    public static void setup(){CONTROLLER.get().setBlockEntity(SimulationControllerBlockEntity.class,CONTROLLER_ENTITY.get(),null,(l,p,s,be)->be.tick());AEBaseBlockEntity.registerBlockEntityItem(CONTROLLER_ENTITY.get(),CONTROLLER_ITEM.get());}
    public static java.util.List<net.minecraft.world.level.block.Block> playerBlocks(){return java.util.List.of(CONTROLLER.get(),FRAME.get(),T1.get(),T2.get(),T3.get(),FORTUNE.get(),OVERLOAD.get(),SMELTING.get());}
}
