<p align="center">
  <img src="resources/logo.png" alt="PASSTA logo" width="650" />
</p>

<h1 align="center">PASSTA</h1>

<p align="center">
  Learning Stochastic Real-Time Automata from execution traces.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/JavaFX-21-007396?style=for-the-badge" alt="JavaFX 21" />
  <img src="https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white" alt="Apache Maven" />
  <img src="https://img.shields.io/badge/version-0.4-red?style=for-the-badge" alt="Version 0.4" />
  <img src="https://img.shields.io/badge/status-in%20development-orange?style=for-the-badge" alt="In development" />
  <img src="https://img.shields.io/badge/platform-cross--platform-lightgrey?style=for-the-badge" alt="Cross-platform" />
  <img src="https://img.shields.io/badge/Licence-Affero_GPL3-blue?style=for-the-badge" alt="AGPL-3.0 License" />
</p>

<p align="center">
  🚧 In development 🚧
</p>

## Status

PASSTA is currently under active development. APIs, command-line options, visualization behaviour, and internal packages may change between versions.

## Quick start

The executable JAR is generated in the `dist/` directory:

```text
dist/Passta-0.4.jar
```

Move into the `dist/` directory:

```bash
cd dist
```

Open the learned automaton in the JavaFX viewer:

```bash
java -jar Passta-0.4.jar view data/traces.json 2
```

Export an automaton to SVG:

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.svg
```

Export an automaton to PNG:

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.png
```

Export an automaton to UPPAAL:

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/model.xml
```

The value of `k` must be greater than or equal to `1`.

## Features

- Learns **Stochastic Real-Time Automata (SRTA)** from JSON execution traces.
- Reads traces lazily through `Stream<Trace>`, avoiding the need to load the complete input into memory.
- Uses a central `TraceReader.readTraces(...)` entry point for JSON files and directories containing JSON files.
- Provides a command-line interface for learning, visualization, and export.
- Includes an interactive JavaFX automaton viewer.
- Exports the current viewer layout to `PNG` or vector `SVG`.
- Exports to `PNG` and `SVG` from the command line without opening the viewer window.
- Exports learned automata to `UPPAAL` XML.
- Supports trace validation against learned automata.
- Returns validation statistics through `ValidationResult`, including total, accepted, and rejected traces.
- Can save rejected traces together with the reason for their rejection.

## Changelog

### 2026-10-09 - Version 0.4

- Replaced the previous browser-oriented visualization with an interactive JavaFX viewer.
- Added interactive movement of automaton locations.
- Added interactive adjustment of transitions, parallel edges, and self-loops.
- Added independently movable transition labels that preserve their relative displacement when transitions or locations move.
- Added zoom, panning, fit-to-window, and layout reset controls.
- Added location and transition selection with a details panel.
- Added PNG and SVG export from the JavaFX viewer while preserving the current manual layout.
- Added headless-style PNG and SVG export from the CLI without opening a JavaFX window.
- Removed the Graphviz/Graphper dependency from graphical export.
- Refactored the merge methods to clarify their responsibilities and reduce duplicated merge logic.
- Updated the CLI for `view`, `PNG`, `SVG`, and `UPPAAL` workflows.
- Enforced `k >= 1` in positional and named CLI arguments.
- Updated the executable artifact name to `Passta-0.4.jar`.

### 2026-10-01 - Version 0.3.1

- `TraceReader.readTraces(...)` now returns a lazy `Stream<Trace>` instead of a materialized `List<Trace>`.
- Trace reading has been centralized in `TraceReader.readTraces(...)`, which accepts either a JSON file or a directory containing JSON files.
- Input streams and Jackson iterators are closed through the stream `onClose` mechanism and should be consumed using `try-with-resources`.
- The previous public streaming-specific usage based on `MappingIterator` has been replaced by the standard Java Stream API.
- Validation entry points have been refactored to accept a source `Path` or `String` directly, instead of requiring callers to load traces beforehand.
- Validation now processes traces incrementally and compresses each trace before checking it against the automaton.
- `Validator.nValidTraces(...)` now returns a `ValidationResult` containing the total and accepted trace counts. The rejected count and acceptance rate are derived from these values.
- Rejected traces can still be written to a JSON destination, including diagnostic information explaining the rejection.
- Java API examples and documentation have been updated for the stream-based trace-reading and validation APIs.

### 2026-07-03 - Version 0.3

- Project refactored into clearer core, I/O, automaton, trace, validation, and CLI packages.
- JSON trace processing was updated to reduce memory usage for large trace files.
- Edge labels in the browser-based automaton visualization were improved.
- Command-line interface mode added.
- Browser visualization command added.
- Location legend added to the UPPAAL export.

### 2026-01-12

- Parser `show()` method improved to work with all operating systems and browsers.
- The validation module was extended with methods that save rejected traces together with the reason for their rejection.

### 2025-10-15

- Adjustments in the merge algorithm to fix indeterminism as soon as possible in the in-out edges of the resulting merged location.

### 2025-07-16

- New custom UPPAAL parser developed.
- UPPAAL API dependency removed.

## Description

PASSTA is a tool that integrates an automata learning algorithm to automatically construct abstract models, **Stochastic Real-Time Automata (SRTA)**, from observations of real systems, such as execution traces.

PASSTA can be used in two ways:

- as a **command-line tool** through the provided CLI;
- as a **Java API** from another Java program.

## Architecture overview

```text
JSON trace file or directory
    │
    ▼
