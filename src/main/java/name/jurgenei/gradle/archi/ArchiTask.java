package name.jurgenei.gradle.archi;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task that runs Archi model export/transformation via configured backend.
 */
@DisableCachingByDefault(because = "Archi execution depends on external runtime state and environment")
public abstract class ArchiTask extends DefaultTask {

    /**
     * Input model file.
     *
     * @return input file property.
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Property<File> getInputFile();

    /**
     * Output file produced by Archi processing.
     *
     * @return output file property.
     */
    @Optional
    @OutputFile
    public abstract Property<File> getOutputFile();

    /**
     * Chooses stub backend when true, CLI backend when false.
     *
     * @return stub toggle property.
     */
    @Input
    public abstract Property<Boolean> getStub();

    /**
     * Optional script path passed to Archi.
     *
     * @return script property.
     */
    @Input
    public abstract Property<String> getScript();

    /**
     * Optional Excel export target.
     *
     * @return excel export property.
     */
    @Input
    public abstract Property<String> getExcel();

    /**
     * Additional backend arguments.
     *
     * @return argument list property.
     */
    @Input
    public abstract ListProperty<String> getArgs();

    /**
     * Additional backend environment variables.
     *
     * @return environment map property.
     */
    @Input
    public abstract MapProperty<String, String> getEnvs();

    /**
     * Creates task with default conventions.
     */
    public ArchiTask() {
        getStub().convention(true);
        getArgs().convention(List.of());
        getEnvs().convention(Map.of());
        getScript().convention("");
        getExcel().convention("");
    }

    /**
     * Sets input model file.
     *
     * @param value file path/object resolvable by Gradle.
     */
    public void input(Object value) {
        getInputFile().set(getProject().file(value));
    }

    /**
     * Sets output file.
     *
     * @param value file path/object resolvable by Gradle.
     */
    public void output(Object value) {
        getOutputFile().set(getProject().file(value));
    }

    /**
     * Sets stub backend usage.
     *
     * @param value true to use stub backend.
     */
    public void stub(boolean value) {
        getStub().set(value);
    }

    /**
     * Sets script argument value.
     *
     * @param value script path/value.
     */
    public void script(Object value) {
        getScript().set(String.valueOf(value));
    }

    /**
     * Sets Excel export argument value.
     *
     * @param value Excel target value.
     */
    public void excel(Object value) {
        getExcel().set(String.valueOf(value));
    }

    /**
     * Appends a single backend argument.
     *
     * @param value argument value.
     */
    public void arg(String value) {
        List<String> next = new ArrayList<>(getArgs().getOrElse(List.of()));
        next.add(value);
        getArgs().set(next);
    }

    /**
     * Adds/overrides one backend environment variable.
     *
     * @param name environment variable name.
     * @param value environment variable value.
     */
    public void env(String name, Object value) {
        Map<String, String> next = new HashMap<>(getEnvs().getOrElse(Map.of()));
        next.put(name, String.valueOf(value));
        getEnvs().set(next);
    }

    /**
     * Executes Archi processing using configured task inputs.
     */
    @TaskAction
    public void runArchi() {

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

        File inputFile = getInputFile().getOrNull();
        File outputFile = getOutputFile().getOrNull();

        ArchiBackend backend = getStub().get() ? new StubArchiBackend() : new CliArchiBackend();
        new ArchimateRunner(backend).run(
                getProject(),
                inputFile,
                outputFile,
                resolvedArgs,
                envs
        );
    }
}

