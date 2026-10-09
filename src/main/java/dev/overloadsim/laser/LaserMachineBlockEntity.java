package dev.overloadsim.laser;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionHost;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingAccess;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.compat.FrequencyBindingLifecycle;
import dev.overloadsim.compat.AppliedFluxRefunds;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.core.LongEnergyBuffer;
import dev.overloadsim.core.ParticleBuffer;
import dev.overloadsim.core.ParticleClock;
import dev.overloadsim.config.MachineSettings;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;

/** Authoritative controller state. Rendering consumes a bounded snapshot and never settles resources. */
public final class LaserMachineBlockEntity extends AENetworkedBlockEntity implements IActionHost, FrequencyBindingHost {
    public static final long RECEIVER_FE_CAPACITY = 1_024_000_000_000_000_000L;
    public static final long MACHINE_FE_CAPACITY = 1_000_000_000L;
    public static final long PARTICLE_CAPACITY = 125_000L;
    public static final int SIDE_OFF = 0, SIDE_INPUT = 1, SIDE_OUTPUT = 2;
    private static final int HORIZONTAL_MASK = 60;
    private static final ResourceLocation OVERLOADED_INTERFACE = ResourceLocation.parse("ae2lt:overloaded_interface");
    private UUID identity = UUID.randomUUID();
    private LaserStructure structure;
    private boolean structureDirty = true, wasFormed, visualFormed, visualNetworkActive, explosionTriggered;
    private boolean receiverRebuildPending;
    private boolean receiverPedestalLayout = true;
    // Pre-alpha.9 anchors and physical poles stay intact; the optical network ignores vertical poles.
    private boolean deflectorLegacyLayout;
    private int opticalInputMask;
    private final Object receiverVisualToken = new Object();
    private BlockPos visualCenter;
    private Direction visualFacing;
    private long refundExtreme;
    private long networkFeCredit;
    private UUID networkFeRefundIdentity = UUID.randomUUID();
    private final LongEnergyBuffer energy;
    private final LaserGridBridge bridge = new LaserGridBridge(this);
    private final FrequencyBindingAccess frequency = FrequencyApi.createBinding(this);
    private final LaserLightManager lights = new LaserLightManager(this);
    // Old binding data is saved by frequency, but every externally accessible core operation is inert.
    private final FrequencyBindingAccess coreBinding = new FrequencyBindingAccess() {
        @Override public int getFrequencyId() { return -1; }
        @Override public void setFrequency(int id) {}
        @Override public void clearFrequency() {}
        @Override public boolean isConnected() { return false; }
        @Override public void serverTick() {}
        @Override public void onReady() {}
        @Override public void setRemoved() {}
        @Override public void clearRemoved() {}
        @Override public void onMainNodeStateChanged(IGridNodeListener.State state) {}
        @Override public void save(CompoundTag tag) { frequency.save(tag); }
        @Override public void load(CompoundTag tag) { frequency.load(tag); }
        @Override public int getGridUsedChannels() { return 0; }
        @Override public int getGridMaxChannels() { return 0; }
    };
    // NeoForge follows onChunkUnloaded with setRemoved; that second callback is not a permanent deletion.
    private boolean frequencyUnloading;
    double aeInputFraction, aeOutputFraction;

    boolean enabled = true;
    long boost = 100_000, extremeLightning, coreRemainder, activeUntil, lastPower;
    private final dev.overloadsim.core.CoreCollapseCycle collapse = new dev.overloadsim.core.CoreCollapseCycle();
    int outputMask = 63, fuelRemaining, particleRemaining;
    long fuelProcessingCredit;
    boolean conversionInProgress;
    String activeFuel = "", status = "no_structure";
    Direction particleDirection = Direction.SOUTH;
    List<LaserSegment> segments = new ArrayList<>();
    final ParticleBuffer particles = new ParticleBuffer(MachineSettings.longValue(MachineSettings.RECEIVER_PARTICLE_CAPACITY),1_000_000_000L);
    private long visualCoreThreshold=LaserMath.CORE_THRESHOLD;

