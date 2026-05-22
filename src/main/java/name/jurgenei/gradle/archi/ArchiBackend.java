package name.jurgenei.gradle.archi;

import org.gradle.api.Project;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ArchiBackend {
    void run(Project project, File input, File output, List<String> args, Map<String, Object> envs);
}


