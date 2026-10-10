package dev.overloadsim.compat;

import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Resolves the level captured by a meshing view without requiring its light engine. */
public final class BlockRenderViewLevel {
    // Vanilla RenderChunkRegion and Sodium LevelSlice both capture a final Level.
    // Resolve by declared type so loader mappings and optional renderer field names
    // do not become dependencies. Keep the actual captured world: a global current
    // client level could give an old worker the new dimension's shell ownership.
    private static final ClassValue<Optional<Field>> LEVEL_FIELDS=new ClassValue<>() {
        @Override protected Optional<Field> computeValue(Class<?> type) {
            for(Class<?> current=type;current!=null;current=current.getSuperclass()) {
                for(var field:current.getDeclaredFields()) {
                    if(java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            ||!Level.class.isAssignableFrom(field.getType()))continue;
                    try {if(field.trySetAccessible())return Optional.of(field);}
                    catch(SecurityException inaccessible) { /* Unsupported views keep their ordinary shell. */ }
                }
            }
            return Optional.empty();
        }
    };
    private BlockRenderViewLevel() {}
    public static @Nullable Level level(BlockAndTintGetter view) {
        if(view instanceof Level level)return level;
        var field=LEVEL_FIELDS.get(view.getClass());
        if(field.isEmpty())return null;
        try {return field.get().get(view) instanceof Level level?level:null;}
        catch(IllegalAccessException inaccessible) {return null;}
    }
}
