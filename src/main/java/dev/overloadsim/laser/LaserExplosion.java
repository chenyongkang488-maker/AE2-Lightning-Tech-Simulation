package dev.overloadsim.laser;

import dev.overloadsim.api.LaserEvents;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Incremental destruction; never requests or generates a chunk. */
public final class LaserExplosion {
    private static final Map<ServerLevel,ArrayDeque<Blast>> JOBS=new WeakHashMap<>();
    private static final class Blast {
        final int minX,minY,minZ,width,height;int cursor;
        Blast(ServerLevel level,BlockPos pos,int radius){minX=((pos.getX()>>4)-radius)<<4;minZ=((pos.getZ()>>4)-radius)<<4;width=(radius*2+1)*16;minY=Math.max(level.getMinBuildHeight(),pos.getY()-LaserSettings.blastHeight());height=Math.min(level.getMaxBuildHeight(),pos.getY()+LaserSettings.blastHeight()+1)-minY;}
        boolean done(){return cursor>=width*width*height;}
        BlockPos next(){int c=cursor++;return new BlockPos(minX+c%width,minY+c/(width*width),minZ+c/width%width);}
    }
    private LaserExplosion(){}
    public static void trigger(LaserMachineBlockEntity host){
        if(!(host.getLevel() instanceof ServerLevel level)||host.explosionTriggered())return;
        var event=new LaserEvents.BeforeExplosion(level,host.getBlockPos(),host.formed()?"particle_overflow":"containment_broken",1);NeoForge.EVENT_BUS.post(event);
        if(event.isCanceled()||!host.markExplosionTriggered())return;
        host.particles().load(0);host.setEnabled(false);host.sync();host.saveChanges();
        var pos=host.getBlockPos();JOBS.computeIfAbsent(level,l->new ArrayDeque<>()).add(new Blast(level,pos,event.radiusChunks()));
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,pos.getX()+.5,pos.getY()+3.5,pos.getZ()+.5,1,0,0,0,0);
        level.playSound(null,pos,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.BLOCKS,6,0.65F);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate((event.radiusChunks()*2+1)*8)))entity.hurt(level.damageSources().explosion(null,null),80);
    }
    public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var jobs=JOBS.get(level);if(jobs==null)return;
        int budget=LaserSettings.blastBudget();
        while(budget>0&&!jobs.isEmpty()){
            var job=jobs.peek();while(budget-->0&&!job.done())LaserNetwork.breakCell(level,job.next());
            if(job.done())jobs.remove();
        }
        if(jobs.isEmpty())JOBS.remove(level);
    }
}
