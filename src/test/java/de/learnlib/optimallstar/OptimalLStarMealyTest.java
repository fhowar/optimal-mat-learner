package de.learnlib.optimallstar;


import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.statistic.oracle.MealyCounterOracle;
import de.learnlib.importers.dot.DOTImporter;
import de.learnlib.oracle.equivalence.SimulatorEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.util.Experiment;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

public class OptimalLStarMealyTest {

    private CompactMealy<String, String> mealy;

    @Test
    public void testOptimalLStarMealy() throws IOException {

        final Random s = new Random();
        long seed = 1130032374111885818L; //s.nextLong();
        System.out.println(seed);
        final Random r = new Random(seed);

        final int ceLength = 100;
        mealy = DOTImporter.read(OptimalLStarMealyTest.class.getResourceAsStream(
                "/learnresult_old_500_10-15_fix.dot"));
        //mealy = constructMealy();

        Alphabet<String> inputs = mealy.getInputAlphabet();

        MembershipOracle.MealyMembershipOracle<String, String> sul =
                new SimulatorOracle.MealySimulatorOracle<>(mealy);

        MealyCounterOracle<String, String> mqOracle =
                new MealyCounterOracle<>(sul, "mq");

        MealyCounterOracle<String, String> ceOracle =
                new MealyCounterOracle<>(sul, "ce");


        // construct L* instance
        OptimalLStarMealy<String, String> lstar = new OptimalLStarMealy<>(
                inputs, mqOracle, ceOracle);


        EquivalenceOracle.MealyEquivalenceOracle<String, String> eqOracle =
//               new SimulatorEQOracle.MealySimulatorEQOracle<>(mealy);
                new EquivalenceOracle.MealyEquivalenceOracle<String, String>() {
                    @Override
                    public DefaultQuery<String, Word<String>> findCounterExample(
                            MealyMachine<?, String, ?, String> hyp, Collection<? extends String> collection) {
                        return generateCounterexample( r, mealy, hyp, ceLength);
                    }
                };

        Experiment.MealyExperiment<String, String> experiment =
                new Experiment.MealyExperiment<>(lstar, eqOracle, inputs);

        // turn on time profiling
        experiment.setProfile(true);

        // enable logging of models
        experiment.setLogModels(true);

        // run experiment
        experiment.run();

        lstar.assertShortPrefixes();

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

    private static <I,O> DefaultQuery<I, Word<O>>
    generateCounterexample(Random random, CompactMealy<I, O> target,
                           MealyMachine<?,I,?,O> hypothesis, int ceLength) {
        Alphabet<I> alphabet = target.getInputAlphabet();

        if(Automata.findSeparatingWord(target, hypothesis, alphabet) == null) {
            return null;
        }

        Word<I> ce = Automata.findSeparatingWord(target, hypothesis, alphabet);
        if(ce == null) {
            return null;
        }

        Word<I> word = null;

        for (int i=0; i<10000; i++) {
            WordBuilder<I> wb = new WordBuilder<>();
            for(int j = 0; j < ceLength; j++) {
                wb.append(alphabet.getSymbol(random.nextInt(alphabet.size())));
            }
            word = wb.toWord();
            Word<O> out1 = target.computeOutput(word);
            Word<O> out2 = hypothesis.computeOutput(word);
            if ( !out1.equals(out2) ) {
                int k = 0;
                while (out1.getSymbol(k).equals(out2.getSymbol(k))) {
                    k++;
                }
                word = word.prefix(k+1);
                break;
            }
            word = null;
        }

        if (word == null) {
            word = ce;
        }

        return new DefaultQuery<>(word, target.computeOutput(word));
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