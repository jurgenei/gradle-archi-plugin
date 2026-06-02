package name.jurgenei.gradle.archi;

import org.gradle.api.Project;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Execution backend for Archi model processing.
 */
public interface ArchiBackend {
    /**
     * Executes an Archi processing run.
     *
     * @param project current Gradle project.
     * @param input source model input file.
     * @param output target output file.
     * @param args command-line style arguments for the backend.
     * @param envs environment variables/options passed to the backend process.
     */
    void run(Project project, File input, File output, List<String> args, Map<String, Object> envs);
}


