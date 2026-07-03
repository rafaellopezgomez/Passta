package es.uma.morse.passta.core;

import es.uma.morse.passta.core.automaton.SRTA;

public interface LearningAlgorithm {
	void learn();

	SRTA getAutomaton();
}
