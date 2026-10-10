package dev.overloadsim.client;

import dev.overloadsim.core.ReceiverShellMesh;
import dev.overloadsim.core.WorkstationRenderLighting;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Keeps the original five-point lighting rule, preparing its fixed positions only once. */
final class PreparedSurfaceLighting {
    private final Function<WorkstationRenderLighting.Point,WorkstationRenderLighting.Point> world;
    private final boolean escapeOpaque;
    private final HashMap<BlockPos,Cell> positions=new HashMap<>();
    private final ArrayList<Cell> cells=new ArrayList<>();
    private final IdentityHashMap<ReceiverShellMesh.Quad,Face> faces=new IdentityHashMap<>();
    // A renderer's weak host cache must not retain the world (which owns the host) through its value.
    private WeakReference<Level> levelReference=new WeakReference<>(null);
    private long revision;

    PreparedSurfaceLighting(Function<WorkstationRenderLighting.Point,WorkstationRenderLighting.Point> world,
            boolean escapeOpaque) {
        this.world=world;this.escapeOpaque=escapeOpaque;
    }

    Frame beginFrame(Level level,int fallback) {
        if(levelReference.get()!=level) {
            positions.clear();cells.clear();faces.clear();
            levelReference=new WeakReference<>(level);revision++;
        }
        boolean changed=false;
        // Each distinct voxel is read once per frame, even when thousands of panels share it.
        // Keep per-frame reads so asynchronous light updates and moving light sources remain visible.
        for(int i=0;i<cells.size();i++)changed|=cells.get(i).refresh(level,fallback,escapeOpaque);
        if(changed)revision++;
        return new Frame(level,fallback);
    }

    final class Frame implements ToIntFunction<ReceiverShellMesh.Quad> {
        private final Level level;
        private final int fallback;
        Frame(Level level,int fallback){this.level=level;this.fallback=fallback;}

        @Override public int applyAsInt(ReceiverShellMesh.Quad quad) {
            Face face=faces.get(quad);
            if(face==null) {
                face=prepare(quad);faces.put(quad,face);
            }
            if(face.revision!=revision) {
                int block=0,sky=0;
                for(var probe:face.probes) {
                    int index=0;
                    while(index<probe.length-1&&probe[index].opaque)index++;
                    int light=probe[index].light;
                    block=Math.max(block,light&65535);sky=Math.max(sky,light>>>16);
                }
                face.light=block|(sky<<16);face.revision=revision;
            }
            return face.light;
        }

        int lightAt(WorkstationRenderLighting.Point point) {
            return cell(BlockPos.containing(point.x(),point.y(),point.z())).light;
        }

        private Cell cell(BlockPos pos) {
            Cell cell=positions.get(pos);
            if(cell==null) {
                cell=new Cell(pos);cell.refresh(level,fallback,escapeOpaque);
                positions.put(pos,cell);cells.add(cell);
            }
            return cell;
        }

        private Face prepare(ReceiverShellMesh.Quad quad) {
            var a=quad.a();var b=quad.b();var c=quad.c();var d=quad.d();
            // Match the old arithmetic and ordering, including centre accumulation rounding.
            double x=a.x()/4+b.x()/4+c.x()/4+d.x()/4;
            double y=a.y()/4+b.y()/4+c.y()/4+d.y()/4;
            double z=a.z()/4+b.z()/4+c.z()/4+d.z()/4;
            return new Face(new Cell[][]{probe(a.x(),a.y(),a.z(),a),probe(b.x(),b.y(),b.z(),b),
                    probe(c.x(),c.y(),c.z(),c),probe(d.x(),d.y(),d.z(),d),probe(x,y,z,a)});
        }

        private Cell[] probe(double x,double y,double z,ReceiverShellMesh.Vertex normal) {
            Cell[] result=new Cell[escapeOpaque?5:1];
            for(int i=0;i<result.length;i++) {
                double offset=i==0?1d/32:i*.25;
                var point=world.apply(new WorkstationRenderLighting.Point(x+normal.nx()*offset,
                        y+normal.ny()*offset,z+normal.nz()*offset));
                result[i]=cell(BlockPos.containing(point.x(),point.y(),point.z()));
            }
            return result;
        }
    }

    private static final class Cell {
        final BlockPos pos;
        int light;
        boolean opaque;
        Cell(BlockPos pos){this.pos=pos;}
        boolean refresh(Level level,int fallback,boolean escapeOpaque) {
            boolean loaded=level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4);
            int next=loaded?LevelRenderer.getLightColor(level,pos):fallback;
            boolean blocked=loaded&&escapeOpaque&&level.getBlockState(pos).getLightBlock(level,pos)>=15;
            boolean changed=light!=next||opaque!=blocked;
            light=next;opaque=blocked;return changed;
        }
    }

    private static final class Face {
        final Cell[][] probes;
        int light;
        long revision=Long.MIN_VALUE;
        Face(Cell[][] probes){this.probes=probes;}
    }
}
