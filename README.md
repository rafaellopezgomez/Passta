<p align="center">
  resources/logo.png
</p>

<p align="center">
  https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white
  https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white
  https://img.shields.io/badge/Licence-Affero_GPL3-blue
</p>

<p align="center">
  🚧 In development 🚧
</p>

## Changelog

### 16/07/2025
- New custom UPPAAL parser has been developed.
- UPPAAL API dependency removed.

### 15/10/2025
- Adjustments in the merge algorithm to fix indeterminism as soon as possible in the in-out edges of the resulting merged state.

### 12/01/2026
- Parser `show()` method improved to work with all operating systems and browsers.
- The validation module has been extended with two additional methods that save rejected traces along with the reason for their rejection.

### 03/07/2026 - Version 0.3
- Project refactored into clearer core, I/O, automaton, trace, and CLI packages.
- JSON trace processing is now performed using streaming, reducing memory usage for large trace files.
- Edge labels in the browser-based automaton visualization have been improved.
- Command-line interface mode added.
- Location legend added to the UPPAAL export.

## Description

Passta is a tool that integrates an automata learning algorithm to automatically construct abstract models — Stochastic Real-Time Automata (SRTA) — from observations of real systems, such as execution traces.

The tool can be used both:

- programmatically as a Java library;
- from the command line through the provided CLI.

## Table of Contents

