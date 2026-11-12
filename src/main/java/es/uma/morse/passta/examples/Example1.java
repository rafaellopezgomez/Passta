package es.uma.morse.passta.examples;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.export.AutomatonExportFormat;
import es.uma.morse.passta.export.AutomatonExporter;
import es.uma.morse.passta.trace.io.TraceReader;
import es.uma.morse.passta.trace.io.TraceWriter;
import es.uma.morse.passta.validation.ValidationResult;
import es.uma.morse.passta.validation.Validator;
import es.uma.morse.passta.visualization.javafx.AutomatonViewer;

public class Example1 {

	public static void main(String[] args) {

		Path directory = Path.of("ptp4lv4");
		String scenario = "disc";
		int k = 2;

		Path trainingPath = directory.resolve(scenario + "5training.json");

		Path validationPath = directory.resolve(scenario + "5validation.json");

		Path trainingCopyPath = directory.resolve(scenario + "training-copy.json");

		Path rejectedTracesPath = directory.resolve(scenario + "5rejected.json");

		Path pngOutputPath = directory.resolve(scenario + "-ptp4lv4.png");

		Path uppaalOutputPath = directory.resolve(scenario + "-ptp4lv4.xml");
		
		try {
            /*
             * Learn the automaton.
             */
			Passta passta = new Passta(trainingPath, k);
			SRTA automaton = passta.getAutomaton();

			/*
			 * Show automaton
			 */
			AutomatonViewer.show(automaton);

            /*
             * Export the automaton.
             */
            AutomatonExporter.export(
                automaton,
                pngOutputPath,
                AutomatonExportFormat.PNG
            );

            AutomatonExporter.export(
                automaton,
                uppaalOutputPath,
                AutomatonExportFormat.UPPAAL
            );

            /*
             * Read and write the training traces.
             */
            try (Stream<Trace> traceStream =
                    TraceReader.readTraces(trainingPath)) {

                List<Trace> traces = traceStream.toList();

                TraceWriter.writeTraces(
                    trainingCopyPath,
                    traces
                );
            }

            /*
             * Validate additional traces.
             */
            ValidationResult result =
                Validator.nValidTraces(
                    validationPath,
                    automaton,
                    rejectedTracesPath
                );

            System.out.println();
            System.out.println("Validation results");
            System.out.println("------------------");
            System.out.println(
                "Validation traces: " + result.totalTraces()
            );
            System.out.println(
                "Accepted traces:   " + result.acceptedTraces()
            );
            System.out.println(
                "Rejected traces:   " + result.rejectedTraces()
            );
            System.out.printf(
                "Acceptance rate:   %.2f%%%n",
                result.acceptanceRate() * 100.0
            );

            if (result.rejectedTraces() > 0) {
                System.out.println(
                    "Rejected traces: "
                        + rejectedTracesPath
                            .toAbsolutePath()
                            .normalize()
                );
            }

		} catch (Exception e) {
			System.err.println("Error while executing Example1: " + e.getMessage());

			e.printStackTrace();
		}
	}
}