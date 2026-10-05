package dev.overloadsim.compat;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for the metadata actually consumed by NeoForge. */
class DependencyCompatibilityTest {
    @Test void acceptsCurrentCurseForgeLightningTech() throws Exception {
        assertAccepted("ae2lt", "2.1.1");
    }

    @Test void acceptsCurrentCurseForgeThunderbolt() throws Exception {
        assertAccepted("thunderbolt", "2.0.2");
    }

    @Test void preservesOriginalRebornStack() throws Exception {
        assertAccepted("ae2lt", "2.1.0");
        assertAccepted("thunderbolt", "2.0.0");
        assertAccepted("ae2", "19.2.17");
        assertAccepted("guideme", "21.1.19");
        assertAccepted("minecraft", "1.21.1");
        assertAccepted("neoforge", "21.1.255");
    }

    @Test void stillRequiresBothLightningTechDependencies() throws Exception {
        assertEquals("required", dependency("ae2lt").get("type"));
        assertEquals("required", dependency("thunderbolt").get("type"));
        assertFalse(range("ae2lt").containsVersion(new DefaultArtifactVersion("2.0.9")),
                "The pre-Reborn API is not supported");
        assertFalse(range("ae2lt").containsVersion(new DefaultArtifactVersion("3.0.0")));
        assertFalse(range("thunderbolt").containsVersion(new DefaultArtifactVersion("3.0.0")));
    }

    private static void assertAccepted(String id, String version) throws Exception {
        assertTrue(range(id).containsVersion(new DefaultArtifactVersion(version)),
                () -> "Packaged dependency declaration rejects " + id + " " + version);
    }

    private static VersionRange range(String id) throws Exception {
        return VersionRange.createFromVersionSpec(dependency(id).get("versionRange"));
    }

    private static Config dependency(String id) throws Exception {
        try (var stream = DependencyCompatibilityTest.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
            assertNotNull(stream, "Processed mod metadata must be on the test classpath");
            var config = new TomlParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            List<Config> dependencies = config.get("dependencies.overload_sim");
            return dependencies.stream().filter(entry -> id.equals(entry.get("modId"))).findFirst().orElseThrow();
        }
    }
}
