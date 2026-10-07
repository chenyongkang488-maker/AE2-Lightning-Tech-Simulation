package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;
import dev.overloadsim.core.ReceiverShellMesh.Quad;

/** Fixed pixel geometry relative to the seven-block chamber's centre: right/up/inward. */
public final class WorkstationVisualMesh {
    private static final List<IndustrialMachineMesh.Surface> STATION = build();
    private WorkstationVisualMesh() {}
    public static List<IndustrialMachineMesh.Surface> surfaces() { return STATION; }

    private static List<IndustrialMachineMesh.Surface> build() {
        var result = new ArrayList<IndustrialMachineMesh.Surface>();
        // A shallow collector derived from the emitter, with no emitter logic or extra input ports.
        for (var face : EmitterReactorMesh.surfaces()) {
            var q = face.quad();
            result.add(new IndustrialMachineMesh.Surface(new Quad(receiver(q.a()),receiver(q.b()),
                    receiver(q.c()),receiver(q.d())),face.tile(),face.glow()));
        }
        // Raised workstation plinth, distinct from the simulation chamber's floating sphere.
        IndustrialMachineMesh.box(result,5,-1.625,-2.5,-1.625,1.625,-2.25,1.625);
        IndustrialMachineMesh.box(result,44,-1.4375,-2.25,-1.4375,1.4375,-2.125,1.4375);
        IndustrialMachineMesh.box(result,7,-1.3125,-2.125,-1.3125,1.3125,-2,1.3125);
        for (int sign : new int[]{-1,1}) {
            double x0 = sign < 0 ? -1.5 : 1.4375;
            IndustrialMachineMesh.box(result,41,x0,-2.1875,-1.4375,x0+.0625,-2.125,1.4375);
            double z0 = sign < 0 ? -1.5 : 1.4375;
            IndustrialMachineMesh.box(result,41,-1.4375,-2.1875,z0,1.4375,-2.125,z0+.0625);
        }
        // Four segmented internal columns and continuous front light channels on a 1/16 lattice.
        for (int sx : new int[]{-1,1}) for (int sz : new int[]{-1,1}) {
            double x=sx*2.375,z=sz*2.375;
            IndustrialMachineMesh.box(result,5,x-.125,-2.5,z-.125,x+.125,2.5,z+.125);
            IndustrialMachineMesh.box(result,44,x-.1875,-2.4375,z-.1875,x+.1875,-2.1875,z+.1875);
            IndustrialMachineMesh.box(result,44,x-.1875,2.1875,z-.1875,x+.1875,2.4375,z+.1875);
            IndustrialMachineMesh.box(result,41,x-.03125,-2.125,z-.1875,x+.03125,2.125,z-.125);
        }
        for (int sx : new int[]{-1,1}) {
            double x = sx < 0 ? -3.125 : 3.0625;
            IndustrialMachineMesh.box(result,41,x,-2.75,-3.5625,x+.0625,2.75,-3.5);
            IndustrialMachineMesh.box(result,44,x-.125,-2.9375,-3.625,x+.1875,-2.6875,-3.5);
            IndustrialMachineMesh.box(result,44,x-.125,2.6875,-3.625,x+.1875,2.9375,-3.5);
        }
        // Nine visible holders align with the menu's actual 3x3 material slots.
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++)
            IndustrialMachineMesh.box(result,38,x*.8125-.3125,-2,z*.8125-.3125,
                    x*.8125+.3125,-1.9375,z*.8125+.3125);
        return List.copyOf(result);
    }

    private static Vertex receiver(Vertex v) {
        return new Vertex(v.x()*1.125,v.y()*1.125,3.3125+v.z()*.125,
                v.nx(),v.ny(),v.nz(),v.u(),v.v());
    }
}
