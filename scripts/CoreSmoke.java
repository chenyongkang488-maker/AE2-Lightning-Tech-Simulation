import dev.overloadsim.core.SimulationRules;
public class CoreSmoke {
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        check(SimulationRules.parallel(0)==1);
        check(SimulationRules.parallel(1)==4);
        check(SimulationRules.parallel(32)==128);
        check(SimulationRules.duration(200,4)==13);
        check(SimulationRules.duration(1,4)==1);
        check(SimulationRules.chance(.099,.1));
        check(!SimulationRules.chance(.1,.1));
        check(SimulationRules.withinRadius(25,5));
        check(!SimulationRules.withinRadius(25.001,5));
        check(SimulationRules.batchEnergy(1000,128)==128000);
        try { SimulationRules.batchEnergy(Long.MAX_VALUE,2); throw new AssertionError(); }
        catch (ArithmeticException expected) {}
        System.out.println("CoreSmoke: all rule checks passed");
    }
}