TraceReader.readTraces(...)
    │
    ▼
Stream<Trace>
    │
    ├── PASSTA learning algorithm
    │       │
    │       ▼
    │    SRTA automaton
    │       │
    │       ▼
    │    SrtaGraphAdapter
    │       │
    │       ▼
    │    JavaFX graph model and layout
    │       │
    │       ├── Interactive JavaFX viewer
    │       ├── PNG export
    │       └── Vector SVG export
    │
    │    SRTA automaton
    │       └── UPPAAL export
    │
    └── Validator
            │
            ├── ValidationResult
            └── Rejected traces JSON
```

## Table of contents

- [Status](#status)
- [Quick start](#quick-start)
- [Features](#features)
- [Changelog](#changelog)
- [Description](#description)
- [Architecture overview](#architecture-overview)
- [Technologies](#technologies)
- [Installation](#installation)
- [Command-line usage](#command-line-usage)
- [CLI examples](#cli-examples)
- [Input traces format](#input-traces-format)
- [Output formats](#output-formats)
- [Java API usage](#java-api-usage)
- [Trace processing](#trace-processing)
- [Learning](#learning)
- [Visualization](#visualization)
- [Exporting automata](#exporting-automata)
- [Validation](#validation)
- [Project structure](#project-structure)
- [Citation](#citation)
- [License](#license)

## Technologies

PASSTA depends on:

- [OpenJDK 21 or higher](https://openjdk.org/)
- [JavaFX 21](https://openjfx.io/)
- [Apache Maven](https://maven.apache.org/)
- [Jackson](https://github.com/FasterXML/jackson)
- [Jackson Blackbird module](https://github.com/FasterXML/jackson-modules-base/tree/2.18/blackbird)
- [Apache Commons IO](https://commons.apache.org/proper/commons-io/)
- [SLF4J](https://www.slf4j.org/)

Graphviz and Graphper are not required for visualization or graphical export in version 0.4.

## Installation

### Requirements

- Java Development Kit, JDK 21 or higher.
- Apache Maven, only required when building from source.
- A graphical desktop environment for the interactive JavaFX viewer.

The `export` command does not open a viewer window. On Linux systems without a display server, JavaFX may still require a virtual display such as Xvfb, depending on the runtime configuration.

### JAR file

The executable JAR file is generated in the `dist/` directory:

```text
dist/Passta-0.4.jar
```

It is a shaded, or fat, JAR that includes the project dependencies.

To run the README examples as written, first move into the `dist/` directory:

```bash
cd dist
```

Then execute PASSTA with:

```bash
java -jar Passta-0.4.jar --help
```

Recent Java versions may display native-access or unnamed-module warnings when JavaFX is loaded from the shaded JAR. If required by the installed Java runtime, native access can be enabled with:

```bash
java --enable-native-access=ALL-UNNAMED -jar Passta-0.4.jar --help
```

### From source code

Build the project from the project root:

```bash
mvn clean package
```

The compiled JAR is generated in the `dist/` directory.

## Command-line usage

PASSTA 0.4 provides a command-line interface.

### General syntax

```bash
java -jar Passta-0.4.jar <command> [options]
```

The value of `k` must be an integer greater than or equal to `1`.

### Available commands

- `view`: learns an automaton and opens it in the JavaFX viewer.
- `export`: learns an automaton and exports it to a file.
- `--help`: shows CLI help.
- `--version`: shows the current version.

### Show help

```bash
java -jar Passta-0.4.jar --help
```

### Show version

```bash
java -jar Passta-0.4.jar --version
```

### View an automaton with JavaFX

```bash
java -jar Passta-0.4.jar view data/traces.json 2
```

Named options are also supported:

```bash
java -jar Passta-0.4.jar view --input data/traces.json --k 2
```

The JavaFX viewer supports:

- zooming with the mouse wheel or the toolbar;
- panning the canvas;
- fitting the automaton to the current window;
- resetting the initial layout;
- moving locations by dragging them;
- adjusting edges and self-loops;
- moving edge labels independently;
- selecting locations and edges to inspect their details;
- exporting the current layout to PNG or SVG.

### Export an automaton

The default export format is inferred from the output file extension:

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.svg
```

