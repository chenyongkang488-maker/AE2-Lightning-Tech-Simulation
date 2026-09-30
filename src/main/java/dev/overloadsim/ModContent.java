package dev.overloadsim;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.*;
import appeng.blockentity.AEBaseBlockEntity;
import dev.overloadsim.api.CrystalData;
import dev.overloadsim.data.SimulationRecipe;
import dev.overloadsim.item.SimulationCrystalItem;
import dev.overloadsim.machine.*;

public final class ModContent {
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(OverloadSimulation.ID,path);}
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(OverloadSimulation.ID);
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(OverloadSimulation.ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,OverloadSimulation.ID);
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<CrystalData>> CRYSTAL_DATA=COMPONENTS.register("crystal_data",()->DataComponentType.<CrystalData>builder().persistent(CrystalData.CODEC).networkSynchronized(CrystalData.STREAM_CODEC).build());
    public static final DeferredItem<Item> BLANK=ITEMS.register("blank_simulation_crystal",()->new SimulationCrystalItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOUND=ITEMS.register("simulation_crystal",()->new SimulationCrystalItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PERFECT=ITEMS.register("perfect_simulation_crystal",()->new SimulationCrystalItem(new Item.Properties().stacksTo(1)));
    public static final DeferredBlock<SimulationChamberBlock> CHAMBER=BLOCKS.register("overload_simulation_chamber",SimulationChamberBlock::new);
    public static final DeferredItem<BlockItem> CHAMBER_ITEM=ITEMS.registerSimpleBlockItem(CHAMBER);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,OverloadSimulation.ID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SimulationChamberBlockEntity>> CHAMBER_ENTITY=BLOCK_ENTITIES.register("overload_simulation_chamber",()->BlockEntityType.Builder.of(SimulationChamberBlockEntity::new,CHAMBER.get()).build(null));
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,OverloadSimulation.ID);
    public static final DeferredHolder<MenuType<?>,MenuType<SimulationMenu>> MENU=MENUS.register("overload_simulation_chamber",()->SimulationMenu.createType());
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,OverloadSimulation.ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,OverloadSimulation.ID);
    public static final Map<SimulationRecipe.Kind,DeferredHolder<RecipeType<?>,RecipeType<SimulationRecipe>>> RECIPE_TYPES=new EnumMap<>(SimulationRecipe.Kind.class);
    public static final Map<SimulationRecipe.Kind,DeferredHolder<RecipeSerializer<?>,RecipeSerializer<SimulationRecipe>>> SERIALIZERS=new EnumMap<>(SimulationRecipe.Kind.class);
    static {for(var kind:SimulationRecipe.Kind.values()){String name=switch(kind){case BINDING->"crystal_binding";case MOB_BINDING->"mob_crystal_binding";case CULTIVATION->"crystal_cultivation";case PRODUCTION->"overload_simulation";};RECIPE_TYPES.put(kind,TYPES.register(name,()->new RecipeType<SimulationRecipe>(){public String toString(){return id(name).toString();}}));SERIALIZERS.put(kind,RECIPE_SERIALIZERS.register(name,()->new SimulationRecipe.Serializer(kind)));}}
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,OverloadSimulation.ID);
    static {TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.overload_sim")).icon(()->new ItemStack(PERFECT.get())).displayItems((p,o)->{o.accept(BLANK);o.accept(BOUND);o.accept(PERFECT);o.accept(CHAMBER_ITEM);}).build());}
    public static void register(IEventBus bus){ITEMS.register(bus);BLOCKS.register(bus);COMPONENTS.register(bus);BLOCK_ENTITIES.register(bus);MENUS.register(bus);TYPES.register(bus);RECIPE_SERIALIZERS.register(bus);TABS.register(bus);}
    public static void setup(FMLCommonSetupEvent e){e.enqueueWork(()->{
        CHAMBER.get().setBlockEntity(SimulationChamberBlockEntity.class,CHAMBER_ENTITY.get(),null,(level,pos,state,be)->be.tick());
        AEBaseBlockEntity.registerBlockEntityItem(CHAMBER_ENTITY.get(),CHAMBER_ITEM.get());
    });}
    public static void capabilities(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,CHAMBER_ENTITY.get(),(be,context)->be);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,CHAMBER_ENTITY.get(),(be,side)->be.automation());
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,CHAMBER_ENTITY.get(),(be,side)->be.energy());
    }
}
