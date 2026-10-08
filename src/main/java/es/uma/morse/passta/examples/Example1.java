package es.uma.morse.passta.examples;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;
import es.uma.morse.passta.io.AutomatonViewer;
import es.uma.morse.passta.io.TraceReader;
import es.uma.morse.passta.io.TraceWriter;
import es.uma.morse.passta.validation.ValidationResult;
import es.uma.morse.passta.validation.Validator;

public class Example1 {

	public static void main(String[] args) {

		try {
			/*
			 * Configuration
			 */
			Path directoryPath = Path.of("ptp4lv4");
			String scenario = "disc";

			Path trainingPath = directoryPath.resolve(scenario + "5training.json");

			Path validationPath = directoryPath.resolve(scenario + "5validation.json");

			Path learningOutputPath = directoryPath.resolve(scenario + "5learning.json");

			Path rejectedTracesPath = directoryPath.resolve(scenario + "5rejected.json");

			/*
			 * Learning module
			 */
			Passta passta = new Passta(trainingPath, 2);

			/*
			 * Get learned automaton
			 */
			SRTA automaton = passta.getAutomaton();

			/*
			 * Show automaton in browser
			 */
			AutomatonViewer.show(automaton);

			/*
			 * Export automaton as PNG
			 */
			AutomatonExporter.export(automaton,
					directoryPath.resolve(scenario + "-" + directoryPath.getFileName() + ".png"),
					AutomatonExportFormat.PNG);

			/*
			 * Export automaton in UPPAAL format
			 */
			AutomatonExporter.export(automaton,
					directoryPath.resolve(scenario + "-" + directoryPath.getFileName() + ".xml"),
					AutomatonExportFormat.UPPAAL);

			/*
			 * Trace processing module
			 */
			try (Stream<Trace> trainingTraceStream = TraceReader.readTraces(trainingPath)) {

				List<Trace> trainingTraces = trainingTraceStream.toList();

				TraceWriter.writeTraces(learningOutputPath, trainingTraces);
			}

			/*
			 * Validation module
			 *
			 * Validator processes the validation file once and returns the total number of
			 * traces and the number of accepted traces.
			 */
			ValidationResult validationResult = Validator.nValidTraces(validationPath, automaton, rejectedTracesPath);

			/*
			 * Validation summary
			 */
			System.out.println();
			System.out.println("Validation results");
			System.out.println("------------------");

			System.out.println("Validation traces: " + validationResult.totalTraces());

			System.out.println("Accepted traces:   " + validationResult.acceptedTraces());

			System.out.println("Rejected traces:   " + validationResult.rejectedTraces());

			System.out.printf("Acceptance rate:   %.2f%%%n", validationResult.acceptanceRate() * 100.0);

			if (validationResult.rejectedTraces() > 0) {
				System.out.println("Rejected traces file: " + rejectedTracesPath.toAbsolutePath().normalize());
			}

		} catch (Exception e) {
			System.err.println("Error while executing Example1: " + e.getMessage());

			e.printStackTrace();
		}
	}
}