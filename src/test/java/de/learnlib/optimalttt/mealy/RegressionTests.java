package de.learnlib.optimalttt.mealy;

import de.learnlib.examples.LearningExample;
import de.learnlib.examples.LearningExamples;
import de.learnlib.oracle.equivalence.SimulatorEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.util.Experiment;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.StateLocalInputMealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

public class RegressionTests {

    @Test
    public void testRegression1() {

        LearningExample.StateLocalInputMealyLearningExample<?, ?> example =
                LearningExamples.createSLIMealyExamples().get(0);

        final Alphabet<?> alphabet = example.getAlphabet();
        final StateLocalInputMealyMachine<?, ?, ?, ?> automaton =
                example.getReferenceAutomaton();

        final SimulatorOracle<?, Word<?>> mqo = new SimulatorOracle(automaton);
        final SimulatorEQOracle<MealyMachine<?, ?, ?, ?>, ?, Word<?>> eqo =
                new SimulatorEQOracle(automaton);

        final Experiment<MealyMachine<?, ?, ?, ?>> optExp =
                new Experiment(new OptimalTTTMealy(mqo, mqo, alphabet), eqo, alphabet);
        optExp.run();

    }
}
