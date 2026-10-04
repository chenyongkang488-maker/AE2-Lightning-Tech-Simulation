package dev.overloadsim.laser;

public enum LaserKind {
    EMITTER("high_energy_laser_emitter"),
    CONSTRAINT("laser_constraint_matrix"),
    ENHANCEMENT("laser_enhancement_matrix"),
    BURST("laser_burst_matrix"),
    DEFLECTOR("laser_deflector_matrix"),
    FOCUS("laser_focus_matrix"),
    CORE("collapse_core"),
    RECEIVER("particle_constrainer");

    private final String id;
    LaserKind(String id) { this.id = id; }
    public String id() { return id; }
}