- [Technologies](#technologies)
- [Installation](#installation)
- [Usage](#usage)
  - [Traces format](#traces-format)
  - [Command-line interface](#command-line-interface)
  - [Trace processing module](#trace-processing-module)
  - [Learning module](#learning-module)
  - [Automaton visualization](#automaton-visualization)
  - [Automaton export](#automaton-export)
  - [Validation module](#validation-module)

## Technologies

Passta depends on:

- <a href="https://www.java.com/" target="_blank">Java JDK 21 or higher</a>
- <a href="https://github.com/FasterXML/jackson" target="_blank">Jackson</a>
- <a href="https://github.com/FasterXML/jackson-modules-base/tree/2.18/blackbird" target="_blank">Jackson Blackbird module</a>
- <a href="https://github.com/jamisonjiang/graph-support" target="_blank">graph-support</a>
- <a href="https://commons.apache.org/proper/commons-io/" target="_blank">commons-io</a>
- <a href="https://www.slf4j.org/" target="_blank">SLF4J</a>

## Installation

### From source code

Prerequisites:

- Java Development Kit, JDK 21 or higher;
- Apache Maven.

Build the project with:

```bash
mvn clean package
```

The compiled JAR file is generated in the `dist/` directory.

### JAR file

The compiled JAR file is located in the `dist/` directory:

- **`Passta-0.3.jar`**

This is a shaded, or fat, JAR that includes all project dependencies.

It can be used directly as a command-line tool:

```bash
java -jar dist/Passta-0.3.jar view data/traces.json 2
```

or included manually in another Java project if required.

## Usage

### Traces format

The input traces of Passta are JSON files containing a list of traces.

Each trace contains a list of observations. Each observation has:

- `time`: elapsed time from the start of the execution;
- `event`: event name, or an empty string if no event occurred;
- `variables`: list of system attributes observed at that time.

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
        "time": 13986382.0,
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

More examples of traces can be found in:

```text
src/main/resources
```

---

## Command-line interface

Passta 0.3 adds a command-line interface.

### Show help

```bash
java -jar dist/Passta-0.3.jar --help
```

### Show version

```bash
java -jar dist/Passta-0.3.jar --version
```

### Learn and export an automaton

Default export format is inferred from the output file extension.

```bash
java -jar dist/Passta-0.3.jar export data/traces.json 2 out/automaton.svg
```

The following shorthand form is also supported:

```bash
java -jar dist/Passta-0.3.jar data/traces.json 2 out/automaton.svg
```

If no output file is provided, Passta exports to:

```text
automaton.svg
```

Example:

```bash
java -jar dist/Passta-0.3.jar data/traces.json 2
```

### Export as PNG

```bash
java -jar dist/Passta-0.3.jar export data/traces.json 2 out/automaton.png
```

or using named options:

```bash
java -jar dist/Passta-0.3.jar export \
  --input data/traces.json \
  --k 2 \
  --output out/automaton.png
```

### Export to UPPAAL

```bash
java -jar dist/Passta-0.3.jar export data/traces.json 2 out/model.xml
```

or explicitly:

```bash
java -jar dist/Passta-0.3.jar export \
  --input data/traces.json \
  --k 2 \
  --format UPPAAL \
  --output out/model.xml
```

### Visualize in browser

The `view` command learns the automaton and opens an interactive SVG viewer in the default browser.

```bash
java -jar dist/Passta-0.3.jar view data/traces.json 2
```

or:

```bash
java -jar dist/Passta-0.3.jar view \
  --input data/traces.json \
  --k 2
```

The browser viewer supports zooming and panning.

### Verbose mode

Use `--verbose` to print additional execution information:

```bash
java -jar dist/Passta-0.3.jar view data/traces.json 2 --verbose
```

---

## Trace processing module

Traces can be read from JSON using `TraceReader`.

```java
import java.nio.file.Path;
import java.util.List;

import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.TraceReader;

public class TraceReaderExample {

    public static void main(String[] args) {
        Path tracesPath = Path.of("src/main/resources/traces.json");

        List<Trace> traces = TraceReader.readTraces(tracesPath);

        System.out.println("Loaded traces: " + traces.size());
    }
}
```

For large trace files, Passta also supports streaming traces without loading the whole file into memory:

```java
import java.nio.file.Path;

import com.fasterxml.jackson.databind.MappingIterator;

import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.TraceReader;

public class TraceStreamingExample {

    public static void main(String[] args) throws Exception {
        Path tracesPath = Path.of("src/main/resources/traces.json");

        try (MappingIterator<Trace> traces = TraceReader.streamTraces(tracesPath)) {
            while (traces.hasNext()) {
                Trace trace = traces.next();
                System.out.println(trace);
            }
        }
    }
}
```

---

## Learning module

Create a `Passta` instance from a JSON traces file and a value for `k`.

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;

public class LearningExample {

    public static void main(String[] args) {
        Path tracesPath = Path.of("src/main/resources/traces.json");
        int k = 2;

        Passta passta = new Passta(tracesPath, k);
        SRTA automaton = passta.getAutomaton();

        System.out.println(automaton);
    }
}
```

You can also create the learner from a string path:

```java
import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;

public class LearningFromStringExample {

    public static void main(String[] args) {
        Passta passta = new Passta("src/main/resources/traces.json", 2);
        SRTA automaton = passta.getAutomaton();

        System.out.println(automaton);
    }
}
```

---

## Automaton visualization

Automata can be visualized in the default web browser using `AutomatonViewer`.

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonViewer;

public class BrowserVisualizationExample {

    public static void main(String[] args) {
        Passta passta = new Passta(Path.of("src/main/resources/traces.json"), 2);
        SRTA automaton = passta.getAutomaton();

        AutomatonViewer.show(automaton);
    }
}
```

The browser visualization provides an interactive SVG view with zooming and panning.

---

## Automaton export

Automata can be exported using `AutomatonExporter`.

Supported export formats are:

- `SVG`
- `PNG`
- `UPPAAL`

### Export to SVG

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;

public class SvgExportExample {

    public static void main(String[] args) {
        Passta passta = new Passta(Path.of("src/main/resources/traces.json"), 2);
        SRTA automaton = passta.getAutomaton();

        AutomatonExporter.export(
            automaton,
            Path.of("out/automaton.svg"),
            AutomatonExportFormat.SVG
        );
    }
}
```

### Export to PNG

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;

public class PngExportExample {

    public static void main(String[] args) {
        Passta passta = new Passta(Path.of("src/main/resources/traces.json"), 2);
        SRTA automaton = passta.getAutomaton();

        AutomatonExporter.export(
            automaton,
            Path.of("out/automaton.png"),
            AutomatonExportFormat.PNG
        );
    }
}
```

### Export to UPPAAL

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;

public class UppaalExportExample {

    public static void main(String[] args) {
        Passta passta = new Passta(Path.of("src/main/resources/traces.json"), 2);
        SRTA automaton = passta.getAutomaton();

        AutomatonExporter.export(
            automaton,
            Path.of("out/model.xml"),
            AutomatonExportFormat.UPPAAL
        );
    }
}
```

---

## Validation module

The validation module can be used to check whether traces are accepted by a learned automaton.

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.validator.Validator;

public class ValidationExample {

    public static void main(String[] args) {
        Path trainingTraces = Path.of("src/main/resources/training-traces.json");
        Path validationTraces = Path.of("src/main/resources/validation-traces.json");

        Passta passta = new Passta(trainingTraces, 2);
        SRTA automaton = passta.getAutomaton();

        Validator.nValidTraces(validationTraces.toString(), automaton);
    }
}
```

Rejected traces can also be saved together with the reason for their rejection:

```java
import java.nio.file.Path;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.validator.Validator;

public class ValidationWithRejectedTracesExample {

    public static void main(String[] args) {
        Path trainingTraces = Path.of("src/main/resources/training-traces.json");
        Path validationTraces = Path.of("src/main/resources/validation-traces.json");
        Path rejectedOutput = Path.of("out/rejected-traces.json");

        Passta passta = new Passta(trainingTraces, 2);
        SRTA automaton = passta.getAutomaton();

        Validator.nValidTraces(
            validationTraces.toString(),
            automaton,
            rejectedOutput.toString()
        );
    }
}
```

