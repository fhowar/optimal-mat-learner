package de.learnlib.optimallstar;


import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.MembershipOracle;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.FastMealy;
import net.automatalib.automata.transducers.impl.FastMealyState;

import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import java.util.*;


public class OptimalLStarMealy<I, O> extends ObservationTable<MealyMachine<?, I, ?, O>, I, Word<O>>
        implements LearningAlgorithm.MealyLearner<I, O> {

    private FastMealy<I,O> hypothesis = null;

    private final Map<FastMealyState<O>, Word<O>[]> hypStateMap = new LinkedHashMap<>();

    public OptimalLStarMealy(Alphabet<I> sigma, MembershipOracle<I, Word<O>> mqs, MembershipOracle<I, Word<O>> ceqs) {
        super(sigma, mqs, ceqs);
    }

    @Override
    public int size() {
        return hypothesis.size();
    }

    @Override
    public Word<O>[] rowForState(Word<I> input) {
        return hypStateMap.get(hypothesis.getState(input));
    }

    @Override
    public Word<O> getOutput(Word<I> input, int length) {
        assert !input.isEmpty();
        return hypothesis.computeOutput(input).suffix(length);
    }

    @Override
    public MealyMachine<?, I, ?, O> getModel() {
        return hypothesis;
    }

    @Override
    Word<I>[] initSuffixes() {
        Word<I>[] suffixes = new Word[0];
        return suffixes;
    }

    @Override
    Word<O>[] newRowVector(int i) {
        return new Word[i];
    }

    @Override
    int maxSearchIndex(int ceLength) {
        return ceLength;
    }

    @Override
    boolean symbolInconsistency(Word<I> u1, Word<I> u2, I a) {
        O o1 = mqs.answerQuery(u1.append(a)).lastSymbol();
        O o2 = mqs.answerQuery(u2.append(a)).lastSymbol();
        if (!o1.equals(o2)) {
            Word<I>[] tmpSuffixes = suffixes;
            suffixes = new Word[suffixes.length+1];
            System.arraycopy(tmpSuffixes, 0, suffixes, 0, tmpSuffixes.length);
            suffixes[suffixes.length-1] = Word.fromLetter(a);
            return true;
        }
        return false;
    }

    @Override
    void automatonFromTable() {
        hypStateMap.clear();
        FastMealy<I,O> hyp = new FastMealy<>(getSigma());
        Map<List<Word<O>>, FastMealyState<O>> stateMap = new HashMap<>();
        Word<O>[] rowData = getRow( Word.<I>epsilon() );
        FastMealyState<O> q = hyp.addInitialState();
        stateMap.put(Arrays.asList(rowData), q);
        hypStateMap.put(q, rowData);

        for (Word<I> u : getShortPrefixes()) {
            rowData = getRow(u);
            if (stateMap.containsKey(Arrays.asList(rowData))) {
                continue;
            }
            q = hyp.addState();
            stateMap.put(Arrays.asList(rowData), q);
            hypStateMap.put(q, rowData);
        }

        for (Map.Entry<FastMealyState<O>, Word<O>[]> e : hypStateMap.entrySet()) {
            Word<I> u = getShortPrefixes(e.getValue()).get(0);
            Word<O>[] srcData = getRow(u);
            for (I a : getSigma()) {
                Word<O>[] destData = getRow(u.append(a));
                assert destData != null;
                FastMealyState<O> dst = stateMap.get(Arrays.asList(destData));
                //O o = srcData[getSigma().getSymbolIndex(a)].lastSymbol();
                O o = mqs.answerQuery(u.append(a)).lastSymbol();
                //System.out.println(Arrays.toString(destData) + " " + dst);
                //System.out.println("Transition: " + u + " (" +
                //        e.getKey().isAccepting() + ") -" + a + "-> " +
                //        getShortPrefixes(destData).get(0) + "(" +
                //        dst.isAccepting() + ")");
                hyp.setTransition(e.getKey(), a, dst, o);
            }
        }
        this.hypothesis = hyp;
    }

    @Override
    Word<O> suffix(Word<O> output, int length) {
        return output.suffix(length);
    }

}
