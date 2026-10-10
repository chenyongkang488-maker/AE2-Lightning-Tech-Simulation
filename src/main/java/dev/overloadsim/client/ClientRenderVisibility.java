package dev.overloadsim.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

/** Camera culling inside global renderers preserves geometry crossing controller chunk boundaries. */
final class ClientRenderVisibility {
    private record Shadows(MethodHandle active,boolean uncertain) {}
    private static final Shadows SHADOWS=findShadows();
    private ClientRenderVisibility() {}

    static boolean visible(AABB bounds) {
        // A machine behind the player may still cast a shadow onto visible terrain.
        if(shadowPass())return true;
        var frustum=Minecraft.getInstance().levelRenderer.getFrustum();
        return frustum==null||frustum.isVisible(bounds);
    }

    private static Shadows findShadows() {
        try {
            var type=Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState",false,
                    ClientRenderVisibility.class.getClassLoader());
            return new Shadows(MethodHandles.publicLookup().findStatic(type,
                    "areShadowsCurrentlyBeingRendered",MethodType.methodType(boolean.class)),false);
        }catch(ClassNotFoundException absent){return new Shadows(null,false);}
        catch(ReflectiveOperationException|LinkageError unavailable){return new Shadows(null,true);}
    }

    private static boolean shadowPass() {
        if(SHADOWS.uncertain)return true;
        if(SHADOWS.active==null)return false;
        try{return (boolean)SHADOWS.active.invokeExact();}
        catch(Throwable unavailable){return true;} // Preserve rendering if an optional shader bridge changes.
    }
}
