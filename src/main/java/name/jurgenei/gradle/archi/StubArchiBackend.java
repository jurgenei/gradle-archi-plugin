package name.jurgenei.gradle.archi;

import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class StubArchiBackend implements ArchiBackend {
    @Override
    public void run(Project project, File input, File output, List<String> args, Map<String, Object> envs) {
        if (input == null || output == null) {
            project.getLogger().error("Input or output is null");
            return;
        }
        String content = "<archimate>stub-processed</archimate>";
        try {
            if (input.exists()) {
                content = Files.readString(input.toPath()).replace("</", "<!--processed--></");
            }
            File parent = output.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new RuntimeException("Failed to create output directory: " + parent.getAbsolutePath());
            }
            Files.writeString(output.toPath(), content);
        } catch (Exception e) {
            throw new RuntimeException("Stub backend failed", e);
        }
    }
}


