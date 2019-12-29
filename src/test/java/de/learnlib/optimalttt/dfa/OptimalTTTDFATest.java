/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt.dfa;


import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.util.Experiment;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.util.automata.random.RandomAutomata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

/**
 *
 * @author falk
 */
public class OptimalTTTDFATest {
    
    private CompactDFA dfa;
    
    @Test
    public void testOptimalLStarDFA() throws IOException {
        
        final Random s = new Random();
        long seed = s.nextLong();
        System.out.println(seed);
        final Random r = new Random(seed);
        
//        dfa = createDFA(r, 60, 2);
        final int ceLength = 200;
//        dfa = AUTImporter.read(
//                OptimalLStarDFATest.class.getResourceAsStream("/peterson2.dfa.gz"));
        dfa = constructSUL();
        
        Alphabet<Character> inputs = dfa.getInputAlphabet();

        MembershipOracle.DFAMembershipOracle<Character> sul =
                new SimulatorOracle.DFASimulatorOracle<>(dfa);

        CounterOracle.DFACounterOracle<Character> mqOracle =
                new CounterOracle.DFACounterOracle<>(sul, "mq");

        CounterOracle.DFACounterOracle<Character> ceOracle =
                new CounterOracle.DFACounterOracle<>(sul, "ce");

        OptimalTTTDFA ttt = new OptimalTTTDFA(mqOracle, ceOracle, inputs);
        
        EquivalenceOracle.DFAEquivalenceOracle<Character> eqOracle =
//                new SimulatorEQOracle.DFASimulatorEQOracle(dfa);
                new EquivalenceOracle.DFAEquivalenceOracle() {
            @Override
            public DefaultQuery findCounterExample(Object a, Collection clctn) {
                 return generateCounterexample( (DFA<?,Character>)a, ceLength, r);
            }
        };
                
        Experiment.DFAExperiment<Character> experiment = new Experiment.DFAExperiment<>(ttt, eqOracle, inputs);

        // turn on time profiling
        experiment.setProfile(true);

        // enable logging of models
        experiment.setLogModels(true);

        // run experiment
        experiment.run();

        // get learned model
        DFA<?, Character> result = experiment.getFinalHypothesis();

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

    /**
     * 
     */
    private CompactDFA<Character> constructSUL() {
        // input alphabet contains characters 'a'..'b'
        Alphabet<Character> sigma = Alphabets.characters('a', 'b');

        // @formatter:off
        // create automaton
        return AutomatonBuilders.newDFA(sigma)
                .withInitial("q0")
                .from("q0")
                    .on('a').to("q1")
                    .on('b').to("q0")
                .from("q1")
                    .on('a').to("q2")
                    .on('b').to("q1")
                .from("q2")
                    .on('a').to("q3")
                    .on('b').to("q2")
                .from("q3")
                    .on('a').to("q4")
                    .on('b').to("q3")
                .from("q4")
                    .on('a').to("q5")
                    .on('b').to("q4")
                .from("q5")
                    .on('a').to("q5")
                    .on('b').to("q5")                
                .withAccepting("q0")
                .withAccepting("q5")
                .create();
        // @formatter:on
    }

    /**
     * creates example from Angluin's seminal paper.
     *
     * @return example dfa
     */
    private CompactDFA<Character> constructAngluinSUL() {
        // input alphabet contains characters 'a'..'b'
        Alphabet<Character> sigma = Alphabets.characters('a', 'b');

        // @formatter:off
        // create automaton
        return AutomatonBuilders.newDFA(sigma)
                .withInitial("q0")
                .from("q0")
                    .on('a').to("q1")
                    .on('b').to("q2")
                .from("q1")
                    .on('a').to("q0")
                    .on('b').to("q3")
                .from("q2")
                    .on('a').to("q3")
                    .on('b').to("q0")
                .from("q3")
                    .on('a').to("q2")
                    .on('b').to("q1")
                .withAccepting("q0")
                .create();
        // @formatter:on
    }
    
	private static CompactDFA<Character> createDFA(Random random, int numStates, int alphabetSize) {
		Alphabet<Character> alphabet = Alphabets.characters('a', (char) (((int) 'a') + alphabetSize - 1) );
		
		CompactDFA<Character> dfa;
		
		do {
			dfa = RandomAutomata.randomDFA(random, numStates, alphabet);
		}
		while(dfa.size() < numStates);
		
		return dfa;
	}
	
	private static <I> DefaultQuery<I,Boolean>
	generateCounterexample(Random random, CompactDFA<I> target, DFA<?,I> hypothesis, int ceLength) {
		Alphabet<I> alphabet = target.getInputAlphabet();
		
		if(Automata.findSeparatingWord(target, hypothesis, alphabet) == null) {
			return null;
		}
		
		Word<I> word;
		
		do {
			WordBuilder<I> wb = new WordBuilder<>();
			for(int i = 0; i < ceLength; i++) {
				wb.append(alphabet.getSymbol(random.nextInt(alphabet.size())));
			}
			word = wb.toWord();
		} while(target.accepts(word) == hypothesis.accepts(word));
		
		return new DefaultQuery<>(word, target.computeOutput(word));
	}
	
	private DefaultQuery<Character,Boolean>
	generateCounterexample(DFA<?,Character> hypothesis, int ceLength, Random random) {
		return generateCounterexample(random, dfa, hypothesis, ceLength);
	}    
    
}
