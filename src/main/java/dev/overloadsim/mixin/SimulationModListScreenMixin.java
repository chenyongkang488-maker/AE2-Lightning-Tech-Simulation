package dev.overloadsim.mixin;

import dev.overloadsim.OverloadSimulation;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.neoforge.client.gui.ModListScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Show the public project name in its detail panel while preserving the registry namespace. */
@Mixin(value=ModListScreen.class, remap=false)
public abstract class SimulationModListScreenMixin {
    @Redirect(method="updateCache", at=@At(value="INVOKE",
            target="Lnet/neoforged/fml/i18n/FMLTranslations;parseMessage(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
            remap=false))
    private String overloadsim$projectName(String key, Object[] arguments) {
        if ("fml.menu.mods.info.idstate".equals(key) && arguments.length > 0
                && OverloadSimulation.ID.equals(arguments[0]))
            return I18n.get("gui.overload_sim.mod_name", I18n.get("itemGroup.overload_sim"));
        return FMLTranslations.parseMessage(key, arguments);
    }
}