The shorthand form is also supported:

```bash
java -jar Passta-0.4.jar data/traces.json 2 out/automaton.svg
```

If no output file or format is provided, PASSTA exports to:

```text
automaton.svg
```

Example:

```bash
java -jar Passta-0.4.jar data/traces.json 2
```

### Export as PNG

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.png
```

Using named options:

```bash
java -jar Passta-0.4.jar export --input data/traces.json --k 2 --output out/automaton.png
```

### Export as SVG

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.svg
```

### Export to UPPAAL

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/model.xml
```

Or explicitly:

```bash
java -jar Passta-0.4.jar export --input data/traces.json --k 2 --format UPPAAL --output out/model.xml
```

### Verbose mode

Use `--verbose` to print additional execution information:

```bash
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.svg --verbose
```

## CLI examples

```bash
# Open the learned automaton in the JavaFX viewer
java -jar Passta-0.4.jar view data/traces.json 2

# Export to SVG without opening the viewer
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.svg

# Export to PNG without opening the viewer
java -jar Passta-0.4.jar export data/traces.json 2 out/automaton.png

# Export to UPPAAL
java -jar Passta-0.4.jar export data/traces.json 2 out/model.xml
```

## Input traces format

PASSTA expects JSON files whose root element is an array of traces. Each trace contains a list of observations.

Each observation has:

- `time`: elapsed time from the beginning of the trace;
- `event`: event name, or an empty string when no event occurs;
- `variables`: list of observed system attributes.

Example:

```json
[
  {
    "obs": [
      {
        "time": 0.0,
        "event": "",
        "variables": ["Initializing"]
      },
      {
        "time": 13983775.0,
        "event": "Init_complete",
        "variables": ["Listening"]
      },
      {
        "time": 14311815.0,
        "event": "Rs_slave",
        "variables": ["Uncalibrated"]
      },
      {
        "time": 14881755.0,
        "event": "Master_clock_selected",
        "variables": ["Slave"]
      }
    ]
  }
]
```

`TraceReader.readTraces(...)` accepts either:

- a regular `.json` file;
- a directory whose directly contained `.json` files are processed in filename order.

Subdirectories are not traversed recursively.

## Output formats

PASSTA supports:

- interactive JavaFX visualization;
- `SVG` vector graphics;
- `PNG` raster images;
- `UPPAAL` XML models;
- rejected-trace JSON files with validation diagnostics.

PNG and SVG exports from the CLI use the JavaFX renderer in memory and do not open a viewer window. Exports initiated from the viewer preserve the current positions of locations, transitions, loops, and labels.

## Java API usage

PASSTA can also be used programmatically from Java.

A minimal example that learns an automaton and exports it to SVG:

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;
import es.uma.morse.passta.io.JavaFxRuntime;

public class Example {

    public static void main(String[] args) {
        Passta passta = new Passta(
            Path.of("data/traces.json"),
            2
        );

        SRTA automaton = passta.getAutomaton();

        try {
            AutomatonExporter.export(
                automaton,
                Path.of("out/automaton.svg"),
                AutomatonExportFormat.SVG
            );
        } finally {
            JavaFxRuntime.shutdown();
        }
    }
}
```