    private final ItemStackHandler fuelInput = new ItemStackHandler(4) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return kind() == LaserKind.RECEIVER && ParticleFuelData.match(stack).isPresent(); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public ItemStack getStackInSlot(int slot) { return super.getStackInSlot(slot).copy(); }
        @Override protected void onContentsChanged(int slot) { saveChanges(); }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return fuelInput.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return fuelInput.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return fuelInput.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return fuelInput.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return fuelInput.isItemValid(slot, stack); }
    };

    private final IEnergyStorage feCapability = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            if (amount <= 0 || !canReceive()) return 0;
            int received = (int) energy.insert(amount, simulate);
            if (received > 0 && !simulate) saveChanges();
            return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            if (amount <= 0 || !canExtract()) return 0;
            long available = Math.max(0, energy.stored() - (aeOutputFraction > 0 ? 1 : 0));
            int extracted = (int) energy.extract(Math.min(amount, available), simulate);
            if (extracted > 0 && !simulate) saveChanges();
            return extracted;
        }
        @Override public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, energy.stored()); }
        @Override public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, energy.capacity()); }
        @Override public boolean canExtract() { return kind() == LaserKind.RECEIVER && formed() && !explosionTriggered; }
        @Override public boolean canReceive() { return kind() != LaserKind.RECEIVER && kind() != LaserKind.CORE && !explosionTriggered && !isRemoved() && level instanceof ServerLevel
                && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == LaserMachineBlockEntity.this; }
    };

    public LaserMachineBlockEntity(BlockPos pos, BlockState state) {
        super(LaserContent.MACHINE_ENTITY.get(), pos, state);
        energy = new LongEnergyBuffer(configuredEnergyCapacity(),RECEIVER_FE_CAPACITY);
        if (kind() == LaserKind.DEFLECTOR) outputMask = 0;
        particleDirection = forward();
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(kind() == LaserKind.CORE ? 0 : 2).setVisualRepresentation(state.getBlock().asItem());
        if (kind() == LaserKind.CORE) getMainNode().setExposedOnSides(Set.of());
    }

    public LaserKind kind() { return ((LaserMachineBlock) getBlockState().getBlock()).kind(); }
    public Direction forward() { return getBlockState().getValue(LaserMachineBlock.FACING).getOpposite(); }
    public BlockPos center() {
        if (level != null && level.isClientSide && visualCenter != null) return visualCenter;
        return structure == null ? worldPosition.offset(LaserStructureGeometry.centerOffset(kind(), forward().getOpposite(), receiverPedestalLayout(), deflectorLegacyLayout())) : structure.center();
    }
    public boolean receiverPedestalLayout() { return kind() == LaserKind.RECEIVER && receiverPedestalLayout; }
    public boolean deflectorLegacyLayout() { return kind() == LaserKind.DEFLECTOR && deflectorLegacyLayout; }
    public Map<BlockPos, LaserStructureGeometry.Part> structureTemplate() {
        var facing = level != null && level.isClientSide ? receiverVisualFacing() : getBlockState().getValue(LaserMachineBlock.FACING);
        var canonical = LaserStructureGeometry.template(kind(), facing, receiverPedestalLayout(), deflectorLegacyLayout());
        return kind() == LaserKind.RECEIVER ? ReceiverOpticalPorts.resolve(this, canonical) : canonical;
    }
    public boolean acceptsParticleInput(BlockPos port, Direction incoming) { return ReceiverOpticalPorts.accepts(this, port, incoming); }
    public long networkFeCredit() { return networkFeCredit; }
    public void setNetworkFeCredit(long value) { networkFeCredit = Math.max(0, value); saveChanges(); }
    void claimNetworkFeRefund() {
        if (isRemoved()) return;
        long restored = AppliedFluxRefunds.claim(level, networkFeRefundIdentity);
        if (restored > 0) setNetworkFeCredit(Math.addExact(networkFeCredit, restored));
    }
    void retainNetworkFeRefund(long amount) {
        if (amount <= 0) return;
        if (isRemoved()) AppliedFluxRefunds.deposit(level, networkFeRefundIdentity, amount);
        else setNetworkFeCredit(Math.addExact(networkFeCredit, amount));
    }
    public Direction receiverVisualFacing() { return visualFacing == null ? getBlockState().getValue(LaserMachineBlock.FACING) : visualFacing; }
    public boolean receiverSkinVisible() { return ReceiverShellVisuals.visible(this, receiverVisualToken); }
    public void publishReceiverVisuals() { ReceiverShellVisuals.publish(this, receiverVisualToken); }
    public UUID identity() { return identity; }
    public LaserStructure structure() { return structure; }
    public boolean formed() { return level != null && level.isClientSide ? visualFormed && !isRemoved()
            : structure != null && loaded() && !explosionTriggered && !isRemoved() && level.getBlockEntity(worldPosition) == this; }
    public boolean networkActive() { return kind() != LaserKind.CORE && (level != null && level.isClientSide ? visualNetworkActive : getMainNode().isActive()); }
    public boolean enabled() { return enabled; }
    public long boost() {
        if(level!=null&&level.isClientSide)return boost;
        long minimum=MachineSettings.boostMinimum(),step=MachineSettings.boostStep();
        return minimum+(Math.clamp(boost,minimum,MachineSettings.boostMaximum())-minimum)/step*step;
    }
    public long coreThreshold(){return level!=null&&level.isClientSide?visualCoreThreshold:MachineSettings.longValue(MachineSettings.CORE_ACTIVATION_THRESHOLD);}
    private long configuredEnergyCapacity(){return MachineSettings.longValue(kind()==LaserKind.RECEIVER?MachineSettings.RECEIVER_ENERGY_CAPACITY:MachineSettings.LASER_MACHINE_ENERGY_CAPACITY);}
    private void refreshCapacities(){if(level==null||!level.isClientSide){energy.setCapacity(configuredEnergyCapacity());particles.setCapacity(MachineSettings.longValue(MachineSettings.RECEIVER_PARTICLE_CAPACITY));}}
    public long extremeLightning() { return extremeLightning; }
    public long coreRemainder() { return coreRemainder; }
    public long lastPower() { return lastPower; }
    public long currentPower() { return level != null && enabled && formed() && activeUntil > level.getGameTime() ? lastPower : 0; }
    public long collapseStartTick() { return collapse.startTick(); }
    void stopCollapseVisuals(){collapse.stop();}
    public long activeUntil() { return activeUntil; }
    public int outputMask() { return outputMask; }
    public int inputMask() { return kind() == LaserKind.DEFLECTOR ? opticalInputMask : 0; }
    /** World-facing modes: 0=off, 1=input, 2=output. Vertical faces are always off. */
    public int sideMode(Direction side) {
        if (kind() != LaserKind.DEFLECTOR || side == null || side.getAxis() == Direction.Axis.Y) return SIDE_OFF;
        int bit = 1 << side.get3DDataValue();
        return (opticalInputMask & bit) != 0 ? SIDE_INPUT : (outputMask & bit) != 0 ? SIDE_OUTPUT : SIDE_OFF;
    }
    public int fuelRemaining() { return fuelRemaining; }
    public int particleRemaining() { return particleRemaining; }
    public long fuelProcessingCredit() { return fuelProcessingCredit; }
    public String activeFuel() { return activeFuel; }
    public String status() { return status; }
    public Direction particleDirection() { return particleDirection; }
    public List<LaserSegment> segments() { return segments; }
    public ParticleBuffer particles() { refreshCapacities();return particles; }
    public LongEnergyBuffer energy() { refreshCapacities();return energy; }
    public ItemStackHandler fuelInput() { return fuelInput; }
    public IItemHandler automation() { return automation; }
    public IEnergyStorage feCapability() { return feCapability; }
    public LaserGridBridge bridge() { return bridge; }
    public boolean explosionTriggered() { return explosionTriggered; }

    public void setEnabled(boolean value) { enabled = value; LaserNetwork.unregister(this); clearBeam(); if(!value)collapse.stop(); sync(); }
    public void setBoost(long value) { if (value < MachineSettings.boostMinimum() || value > MachineSettings.boostMaximum() || (value-MachineSettings.boostMinimum())%MachineSettings.boostStep()!=0) return; boost = value; LaserNetwork.unregister(this); clearBeam(); sync(); }
    public void setOutputMask(int value) {
        if (value < 0 || value > 63) return;
        outputMask = kind() == LaserKind.DEFLECTOR ? value & HORIZONTAL_MASK & ~opticalInputMask : value;
        LaserNetwork.unregister(this); clearBeam(); sync();
    }
    public void setSideMode(Direction side, int mode) {
        if (kind() != LaserKind.DEFLECTOR || side == null || side.getAxis() == Direction.Axis.Y || mode < SIDE_OFF || mode > SIDE_OUTPUT) return;
        int bit = 1 << side.get3DDataValue();
        opticalInputMask &= ~bit; outputMask &= ~bit;
        if (mode == SIDE_INPUT) opticalInputMask |= bit;
        else if (mode == SIDE_OUTPUT) outputMask |= bit;
        LaserNetwork.unregister(this); clearBeam(); sync();
    }
    public void setExtremeLightning(long value) { extremeLightning = Math.max(0, value); saveChanges(); }
    public boolean markExplosionTriggered() {
        if (explosionTriggered) return false;
        explosionTriggered = true; enabled = false; clearBeam(); sync();
        return true;
    }
    public void markStructureDirty() { structureDirty = true; }
    private void clearBeam() { lights.clear(); segments = new ArrayList<>(); activeUntil = 0; lastPower = 0; collapse.stop(); }
    public void refreshLaserLights() { lights.refresh(); }
    public void sync() { saveChanges(); markForClientUpdate(); }

    public boolean loaded() {
        if (level == null || structure == null) return false;
        for (var member : structure.members()) if (!level.hasChunkAt(member)) return false;
        return true;
    }

    /** <=441 cell reads, each guarded against chunk loading. AIR cells reserve the aperture too. */
    public void checkStructure() {
        if (level == null || level.isClientSide || isRemoved() || explosionTriggered) return;
        LaserStructureIndex.register(this);
        if (kind() == LaserKind.CORE) {
            var next = new LaserStructure(worldPosition, List.of(worldPosition), null);
            if (!next.equals(structure)) { structure = next; wasFormed = true; LaserStructureIndex.bind(this, next); status = "idle"; sync(); }
            structureDirty = false;
            return;
        }
        var template = structureTemplate();
        var members = new ArrayList<BlockPos>(template.size());
        BlockPos networkInterface = null;
        String error = "";
        for (var entry : template.entrySet()) {
            BlockPos pos = worldPosition.offset(entry.getKey());
            if (!level.hasChunkAt(pos)) { error = "unloaded"; break; }
            if (LaserStructureIndex.occupied(level, pos, this)||dev.overloadsim.workstation.WorkstationStructureIndex.occupiedByOther(level,pos,this)) { error = "owned"; break; }
            if (LaserStructureIndex.touching(level, pos, this)) { error = "spacing"; break; }
            var state = level.getBlockState(pos);
            boolean match;
            switch (entry.getValue()) {
                case CONTROLLER -> match = pos.equals(worldPosition) && state.is(LaserContent.machine(kind())) && level.getBlockEntity(pos) == this;
                case AIR -> match = LaserLightManager.isBeamAir(state);
                case GLASS -> match = state.is(LaserContent.GLASS.get());
                case SOURCE -> match = state.is(LaserContent.SOURCE.get());
                case PORT -> match = state.is(LaserContent.PORT.get());
                case FRAME -> {
                    match = state.is(LaserContent.FRAME.get());
                    if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(OVERLOADED_INTERFACE)) {
                        var relative = pos.subtract(center()); var normal = forward().getNormal();
                        boolean upstream = relative.getX() * normal.getX() + relative.getY() * normal.getY() + relative.getZ() * normal.getZ() <= 0;
                        if (networkInterface != null || !upstream) { error = "interface_position"; match = false; }
                        else { match = true; networkInterface = pos.immutable(); }
                    }
                }
                default -> throw new IllegalStateException("unknown laser structure part");
            }
            if (!match) { if (error.isEmpty()) error = entry.getValue().name().toLowerCase(java.util.Locale.ROOT); break; }
            members.add(pos.immutable());
        }
        structureDirty = false;
        if (!error.isEmpty()) {
            boolean previouslyFormed = structure != null || wasFormed;
            boolean chargedBreak = !receiverRebuildPending && previouslyFormed && !error.equals("unloaded") && kind() == LaserKind.RECEIVER && particles.stored() > 0;
            String previousStatus = status;
            invalidateStructure(); status = receiverRebuildPending && !error.equals("unloaded") ? "receiver_rebuild_required" : error;
            if (error.equals("unloaded")) wasFormed = previouslyFormed;
            if (chargedBreak) LaserExplosion.trigger(this);
            if (receiverRebuildPending && !status.equals(previousStatus)) sync();
            return;
        }
        var next = new LaserStructure(worldPosition.offset(LaserStructureGeometry.centerOffset(kind(), forward().getOpposite(), receiverPedestalLayout(), deflectorLegacyLayout())), members, networkInterface);
        boolean changed = !next.equals(structure);
        structure = next; wasFormed = true;
        receiverRebuildPending = false; // A valid sphere rearms the normal charged-containment safety policy.
        if (changed) LaserStructureIndex.bind(this, next);
        if (changed) { status = "idle"; sync(); }
    }

    public void invalidateStructure() {
        boolean changed = structure != null || !segments.isEmpty() || activeUntil != 0;
        LaserNetwork.unregister(this);
        structure = null; wasFormed = false; clearBeam();
        LaserStructureIndex.release(this); bridge.disconnect();
        if (changed) sync();
    }

    public void memberRemoved() {
        if (level == null || level.isClientSide) return;
        boolean charged = !receiverRebuildPending && kind() == LaserKind.RECEIVER && particles.stored() > 0;
        invalidateStructure(); status = "structure_changed"; structureDirty = true;
        if (charged) LaserExplosion.trigger(this);
    }

    public void tick() {
        if (level == null || level.isClientSide || isRemoved()) return;
        if (kind() != LaserKind.CORE) frequency.serverTick();
        // Every-tick validation covers commands, automation, unloaded chunks and cancelled placement events.
        checkStructure(); bridge.refresh();
        if (refundExtreme > 0) {
            long returned = bridge.insertExtreme(refundExtreme);
            if (returned > 0) { refundExtreme -= returned; saveChanges(); }
        }
        if (!formed()) { lights.clear(); if (!segments.isEmpty() || activeUntil != 0) { clearBeam(); sync(); } return; }
        bridge.charge();
        if (kind() == LaserKind.RECEIVER) { ParticleConversion.tick(this); bridge.export(); }
        LaserNetwork.tick(this);
        if (activeUntil > 0 && level.getGameTime() >= activeUntil && (!segments.isEmpty() || lastPower > 0)) { clearBeam(); sync(); }
        lights.refresh();
        if (level.getGameTime() % 20 == 0) sync();
    }

    /** Simulation never mutates. Partial real extraction is refunded before any FE or local EHV is committed. */
    public boolean tryPay(long fe, long ehv, boolean simulate) {
        if (fe < 0 || ehv < 0 || energy.stored() < fe || explosionTriggered || isRemoved() || level == null || level.isClientSide
                || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return false;
        long local = Math.min(extremeLightning, ehv), needed = ehv - local;
        if (needed > 0 && bridge.extractExtreme(needed, true) != needed) return false;
        if (simulate) return true;
        if (needed > 0) {
            long drawn = bridge.extractExtreme(needed, false);
            if (drawn != needed || energy.stored() < fe || extremeLightning < local || explosionTriggered || isRemoved()) {
                long returned = bridge.insertExtreme(drawn);
                refundExtreme = saturatingAdd(refundExtreme, drawn - returned);
                saveChanges(); return false;
            }
        }
        energy.extract(fe, false); extremeLightning -= local;
        saveChanges(); return true;
    }

    public long acceptCore(long fe, Direction incoming, long tick) {
        return acceptCore(fe,incoming,tick,()->fe);
    }
    /** Revalidate the still-connected paid contribution after extension callbacks, before committing core resources. */
    public long acceptCore(long fe, Direction incoming, long tick, java.util.function.LongSupplier validEnergy) {
        if (!(level instanceof ServerLevel server) || kind() != LaserKind.CORE || !formed() || !enabled || incoming == null || fe < coreThreshold() || fe > MachineSettings.beamLimit() || tick < 0) return 0;
        long previousRemainder = coreRemainder;
        var accumulation = LaserMath.accumulate(previousRemainder, fe);
        var event = new LaserEvents.BeforeCoreEmission(server, worldPosition, fe, accumulation.particles(), accumulation.remainder());
        if (NeoForge.EVENT_BUS.post(event).isCanceled() || isRemoved() || !formed() || !enabled || explosionTriggered || coreRemainder != previousRemainder
                || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return 0;
        long remainingFe = Math.clamp(validEnergy.getAsLong(),0L,fe);
        if(remainingFe<coreThreshold()||coreRemainder!=previousRemainder||isRemoved()||!formed()||!enabled||explosionTriggered
                ||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this)return 0;
        var remaining = LaserMath.accumulate(previousRemainder,remainingFe);
        long produced = event.particles()==accumulation.particles()?remaining.particles():Math.min(event.particles(),remaining.particles());
        long remainder = event.remainder()==accumulation.remainder()?remaining.remainder():Math.min(event.remainder(),remaining.remainder());
        coreRemainder = remainder; lastPower = remainingFe; particleDirection = incoming;
        activeUntil = saturatingAdd(tick, 21);
        if(collapse.accept(tick))server.playSound(null,worldPosition,LaserContent.COLLAPSE_TRANSITION.get(),net.minecraft.sounds.SoundSource.BLOCKS,.65f,1);
        sync(); return produced;
    }

    public boolean acceptParticles(long count) {
        if (!(level instanceof ServerLevel) || count < 0 || kind() != LaserKind.RECEIVER || !formed() || !enabled || explosionTriggered) return false;
        refreshCapacities();
        if(particles.stored()>particles.capacity()){status="particles_above_capacity";sync();return false;}
        if (!particles.receive(count)) { status = "particle_overflow"; LaserExplosion.trigger(this); sync(); return false; }
        sync(); return true;
    }

    public CompoundTag saveMachine(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putUUID("Identity", identity); tag.putBoolean("Enabled", enabled); tag.putLong("Boost", boost);
        tag.putLong("ExtremeLightning", extremeLightning); tag.putLong("RefundExtreme", refundExtreme);
        tag.putLong("NetworkFeCredit", networkFeCredit);
        tag.putUUID("NetworkFeRefundIdentity", networkFeRefundIdentity);
        tag.putLong("CoreRemainder", coreRemainder); tag.putLong("ActiveUntil", activeUntil); tag.putLong("LastPower", lastPower);
        tag.putLong("CollapseStart",collapse.startTick()); tag.putLong("CollapseInput",collapse.lastInputTick());
        tag.putLong("Energy", energy.stored()); tag.putLong("Particles", particles.stored()); tag.putInt("OutputMask", outputMask);
        tag.putLong("EnergyCapacity",Math.max(energy.capacity(),energy.stored()));
        tag.putLong("ParticleCapacity",Math.max(particles.capacity(),particles.stored()));
        tag.putInt("FuelRemaining", fuelRemaining); tag.putInt("ParticleRemaining", particleRemaining); tag.putString("ActiveFuel", activeFuel);
        tag.putLong("FuelProcessingCredit", fuelProcessingCredit);
        tag.putString("Status", status); tag.putInt("ParticleDirection", particleDirection.get3DDataValue());
        tag.put("FuelInput", fuelInput.serializeNBT(registries)); tag.putBoolean("HadStructure", structure != null || wasFormed);
        tag.putBoolean("ExplosionTriggered", explosionTriggered); tag.putDouble("AeInputFraction", aeInputFraction); tag.putDouble("AeOutputFraction", aeOutputFraction);
        if (kind() == LaserKind.RECEIVER) {
            tag.putBoolean("ReceiverPedestalLayout", receiverPedestalLayout);
            tag.putInt("ReceiverShapeVersion", 1);
            tag.putBoolean("ReceiverRebuildPending", receiverRebuildPending);
        }
        if (kind() == LaserKind.DEFLECTOR) {
            tag.putInt("OpticalInputMask", opticalInputMask); tag.putInt("OpticalOutputMask", outputMask);
            tag.putInt("DeflectorShapeVersion", 1); tag.putBoolean("DeflectorLegacyLayout", deflectorLegacyLayout);
        }
        return tag;
    }

    public void loadMachine(CompoundTag tag, HolderLookup.Provider registries) {
        lights.clear();
        LaserNetwork.unregister(this);
        LaserStructureIndex.release(this); structure = null; structureDirty = true;
        if (tag.hasUUID("Identity")) identity = tag.getUUID("Identity");
        enabled = kind() == LaserKind.CORE || !tag.contains("Enabled") || tag.getBoolean("Enabled");
        boost = tag.contains("Boost") ? Math.clamp(tag.getLong("Boost"), 1L, 1_000_000_000L) : 100_000L;
        extremeLightning = Math.max(0, tag.getLong("ExtremeLightning")); refundExtreme = Math.max(0, tag.getLong("RefundExtreme"));
        networkFeCredit = Math.max(0, tag.getLong("NetworkFeCredit"));
        if (tag.hasUUID("NetworkFeRefundIdentity")) networkFeRefundIdentity = tag.getUUID("NetworkFeRefundIdentity");
        coreRemainder = Math.clamp(tag.getLong("CoreRemainder"), 0L, LaserMath.PARTICLE_COST - 1);
        activeUntil = Math.max(0, tag.getLong("ActiveUntil")); lastPower = Math.clamp(tag.getLong("LastPower"), 0L, LaserMath.MAX_BEAM);
        collapse.restore(tag.contains("CollapseStart")?tag.getLong("CollapseStart"):-1,tag.contains("CollapseInput")?tag.getLong("CollapseInput"):-1);
        refreshCapacities();
        energy.restore(Math.max(0,tag.getLong("Energy")),tag.contains("EnergyCapacity")?Math.clamp(tag.getLong("EnergyCapacity"),1L,RECEIVER_FE_CAPACITY):kind()==LaserKind.RECEIVER?RECEIVER_FE_CAPACITY:MACHINE_FE_CAPACITY);
        particles.restore(Math.max(0,tag.getLong("Particles")),tag.contains("ParticleCapacity")?Math.clamp(tag.getLong("ParticleCapacity"),1L,1_000_000_000L):PARTICLE_CAPACITY);
        outputMask = tag.contains("OutputMask") ? tag.getInt("OutputMask") & 63 : 63;
        if (kind() == LaserKind.DEFLECTOR) {
            // This independent marker preserves valid old shells without replacing blocks or charging materials.
            deflectorLegacyLayout = tag.getInt("DeflectorShapeVersion") < 1 || tag.getBoolean("DeflectorLegacyLayout");
            if (tag.contains("OpticalInputMask") && tag.contains("OpticalOutputMask")) {
                opticalInputMask = tag.getInt("OpticalInputMask") & HORIZONTAL_MASK;
                outputMask = tag.getInt("OpticalOutputMask") & HORIZONTAL_MASK & ~opticalInputMask;
            } else {
                Direction legacyInput = forward().getOpposite();
                opticalInputMask = legacyInput.getAxis() == Direction.Axis.Y ? 0 : 1 << legacyInput.get3DDataValue();
                outputMask = (tag.contains("OutputMask") || opticalInputMask != 0 ? outputMask : 0) & HORIZONTAL_MASK & ~opticalInputMask;
            }
        }
        fuelRemaining = Math.clamp(tag.getInt("FuelRemaining"), 0, ParticleClock.MAX_DURATION);
        particleRemaining = Math.clamp(tag.getInt("ParticleRemaining"), 0, ParticleClock.MAX_DURATION);
        fuelProcessingCredit = Math.clamp(tag.getLong("FuelProcessingCredit"), 0L, ParticleClock.PROCESSING_UNIT - 1);
        activeFuel = boundedText(tag.getString("ActiveFuel")); status = boundedText(tag.getString("Status"));
        int direction = tag.getInt("ParticleDirection"); particleDirection = direction >= 0 && direction < 6 ? Direction.from3DDataValue(direction) : forward();
        var fuelTag = tag.getCompound("FuelInput").copy(); fuelTag.putInt("Size", 4);
        fuelInput.deserializeNBT(registries, fuelTag);
        wasFormed = tag.getBoolean("HadStructure"); explosionTriggered = tag.getBoolean("ExplosionTriggered");
        // The independent layout marker is absent in all pre-pedestal saves, including valid charged spheres.
        receiverPedestalLayout = kind() == LaserKind.RECEIVER && tag.getBoolean("ReceiverPedestalLayout");
        // Old cubic receivers must pause for reconstruction, not explode merely because their saved template changed.
        // This flag survives intermediate saves until a valid sphere forms; no resources or paid clocks are reset.
        receiverRebuildPending = kind() == LaserKind.RECEIVER && (tag.getBoolean("ReceiverRebuildPending")
                || tag.getInt("ReceiverShapeVersion") < 1 && (wasFormed || particles.stored() > 0));
        aeInputFraction = fraction(tag.getDouble("AeInputFraction")); aeOutputFraction = fraction(tag.getDouble("AeOutputFraction"));
        segments = new ArrayList<>(); // Light paths are recomputed after loaded-world validation; saved paths cannot survive a stale owner.
    }

    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); frequency.save(tag); tag.put("LaserMachine", saveMachine(registries)); }
    @Override public void loadTag(CompoundTag tag, HolderLookup.Provider registries) { super.loadTag(tag, registries); frequency.load(tag); loadMachine(tag.getCompound("LaserMachine"), registries); }

    @Override protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(formed()); data.writeBoolean(networkActive()); data.writeBoolean(enabled); data.writeBoolean(explosionTriggered);
        data.writeBoolean(receiverPedestalLayout());
        data.writeBoolean(deflectorLegacyLayout());
        data.writeBlockPos(center()); data.writeByte(getBlockState().getValue(LaserMachineBlock.FACING).get3DDataValue());
        data.writeLong(boost()); data.writeLong(extremeLightning); data.writeLong(coreRemainder); data.writeLong(activeUntil); data.writeLong(lastPower);
        data.writeLong(coreThreshold());data.writeLong(energy.capacity());data.writeLong(particles.capacity());
        data.writeLong(collapse.startTick()); data.writeLong(collapse.lastInputTick());
        data.writeLong(energy.stored()); data.writeLong(particles.stored()); data.writeByte(outputMask); data.writeByte(inputMask());
        data.writeVarInt(fuelRemaining); data.writeVarInt(particleRemaining); data.writeUtf(boundedText(activeFuel), 256); data.writeUtf(boundedText(status), 256);
        data.writeByte(particleDirection.get3DDataValue());
        int count = Math.min(128, segments.size()); data.writeVarInt(count);
        for (int index = 0; index < count; index++) segments.get(index).write(data);
    }

    @Override protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        super.readFromStream(data);
        visualFormed = data.readBoolean(); visualNetworkActive = data.readBoolean(); enabled = data.readBoolean(); explosionTriggered = data.readBoolean();
        receiverPedestalLayout = data.readBoolean();
        deflectorLegacyLayout = data.readBoolean();
        visualCenter = data.readBlockPos();
        int facing = data.readUnsignedByte(); visualFacing = facing < 6 ? Direction.from3DDataValue(facing) : getBlockState().getValue(LaserMachineBlock.FACING);
        boost = Math.clamp(data.readLong(), 1L, 1_000_000_000L); extremeLightning = Math.max(0, data.readLong());
        coreRemainder = Math.clamp(data.readLong(), 0L, LaserMath.PARTICLE_COST - 1); activeUntil = Math.max(0, data.readLong()); lastPower = Math.clamp(data.readLong(), 0L, LaserMath.MAX_BEAM);
        visualCoreThreshold=Math.clamp(data.readLong(),1L,LaserMath.MAX_BEAM);
        energy.setCapacity(Math.clamp(data.readLong(),1L,RECEIVER_FE_CAPACITY));particles.setCapacity(Math.clamp(data.readLong(),1L,1_000_000_000L));
        collapse.restore(data.readLong(),data.readLong());
        energy.restore(Math.max(0, data.readLong()),RECEIVER_FE_CAPACITY); particles.restore(Math.max(0, data.readLong()),1_000_000_000L); outputMask = data.readUnsignedByte() & 63;
        opticalInputMask = data.readUnsignedByte() & HORIZONTAL_MASK;
        if (kind() == LaserKind.DEFLECTOR) outputMask &= HORIZONTAL_MASK & ~opticalInputMask;
        fuelRemaining = Math.clamp(data.readVarInt(), 0, ParticleClock.MAX_DURATION); particleRemaining = Math.clamp(data.readVarInt(), 0, ParticleClock.MAX_DURATION);
        activeFuel = data.readUtf(256); status = data.readUtf(256);
        int direction = data.readUnsignedByte(); particleDirection = direction < 6 ? Direction.from3DDataValue(direction) : forward();
        int count = data.readVarInt(); if (count < 0 || count > 128) throw new IllegalArgumentException("laser segment count");
        var next = new ArrayList<LaserSegment>(count); for (int index = 0; index < count; index++) next.add(LaserSegment.read(data));
        segments = next;
        publishReceiverVisuals();
        return true;
    }

    @Override protected void saveVisualState(CompoundTag tag) {
        super.saveVisualState(tag);
        tag.putBoolean("LaserFormed", formed()); tag.putBoolean("LaserNetwork", networkActive());
        tag.putBoolean("LaserReceiverPedestalLayout", receiverPedestalLayout());
        tag.putLong("LaserCenter", center().asLong()); tag.putInt("LaserFacing", getBlockState().getValue(LaserMachineBlock.FACING).get3DDataValue());
        if (level != null) tag.put("LaserMachine", saveMachine(level.registryAccess()));
    }
    @Override protected void loadVisualState(CompoundTag tag) {
        super.loadVisualState(tag);
        visualFormed = tag.getBoolean("LaserFormed"); visualNetworkActive = tag.getBoolean("LaserNetwork");
        receiverPedestalLayout = kind() == LaserKind.RECEIVER && tag.getBoolean("LaserReceiverPedestalLayout");
        visualCenter = tag.contains("LaserCenter") ? BlockPos.of(tag.getLong("LaserCenter")) : null;
        int facing = tag.getInt("LaserFacing");
        visualFacing = tag.contains("LaserFacing") && facing >= 0 && facing < 6 ? Direction.from3DDataValue(facing) : getBlockState().getValue(LaserMachineBlock.FACING);
        if (level != null && tag.contains("LaserMachine")) loadMachine(tag.getCompound("LaserMachine"), level.registryAccess());
        publishReceiverVisuals();
    }

    @Override public void onLoad() {
        frequencyUnloading = false;
        super.onLoad();
        publishReceiverVisuals();
        if (level != null && !level.isClientSide) { clearBeam(); LaserStructureIndex.register(this); LaserNetwork.register(this); checkStructure(); }
    }
    @Override public AENetworkedBlockEntity getFrequencyBindingBlockEntity() { return this; }
    @Override public FrequencyBindingAccess getFrequencyBindingAccess() { return kind() == LaserKind.CORE ? coreBinding : frequency; }
    @Override public void saveFrequencyBindingChanges() { saveChanges(); }
    @Override public void markFrequencyBindingForUpdate() { markForClientUpdate(); }
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason) { if (kind() != LaserKind.CORE) frequency.onMainNodeStateChanged(reason); }
    @Override public void onReady() { super.onReady(); if (kind() != LaserKind.CORE) frequency.onReady(); }
    @Override public void setRemoved() { if (!frequencyUnloading) frequency.setRemoved(); ReceiverShellVisuals.remove(this, receiverVisualToken); LaserStructureIndex.unregister(this); LaserNetwork.unregister(this); bridge.disconnect(); clearBeam(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { frequencyUnloading = true; FrequencyBindingLifecycle.chunkUnloaded(frequency); ReceiverShellVisuals.remove(this, receiverVisualToken); LaserStructureIndex.unregister(this); LaserNetwork.unregister(this); bridge.disconnect(); clearBeam(); super.onChunkUnloaded(); }
    @Override public void clearRemoved() { frequencyUnloading = false; super.clearRemoved(); if (kind() != LaserKind.CORE) frequency.clearRemoved(); }
    @Override public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder, Player player) {
        super.exportSettings(mode, builder, player);
        if (kind() != LaserKind.CORE) frequency.exportMemorySettings(mode, builder, tag -> {});
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            claimNetworkFeRefund();
            if (networkFeCredit > 0 || bridge.feTransferInProgress()) AppliedFluxRefunds.writeItem(builder, networkFeRefundIdentity, networkFeCredit);
        }
    }
    @Override public void importSettings(SettingsFrom mode, DataComponentMap settings, Player player) {
        super.importSettings(mode, settings, player);
        if (kind() != LaserKind.CORE) frequency.importMemorySettings(mode, settings, tag -> {});
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var refund = AppliedFluxRefunds.readItem(settings);
            if (refund != null) {
                networkFeRefundIdentity = refund.identity(); networkFeCredit = refund.credit();
                claimNetworkFeRefund(); saveChanges();
            }
        }
    }
    @Override protected void onOrientationChanged(BlockOrientation orientation) { super.onOrientationChanged(orientation); invalidateStructure(); structureDirty = true; }
    @Override public IGridNode getActionableNode() { return kind() == LaserKind.CORE ? null : getMainNode().getNode(); }
    @Override public Set<Direction> getGridConnectableSides(BlockOrientation orientation) { return kind() == LaserKind.CORE ? Set.of() : EnumSet.allOf(Direction.class); }
    @Override public AECableType getCableConnectionType(Direction side) { return kind() == LaserKind.CORE ? AECableType.NONE : AECableType.SMART; }
    @Override protected Item getItemFromBlockEntity() { return getBlockState().getBlock().asItem(); }

    private static String boundedText(String text) { return text.length() > 256 ? text.substring(0, 256) : text; }
    private static double fraction(double value) { return Double.isFinite(value) && value >= 0 && value < 1 ? value : 0; }
    private static long saturatingAdd(long first, long second) { return second > Long.MAX_VALUE - first ? Long.MAX_VALUE : first + second; }
}
