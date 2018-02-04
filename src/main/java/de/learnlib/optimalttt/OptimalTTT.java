/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt;

import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.MembershipOracle.DFAMembershipOracle;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class OptimalTTT implements LearningAlgorithm.DFALearner<Character> {
    
    private final DFAMembershipOracle<Character> ceqs;
    
    private final Hypothesis hypothesis;
    
    private final DecisionTree dtree;
    
    private final SuffixTrie strie;
    
    private final PrefixTree ptree;
    
    public OptimalTTT(DFAMembershipOracle<Character> mqs,
            DFAMembershipOracle<Character> ceqs,
            Alphabet<Character> sigma) {

        this.ceqs = ceqs;
        
        this.ptree = new PrefixTree();
        this.strie = new SuffixTrie();
        this.dtree = new DecisionTree(mqs, sigma, strie);
        this.hypothesis = new Hypothesis(ptree, dtree);
    }
    
    @Override
    public void startLearning() {        
        dtree.sift(ptree.root());
        makeConsistent();
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<Character, Boolean> ce) {
        boolean hypOut = hypothesis.accepts(ce.getInput());
        if (hypOut == ce.getOutput()) {
            return false;
        }
        do {
            analyzeCounterexample(ce.getInput(), ce.getOutput());
            makeConsistent();
            hypOut = hypothesis.accepts(ce.getInput());
        } while (hypOut != ce.getOutput());
        return true;
    }

    @Override
    public DFA<?, Character> getHypothesisModel() {
        return hypothesis;
    }
    
    private void makeConsistent() {
        while (dtree.makeConsistent()) {
            // do nothing ...
        };
    }
    
    private void analyzeCounterexample(Word<Character> ce, boolean refOut) {
        PrefixTree.Node ua = null;
        int upper=ce.length();
        int lower=0;
        
        while (upper - lower > 1) {
            int mid = (upper + lower) / 2;
            Word<Character> prefix = ce.prefix(mid);
            Word<Character> suffix = ce.suffix(ce.length() - mid);
            
            DecisionTree.LeafNode q = hypothesis.getState(prefix);
            assert q != null;
            
            boolean stillCe = false;
            for (PrefixTree.Node u : q.getShortPrefixes()) {
                boolean sysOut = ceqs.answerQuery(u.word(), suffix);                
                if (sysOut == refOut) {                    
                    ua = u.succ(suffix.firstSymbol());
                    lower = mid;
                    stillCe = true;
                    break;
                }
            }
            if (stillCe) {
                continue;
            }
            upper = mid;   
        } 
        
        if (ua == null) {
            assert upper == 1;
            ua = ptree.root().succ(ce.firstSymbol());
        }
        
        System.out.println("New short prefix (ce): " + ua.word());
        ua.makeShortPrefix();        
    }
}
