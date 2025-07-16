![Logo Passta](resources/logo.png)


![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Static Badge](https://img.shields.io/badge/Licence-Affero_GPL3-blue)

🚧 In development 🚧

## Changelog 07/16/2025
- New custom UPPAAL parser has been developed.
- UPPAAL api dependency removed.

## Description

Passta is a passive automata learning algorithm to automatically construct abstract models (Stochastic Real-Time Automata) from observations (execution traces) of real systems.

## Table of Contents
- [Technologies](#Technologies)
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

## Usage

### Traces format
The input traces of LearnTA have the form of:
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

### Learning module
Create a Passta class using the two possible constructors. And learn an automaton from the traces.
```java
import learning_algorithm.Passta;

Passta la = new Passta(String "path to traces", int k);
Passta la = new Passta(ArrayList<Trace> traces, int k);
```

### Get and visualize the automaton
```java
import automaton.EDRTA;
import parser.Parser;

//Get automaton
EDRTA a = la.getEDRTA();
// Compute probabilities
a.computeProbs();

// Visualize the automaton in web browser
Parser.show(a);
```

### Parsing module
```java
import automaton.EDRTA;
import parser.Parser;

// Export to an image format (available formats: PNG, SVG)
Parser.exportTo("export path", a, Parser.Export.PNG);

// Export to UPPAAL model checker format (STA)
Parser.exportTo("export path", a, Parser.Export.UPPAAL);

```

### Verification module
```java
import validator.Validator;
// Check if traces are recognized by the automaton
Validator.nValidTraces(String "path to traces", a);
Validator.nValidTraces(ArrayList<Trace> traces, a);
```

### Create a list of traces from JSON

```java
import learning_algorithm.Passta;
// Import traces
ArrayList<Trace> newTraces = Passta.readTraces("src/main/resources/"+traces);
```