The explicit `JavaFxRuntime.shutdown()` call is appropriate for short-lived command-line programs that export PNG or SVG and then terminate. Do not call it after an export performed from an active JavaFX viewer.

## Trace processing

The central entry point for reading traces is `TraceReader.readTraces(...)`. It returns a sequential `Stream<Trace>` and accepts either a JSON file or a directory containing JSON files.

Because the stream may keep input files open while it is consumed, close it using `try-with-resources`:

```java
import java.nio.file.Path;
import java.util.stream.Stream;

import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.TraceReader;

public class TraceReaderExample {

    public static void main(String[] args) {
        Path source = Path.of(
            "src/main/resources/traces.json"
        );

        try (Stream<Trace> traces =
                TraceReader.readTraces(source)) {

            traces.forEach(System.out::println);
        }
    }
}
```

The same method can read all JSON files directly contained in a directory:

```java
Path sourceDirectory = Path.of(
    "src/main/resources/traces"
);

try (Stream<Trace> traces =
        TraceReader.readTraces(sourceDirectory)) {

    long numberOfTraces = traces.count();

    System.out.println(
        "Loaded traces: " + numberOfTraces
    );
}
```

A stream is single-use. After a terminal operation such as `count()`, `forEach(...)`, `toList()`, or `collect(...)`, open a new stream if the traces must be processed again.

If a component requires a list, materialize the stream explicitly:

```java
try (Stream<Trace> traceStream =
        TraceReader.readTraces(source)) {

    List<Trace> traces = traceStream.toList();

    TraceWriter.writeTraces(
        destination,
        traces
    );
}
```

## Learning

Create a `Passta` instance from a JSON trace source and a value for `k`. The value of `k` must be at least `1`.

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;

public class LearningExample {

    public static void main(String[] args) {
        Path tracesPath = Path.of(
            "src/main/resources/traces.json"
        );

        int k = 2;

        Passta passta = new Passta(
            tracesPath,
            k
        );

        SRTA automaton = passta.getAutomaton();

        System.out.println(automaton);
    }
}
```

## Visualization

Automata can be shown in the JavaFX viewer using `AutomatonViewer`:

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonViewer;

public class JavaFxVisualizationExample {

    public static void main(String[] args) {
        Passta passta = new Passta(
            Path.of("src/main/resources/traces.json"),
            2
        );

        SRTA automaton = passta.getAutomaton();

        AutomatonViewer.show(automaton);
    }
}
```

The viewer supports interactive editing of the displayed layout. These changes affect exports initiated from the viewer but do not modify the underlying learned `SRTA` model.

## Exporting automata

Automata can be exported using `AutomatonExporter`.

### Export to SVG

