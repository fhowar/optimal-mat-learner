package de.learnlib.optimalttt.dfa;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;


import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.util.automata.fsa.MutableDFAs;
import net.automatalib.util.automata.random.RandomAutomata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.abstractimpl.AbstractAlphabet;

public class DFAGenerator {

    private static final int NUM_CALLS = 5;
    private static final int NUM_INTERNAL = 10;
    private static final int SIZE = 100;
    private static final int SEED = 25;

    public static CompactDFA<Integer> generateDefault() {
        return createPartialSystem(NUM_INTERNAL, NUM_CALLS, SIZE, new Random(SEED));
    }

    public static CompactDFA<Integer> createCompleteSystem(int numInternals,
                                                            int numCalls,
                                                            int procedureSize,
                                                            Random random) {

        final IntervalAlphabet alphabet = new IntervalAlphabet(0, numInternals + numCalls + 1);
        final IntervalAlphabet proceduralAlphabet = new IntervalAlphabet(0, numInternals + numCalls);
        final int returnSymbol = numInternals + numCalls;

        final CompactDFA<Integer> result = new CompactDFA<>(alphabet);
        RandomAutomata.randomDeterministic(random,
                procedureSize - 2,
                proceduralAlphabet,
                Collections.singletonList(Boolean.TRUE),
                DFA.TRANSITION_PROPERTIES,
                result,
                false);

        final Collection<Integer> oldStates = result.getStates();
        final int successSink = result.addState(true);

        for (int s : oldStates) {
            if (random.nextBoolean()) {
                result.setTransition(s, returnSymbol, successSink, null);
            }
        }

        assert DFAs.isPrefixClosed(result, alphabet);
        MutableDFAs.complete(result, alphabet, true);
        return result;
    }

    private static CompactDFA<Integer> createPartialSystem(int numInternals,
                                                           int numCalls,
                                                           int procedureSize,
                                                           Random random) {

        final IntervalAlphabet alphabet = new IntervalAlphabet(0, numInternals + numCalls + 1);
        final IntervalAlphabet proceduralAlphabet = new IntervalAlphabet(0, numInternals + numCalls);
        final int returnSymbol = numInternals + numCalls;

        final int numWithoutSinks = procedureSize - 2;
        final CompactDFA<Integer> result = new CompactDFA<>(alphabet);
        result.addInitialState(true);

        for (int i = 1; i < numWithoutSinks; i++) {
            result.addState(true);
        }

        for (int i = 0; i < numWithoutSinks; i++) {
            for (Integer sym : proceduralAlphabet) {
                if (random.nextBoolean()) {
                    final int state = random.nextInt(result.size());
                    result.setTransition(i, sym, state, null);
                }
            }
        }

        int success = result.addState(true);
        for (int i = 0; i < numWithoutSinks; i++) {
            if (random.nextBoolean()) {
                result.setTransition(i, returnSymbol, success, null);
            }
        }

        assert DFAs.isPrefixClosed(result, alphabet);
        MutableDFAs.complete(result, alphabet, true);
        return result;
    }

    private static final class IntervalAlphabet extends AbstractAlphabet<Integer> implements Alphabet<Integer> {

        private final int min;
        private final int max;

        public IntervalAlphabet(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public int size() {
            return max - min + 1;
        }

        @Override
        public Integer getSymbol(int index) {
            return index + min;
        }

        @Override
        public int getSymbolIndex(Integer symbol) {
            return symbol - min;
        }

        @Override
        public boolean containsSymbol(Integer symbol) {
            return symbol <= max && symbol >= min;
        }
    }
}
