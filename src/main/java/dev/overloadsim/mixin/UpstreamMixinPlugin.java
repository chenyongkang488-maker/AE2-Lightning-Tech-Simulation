package dev.overloadsim.mixin;

import dev.overloadsim.compat.UpstreamApiPresence;
import dev.overloadsim.compat.JeiPreviewCompatibility;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Inspect bytecode without loading target classes before Mixin applies. */
public final class UpstreamMixinPlugin implements IMixinConfigPlugin {
    private boolean modern;
    @Override public void onLoad(String mixinPackage) {
        modern=UpstreamApiPresence.available(name->{
            try { return MixinService.getService().getBytecodeProvider().getClassNode(name)!=null; }
            catch(ClassNotFoundException missing) { return false; }
            catch(java.io.IOException error) { throw new IllegalStateException("Could not inspect AE2LT API "+name,error); }
        });
    }
    @Override public boolean shouldApplyMixin(String targetClassName,String mixinClassName) {
        if (mixinClassName.startsWith("dev.overloadsim.mixin.jei.")) {
            if (mixinClassName.endsWith("MultiblockSceneFitMixin"))
                return JeiPreviewCompatibility.scenePatch(UpstreamMixinPlugin::hasMethod);
            return mixinClassName.endsWith("ModernJeiMultiblockLayoutMixin")
                    ? JeiPreviewCompatibility.modernLayoutPatch(UpstreamMixinPlugin::hasMethod)
                    : JeiPreviewCompatibility.categoryPatch(UpstreamMixinPlugin::hasMethod);
        }
        if(mixinClassName.startsWith("dev.overloadsim.mixin.modern.")) return modern;
        return !modern||!Set.of("dev.overloadsim.mixin.CollectorInventoryMixin",
                "dev.overloadsim.mixin.CollectorMixin","dev.overloadsim.mixin.CoilWorkbenchMixin").contains(mixinClassName);
    }
    private static boolean hasMethod(String owner, String signature) {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(owner);
            return node != null && node.methods.stream().anyMatch(m -> (m.name + m.desc).equals(signature));
        } catch (ClassNotFoundException missing) {
            return false;
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not inspect optional JEI preview API " + owner, error);
        }
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine,Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void postApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
}
