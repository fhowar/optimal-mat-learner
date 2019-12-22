package de.learnlib.optimallstar;

import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.MembershipOracle.DFAMembershipOracle;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;


/**
 *
 * @author falk
 */
public abstract class ObservationTable<M, I, D> implements LearningAlgorithm<M, I, D>, Hypothesis<M,I,D> {

    private final Alphabet<I> sigma;

    private final MembershipOracle<I,D> mqs;

    private final MembershipOracle<I, D> ceqs;

    private Word<I>[] suffixes = null;

    private final Set<Word<I>> shortPrefixes = new LinkedHashSet<>();

    private final Map<Word<I>, D[]> rows = new LinkedHashMap<>();

    private DefaultQuery<I, D> counterexample = null;

    abstract Word<I>[] initSuffixes();

    abstract D[] newRowVector(int i);

    abstract int maxSearchIndex(int ceLength);

    abstract void automatonFromTable();

    abstract D suffix(D output, int length);

    public ObservationTable(Alphabet<I> sigma, MembershipOracle<I,D> mqs, MembershipOracle<I,D> ceqs) {
        this.sigma = sigma;
        this.mqs = mqs;
        this.ceqs = ceqs;
    }
    
    @Override
    public void startLearning() {
        initTable();
        learnLoop();
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<I, D> dq) {
        System.out.println("Refine hypothesis with counterexample: " + dq);
        this.counterexample = dq;        
        while(counterExampleValid()) {
            analyzeCounterexample();
            learnLoop();
        }
        
        assert size() == shortPrefixes.size();
        return true;
    }

    @Override
    public M getHypothesisModel() {
        return getModel();
    }

    public Word<I>[] getSuffixes() {
        return suffixes;
    }

    public Alphabet<I> getSigma() {
        return sigma;
    }

    private void initTable() {
        Word<I> epsilon = Word.epsilon() ;
        suffixes = initSuffixes();
        D[] rowData = initRow(epsilon);
        rows.put(epsilon, rowData);
        addShortPrefix(epsilon);
    }

    private void analyzeCounterexample() {
        Word<I> ceInput = counterexample.getInput();
        Word<I> ua = null;
        int upper=maxSearchIndex(ceInput.length());
        int lower=0;
        
        while (upper - lower > 1) {
            int mid = (upper + lower) / 2;
            System.out.println("Index: " + mid);

            Word<I> prefix = ceInput.prefix(mid);
            Word<I> suffix = ceInput.suffix(ceInput.length() - mid);
            //System.out.println(prefix + " . " + suffix);
            //S q = hypothesis.getState(prefix);
            D[] rowData = rowForState(prefix);
            boolean stillCe = false;
            int asCount = getShortPrefixes(rowData).size();
            if (asCount > 1) {
                System.out.println("===================================================================== AS COUNT: " + asCount);
            }
            for (Word<I> u : getShortPrefixes(rowData)) {
                D sysOut = suffix(ceqs.answerQuery(u, suffix), suffix.length());
                System.out.println("  Short prefix: " + u + " : " + sysOut);
                if (sysOut.equals(suffix(counterexample.getOutput(), suffix.length()))) {
                    System.out.println("Still counterexample - moving right");
                    ua = u.append(suffix.firstSymbol());
                    lower = mid;
                    stillCe = true;
                    break;
                }
            }
            if (stillCe) {
                continue;
            }
            System.out.println("No counterexample - moving left");
            upper = mid;   
        } 
        
        if (ua == null) {
            assert upper == 1;
            ua = ceInput.prefix(1);
        }
        
        System.out.println("ua " + ua);
        addShortPrefix(ua);
    }
    
    private boolean counterExampleValid() {
        D hypOut = getOutput(counterexample.getInput());
        return !hypOut.equals(counterexample.getOutput());
    }
    
    private void learnLoop() {
        while (findInconsistency() || findUncloesedness()) {
            completeObservations();  
        }
        automatonFromTable();
    }
    
    private boolean findInconsistency() {
        System.out.println("Checking consistency");
        Word<I>[] shortAsArray = shortPrefixes.toArray(new Word[] {});
        for (int left=0; left< shortAsArray.length-1; left++) {
            for (int right=left+1; right<shortAsArray.length; right++) {
                if (findInconsistency(shortAsArray[left], shortAsArray[right])) {
                    return true;
                }
            }
        }
        System.out.println("Obs is consistent");        
        return false;
    }
    
