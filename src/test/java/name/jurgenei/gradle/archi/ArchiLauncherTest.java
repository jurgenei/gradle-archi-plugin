package name.jurgenei.gradle.archi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ArchiLauncherTest {
    @TempDir
    Path tempDir;

    @Test
    void testLauncherScriptExists() throws Exception {
        Path launcherPath = Path.of("src/main/resources/archi/scripts/archi-launcher.sh");
        assertTrue(Files.exists(launcherPath), "Launcher script should exist");
        assertTrue(Files.isReadable(launcherPath), "Launcher script should be readable");

        String content = Files.readString(launcherPath);
        assertTrue(content.contains("resolve_archi_executable"), "Should resolve Archi executable");
        assertTrue(content.contains("install_plugins"), "Should have install_plugins function");
    }

    @Test
    void testLauncherDetectsOS() throws Exception {
        String content = Files.readString(Path.of("src/main/resources/archi/scripts/archi-launcher.sh"));
        assertTrue(content.contains("detect_os()"), "Should detect OS");
        assertTrue(content.contains("darwin"), "Should support macOS");
        assertTrue(content.contains("linux-gnu"), "Should support Linux");
    }

    @Test
    void testInstallerScriptExists() {
        Path installer = Path.of("src/main/resources/archi/bin/install-archiplugin.sh");
        assertTrue(Files.exists(installer), "Plugin installer should exist");
        assertTrue(Files.isReadable(installer), "Plugin installer should be readable");
    }

    @Test
    void testResourcesStructure() {
        Path resourcesRoot = Path.of("src/main/resources/archi");
        assertTrue(Files.isDirectory(resourcesRoot.resolve("scripts")), "scripts directory should exist");
        assertTrue(Files.isDirectory(resourcesRoot.resolve("plugins")), "plugins directory should exist");
        assertTrue(Files.isDirectory(resourcesRoot.resolve("ajs")), "ajs directory should exist");
    }
}

