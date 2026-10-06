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

/** Exercise the version ranges actually packaged for NeoForge. */
class DependencyCompatibilityTest {
    @Test void acceptsOriginalAndCurrentAE2() throws Exception {
        for (String version : List.of("19.2.17", "19.2.18")) assertAccepted("ae2", version);
    }

    @Test void permitsLaterAE2UpdatesWithinMajorVersion() throws Exception {
        for (String version : List.of("19.2.19", "19.2.99", "19.3.0")) assertAccepted("ae2", version);
    }

    @Test void acceptsMigratedLightningTechAndLaterMinorUpdates() throws Exception {
        for (String version : List.of("2.1.0", "2.1.1", "2.1.2", "2.2.0")) assertAccepted("ae2lt", version);
    }

    @Test void acceptsOriginalAndLaterThunderboltUpdates() throws Exception {
        for (String version : List.of("2.0.0", "2.0.2", "2.0.3", "2.1.0")) assertAccepted("thunderbolt", version);
    }

    @Test void permitsGuidePatchUpdatesForThisMinecraftSeries() throws Exception {
        for (String version : List.of("21.1.19", "21.1.20", "21.1.99")) assertAccepted("guideme", version);
    }

    @Test void retainsMinecraftAndLoaderCompatibilityBoundaries() throws Exception {
        assertAccepted("minecraft", "1.21.1");
        assertAccepted("neoforge", "21.1.252");
        assertAccepted("neoforge", "21.1.255");
        assertRejected("minecraft", "1.21.2");
        assertRejected("neoforge", "21.1.251");
        assertRejected("neoforge", "21.2.0");
    }

    @Test void rejectsMissingAPIAndDifferentMajorVersions() throws Exception {
        assertRejected("ae2", "19.2.16");
        assertRejected("ae2", "20.0.0");
        assertRejected("ae2lt", "2.0.9");
        assertRejected("ae2lt", "3.0.0");
        assertRejected("thunderbolt", "1.9.9");
        assertRejected("thunderbolt", "3.0.0");
        assertRejected("guideme", "21.1.18");
        assertRejected("guideme", "21.2.0");
    }

    @Test void keepsDependenciesMandatoryAndSaveIdentityStable() throws Exception {
        for (String id : List.of("minecraft", "neoforge", "ae2", "ae2lt", "thunderbolt", "guideme"))
            assertEquals("required", dependency(id).get("type"));
        assertEquals("overload_sim", ((List<Config>) metadata().get("mods")).getFirst().get("modId"));
    }

    private static void assertAccepted(String id, String version) throws Exception {
        assertTrue(range(id).containsVersion(new DefaultArtifactVersion(version)),
                () -> "Metadata rejects " + id + " " + version);
    }

    private static void assertRejected(String id, String version) throws Exception {
        assertFalse(range(id).containsVersion(new DefaultArtifactVersion(version)),
                () -> "Metadata must not claim another API or game series: " + id + " " + version);
    }

    private static VersionRange range(String id) throws Exception {
        return VersionRange.createFromVersionSpec(dependency(id).get("versionRange"));
    }

    private static Config dependency(String id) throws Exception {
        List<Config> dependencies = metadata().get("dependencies.overload_sim");
        return dependencies.stream().filter(entry -> id.equals(entry.get("modId"))).findFirst().orElseThrow();
    }

    private static Config metadata() throws Exception {
        try (var stream = DependencyCompatibilityTest.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
            assertNotNull(stream, "Processed mod metadata must be on the test classpath");
            return new TomlParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
