package name.jurgenei.gradle.archi;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchimateRunnerTest {
    @Test
    void stubShouldProduceOutput() throws Exception {
        File dir = Files.createTempDirectory("archi").toFile();
        File input = new File(dir, "in.xml");
        Files.writeString(input.toPath(), "<a></a>");
        File output = new File(dir, "out.xml");

        Project project = ProjectBuilder.builder().build();
        new ArchimateRunner(new StubArchiBackend()).run(project, input, output, null, null);

        assertTrue(output.exists());
        assertTrue(Files.readString(output.toPath()).contains("processed"));
    }

    @Test
    void cliShouldProduceOutput() throws Exception {
        File dir = Files.createTempDirectory("archi-cli").toFile();
        File input = new File(dir, "in.archimate");
        Files.writeString(input.toPath(), "<archimate/>");
        File output = new File(dir, "out.xml");

        Project project = ProjectBuilder.builder().build();
        new ArchimateRunner(new CliArchiBackend()).run(project, input, output, null, null);

        assertTrue(output.exists());
        assertTrue(Files.readString(output.toPath()).contains("http://www.opengroup.org/xsd/archimate/3.0/"));
    }
}


