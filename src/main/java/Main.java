import java.nio.file.Paths;

import automaton.EDRTA;
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
			
			/**** Get automaton *****/
			EDRTA a = la.getEDRTA();
			a.computeProbs();
			
			/***** Show in browser *****/
			Parser.show(a);
			
			/**** Parsing module *****/
//			Parser.exportTo(directoryPath + "/test", a, Parser.Export.PNG);
//			Parser.exportTo(directoryPath + "/" + scenario + "-" + directoryPath, a, Parser.Export.UPPAAL);
			
			/**** Validation module *****/
			var testTraces = Passta.readTraces(Paths.get(testPath).toFile());
			System.out.println(Validator.nValidTraces(testTraces, a, directoryPath + "/" + scenario + "Rejected/"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
