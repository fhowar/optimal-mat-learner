package de.learnlib.optimallstar;

import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.MembershipOracle.DFAMembershipOracle;
import de.learnlib.oracles.DefaultQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;


/**
 *
 * @author falk
 */
public class OptimalLStar implements LearningAlgorithm.DFALearner<Character> {

    private final DFAMembershipOracle<Character> mqs;
    
    private final DFAMembershipOracle<Character> ceqs;
    
    private final Alphabet<Character> sigma;
    
    private Word<Character>[] suffixes = null; 
    
    private final Set<Word<Character>> shortPrefixes;
    
    private final Map<Word<Character>, Boolean[]> rows;

    private final Map<FastDFAState, Boolean[]> hypStateMap;
    
    private FastDFA<Character> hypothesis = null;
    
    private DefaultQuery<Character, Boolean> counterexample = null;
    
    public OptimalLStar(
            DFAMembershipOracle<Character> mqs, 
            DFAMembershipOracle<Character> ceqs, 
            Alphabet<Character> sigma) {
        
        this.mqs = mqs;
        this.ceqs = ceqs;
        this.sigma = sigma;
        this.shortPrefixes = new HashSet<>();
        this.rows = new HashMap<>();
        this.hypStateMap = new HashMap<>();
    }
    
    @Override
    public void startLearning() {
        initTable();
        learnLoop();
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<Character, Boolean> dq) {
        System.out.println("Refine hypothesis with counterexample: " + dq);
        this.counterexample = dq;        
        while(counterExampleValid()) {
            analyzeCounterexample();
            learnLoop();
            //throw new IllegalStateException("Not implemented yet.");
        }
        
        assert hypothesis.getStates().size() == shortPrefixes.size();
        return true;
    }

    public Word<Character>[] getSuffixes() {
        return suffixes;
    }

