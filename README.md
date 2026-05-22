# gradle-archi-plugin

Standalone Gradle plugin that extracts and runs Archi execution functionality from the legacy spindle execution module.

## Plugin

- Plugin id: `name.jurgenei.gradle.archi`
- Main package: `name.jurgenei.gradle.archi`
- Main task: `archi`

## Example

```groovy
plugins {
    id 'name.jurgenei.gradle.archi'
}

archi {
    input file('simple-helix.archimate')
    output file('simple-helix.export.xml')
    // Regular task configuration
    stub false
    env 'PACKAGE_NAME', 'simple-helix'
}

tasks.named('archi').configure {
    // Optional extra CLI flags
    arg '--verbose'
}
```

## Run tests

```bash
./gradlew test
```
