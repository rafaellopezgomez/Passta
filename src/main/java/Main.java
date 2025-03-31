import automaton.EDRTA;
import learning_algorithm.Passta;
import parser.Parser;
import validator.Validator;


public class Main {

	public static void main(String[] args) {
		
		try {
//			String traces = "src/main/resources/normal.json";
//			String traces = "src/main/resources/disconnection.json";
			String traces = "src/main/resources/at.json";
//			String traces = "src/main/resources/st.json";
//			String traces = "src/main/resources/delay.json";
//			String traces = "src/main/resources/example.json";
			
			/***** Create new LearnTA class *****/
			Passta la = new Passta(traces, 2);
			
			/**** Get automaton *****/
			EDRTA a = la.getEDRTA();
//			Parser.show(a);
			a.computeProbs();
			
			/***** Show in browser *****/
//			Parser.show(a);
//			Parser.show(ga);

			/***** Import *****/
//			EDRTA b = Parser.importFrom("Automaton.dot", Parser.Import.DOT);
			
			/**** Parsing module *****/
//			Parser.exportTo("images/st", a, Parser.Export.PNG);
//			Parser.exportTo("images/st", a, Parser.Export.PNG);
			Parser.exportTo("test/test", a, Parser.Export.UPPAAL);
			
			/**** Verification module *****/
			Validator.nValidTraces(traces, a);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
