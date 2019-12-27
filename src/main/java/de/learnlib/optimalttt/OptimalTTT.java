/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt;

import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.MembershipOracle;
import de.learnlib.optimalttt.dt.DTLeaf;
import de.learnlib.optimalttt.dt.DecisionTree;
import de.learnlib.optimalttt.pt.PTNode;
import de.learnlib.optimalttt.pt.PrefixTree;
import de.learnlib.optimalttt.st.SuffixTrie;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public abstract class OptimalTTT<M, I, D> implements LearningAlgorithm<M, I, D> {
    
    private final MembershipOracle<I, D> ceqs;

    protected final SuffixTrie<I> strie = new SuffixTrie<>();

    protected final PrefixTree<I> ptree = new PrefixTree<>();
    
    protected OptimalTTT(MembershipOracle<I, D> ceqs) {
        this.ceqs = ceqs;
    }

    protected abstract int maxSearchIndex(int ceLength);

    abstract protected D hypOutput(Word<I> word);

    abstract protected DTLeaf<I, D> getState(Word<I> prefix);

    abstract protected M hypothesis();

    abstract protected DecisionTree<I, D> dtree();

    abstract protected D suffix(D output, int length);

    @Override
    public void startLearning() {
        assert dtree() != null && hypothesis() != null;
        dtree().sift(ptree.root());
        makeConsistent();
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<I, D> ce) {
        System.out.println("Refine with ce: " +  ce);
        D hypOut = hypOutput(ce.getInput());
        if (hypOut.equals(ce.getOutput())) {
            return false;
        }
        do {
            analyzeCounterexample(ce.getInput(), ce.getOutput());
            makeConsistent();
            hypOut = hypOutput(ce.getInput());
        } while (!hypOut.equals(ce.getOutput()));
        return true;
    }

    @Override
    public M getHypothesisModel() {
        return hypothesis();
    }


    private void makeConsistent() {
        while (dtree().makeConsistent()) {
            // do nothing ...
        };
    }
    
    private void analyzeCounterexample(Word<I> ce, D refOut) {
        PTNode ua = null;
        int upper = maxSearchIndex(ce.length());
        int lower = 0;
        
        while (upper - lower > 1) {
            int mid = (upper + lower) / 2;
            //System.out.println("Index: " + mid);
            Word<I> prefix = ce.prefix(mid);
            Word<I> suffix = ce.suffix(ce.length() - mid);
            System.out.println(prefix + " . " + suffix);

            DTLeaf<I, D> q = getState(prefix);
            assert q != null;
            int asCount = q.getShortPrefixes().size();
            //System.out.println("===================================================================== AS COUNT: " + asCount);

            boolean stillCe = false;
            for (PTNode<I> u : q.getShortPrefixes()) {
                D sysOut = suffix(ceqs.answerQuery(u.word(), suffix), suffix.size());  // Fix Suffix Length ...
                //System.out.println("  Short prefix: " + u.word() + " : " + sysOut);
                if (sysOut.equals(suffix(refOut, suffix.size()))) {
                    //System.out.println("Still counterexample - moving right");
                    ua = u.succ(suffix.firstSymbol());
                    lower = mid;
                    stillCe = true;
                    break;
                }
            }
            if (stillCe) {
                continue;
            }
            //System.out.println("No counterexample - moving left");
            upper = mid;   
        } 
        
        if (ua == null) {
            assert upper == 1;
            ua = ptree.root().succ(ce.firstSymbol());
        }
        
        //System.out.println("New short prefix (ce): " + ua.word());
        ua.makeShortPrefix();        
    }
}
