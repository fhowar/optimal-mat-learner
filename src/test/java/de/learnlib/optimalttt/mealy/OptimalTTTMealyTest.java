package de.learnlib.optimalttt.mealy;

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.MembershipOracle;
import de.learnlib.eqtests.basic.SimulatorEQOracle;
import de.learnlib.experiments.Experiment;
import de.learnlib.importers.dot.DOTImporter;
import de.learnlib.oracles.CounterOracle;
import de.learnlib.oracles.SimulatorOracle;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.compact.CompactMealy;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Random;

public class OptimalTTTMealyTest {

    private CompactMealy<String, String> mealy;

    @Test
    public void testOptimalLStarMealy() throws IOException {

        final Random s = new Random();
        long seed = s.nextLong();
        System.out.println(seed);
        final Random r = new Random(seed);

        final int ceLength = 200;
        mealy = DOTImporter.read(OptimalTTTMealyTest.class.getResourceAsStream(
                "/VerneMQ__two_client_will_retain.dot"));
        //mealy = constructMealy();

        Alphabet<String> inputs = mealy.getInputAlphabet();

        MembershipOracle.MealyMembershipOracle<String, String> sul =
                new SimulatorOracle.MealySimulatorOracle<>(mealy);

        CounterOracle.MealyCounterOracle<String, String> mqOracle =
                new CounterOracle.MealyCounterOracle<>(sul, "mq");

        CounterOracle.MealyCounterOracle<String, String> ceOracle =
                new CounterOracle.MealyCounterOracle<>(sul, "ce");


        // construct L* instance
        OptimalTTTMealy<String, String> ttt = new OptimalTTTMealy<>(
                mqOracle, ceOracle, inputs);


        EquivalenceOracle.MealyEquivalenceOracle<String, String> eqOracle =
               new SimulatorEQOracle.MealySimulatorEQOracle<>(mealy);
//                new EquivalenceOracle.MealyEquivalenceOracle<String, String> {
//                    @Override
//                    public DefaultQuery findCounterExample(Object a, Collection clctn) {
//                        return generateCounterexample( (DFA<?,Character>)a, ceLength, r);
//                    }
//                };

        Experiment.MealyExperiment<String, String> experiment =
                new Experiment.MealyExperiment<>(ttt, eqOracle, inputs);

        // turn on time profiling
        experiment.setProfile(true);

        // enable logging of models
        experiment.setLogModels(true);

        // run experiment
        experiment.run();

        // get learned model
        MealyMachine<?, String, ?, String> result = experiment.getFinalHypothesis();

        // report results
        System.out.println("-------------------------------------------------------");

        // profiling
        System.out.println(SimpleProfiler.getResults());

        // learning statistics
        System.out.println(experiment.getRounds().getSummary());
        System.out.println(mqOracle.getStatisticalData().getSummary());
        System.out.println(ceOracle.getStatisticalData().getSummary());

        // model statistics
        System.out.println("States: " + result.size());
        System.out.println("Sigma: " + inputs.size());

        // show model
        System.out.println();
        System.out.println("Model: ");


        System.out.println("-------------------------------------------------------");

        System.out.println("Final observation table:");
        //System.out.println(Arrays.toString(lstar.getSuffixes()));

    }


    private CompactMealy<String, String> constructMealy() {
        // input alphabet contains characters 'a'..'b'
        Alphabet<String> sigma = Alphabets.fromArray("a", "b");

        // @formatter:off
        // create automaton
        return AutomatonBuilders.<String, String>newMealy(sigma)
                .withInitial("q0")
                .from("q0")
                .on("a").withOutput("0").to("q0")
                .on("b").withOutput("0").to("q1")
                .from("q1")
                .on("a").withOutput("0").to("q0")
                .on("b").withOutput("0").to("q2")
                .from("q2")
                .on("a").withOutput("1").to("q2")
                .on("b").withOutput("0").to("q0")
                .create();
        // @formatter:on
    }
}