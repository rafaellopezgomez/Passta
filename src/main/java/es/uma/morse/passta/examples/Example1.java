package es.uma.morse.passta.examples;

import java.nio.file.Path;
import java.util.List;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;
import es.uma.morse.passta.io.AutomatonViewer;
import es.uma.morse.passta.io.TraceReader;
import es.uma.morse.passta.io.TraceWriter;
import es.uma.morse.passta.validation.Validator;

public class Example1 {

    public static void main(String[] args) {
        try {
            String directoryPath = "ptp4lv4";
            String scenario = "st";

            Path tracesPath = Path.of(directoryPath, scenario + "5training.json");
            Path testPath = Path.of(directoryPath, scenario + "5validation.json");

            /*
             * Learning module
             */
            Passta passta = new Passta(tracesPath, 2);

            /*
             * Get automaton
             */
            SRTA automaton = passta.getAutomaton();

            /*
             * Show automaton in browser
             */
            AutomatonViewer.show(automaton);

            /*
             * Export module
             */
            AutomatonExporter.export(
                    automaton,
                    Path.of(directoryPath, "test.png"),
                    AutomatonExportFormat.PNG
            );

            AutomatonExporter.export(
                    automaton,
                    Path.of(directoryPath, scenario + "-" + directoryPath + ".xml"),
                    AutomatonExportFormat.UPPAAL
            );

            /*
             * Trace processing module
             */
            List<Trace> trainingTraces = TraceReader.readTraces(tracesPath);

            TraceWriter.writeTraces(
                    Path.of("learning.json"),
                    trainingTraces
            );

            /*
             * Validation module
             */
            List<Trace> testTraces = TraceReader.readTraces(testPath);

            System.out.println(
                    Validator.nValidTraces(
                            testTraces,
                            automaton,
                            Path.of(directoryPath, scenario + "Rejected").toString()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}