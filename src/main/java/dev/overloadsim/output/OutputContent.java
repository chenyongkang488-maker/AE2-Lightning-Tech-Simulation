package dev.overloadsim.output;

import appeng.blockentity.AEBaseBlockEntity;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.*;

public final class OutputContent {
    public static final DeferredBlock<OutputInterfaceBlock> BLOCK = ModContent.BLOCKS.register("overload_output_interface", OutputInterfaceBlock::new);
    public static final DeferredItem<BlockItem> ITEM = ModContent.ITEMS.registerSimpleBlockItem(BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OverloadSimulation.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OverloadSimulation.ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutputInterfaceBlockEntity>> ENTITY = ENTITIES.register("overload_output_interface", () -> BlockEntityType.Builder.of(OutputInterfaceBlockEntity::new, BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<OutputMenu>> MENU = MENUS.register("overload_output_interface", () -> OutputMenu.TYPE);
    public static void register(IEventBus bus) { ENTITIES.register(bus); MENUS.register(bus); }
    public static void setup() {
        BLOCK.get().setBlockEntity(OutputInterfaceBlockEntity.class, ENTITY.get(), null, (level, pos, state, host) -> host.tick());
        AEBaseBlockEntity.registerBlockEntityItem(ENTITY.get(), ITEM.get());
        appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.CRAFTING_CARD, ITEM.get(), 1);
        appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.FUZZY_CARD, ITEM.get(), 1);
        var induction = com.moakiee.ae2lt.logic.energy.AppFluxBridge.getInductionCard();
        if (induction != null) appeng.api.upgrades.Upgrades.add(induction, ITEM.get(), 1);
    }
    public static void capabilities(RegisterCapabilitiesEvent event) { OutputInterfaceBlockEntity.registerCapabilities(event, ENTITY.get()); }
    private OutputContent() {}
}
