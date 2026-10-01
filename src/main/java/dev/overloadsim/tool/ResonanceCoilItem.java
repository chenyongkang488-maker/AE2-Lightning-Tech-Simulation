package dev.overloadsim.tool;

import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.device.DeviceKind;
import net.minecraft.world.item.Item;
import java.util.List;
import dev.overloadsim.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.*;

public class ResonanceCoilItem extends Item implements DeviceItem {
    public ResonanceCoilItem(){super(new Properties().stacksTo(1));}
    /** Upstream enum is closed; our adapter is selected by exact item, never this fallback kind. */
    public DeviceKind deviceKind(){return DeviceKind.RAILGUN;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(CoilWrench.active(stack))return InteractionResultHolder.pass(stack);
        if(!CoilModules.hasCore(stack)){if(player instanceof ServerPlayer sp)sp.displayClientMessage(Component.translatable("message.overload_sim.coil.no_core"),true);return InteractionResultHolder.fail(stack);}
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(CoilWrench.active(stack))return InteractionResult.PASS;
        if(player.isShiftKeyDown()&&CoilModules.miningReady(stack)){
            if(!CoilMining.ready(stack))return InteractionResult.FAIL;
            var before=context.getLevel().getBlockState(context.getClickedPos());
            // Preserve NeoForge snapshots, placement protection and item-component rollback.
            var result=stack.useOn(context);
            if(result.consumesAction()&&player instanceof ServerPlayer sp)
                CoilMining.afterTerrain(stack,sp,context.getClickedPos(),before);
            return result;
        }
        return use(context.getLevel(),player,context.getHand()).getResult();
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(CoilWrench.active(context.getItemInHand()))return CoilWrench.use(context);
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(player.isShiftKeyDown()&&CoilMining.modifyTerrain(context))return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        return use(context.getLevel(),player,context.getHand()).getResult();
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
        if(CoilWrench.active(stack))return;
        if(entity instanceof ServerPlayer player&&player.getItemInHand(player.getUsedItemHand())==stack){
            var result=CoilLightning.fire(player,stack,72000-remaining>=SimulationConfig.COIL_SELF_CHARGE.get());
            if(result!=CoilLightning.Result.SUCCESS&&result!=CoilLightning.Result.COOLDOWN)player.displayClientMessage(Component.translatable("message.overload_sim.coil."+result.name().toLowerCase(java.util.Locale.ROOT)),true);
        }
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(entity instanceof ServerPlayer player&&(selected||player.getOffhandItem()==stack))CoilEnergy.INSTANCE.refill(stack,player);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){
        return CoilModules.miningReady(stack)&&(CoilModules.has(stack,CoilModuleItem.Type.ULTIMATE)||!state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL));
    }
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){
        if(!CoilModules.miningReady(stack))return 1;
        int efficiency=CoilSettings.read(stack).efficiencyLevel(stack);return 9+(efficiency>0?efficiency*efficiency+1:0);
    }
    @Override public boolean canPerformAction(ItemStack stack,ItemAbility ability){
        if(CoilWrench.ability(stack,ability))return true;
        return CoilMining.ready(stack)&&(ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability)||ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)||ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability)||ItemAbilities.DEFAULT_HOE_ACTIONS.contains(ability)||ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability)||ability==ItemAbilities.SHEARS_DIG||ability==ItemAbilities.SHEARS_DISARM||ability==ItemAbilities.SHEARS_CARVE);
    }
    @Override public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack){
        if(!CoilModules.miningReady(stack)||CoilEnergy.read(stack)<SimulationConfig.COIL_MINING_FE.get())return ItemAttributeModifiers.EMPTY;
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,7,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();
    }
    @Override public int getEnchantmentLevel(ItemStack stack,Holder<Enchantment> enchantment){
        if(enchantment.is(Enchantments.SILK_TOUCH))return CoilSettings.read(stack).silkActive(stack)?1:0;
        if(enchantment.is(Enchantments.FORTUNE))return CoilSettings.read(stack).fortuneLevel(stack);
        return super.getEnchantmentLevel(stack,enchantment);
    }
    @Override public ItemEnchantments getAllEnchantments(ItemStack stack,HolderLookup.RegistryLookup<Enchantment> lookup){
        var out=new ItemEnchantments.Mutable(super.getAllEnchantments(stack,lookup));
        out.set(lookup.getOrThrow(Enchantments.SILK_TOUCH),CoilSettings.read(stack).silkActive(stack)?1:0);
        out.set(lookup.getOrThrow(Enchantments.FORTUNE),CoilSettings.read(stack).fortuneLevel(stack));return out.toImmutable();
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity miner){
        return true; // Actual block removal and payment are committed by CoilMiningCommitMixin.
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(attacker instanceof ServerPlayer player){CoilEnergy.INSTANCE.tryConsume(stack,player,SimulationConfig.COIL_MINING_FE.get());CoilMining.spark(player,target.getBoundingBox().getCenter());}return true;
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack old,ItemStack next,boolean slotChanged){return slotChanged||old.getItem()!=next.getItem();}
    @Override public boolean doesSneakBypassUse(ItemStack stack,net.minecraft.world.level.LevelReader level,BlockPos pos,Player player){return CoilWrench.active(stack)&&(!CoilWrench.mekanism()||CoilSettings.read(stack).wrenchMode()==0);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable(CoilWrench.active(stack)?"tooltip.overload_sim.coil.wrench_controls":"tooltip.overload_sim.coil.controls").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.overload_sim.coil.config").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(CoilModules.hasCore(stack)?"tooltip.overload_sim.coil.core_ready":"message.overload_sim.coil.no_core").withStyle(CoilModules.hasCore(stack)?ChatFormatting.LIGHT_PURPLE:ChatFormatting.RED));
        lines.add(CoilWrench.active(stack)?Component.translatable("tooltip.overload_sim.coil.wrench_active",Component.translatable("gui.overload_sim.coil.wrench_mode."+CoilSettings.read(stack).wrenchMode())).withStyle(ChatFormatting.LIGHT_PURPLE):Component.translatable("tooltip.overload_sim.coil.voltage",CoilSettings.read(stack).natural(stack)?"EHV":"HV").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("FE: "+CoilEnergy.read(stack)+" / "+CoilModules.capacity(stack)).withStyle(ChatFormatting.GRAY));
    }
}
