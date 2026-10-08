package dev.overloadsim.workstation;

import appeng.blockentity.AEBaseBlockEntity;
import dev.overloadsim.*;
import dev.overloadsim.api.LaserExtensions;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.registries.*;

public final class WorkstationContent {
    public static final DeferredBlock<HighEnergyWorkstationBlock> CONTROLLER=ModContent.BLOCKS.register("high_energy_workstation",HighEnergyWorkstationBlock::new);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CONTROLLER_ITEM=ModContent.ITEMS.registerSimpleBlockItem(CONTROLLER);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,OverloadSimulation.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,OverloadSimulation.ID);
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,OverloadSimulation.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,OverloadSimulation.ID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<HighEnergyWorkstationBlockEntity>> CONTROLLER_ENTITY=ENTITIES.register("high_energy_workstation",()->BlockEntityType.Builder.of(HighEnergyWorkstationBlockEntity::new,CONTROLLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<HighEnergyWorkstationMenu>> MENU=MENUS.register("high_energy_workstation",HighEnergyWorkstationMenu::createType);
    public static final DeferredHolder<RecipeType<?>,RecipeType<HighEnergyWorkstationRecipe>> RECIPE_TYPE=TYPES.register("high_energy_workstation",()->new RecipeType<>(){public String toString(){return ModContent.id("high_energy_workstation").toString();}});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<HighEnergyWorkstationRecipe>> RECIPE_SERIALIZER=SERIALIZERS.register("high_energy_workstation",HighEnergyWorkstationRecipe.Serializer::new);
    private WorkstationContent() {}
    public static List<Block> playerBlocks(){return List.of(CONTROLLER.get());}
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);TYPES.register(bus);SERIALIZERS.register(bus);}
    public static void setup(){
        CONTROLLER.get().setBlockEntity(HighEnergyWorkstationBlockEntity.class,CONTROLLER_ENTITY.get(),null,(level,pos,state,host)->host.tick());
        AEBaseBlockEntity.registerBlockEntityItem(CONTROLLER_ENTITY.get(),CONTROLLER_ITEM.get());WorkstationStructureIndex.register();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent event)->WorkstationRecipes.invalidate());
        var receiverTarget=new LaserExtensions.LaserTarget(){
            @Override public boolean accept(LaserExtensions.TargetContext context){
                var host=WorkstationStructureIndex.owner(context.level(),context.position());
                if(host==null||!context.position().equals(host.receiver()))return false;
                return context.source()==null||context.sourceIdentity()==null?host.receiveLaser(context.incoming(),context.energy(),context.batchTick())
                        :host.receiveLaserFromSource(context.incoming(),context.energy(),context.batchTick(),context.source(),context.sourceIdentity());
            }
            @Override public void deactivate(LaserExtensions.TargetContext context){var host=WorkstationStructureIndex.owner(context.level(),context.position());if(host!=null&&context.source()!=null&&context.sourceIdentity()!=null)host.releaseLaserSource(context.source(),context.sourceIdentity());}
            @Override public void refreshLease(LaserExtensions.TargetContext context){var host=WorkstationStructureIndex.owner(context.level(),context.position());if(host!=null&&context.source()!=null&&context.sourceIdentity()!=null){host.releaseLaserSource(context.source(),context.sourceIdentity());host.receiveLaserFromSource(context.incoming(),context.energy(),context.batchTick(),context.source(),context.sourceIdentity());}}
        };
        // Only the validated workstation's exact shared optical port accepts this target.
        LaserExtensions.registerTarget(ModContent.id("laser_optical_port"),receiverTarget);
    }
    public static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,CONTROLLER_ENTITY.get(),(host,context)->host);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,CONTROLLER_ENTITY.get(),(host,side)->host.automation(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,CONTROLLER_ENTITY.get(),(host,side)->host.fluidAutomation(side));
    }
}
