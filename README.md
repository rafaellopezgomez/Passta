![Logo Passta](resources/logo.png)


![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Static Badge](https://img.shields.io/badge/Licence-Affero_GPL3-blue)

🚧 In development 🚧

## Changelogs

### 07/16/2025
- New custom UPPAAL parser has been developed.
- UPPAAL api dependency removed.

### 15/10/2025
- Adjustments in merge algorithm to fix indeterminism as soon as possible in in-out edges of the resulting merged state

### 12/01/2026
- Parser show() method improved to work with all operating systems and browsers.
- The validation module has been extended with two additional methods that save rejected traces along with the reason for their rejection.

## Description

Passta is a tool that integrates an automata learning algorithm to automatically construct abstract models—Stochastic Real-Time Automata (SRTA)—from observations of real systems, such as execution traces.

## Table of Contents
- [Technologies](#technologies)
- [Installation](#installation)
- [Usage](#usage)

## Technologies
LearnTA+ depends on:
- <a href="https://www.java.com/es/" target="_blank">Java (JDK 21 or higher)</a>
- <a href="https://github.com/FasterXML/jackson" target="_blank">Jackson</a>
- <a href="https://github.com/FasterXML/jackson-modules-base/tree/2.18/blackbird" target="_blank">Jackson Blackbird module</a>
- <a href="https://github.com/jamisonjiang/graph-support" target="_blank">graph-support</a>
- <a href="https://commons.apache.org/proper/commons-io/" target="_blank">commons-io</a>

## Installation

### From source code
Pre-requisite: Java Development Kit (JDK)  21 or higher, Maven.

### JAR Files

The compiled JAR file is located in the `dist/` directory:

- **`Passta-0.2.jar`**  
  This is the shaded (fat) JAR, which includes all project dependencies.  
  Use this if you want to run or include the library without managing dependencies manually.

## Usage

### Traces format
The input traces of Passta have the form of:
```json
[ {
  "obs" : [ // Every trace starts is a list of observations
  { // Each observation has 3 fields:
    "time" : 0.0, // Elapsed time from the start of the execution
    "event" : "", // String that collects if an event has happened
    "variables" : [ "Initializing" ] // A list of system attributes
  }, {
    "time" : 1.3983775E7,
    "event" : "Init_complete",
    "variables" : [ "Listening" ]
  }, {
    "time" : 1.3986382E7,
    "event" : "Init_complete",
    "variables" : [ "Listening" ]
  }, {
    "time" : 1.4311815E7,
    "event" : "Rs_slave",
    "variables" : [ "Uncalibrated" ]
  }, {
    "time" : 1.4881755E7,
    "event" : "Master_clock_selected",
    "variables" : [ "Slave" ]
  } ]
} ]
```

There are more examples of traces in "src/main/resources".

### Trace processing module
Create a list of traces from JSON
```java
import learning_algorithm.Passta;
// Import traces
ArrayList<Trace> newTraces = Passta.readTraces("src/main/resources/"+traces);
```


### Learning module
Create a Passta class using the two possible constructors. And learn an automaton from the traces.
```java
import learning_algorithm.Passta;

Passta la = new Passta(String "path to traces", int k);
Passta la = new Passta(ArrayList<Trace> traces, int k);
```

### Get and visualize the automaton
```java
import automaton.SRTA;
import parser.Parser;

//Get automaton
SRTA a = la.getAutomaton();

// Visualize the automaton in web browser
Parser.show(a);
```

### Parsing module
```java
import automaton.SRTA;
import parser.Parser;

// Export to an image format (available formats: PNG, SVG)
Parser.exportTo("export path", a, Parser.Export.PNG);

// Export to UPPAAL model checker format (STA)
Parser.exportTo("export path", a, Parser.Export.UPPAAL);

```

### Validation module
```java
import validator.Validator;
// Check if traces are recognized by the automaton
Validator.nValidTraces(String "path to traces", a);
Validator.nValidTraces(ArrayList<Trace> traces, a);
Validator.nValidTraces(String "path to traces", a, String "path to save rejected traces");
Validator.nValidTraces(ArrayList<Trace> traces, a, String "path to save rejected traces");
```
