package dev.overloadsim.laser;

import com.mojang.authlib.GameProfile;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.api.LaserExtensions;
import dev.overloadsim.core.LaserMath;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Loaded-only, bounded optical traversal. No resource-carrying beam or particle entities. */
public final class LaserNetwork {
    private static final int MAX_HOPS=128,MAX_STEPS=512;
    private static final GameProfile OPERATOR=new GameProfile(UUID.fromString("eeaab42b-c9b2-4b91-bb26-7d8682c8c804"),"[Simulation Laser]");
    private static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ModContent.id("high_energy_laser"));
    record ExternalTarget(LaserExtensions.LaserTarget hook,LaserExtensions.TargetContext context) {}
    record Path(List<LaserMachineBlockEntity> nodes,List<BlockPos> clearCells,Map<BlockPos,BlockState> watched,
            List<ExternalTarget> targets){}
    static final Map<LaserMachineBlockEntity,Path> PATHS=new WeakHashMap<>();
    private static final Map<ServerLevel,Claims> CLAIMS=new WeakHashMap<>();
    private static final class Claims {long tick=-1;final Set<BlockPos> inputs=new HashSet<>();}
    private record Beam(BlockPos start,Direction direction,long energy,int range,Set<BlockPos> visited){}
    private LaserNetwork(){}
    public static void register(LaserMachineBlockEntity host){}
    public static void unregister(LaserMachineBlockEntity host){
        clear(host);
        for(var root:List.copyOf(PATHS.keySet()))if(PATHS.get(root)!=null&&PATHS.get(root).nodes().contains(host))clear(root);
    }
    public static void tick(LaserMachineBlockEntity host){
        if(!(host.getLevel() instanceof ServerLevel level))return;
        if(host.kind()!=LaserKind.EMITTER)return;
        var path=PATHS.get(host);
        if(!host.enabled()||!host.formed()||path!=null&&!valid(level,path)){clear(host);return;}
        long now=level.getGameTime();
        if(now%20==0)processEmitter(host);
        if(now%2==0&&host.activeUntil>now&&!host.segments.isEmpty()){
            var current=PATHS.get(host);
            damage(level,host.segments,current==null?List.of():current.nodes().stream().filter(node->DeflectorBatchRouter.ownsNode(host,node)).toList());
        }
    }
    static boolean valid(ServerLevel level,Path path){
        for(var node:path.nodes()){
            if(node.isRemoved()||!level.hasChunkAt(node.getBlockPos())||!node.enabled())return false;
            node.checkStructure();if(!node.formed())return false;
        }
        for(var pos:path.clearCells())if(!level.hasChunkAt(pos)||!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())return false;
        for(var entry:path.watched().entrySet())if(!level.hasChunkAt(entry.getKey())||!level.getBlockState(entry.getKey()).equals(entry.getValue()))return false;
        return true;
    }
    private static void clear(LaserMachineBlockEntity root){
        clear(root,true);
    }
    static void clear(LaserMachineBlockEntity root,boolean stopped){
        var old=PATHS.remove(root);
        DeflectorBatchRouter.remove(root,stopped);
        if(stopped)stopLostTargets(old,List.of());
        if(old!=null)for(var node:old.nodes()){if(DeflectorBatchRouter.retained(node))continue;node.activeUntil=0;node.lastPower=0;if(stopped)node.stopCollapseVisuals();node.sync();}
        if(!root.segments.isEmpty()||root.lastPower!=0){root.segments=List.of();root.lastPower=0;root.activeUntil=0;root.sync();}
        if(stopped)root.refreshLaserLights();
    }
    public static void processEmitter(LaserMachineBlockEntity root){
        if(!(root.getLevel() instanceof ServerLevel level))return;
        if(DeflectorBatchRouter.process(root))return;
        var previous=PATHS.get(root);clear(root,false);root.checkStructure();
        if(!root.enabled()||!root.formed()||!root.tryPay(LaserSettings.emitterFe(),0,false)){stopLostCores(previous,List.of());stopLostTargets(previous,List.of());root.refreshLaserLights();root.status="no_energy";root.sync();return;}
        long now=level.getGameTime();
        var candidates=LaserStructureIndex.controllers(level).stream().limit(256).toList();
        for(var node:candidates)node.checkStructure();
        var ports=new HashMap<BlockPos,LaserMachineBlockEntity>();
        for(var node:candidates){
            if(!node.formed()||!node.enabled()||node==root)continue;
            if(node.kind()==LaserKind.CORE)ports.put(node.getBlockPos(),node);
            else if(node.kind()==LaserKind.DEFLECTOR){
                for(var side:Direction.Plane.HORIZONTAL){
                    var port=node.center().relative(side,3);
                    if(!port.equals(node.getBlockPos())&&level.getBlockState(port).is(LaserContent.PORT.get()))ports.put(port,node);
                }
            }
            else if(node.kind()==LaserKind.RECEIVER)ports.put(node.center().relative(node.forward().getOpposite(),3),node);
            else if(node.kind()!=LaserKind.EMITTER)ports.put(node.center(),node);
        }
        var lines=new ArrayList<LaserSegment>();var nodes=new ArrayList<LaserMachineBlockEntity>();nodes.add(root);
        var clearCells=new ArrayList<BlockPos>();var watched=new HashMap<BlockPos,BlockState>();var queue=new ArrayDeque<Beam>();
        var acceptedTargets=new ArrayList<ExternalTarget>();
        var start=root.center().relative(root.forward(),2);
        queue.add(new Beam(start,root.forward(),LaserSettings.emitterFe(),6,new HashSet<>(Set.of(root.getBlockPos()))));
        var claims=CLAIMS.computeIfAbsent(level,l->new Claims());if(claims.tick!=now){claims.tick=now;claims.inputs.clear();}
        int hops=0,steps=0;
        while(!queue.isEmpty()&&hops++<MAX_HOPS&&steps<MAX_STEPS&&lines.size()<128){
            var beam=queue.removeFirst();BlockPos end=beam.start();boolean drawn=false;
            for(int distance=1;distance<=beam.range()&&steps++<MAX_STEPS;distance++){
                var pos=beam.start().relative(beam.direction(),distance);
                if(!level.hasChunkAt(pos))break;
                var target=ports.get(pos);
                if(target!=null){
                    end=pos;add(lines,beam.start(),end,beam.energy(),false,false);drawn=true;
                    if(target.kind()==LaserKind.DEFLECTOR){
                        var incomingFace=beam.direction().getOpposite();
                        if(target.sideMode(incomingFace)!=LaserMachineBlockEntity.SIDE_INPUT||!target.center().relative(incomingFace,3).equals(pos))break;
                    }else if(target.forward()!=beam.direction()||target.kind()==LaserKind.CORE||target.kind()==LaserKind.RECEIVER)break;
                    if(beam.visited().contains(target.getBlockPos())||!claims.inputs.add(target.getBlockPos())){target.status="invalid_loop_or_input";target.sync();break;}
                    var visited=new HashSet<>(beam.visited());visited.add(target.getBlockPos());nodes.add(target);
                    long power=beam.energy(),cost=0,ehv=0;int range=beam.range()-distance;
                    try{
                        switch(target.kind()){
                            case CONSTRAINT->{cost=LaserSettings.constraintFe();range=6;}
                            case ENHANCEMENT->{cost=target.boost();power=LaserMath.boost(power,cost);}
                            case BURST->{ehv=LaserSettings.burstEhv();power=LaserMath.amplify(power);}
                            case FOCUS->{range=7;}
                            default->{}
                        }
                    }catch(IllegalArgumentException cap){target.status="beam_limit";target.sync();break;}
                    var event=new LaserEvents.BeforeModifier(level,target.getBlockPos(),BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()),beam.energy(),power,range);
                    NeoForge.EVENT_BUS.post(event);if(event.isCanceled()){target.status="modifier_cancelled";break;}
                    target.checkStructure();
                    root.checkStructure();
                    if(target.isRemoved()||!level.hasChunkAt(target.getBlockPos())||level.getBlockEntity(target.getBlockPos())!=target||!target.formed()||!target.enabled()||root.isRemoved()||!root.enabled())break;
                    if(!valid(level,new Path(List.copyOf(nodes),List.copyOf(clearCells),Map.copyOf(watched),List.of())))break;
                    if(target.kind()==LaserKind.DEFLECTOR&&target.sideMode(beam.direction().getOpposite())!=LaserMachineBlockEntity.SIDE_INPUT)break;
                    power=event.outputEnergy();range=event.range();
                    if(power==0){target.status="idle";target.sync();break;}
                    if(!target.tryPay(cost,ehv,false)){target.status="no_energy";target.sync();break;}
                    target.lastPower=power;target.activeUntil=now+22;target.status="working";target.sync();
                    if(target.kind()==LaserKind.FOCUS){long focusPower=power;focus(level,target,power,now,lines,nodes,clearCells,watched,
                            ()->valid(level,new Path(List.copyOf(nodes),List.copyOf(clearCells),Map.copyOf(watched),List.of()))?focusPower:0);}
                    else if(target.kind()==LaserKind.DEFLECTOR){
                        // Owned apertures are solid ports; internal axes are validated by the structure,
                        // never added as external clear cells or rendered as a diagonal through the shell.
                        add(lines,pos,target.center(),beam.energy(),false,false);
                        var faces=Arrays.stream(Direction.values()).filter(d->d.getAxis()!=Direction.Axis.Y&&target.sideMode(d)==LaserMachineBlockEntity.SIDE_OUTPUT
                                &&!target.center().relative(d,3).equals(target.getBlockPos())).toList();
                        if(!faces.isEmpty()){
                            var split=LaserMath.split(power,faces.size());
                            for(int i=0;i<faces.size();i++)if(split[i]>0){var d=faces.get(i);var out=target.center().relative(d,3);add(lines,target.center(),out,split[i],false,false);queue.add(new Beam(out,d,split[i],6,new HashSet<>(visited)));}
                        }else{target.status="no_outputs";target.activeUntil=0;target.sync();}
                    }else if(power>0&&range>0)queue.add(new Beam(pos,beam.direction(),power,range,visited));
                    break;
                }
                var state=level.getBlockState(pos);var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
                var customTarget=LaserExtensions.target(id);
                if(customTarget!=null)watched.put(pos.immutable(),state);
                if(customTarget!=null){
                    var context=new LaserExtensions.TargetContext(level,pos.immutable(),beam.direction(),beam.energy(),now,root.getBlockPos().immutable(),root.identity());
                    if(customTarget.accept(context)){acceptedTargets.add(new ExternalTarget(customTarget,context));end=pos;break;}
                }
                var custom=LaserExtensions.modifier(id);
                if(custom!=null){
                    watched.put(pos.immutable(),state);
                    end=pos;add(lines,beam.start(),end,beam.energy(),false,false);drawn=true;
                    if(beam.visited().contains(pos))break;
                    var result=custom.modify(new LaserExtensions.BeamContext(level,pos,beam.direction(),beam.energy(),beam.range()-distance,now));
                    if(result!=null&&level.hasChunkAt(pos)){
                        var event=new LaserEvents.BeforeModifier(level,pos,id,beam.energy(),result.energy(),result.range());NeoForge.EVENT_BUS.post(event);
                        if(!event.isCanceled()&&level.hasChunkAt(pos)&&level.getBlockState(pos).equals(state)&&!root.isRemoved()&&root.enabled()
                                &&valid(level,new Path(List.copyOf(nodes),List.copyOf(clearCells),Map.copyOf(watched),List.of()))&&event.outputEnergy()>0&&event.range()>0){var visited=new HashSet<>(beam.visited());visited.add(pos);queue.add(new Beam(pos,beam.direction(),event.outputEnergy(),event.range(),visited));}
                    }
                    break;
                }
                if(!state.getCollisionShape(level,pos).isEmpty()){end=pos;break;}
                end=pos;clearCells.add(pos);
                if(distance==beam.range()){add(lines,beam.start(),end,beam.energy(),false,false);drawn=true;}
            }
            if(!drawn)add(lines,beam.start(),end,beam.energy(),false,false);
        }
        if(root.isRemoved()||!level.hasChunkAt(root.getBlockPos())||level.getBlockEntity(root.getBlockPos())!=root||!root.formed()||!root.enabled()){
            stopLostCores(previous,List.of());stopLostTargets(previous,acceptedTargets);deactivateTargets(acceptedTargets);return;
        }
        root.segments=List.copyOf(lines);root.lastPower=LaserSettings.emitterFe();root.activeUntil=now+22;root.status="working";
        var path=new Path(List.copyOf(nodes),List.copyOf(clearCells),Map.copyOf(watched),List.copyOf(acceptedTargets));
        PATHS.put(root,path);stopLostCores(previous,nodes);stopLostTargets(previous,acceptedTargets);if(!valid(level,path)){clear(root);return;}root.refreshLaserLights();root.sync();root.saveChanges();
    }
    private static void stopLostTargets(Path previous,List<ExternalTarget> next){
        if(previous==null)return;
        for(var old:previous.targets())if(next.stream().noneMatch(target->target.hook()==old.hook()
                &&target.context().position().equals(old.context().position())))old.hook().deactivate(old.context());
    }
    private static void deactivateTargets(List<ExternalTarget> targets){
        for(var target:targets)target.hook().deactivate(target.context());
    }
    private static void stopLostCores(Path previous,List<LaserMachineBlockEntity> next){
        if(previous!=null)for(var node:previous.nodes())if(node.kind()==LaserKind.CORE&&!next.contains(node)){node.stopCollapseVisuals();node.sync();}
    }
    static void add(List<LaserSegment> lines,BlockPos from,BlockPos to,long power,boolean focus,boolean particle){if(lines.size()<128&&!from.equals(to))lines.add(new LaserSegment(from.getCenter(),to.getCenter(),power,focus,particle));}
    static void focus(ServerLevel level,LaserMachineBlockEntity matrix,long energy,long now,List<LaserSegment> lines,List<LaserMachineBlockEntity> nodes,List<BlockPos> clearCells,Map<BlockPos,BlockState> watched){
        focus(level,matrix,energy,now,lines,nodes,clearCells,watched,()->energy);
    }
    static void focus(ServerLevel level,LaserMachineBlockEntity matrix,long energy,long now,List<LaserSegment> lines,List<LaserMachineBlockEntity> nodes,List<BlockPos> clearCells,Map<BlockPos,BlockState> watched,java.util.function.LongSupplier validEnergy){
        java.util.function.LongSupplier available=()->{
            long power=Math.clamp(validEnergy.getAsLong(),0L,energy);
            if(power<=0)return 0;
            for(var cell:clearCells)if(!level.hasChunkAt(cell)||!level.getBlockState(cell).getCollisionShape(level,cell).isEmpty())return 0;
            for(var entry:watched.entrySet())if(!level.hasChunkAt(entry.getKey())||!level.getBlockState(entry.getKey()).equals(entry.getValue()))return 0;
            return power;
        };
        var start=matrix.center();var end=start;
        for(int i=1;i<=7;i++){
            if(available.getAsLong()<=0)break;
            var pos=start.relative(matrix.forward(),i);if(!level.hasChunkAt(pos))break;end=pos;
            if(i==7){
                if(level.getBlockEntity(pos) instanceof LaserMachineBlockEntity core&&core.kind()==LaserKind.CORE&&core.enabled()){
                    nodes.add(core);long produced=core.acceptCore(available.getAsLong(),matrix.forward(),now,available);
                    var receiver=produced>0?sendParticles(core,produced,matrix.forward(),now):null;
                    if(receiver!=null){
                        nodes.add(receiver);
                        var port=receiver.center().relative(matrix.forward().getOpposite(),3);
                        watched.put(port.immutable(),level.getBlockState(port));
                        if(lines.size()<128)lines.add(new LaserSegment(pos.getCenter(),port.getCenter(),core.lastPower,false,true,produced,now));
                    }
                }break;
            }
            var state=level.getBlockState(pos);
            if(!LaserLightManager.isBeamAir(state)&&!breakCell(level,pos))break;
            clearCells.add(pos);
        }
        long remaining=available.getAsLong();if(remaining>0)add(lines,start,end,remaining,true,false);
    }
    /** Settlement is immediate; a zero production batch has no delivery or visual lease. */
    public static LaserMachineBlockEntity sendParticles(LaserMachineBlockEntity core,long count,Direction direction,long tick){
        if(count<=0||direction==null||tick<0||!(core.getLevel() instanceof ServerLevel level))return null;
        core.particleDirection=direction;
        var candidates=LaserStructureIndex.controllers(level);
        for(int i=1;i<=7;i++){
            var pos=core.getBlockPos().relative(direction,i);if(!level.hasChunkAt(pos))break;
            for(var receiver:candidates)if(receiver.kind()==LaserKind.RECEIVER&&receiver.enabled()){
                receiver.checkStructure();
                if(receiver.acceptsParticleInput(pos,direction)){
                    if(!receiver.acceptParticles(count))return null;
                    receiver.activeUntil=tick+22;receiver.lastPower=core.lastPower;receiver.sync();return receiver;
                }
            }
            if(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())break;
        }
        return null;
    }
    static boolean breakCell(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        var state=level.getBlockState(pos);if(LaserLightManager.isBeamAir(state))return true;
        if(state.getDestroySpeed(level,pos)<0)return false;
        var operator=FakePlayerFactory.get(level,OPERATOR);
        if(!level.mayInteract(operator,pos))return false;
        var event=new BlockEvent.BreakEvent(level,pos,state,operator);NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled()&&level.hasChunkAt(pos)&&level.getBlockState(pos).equals(state)&&level.destroyBlock(pos,false,operator);
    }
    private static void damage(ServerLevel level,List<LaserSegment> lines,List<LaserMachineBlockEntity> nodes){
        var hits=new HashMap<LivingEntity,Float>();
        for(var line:lines){
            if(line.particle())continue;
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,line.bounds(),e->e.isAlive()&&!e.isSpectator())){
                if(!line.intersects(entity.getBoundingBox()))continue;
                hits.merge(entity,damageAmount(line.energy()),Math::max);
            }
        }
        for(var host:nodes){
            if(host.kind()!=LaserKind.CONSTRAINT||!host.enabled()||!host.formed()||host.currentPower()<=0)continue;
            var center=host.center();
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,ConstraintFields.bounds(center),e->e.isAlive()&&!e.isSpectator())){
                if(ConstraintFields.intersects(center,entity.getBoundingBox()))hits.merge(entity,damageAmount(host.currentPower()),Math::max);
            }
        }
        var source=new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
        hits.forEach((entity,amount)->entity.hurt(source,amount));
    }
    private static float damageAmount(long power){return (float)Math.min(1_000_000D,power/10000D*LaserSettings.damageScale());}
}