```java
AutomatonExporter.export(
    automaton,
    Path.of("out/automaton.svg"),
    AutomatonExportFormat.SVG
);
```

### Export to PNG

```java
AutomatonExporter.export(
    automaton,
    Path.of("out/automaton.png"),
    AutomatonExportFormat.PNG
);
```

### Export to UPPAAL

```java
AutomatonExporter.export(
    automaton,
    Path.of("out/model.xml"),
    AutomatonExportFormat.UPPAAL
);
```

PNG and SVG export initialize JavaFX internally. A standalone Java process that performs a graphical export and then ends should close the runtime with `JavaFxRuntime.shutdown()`.

## Validation

The validation module checks whether traces are accepted by a learned automaton. Callers pass the validation source directly to `Validator`, so validation traces do not need to be loaded into a list beforehand.

`Validator.nValidTraces(...)` returns a `ValidationResult` with:

- `totalTraces()`: total number of processed traces;
- `acceptedTraces()`: number of traces accepted by the automaton;
- `rejectedTraces()`: number of rejected traces;
- `acceptanceRate()`: accepted proportion from `0.0` to `1.0`.

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.validation.ValidationResult;
import es.uma.morse.passta.validation.Validator;

public class ValidationExample {

    public static void main(String[] args) {
        Path trainingSource = Path.of(
            "src/main/resources/training-traces.json"
        );

        Path validationSource = Path.of(
            "src/main/resources/validation-traces.json"
        );

        Passta passta = new Passta(
            trainingSource,
            2
        );

        SRTA automaton = passta.getAutomaton();

        ValidationResult result =
            Validator.nValidTraces(
                validationSource,
                automaton
            );

        System.out.println(
            "Total traces: "
                + result.totalTraces()
        );

        System.out.println(
            "Accepted traces: "
                + result.acceptedTraces()
        );

        System.out.println(
            "Rejected traces: "
                + result.rejectedTraces()
        );

        System.out.printf(
            "Acceptance rate: %.2f%%%n",
            result.acceptanceRate() * 100.0
        );
    }
}
```

Rejected traces can be saved with their rejection reason:

```java
Path rejectedOutput = Path.of(
    "out/rejected-traces.json"
);

ValidationResult result = Validator.nValidTraces(
    validationSource,
    automaton,
    rejectedOutput
);
```

During validation, each trace is compressed using `Passta.compressTrace(...)` before it is checked against the automaton. For direct validation of a single trace, use:

```java
boolean accepted = Validator.checkTrace(
    trace,
    automaton
);
```

## Project structure

```text
src/main/java/es/uma/morse/passta
├── cli               Command-line interface
├── core              PASSTA learning algorithm and core model
├── core/automaton    SRTA locations, transitions, and automata
├── core/trace        Trace and observation model
├── io                Readers, writers, JavaFX viewer, exporters, and UPPAAL output
└── validation        Trace validation and ValidationResult
```

The graphical export implementation is separated into runtime, preparation, path-resolution, PNG, and SVG components so that the viewer and command-line exports share the same rendered geometry.

## Citation

If you use PASSTA in academic work, please cite the following article:

```bibtex
@article{LOPEZGOMEZ2026101142,
  title = {Towards a formal digital twin of the PTP protocol using automata learning},
  journal = {Journal of Logical and Algebraic Methods in Programming},
  volume = {151},
  pages = {101142},
  year = {2026},
  issn = {2352-2208},
  doi = {10.1016/j.jlamp.2026.101142},
  url = {https://www.sciencedirect.com/science/article/pii/S2352220826000349},
  author = {Rafael López-Gómez and Delia Rico and Laura Panizo and María-del-Mar Gallardo},
  keywords = {Time-Sensitive Systems, Formal Digital Twin, Formal Verification}
}
```

## License

This project is licensed under the GNU Affero General Public License v3.0.

See the `LICENSE` file for details.
