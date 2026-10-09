package dev.overloadsim.workstation;

import dev.overloadsim.config.MachineSettings;

/** One coherent set of operating limits for selection and settlement in a server tick. */
record WorkstationSettings(long laserThreshold,int maxParallel,int cycleTicks,int slotCapacity,
        int tankCapacity,int itemExportLimit,int fluidExportLimit) {
    static WorkstationSettings capture() {
        return new WorkstationSettings(MachineSettings.longValue(MachineSettings.WORKSTATION_LASER_THRESHOLD),
                MachineSettings.intValue(MachineSettings.WORKSTATION_MAX_PARALLEL),
                MachineSettings.intValue(MachineSettings.WORKSTATION_CYCLE_TICKS),
                slotLimit(),tankLimit(),MachineSettings.intValue(MachineSettings.WORKSTATION_ITEM_EXPORT_LIMIT),
                MachineSettings.intValue(MachineSettings.WORKSTATION_FLUID_EXPORT_LIMIT));
    }
    static int slotLimit(){return MachineSettings.intValue(MachineSettings.WORKSTATION_SLOT_CAPACITY);}
    static int tankLimit(){return MachineSettings.intValue(MachineSettings.WORKSTATION_TANK_CAPACITY);}
}
