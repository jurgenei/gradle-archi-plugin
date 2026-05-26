package name.jurgenei.gradle.archi;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisableCachingByDefault(because = "Archi execution depends on external runtime state and environment")
public abstract class ArchiTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Property<File> getInputFile();

    @OutputFile
    public abstract Property<File> getOutputFile();

    @Input
    public abstract Property<Boolean> getStub();

    @Input
    public abstract Property<String> getScript();

    @Input
    public abstract Property<String> getExcel();

    @Input
    public abstract ListProperty<String> getArgs();

    @Input
    public abstract MapProperty<String, String> getEnvs();

    public ArchiTask() {
        getStub().convention(true);
        getArgs().convention(List.of());
        getEnvs().convention(Map.of());
        getScript().convention("");
        getExcel().convention("");
    }

    public void input(Object value) {
        getInputFile().set(getProject().file(value));
    }

    public void output(Object value) {
        getOutputFile().set(getProject().file(value));
    }

    public void stub(boolean value) {
        getStub().set(value);
    }

    public void script(Object value) {
        getScript().set(String.valueOf(value));
    }

    public void excel(Object value) {
        getExcel().set(String.valueOf(value));
    }

    public void arg(String value) {
        List<String> next = new ArrayList<>(getArgs().getOrElse(List.of()));
        next.add(value);
        getArgs().set(next);
    }

    public void env(String name, Object value) {
        Map<String, String> next = new HashMap<>(getEnvs().getOrElse(Map.of()));
        next.put(name, String.valueOf(value));
        getEnvs().set(next);
    }

    @TaskAction
    public void runArchi() {
        if (!getInputFile().isPresent()) {
            throw new IllegalStateException("archi.input is required");
        }
        if (!getOutputFile().isPresent()) {
            throw new IllegalStateException("archi.output is required");
        }

        List<String> resolvedArgs = new ArrayList<>(getArgs().getOrElse(List.of()));
        String script = getScript().getOrElse("");
        if (!script.isEmpty()) {
            resolvedArgs.add("--script.runScript");
            resolvedArgs.add(script);
        }
        String excel = getExcel().getOrElse("");
        if (!excel.isEmpty()) {
            resolvedArgs.add("--excel.export");
            resolvedArgs.add(excel);
        }

        Map<String, Object> envs = new HashMap<>();
        envs.putAll(getEnvs().getOrElse(Map.of()));

        ArchiBackend backend = getStub().get() ? new StubArchiBackend() : new CliArchiBackend();
        new ArchimateRunner(backend).run(
                getProject(),
                getInputFile().get(),
                getOutputFile().get(),
                resolvedArgs,
                envs
        );
    }
}

