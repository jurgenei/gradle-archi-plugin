package name.jurgenei.gradle.archi;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ArchiGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register("archi", ArchiTask.class);
    }
}

