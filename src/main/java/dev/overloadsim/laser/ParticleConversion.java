package dev.overloadsim.laser;

import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.core.ParticleClock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.item.ItemStack;

/** Sealed, paid fuel windows. A cancelled transaction leaves all clocks and inventories intact. */
public final class ParticleConversion {
    private ParticleConversion(){}
    public static void tick(LaserMachineBlockEntity host){
        if (host.conversionInProgress) return;
        host.conversionInProgress = true;
        try { advance(host); } finally { host.conversionInProgress = false; }
    }
    private static void advance(LaserMachineBlockEntity host){
        if(!(host.getLevel() instanceof ServerLevel level)||host.kind()!=LaserKind.RECEIVER||!host.formed()||!host.enabled()||host.explosionTriggered())return;
        ParticleFuelData.Recipe recipe=null;int slot=-1;
        if(host.fuelRemaining>0){
            var id=ResourceLocation.tryParse(host.activeFuel);
            if(id!=null)recipe=ParticleFuelData.resolve(id).orElse(null);
            if(recipe==null){host.status="fuel_rule_missing";return;}
            // Reload cannot renew or extend an already-paid window.
            if(host.fuelRemaining>recipe.consumableDurationTicks()||recipe.inputEnergyPerBatch()==0&&host.particleRemaining>recipe.particleIntervalTicks()){
                host.status="fuel_rule_changed";return;
            }
        }else{
            for(int i=0;i<host.fuelInput().getSlots();i++){
                var match=ParticleFuelData.match(host.fuelInput().getStackInSlot(i));
                if(match.isPresent()){recipe=match.get();slot=i;break;}
            }
        }
        if(recipe==null){host.status="no_fuel";return;}
        if(host.particles().stored()==0){host.status="no_particles";return;}
        long space=host.energy().capacity()-host.energy().stored();
        if(space<recipe.energyPerParticle()){host.status="energy_full";return;}
        int oldFuel=host.fuelRemaining,oldParticle=host.particleRemaining;
        long oldCredit=host.fuelProcessingCredit;
        var oldItem=slot>=0?host.fuelInput().getStackInSlot(slot).copy():ItemStack.EMPTY;
        var clock=new ParticleClock();clock.restore(oldFuel,oldParticle,oldCredit);
        var result=clock.advance(recipe.fuel(),slot>=0,host.particles().stored(),space);
        long used=result.particlesUsed(),produced=result.producedFe();
        if(used>0){
            var event=new LaserEvents.BeforeParticleConversion(level,host.getBlockPos(),recipe.id(),used,produced);
            NeoForge.EVENT_BUS.post(event);
            if(event.isCanceled()||event.particles()==0){host.status="conversion_cancelled";return;}
            used=event.particles();produced=event.energy();
        }
        host.checkStructure();
        if(host.isRemoved()||level.getBlockEntity(host.getBlockPos())!=host||!host.formed()||!host.enabled()||host.explosionTriggered()
            ||host.fuelRemaining!=oldFuel||host.particleRemaining!=oldParticle||host.fuelProcessingCredit!=oldCredit||host.particles().stored()<used
            ||host.energy().capacity()-host.energy().stored()<produced
            ||result.consumedItem()&&(!ItemStack.isSameItemSameComponents(oldItem,host.fuelInput().getStackInSlot(slot))||host.fuelInput().getStackInSlot(slot).getCount()!=oldItem.getCount()))return;
        if(result.consumedItem()){
            var stack=host.fuelInput().getStackInSlot(slot).copy();stack.shrink(1);
            host.fuelInput().setStackInSlot(slot,stack);host.activeFuel=recipe.id().toString();
        }
        host.particles().consume(used);host.energy().insert(produced,false);
        host.fuelRemaining=clock.state().fuelRemaining();host.particleRemaining=clock.state().particleRemaining();
        host.fuelProcessingCredit=clock.state().processingCredit();
        host.status="converting";host.activeUntil=Math.max(host.activeUntil,level.getGameTime()+2);
        host.saveChanges();if(produced>0||result.consumedItem())host.sync();
    }
}
