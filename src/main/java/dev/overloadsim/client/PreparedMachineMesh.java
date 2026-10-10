package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overloadsim.core.IndustrialMachineMesh;
import dev.overloadsim.core.ReceiverShellMesh;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

/** CPU-prepared vertices; still uses the normal vertex consumer and its shader extensions. */
final class PreparedMachineMesh {
    private static final Vec3 X=new Vec3(1,0,0),Y=new Vec3(0,1,0),Z=new Vec3(0,0,1);
    private final Face[] faces;
    private TextureAtlasSprite sprite;

    private PreparedMachineMesh(Face[] faces){this.faces=faces;}

    static PreparedMachineMesh surfaces(List<IndustrialMachineMesh.Surface> surfaces) {
        return surfaces(surfaces,Vec3.ZERO,X,Y,Z);
    }

    static PreparedMachineMesh surfaces(List<IndustrialMachineMesh.Surface> surfaces,Vec3 center,
            Vec3 right,Vec3 up,Vec3 forward) {
        var faces=new Face[surfaces.size()];
        for(int i=0;i<faces.length;i++) {
            var s=surfaces.get(i);
            faces[i]=new Face(s.quad(),s.glow(),center,right,up,forward,
                    (s.tile()%8)*16+.5f,(s.tile()/8)*16+.5f,15,15);
        }
        return new PreparedMachineMesh(faces);
    }

    static PreparedMachineMesh panes(List<ReceiverShellMesh.Quad> panes,Vec3 center,
            Vec3 right,Vec3 up,Vec3 forward,float u,float v,float width,float height) {
        var faces=new Face[panes.size()];
        for(int i=0;i<faces.length;i++)faces[i]=new Face(panes.get(i),false,center,right,up,forward,u,v,width,height);
        return new PreparedMachineMesh(faces);
    }

    static PreparedMachineMesh panes(List<ReceiverShellMesh.Quad> panes) {
        return panes(panes,Vec3.ZERO,X,Y,Z,66.5f,2.5f,11,11);
    }

    void draw(PoseStack.Pose pose,VertexConsumer solid,VertexConsumer emissive,TextureAtlasSprite atlas,
            ToIntFunction<ReceiverShellMesh.Quad> lighting) {
        if(sprite!=atlas) {
            // Atlas sprites are replaced on resource reload; keep animated sprite UVs within that atlas.
            for(var face:faces)for(int i=0;i<4;i++) {
                face.uv[i*2]=atlas.getU(face.data[i*8+6]);
                face.uv[i*2+1]=atlas.getV(face.data[i*8+7]);
            }
            sprite=atlas;
        }
        var matrix=pose.pose();
        for(var face:faces) {
            int light=face.glow?LightTexture.FULL_BRIGHT:lighting.applyAsInt(face.quad);
            var consumer=face.glow?emissive:solid;
            var d=face.data;var uv=face.uv;
            for(int vertex=0;vertex<4;vertex++) {
                int offset=vertex*8;
                consumer.addVertex(matrix,d[offset],d[offset+1],d[offset+2])
                        .setColor(255,255,255,255).setUv(uv[vertex*2],uv[vertex*2+1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                        .setNormal(pose,d[offset+3],d[offset+4],d[offset+5]);
            }
        }
    }

    private static final class Face {
        final ReceiverShellMesh.Quad quad;
        final boolean glow;
        final float[] data=new float[32],uv=new float[8];
        Face(ReceiverShellMesh.Quad quad,boolean glow,Vec3 center,Vec3 right,Vec3 up,Vec3 forward,
                float u,float v,float width,float height) {
            this.quad=quad;this.glow=glow;
            var points=new ReceiverShellMesh.Vertex[]{quad.a(),quad.b(),quad.c(),quad.d()};
            for(int i=0;i<4;i++) {
                var p=points[i];int offset=i*8;
                // Match the old double arithmetic and float cast before the camera pose transform.
                data[offset]=(float)(center.x+right.x*p.x()+up.x*p.y()+forward.x*p.z());
                data[offset+1]=(float)(center.y+right.y*p.x()+up.y*p.y()+forward.y*p.z());
                data[offset+2]=(float)(center.z+right.z*p.x()+up.z*p.y()+forward.z*p.z());
                data[offset+3]=(float)(right.x*p.nx()+up.x*p.ny()+forward.x*p.nz());
                data[offset+4]=(float)(right.y*p.nx()+up.y*p.ny()+forward.y*p.nz());
                data[offset+5]=(float)(right.z*p.nx()+up.z*p.ny()+forward.z*p.nz());
                data[offset+6]=(float)(u+p.u()*width)/128;
                data[offset+7]=(float)(v+p.v()*height)/128;
            }
        }
    }
}
