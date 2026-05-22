# gradle-archi-plugin

Gradle plugin to run Archi export processing as a **regular Gradle task**.

The plugin contributes one task named `archi` (type `name.jurgenei.gradle.archi.ArchiTask`) and does not use a custom DSL extension model anymore.

## What It Does

- Registers a configurable `archi` task.
- Supports two execution backends:
  - `stub = true` (default): deterministic local/test behavior, no external Archi dependency.
  - `stub = false`: runs bundled launcher script from plugin resources.
- Accepts additional CLI args and environment variables.
- Writes one output file for each task execution.

## Plugin Coordinates

- Plugin id: `name.jurgenei.gradle.archi`
- Implementation class: `name.jurgenei.gradle.archi.ArchiGradlePlugin`
- Main task type: `name.jurgenei.gradle.archi.ArchiTask`

## Requirements

- Java 21+ (project currently builds and tests on Java 21)
- Gradle 9.x recommended (wrapper is included)

## Quick Start

Add the plugin and configure the `archi` task in `build.gradle`:

```groovy
plugins {
    id 'name.jurgenei.gradle.archi' version '0.1.0'
}

archi {
    input file('simple-helix.archimate')
    output file('build/simple-helix.export.xml')
    // default is true (stub backend)
    // stub false
}
```

Run:

```bash
./gradlew archi
```

## Using This Plugin Before Publishing

If this repository is local and not published yet, use a composite build from your consumer project:

`settings.gradle`

```groovy
pluginManagement {
    includeBuild('/absolute/path/to/gradle-archi-plugin')
}
```

Then in consumer `build.gradle`:

```groovy
plugins {
    id 'name.jurgenei.gradle.archi'
}
```

## Task Configuration

`ArchiTask` supports the following configuration methods/properties:

- `input(Object)` **required**: input model file.
- `output(Object)` **required**: export output file.
- `stub(boolean)` optional, default `true`.
- `script(Object)` optional: adds `--script.runScript <value>`.
- `excel(Object)` optional: adds `---excel.export <value>`.
- `arg(String)` optional, repeatable: appends raw CLI args.
- `env(String, Object)` optional, repeatable: sets process environment vars.

If `input` or `output` is missing, task execution fails with:

- `archi.input is required`
- `archi.output is required`

## Examples

### 1) Stub Mode (Safe Local/CI Default)

```groovy
archi {
    input file('src/main/archi/model.archimate')
    output file('build/model.export.xml')
    stub true
}
```

### 2) CLI Mode (Bundled Launcher)

```groovy
archi {
    input file('src/main/archi/model.archimate')
    output file('build/model.export.xml')
    stub false
    script file('scripts/export-assets.ajs')
    arg '--verbose'
    env 'PACKAGE_NAME', 'simple-helix'
}
```

### 3) Register Multiple Archi Tasks

```groovy
tasks.register('archiCatalog', name.jurgenei.gradle.archi.ArchiTask) {
    input file('catalog/catalog.archimate')
    output file('build/catalog.export.xml')
    stub false
}

tasks.register('archiLandscape', name.jurgenei.gradle.archi.ArchiTask) {
    input file('landscape/landscape.archimate')
    output file('build/landscape.export.xml')
    stub true
}
```

## Runtime Behavior

When `stub = false`, the plugin extracts bundled runtime resources under:

- `build/archi-runtime/`

and executes:

- `scripts/archi-launcher.sh`

with mapped environment values including `HELIX_HOME` and `SPINDLE_HOME`.

## Development

Build and test plugin:

```bash
./gradlew clean test
```

Run static compile checks:

```bash
./gradlew compileJava
```

## Project Structure (Current)

- `src/main/java/name/jurgenei/gradle/archi/ArchiGradlePlugin.java`
- `src/main/java/name/jurgenei/gradle/archi/ArchiTask.java`
- `src/main/java/name/jurgenei/gradle/archi/ArchiBackend.java`
- `src/main/java/name/jurgenei/gradle/archi/CliArchiBackend.java`
- `src/main/java/name/jurgenei/gradle/archi/StubArchiBackend.java`
- `src/main/java/name/jurgenei/gradle/archi/ArchimateRunner.java`

## Testing

Main test suites:

- `src/test/java/name/jurgenei/gradle/archi/ArchiTaskPluginTest.java`
- `src/test/java/name/jurgenei/gradle/archi/ArchimateRunnerTest.java`

Run:

```bash
./gradlew test
```

## Troubleshooting

- **Task not found (`archi`)**: ensure plugin id `name.jurgenei.gradle.archi` is applied.
- **Missing input/output error**: configure both `input` and `output` in task configuration.
- **CLI mode issues**: set `stub true` first to validate wiring, then switch to `stub false`.
- **Path problems**: prefer `file('relative/path')` over raw strings for portability.
