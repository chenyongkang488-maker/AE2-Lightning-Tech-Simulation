package dev.overloadsim.compat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise compatibility selection against actual dependency bytecode, without loading client classes. */
class JeiPreviewCompatibilityTest {
    private static final String LDLIB = "ldlib2-neoforge-1.21.1-2.2.42-all.jar";
    @Test void activatesNewHookForBeta4AndCurrentJei() throws Exception {
        var probe = probe("ae2lt-2.1.2-beta4.jar", "jei-1.21.1-neoforge-19.64.0.455.jar", LDLIB);
        assertTrue(JeiPreviewCompatibility.categoryPatch(probe));
        assertTrue(JeiPreviewCompatibility.modernLayoutPatch(probe));
        assertTrue(JeiPreviewCompatibility.scenePatch(probe));
    }
    @Test void retainsUpstreamsOldHookForBeta4AndOldJei() throws Exception {
        var probe = probe("ae2lt-2.1.2-beta4.jar", "jei-1.21.1-neoforge-19.44.0.403.jar", LDLIB);
        assertTrue(JeiPreviewCompatibility.categoryPatch(probe));
        assertFalse(JeiPreviewCompatibility.modernLayoutPatch(probe));
        assertTrue(JeiPreviewCompatibility.scenePatch(probe));
    }
    @Test void disablesOptionalPatchWithoutJei() throws Exception {
        var probe = probe("ae2lt-2.1.2-beta4.jar");
        assertFalse(JeiPreviewCompatibility.categoryPatch(probe));
        assertFalse(JeiPreviewCompatibility.modernLayoutPatch(probe));
    }
    @Test void skipsStandaloneScenePatchWithoutCompatibleRenderer() throws Exception {
        var probe = probe("ae2lt-2.1.2-beta4.jar", "jei-1.21.1-neoforge-19.64.0.455.jar");
        assertTrue(JeiPreviewCompatibility.modernLayoutPatch(probe));
        assertFalse(JeiPreviewCompatibility.scenePatch(probe));
    }
    @Test void disablesPatchWhenOlderUpstreamHasNoPreview() throws Exception {
        for (String upstream : new String[]{"ae2lt-2.1.0.jar", "ae2lt-2.1.2-beta2.jar"}) {
            var probe = probe(upstream, "jei-1.21.1-neoforge-19.44.0.403.jar");
            assertFalse(JeiPreviewCompatibility.categoryPatch(probe), upstream);
            assertFalse(JeiPreviewCompatibility.modernLayoutPatch(probe), upstream);
        }
    }

    private static BiPredicate<String, String> probe(String... jars) throws IOException {
        Map<String, ClassNode> nodes = new HashMap<>();
        for (String jar : jars) {
            try (var zip = new ZipFile(Path.of("libs", jar).toFile())) {
                for (String owner : new String[]{JeiPreviewCompatibility.CATEGORY,
                        JeiPreviewCompatibility.PREVIEW, JeiPreviewCompatibility.LOGIC,
                        JeiPreviewCompatibility.SCENE, JeiPreviewCompatibility.SCENE_RENDERER}) {
                    var entry = zip.getEntry(owner.replace('.', '/') + ".class");
                    if (entry == null) continue;
                    var node = new ClassNode();
                    try (var stream = zip.getInputStream(entry)) {
                        new ClassReader(stream).accept(node, ClassReader.SKIP_CODE);
                    }
                    nodes.put(owner, node);
                }
            }
        }
        return (owner, signature) -> nodes.containsKey(owner)
                && nodes.get(owner).methods.stream().anyMatch(m -> (m.name + m.desc).equals(signature));
    }
}
