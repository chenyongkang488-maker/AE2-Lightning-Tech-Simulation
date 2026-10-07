package dev.overloadsim.laser;

import appeng.blockentity.AEBaseBlockEntity;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LaserContent {
    private static final Map<LaserKind, DeferredBlock<LaserMachineBlock>> BLOCK_MAP = new EnumMap<>(LaserKind.class);
    static {
        for (var kind : LaserKind.values()) {
            var block = ModContent.BLOCKS.register(kind.id(), () -> new LaserMachineBlock(kind));
            BLOCK_MAP.put(kind, block);
            ModContent.ITEMS.registerSimpleBlockItem(block);
        }
    }
    private static DeferredBlock<LaserPartBlock> part(String id, boolean glass) {
        var block = ModContent.BLOCKS.register(id, () -> new LaserPartBlock(glass));
        ModContent.ITEMS.registerSimpleBlockItem(block);
        return block;
    }
    public static final DeferredBlock<LaserPartBlock> FRAME = part("laser_frame", false);
    public static final DeferredBlock<LaserPartBlock> GLASS = part("laser_glass", true);
    public static final DeferredBlock<LaserPartBlock> SOURCE = part("laser_source_crystal", false);
    public static final DeferredBlock<LaserPartBlock> PORT = part("laser_optical_port", false);
    // Internal source only: no block item, creative entry, recipe or loot.
    public static final DeferredBlock<LaserLightBlock> LIGHT = ModContent.BLOCKS.register("laser_light", LaserLightBlock::new);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OverloadSimulation.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OverloadSimulation.ID);
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, OverloadSimulation.ID);
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> COLLAPSE_TRANSITION = SOUNDS.register("collapse_transition", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ModContent.id("collapse_transition")));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserMachineBlockEntity>> MACHINE_ENTITY = ENTITIES.register("laser_machine", () -> BlockEntityType.Builder.of(LaserMachineBlockEntity::new, BLOCK_MAP.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserLightBlockEntity>> LIGHT_ENTITY = ENTITIES.register("laser_light", () -> BlockEntityType.Builder.of(LaserLightBlockEntity::new, LIGHT.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<LaserMenu>> MENU = MENUS.register("laser_machine", LaserMenu::createType);
    private LaserContent() {}
    public static Map<LaserKind, DeferredBlock<LaserMachineBlock>> blocks() { return Map.copyOf(BLOCK_MAP); }
    public static Block machine(LaserKind kind) { return BLOCK_MAP.get(kind).get(); }
    public static List<Block> playerBlocks() {
        var blocks = new java.util.ArrayList<Block>();
        for (var kind : LaserKind.values()) blocks.add(machine(kind));
        blocks.addAll(List.of(FRAME.get(), GLASS.get(), SOURCE.get(), PORT.get()));
        return List.copyOf(blocks);
    }
    public static void register(IEventBus bus) { ENTITIES.register(bus); MENUS.register(bus); SOUNDS.register(bus); }
    public static void setup() {
        for (var block : BLOCK_MAP.values()) {
            block.get().setBlockEntity(LaserMachineBlockEntity.class, MACHINE_ENTITY.get(), null, (level, pos, state, host) -> host.tick());
        }
        // AE stores one representative item per entity type; every machine still has its own normal block item.
        AEBaseBlockEntity.registerBlockEntityItem(MACHINE_ENTITY.get(), machine(LaserKind.EMITTER).asItem());
        LaserStructureIndex.register();
    }
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, MACHINE_ENTITY.get(), (host, context) -> host.kind() == LaserKind.CORE ? null : host);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE_ENTITY.get(), (host, side) -> host.kind() == LaserKind.CORE ? null : host.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MACHINE_ENTITY.get(), (host, side) -> host.kind() == LaserKind.CORE ? null : host.feCapability());
    }
}
