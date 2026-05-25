package name.jurgenei.gradle.archi;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CliArchiBackend implements ArchiBackend {
    @Override
    public void run(Project project, File input, File output, List<String> args, Map<String, Object> envs) {
        Logger log = project.getLogger();

        List<String> effectiveArgs = new ArrayList<>(args);
        File archiDirectory = resolveArchiRuntime(project, log);
        File launcher = new File(archiDirectory, "scripts/archi-launcher.sh");
        boolean useMock = "true".equalsIgnoreCase(String.valueOf(envs.getOrDefault("ARCHI_USE_MOCK", "false")));
        if (useMock) {
            launcher = new File(archiDirectory, "scripts/archi-launcher-mock.sh");
        }

        if (!containsScriptArg(effectiveArgs)) {
            File defaultScript = new File(archiDirectory, "ajs/export-assets.ajs");
            if (defaultScript.isFile()) {
                effectiveArgs.add("--script.runScript");
                effectiveArgs.add(defaultScript.getAbsolutePath());
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(launcher.getAbsolutePath());

        cmd.addAll(effectiveArgs);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(archiDirectory);

        String packageName = inferPackageName(input, output);
        File exportDir = output != null && output.getParentFile() != null
                ? output.getParentFile()
                : new File(project.getBuildDir(), "archi-export");
        exportDir.mkdirs();

        pb.environment().put("ARCHI_HOME", resolveArchiHome(log));
        pb.environment().put("HELIX_HOME", project.getProjectDir().getAbsolutePath());
        pb.environment().put("ARCHI_RUNTIME", archiDirectory.getAbsolutePath());
        pb.environment().put("ARCHI_FILE", input != null ? input.getAbsolutePath() : "");
        pb.environment().put("EXPORT_DIR", exportDir.getAbsolutePath());
        pb.environment().put("PACKAGE_NAME", packageName);
        pb.environment().put("ARCHI_OUTPUT_FILE", output != null ? output.getAbsolutePath() : "");
        pb.environment().put("EXPORT_LOG", new File(exportDir, "logs").getAbsolutePath());
        envs.forEach((key, value) -> {
            String expandedValue = value instanceof File file ? file.getAbsolutePath() : String.valueOf(value);
            pb.environment().put(key, expandedValue);
        });

        try {
            Process process = pb.start();
            CompletableFuture<Void> stdout = CompletableFuture.runAsync(() ->
                    new BufferedReader(new InputStreamReader(process.getInputStream()))
                            .lines()
                            .forEach(log::info)
            );
            CompletableFuture<Void> stderr = CompletableFuture.runAsync(() ->
                    new BufferedReader(new InputStreamReader(process.getErrorStream()))
                            .lines()
                            .forEach(log::error)
            );

            int exit = process.waitFor();
            CompletableFuture.allOf(stdout, stderr).join();
            if (exit != 0) {
                throw new RuntimeException("Archi launcher exited with code: " + exit);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Archi launcher interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Archi launcher", e);
        }
    }

    private static boolean containsScriptArg(List<String> args) {
        for (String arg : args) {
            if ("--script.runScript".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String inferPackageName(File input, File output) {
        String fileName;
        if (output != null) {
            fileName = output.getName().replace(".export.xml", "");
        } else if (input != null) {
            fileName = input.getName();
        } else {
            return "archi-export";
        }
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private String resolveArchiHome(Logger log) {
        String archiHome = System.getenv("ARCHI_HOME");
        if (archiHome != null && !archiHome.isEmpty()) {
            log.info("Using ARCHI_HOME from environment: {}", archiHome);
            return archiHome;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            String[] candidates = {
                    System.getProperty("user.home") + "/Applications/Archi.app",
                    "/Applications/Archi.app"
            };
            for (String candidate : candidates) {
                if (new File(candidate).exists()) {
                    log.info("Found Archi on macOS: {}", candidate);
                    return candidate;
                }
            }
        } else if (osName.contains("linux")) {
            String[] candidates = {
                    "/opt/archi",
                    "/usr/local/archi",
                    System.getProperty("user.home") + "/.archi"
            };
            for (String candidate : candidates) {
                if (new File(candidate).exists()) {
                    log.info("Found Archi on Linux: {}", candidate);
                    return candidate;
                }
            }
        }

        if (isInDocker()) {
            log.info("Detected container environment, using default /opt/archi");
            return "/opt/archi";
        }

        log.warn("Could not locate Archi installation. Set ARCHI_HOME environment variable.");
        return "";
    }

    private static boolean isInDocker() {
        Path dockerEnv = Paths.get("/.dockerenv");
        if (Files.exists(dockerEnv)) {
            return true;
        }
        String cgroup = readCgroup();
        return cgroup.contains("docker") || cgroup.contains("containerd") || cgroup.contains("kubepods");
    }

    private static String readCgroup() {
        Path cgroup = Paths.get("/proc/1/cgroup");
        if (!Files.isReadable(cgroup)) {
            return "";
        }
        try {
            return Files.readString(cgroup).toLowerCase(Locale.ROOT);
        } catch (IOException ignored) {
            return "";
        }
    }

    private File resolveArchiRuntime(Project project, Logger log) {
        Path runtimePath = project.getLayout().getBuildDirectory().dir("archi-runtime").get().getAsFile().toPath();
        Path marker = runtimePath.resolve(".archi-runtime");

        if (Files.exists(marker)) {
            return runtimePath.toFile();
        }

        try {
            Files.createDirectories(runtimePath);
            extractTree("/archi", runtimePath);
            Files.writeString(marker, "ready");
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare Archi runtime", e);
        }

        log.info("Prepared Archi runtime at {}", runtimePath);
        return runtimePath.toFile();
    }

    private static void extractTree(String resourceRoot, Path targetDir) throws Exception {
        var resourceUrl = CliArchiBackend.class.getResource(resourceRoot);
        if (resourceUrl == null) {
            throw new IllegalStateException("Missing resource root: " + resourceRoot);
        }

        URI uri;
        try {
            uri = resourceUrl.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of())) {
                copyTree(fileSystem.getPath(resourceRoot), targetDir);
            }
        } else {
            copyTree(Paths.get(uri), targetDir);
        }
    }

    private static void copyTree(Path source, Path target) throws Exception {
        Files.walk(source).forEach(path -> {
            try {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    if (destination.toString().endsWith(".sh")) {
                        destination.toFile().setExecutable(true);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