    private boolean findInconsistency(Word<I> u1, Word<I> u2) {
        D[] rowData1 = rows.get(u1);
        D[] rowData2 = rows.get(u2);
        if (!Arrays.equals(rowData1, rowData2)) {
            return false;
        }
        for (I a : sigma) {
            rowData1 = rows.get(u1.append(a));
            rowData2 = rows.get(u2.append(a));
            if (!Arrays.equals(rowData1, rowData2)) {
                System.out.println("Obs is inconsistent");
                for (int i=0; i<rowData1.length; i++) {
                    if (!Objects.equals(rowData1[i], rowData2[i])) {
                        Word<I> newSuffx = suffixes[i].prepend(a);
                        System.out.println("New Suffix: " + newSuffx);
                         Word<I>[] tmpSuffixes = suffixes;
                        suffixes = new Word[suffixes.length+1];
                        System.arraycopy(tmpSuffixes, 0, suffixes, 0, tmpSuffixes.length);
                        suffixes[suffixes.length-1] = newSuffx;
                        return true;
                    }
                }
            }          
        }
        return false;
    }
    
    private List<Word<I>> getShortPrefixes(Word<I> prefix) {
        D[] rowData = rows.get(prefix);
        return getShortPrefixes(rowData);
    }
    
    List<Word<I>> getShortPrefixes(D[] rowData) {
        List<Word<I>> shortReps = new ArrayList<>();
        for (Entry<Word<I>, D[]> e : rows.entrySet()) {
            if (shortPrefixes.contains(e.getKey()) && 
                    Arrays.equals(rowData, e.getValue())) {
                shortReps.add(e.getKey());
            }
        }
        return shortReps;
    }

    Collection<Word<I>> getShortPrefixes() {
        return shortPrefixes;
    }

    D[] getRow(Word<I> key) {
        return rows.get(key);
    }

    private boolean findUncloesedness() {     
        System.out.println("Checking closedness");
        for (Word<I> prefix : rows.keySet()) {
            List<Word<I>> shortReps = getShortPrefixes(prefix);
            if (shortReps.isEmpty()) {
                System.out.println("Obs is unclosed");
                addShortPrefix(prefix);
                return true;
            }
        }
        System.out.println("Obs is closed");
        return false;
    }
    
    private void completeObservations() {
        System.out.println("Completing observations");
        for (Entry<Word<I>, D[]> e : rows.entrySet()) {
            D[] rowData = completeRow(e.getKey(), e.getValue());
            e.setValue(rowData);
        }
    }

    private D[] initRow(Word<I> prefix) {
        D[] rowData = newRowVector(suffixes.length);
        for (int i=0; i<suffixes.length; i++) {
            //System.out.println("MQ: " + prefix + " . " + suffixes[i]);
            rowData[i] = suffix(mqs.answerQuery(prefix, suffixes[i]), suffixes[i].size());
        }
        //System.out.println("Rowdata for " + prefix + ": " + Arrays.toString(rowData));
        return rowData;
    }

    private D[] completeRow(Word<I> prefix, D[] oldData) {
        if (suffixes.length == oldData.length) {
            return oldData;
        }
        
        D[] rowData = newRowVector(suffixes.length);
        System.arraycopy(oldData, 0, rowData, 0, oldData.length);
        for (int i=oldData.length; i<suffixes.length; i++) {
            rowData[i] = suffix(mqs.answerQuery(prefix, suffixes[i]), suffixes[i].size());
        }
        return rowData;
    }
    
    private void addShortPrefix(Word<I> shortPrefix) {
        System.out.println("Adding short prefix: " + shortPrefix);
        assert !shortPrefixes.contains(shortPrefix);
        assert rows.containsKey(shortPrefix);
        shortPrefixes.add(shortPrefix);
        for (I a : sigma) {
            Word<I> newPrefix = shortPrefix.append(a);
            System.out.println("Adding prefix: " + newPrefix);
            D[] rowData = initRow(newPrefix);
            rows.put(newPrefix, rowData);
        }
    }
}