    private void analyzeCounterexample() {
        Word<Character> ceInput = counterexample.getInput();
        Word<Character> ua = null;
        int upper=ceInput.length();
        int lower=0;
        
        while (upper - lower > 1) {
            int mid = (upper + lower) / 2;
            System.out.println("Index: " + mid);

            Word<Character> prefix = ceInput.prefix(mid);
            Word<Character> suffix = ceInput.suffix(ceInput.length() - mid);
            System.out.println(prefix + " . " + suffix);
            FastDFAState q = hypothesis.getState(prefix);
            Boolean[] rowData = hypStateMap.get(q);
            boolean stillCe = false;
            int ascount = getShortPrefixes(rowData).size();
            if (ascount > 1) {
                System.out.println("===================================================================== AS COUNT: " + ascount);
            }
            for (Word<Character> u : getShortPrefixes(rowData)) {
                boolean sysOut = ceqs.answerQuery(u, suffix);
                System.out.println("  Short prefix: " + u + " : " + sysOut);
                if (sysOut == counterexample.getOutput() ) {
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
        boolean hypOut = hypothesis.accepts(counterexample.getInput());
        return hypOut != counterexample.getOutput();
    }
    
    @Override
    public DFA<?, Character> getHypothesisModel() {
        return this.hypothesis;
    }
     
    private void automatonFromTable() {
        hypStateMap.clear();
        FastDFA<Character> hyp = new FastDFA<>(sigma);                
        Map<List<Boolean>, FastDFAState> stateMap = new HashMap<>();
        Boolean[] rowData = rows.get( Word.<Character>epsilon() );
        FastDFAState q = hyp.addInitialState( rowData[0] );
        stateMap.put(Arrays.asList(rowData), q);
        hypStateMap.put(q, rowData);
        
        for (Word<Character> u : shortPrefixes) {
            rowData = rows.get(u);
            if (stateMap.containsKey(Arrays.asList(rowData))) {
                continue;
            }
            q = hyp.addState( rowData[0] );
            stateMap.put(Arrays.asList(rowData), q);
            hypStateMap.put(q, rowData);
        }
        
        for (Entry<FastDFAState, Boolean[]> e : hypStateMap.entrySet()) {
            Word<Character> u = getShortPrefixes(e.getValue()).get(0);
            for (Character a : sigma) {
                Boolean[] destData = rows.get(u.append(a));
                assert destData != null;
                FastDFAState dst = stateMap.get(Arrays.asList(destData));
                //System.out.println(Arrays.toString(destData) + " " + dst);
                //System.out.println("Transition: " + u + " (" + 
                //        e.getKey().isAccepting() + ") -" + a + "-> " + 
                //        getShortPrefixes(destData).get(0) + "(" + 
                //        dst.isAccepting() + ")");                
                hyp.setTransition(e.getKey(), a, dst);
            }            
        }
        this.hypothesis = hyp;     
    }
    
    private void learnLoop() {
        while (findInconsistency() || findUncloesedness()) {
            completeObservations();  
        }
        automatonFromTable();
    }
    
    private boolean findInconsistency() {
        System.out.println("Checking consistency");
        Word<Character>[] shortAsArray = shortPrefixes.toArray(new Word[] {});    
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
    
    private boolean findInconsistency(Word<Character> u1, Word<Character> u2) {
        Boolean[] rowData1 = rows.get(u1);
        Boolean[] rowData2 = rows.get(u2);
        if (!Arrays.equals(rowData1, rowData2)) {
            return false;
        }
        for (Character a : sigma) {
            rowData1 = rows.get(u1.append(a));
            rowData2 = rows.get(u2.append(a));
            if (!Arrays.equals(rowData1, rowData2)) {
                System.out.println("Obs is inconsistent");
                for (int i=0; i<rowData1.length; i++) {
                    if (!Objects.equals(rowData1[i], rowData2[i])) {
                        Word<Character> newSuffx = suffixes[i].prepend(a);
                        System.out.println("New Suffix: " + newSuffx);
                         Word<Character>[] tmpSuffixes = suffixes;
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
    
    private List<Word<Character>> getShortPrefixes(Word<Character> prefix) {
        Boolean[] rowData = rows.get(prefix);
        return getShortPrefixes(rowData);
    }
    
    private List<Word<Character>> getShortPrefixes(Boolean[] rowData) {
        List<Word<Character>> shortReps = new ArrayList<>();
        for (Entry<Word<Character>, Boolean[]> e : rows.entrySet()) {
            if (shortPrefixes.contains(e.getKey()) && 
                    Arrays.equals(rowData, e.getValue())) {
                shortReps.add(e.getKey());
            }
        }
        return shortReps;
    }
    
    private boolean findUncloesedness() {     
        System.out.println("Checking closedness");
        for (Word<Character> prefix : rows.keySet()) {
            List<Word<Character>> shortReps = getShortPrefixes(prefix);
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
        for (Entry<Word<Character>, Boolean[]> e : rows.entrySet()) {
            Boolean[] rowData = completeRow(e.getKey(), e.getValue());
            e.setValue(rowData);
        }
    }
    
    private void initTable() {
        Word<Character> epsilon = Word.epsilon() ;
        suffixes = new Word[] { epsilon };
        Boolean[] rowData = initRow(epsilon);
        rows.put(epsilon, rowData);
        addShortPrefix(epsilon);
    }
    
    private Boolean[] initRow(Word<Character> prefix) {
        Boolean[] rowData = new Boolean[suffixes.length];
        for (int i=0; i<suffixes.length; i++) {
            rowData[i] = mqs.answerQuery(prefix, suffixes[i]);
        }
        System.out.println("Rowdata for " + prefix + ": " + Arrays.toString(rowData));
        return rowData;
    }

    private Boolean[] completeRow(Word<Character> prefix, Boolean[] oldData) {
        if (suffixes.length == oldData.length) {
            return oldData;
        }
        
        Boolean[] rowData = new Boolean[suffixes.length];
        System.arraycopy(oldData, 0, rowData, 0, oldData.length);
        for (int i=oldData.length; i<suffixes.length; i++) {
            rowData[i] = mqs.answerQuery(prefix, suffixes[i]);
        }
        return rowData;        
    }
    
    private void addShortPrefix(Word<Character> shortPrefix) {
        System.out.println("Adding short prefix: " + shortPrefix);
        assert !shortPrefixes.contains(shortPrefix);
        assert rows.containsKey(shortPrefix);
        shortPrefixes.add(shortPrefix);
        for (Character a : sigma) {
            Word<Character> newPrefix = shortPrefix.append(a);
            System.out.println("Adding prefix: " + newPrefix);
            Boolean[] rowData = initRow(newPrefix);
            rows.put(newPrefix, rowData);
        }
    }
}
