package name.jurgenei.gradle.archi;

import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchimateRunner {
    private final ArchiBackend backend;

    public ArchimateRunner(ArchiBackend backend) {
        this.backend = backend;
    }

    public void run(Project project, File input, File output, List<String> args, Map<String, Object> envs) {
        if (args == null) {
            args = new ArrayList<>();
        }
        if (envs == null) {
            envs = new HashMap<>();
        }
        backend.run(project, input, output, args, envs);
    }
}


