import java.nio.file.Paths;

import automaton.SRTA;
import learning_algorithm.Passta;
import parser.Parser;
import validator.Validator;


public class Main {

	public static void main(String[] args) {
		
		try {
//			String directoryPath = "ptp4lv4"; 
			String directoryPath = "ptp4lv4";
			String scenario = "st";
			String traces = directoryPath + "/" + scenario + "5training.json";
			String testPath = directoryPath + "/" + scenario + "5validation.json";
			
			/***** Create new LearnTA class *****/
			Passta la = new Passta(traces, 2);
			
			/***** Get automaton *****/
			SRTA a = la.getAutomaton();

			/***** Show in browser *****/
			Parser.show(a);
			
			/***** Parsing module *****/
			Parser.exportSRTA(directoryPath + "/test", a, Parser.Export.PNG);
			Parser.exportSRTA(directoryPath + "/" + scenario + "-" + directoryPath, a, Parser.Export.UPPAAL);
			Parser.writeTraces("./learning.json", Passta.readTraces(Paths.get(traces).toFile()));
			
			/***** Validation module *****/
			var testTraces = Passta.readTraces(Paths.get(testPath).toFile());
			System.out.println(Validator.nValidTraces(testTraces, a, directoryPath + "/" + scenario + "Rejected/"));
			
			/***** Trace processing module *****/
			var otherTraces = Passta.readTraces(Paths.get(testPath).toFile());
			otherTraces = Passta.compressTraces(otherTraces);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
