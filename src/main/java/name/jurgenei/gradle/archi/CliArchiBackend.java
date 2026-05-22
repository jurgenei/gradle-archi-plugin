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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CliArchiBackend implements ArchiBackend {
    @Override
    public void run(Project project, File input, File output, List<String> args, Map<String, Object> envs) {
        Logger log = project.getLogger();

        List<String> cmd = new ArrayList<>();
        File spindleDirectory = resolveSpindleHome(project, log);
        File executable = new File(spindleDirectory, "scripts/archi-launcher.sh");

        cmd.add(executable.getAbsolutePath());

        if (input != null) {
            cmd.add("--loadModel");
            cmd.add(input.getAbsolutePath());
        }
        if (output != null) {
            cmd.add("--xmlexchange.export");
            File parent = output.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            cmd.add(output.getAbsolutePath());
        }

        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(spindleDirectory);

        pb.environment().put("HELIX_HOME", project.getProjectDir().getAbsolutePath());
        pb.environment().put("SPINDLE_HOME", spindleDirectory.getAbsolutePath());
        pb.environment().put("PATH", "/bin:/usr/bin");
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

    private File resolveSpindleHome(Project project, Logger log) {
        Path runtimePath = project.getLayout().getBuildDirectory().dir("archi-runtime").get().getAsFile().toPath();
        Path marker = runtimePath.resolve(".archi-runtime");

        if (Files.exists(marker)) {
            return runtimePath.toFile();
        }

        try {
            Files.createDirectories(runtimePath);
            extractTree("/spindle", runtimePath);
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


