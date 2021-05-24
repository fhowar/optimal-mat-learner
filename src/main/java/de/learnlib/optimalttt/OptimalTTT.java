/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt;


import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.optimalttt.dt.DTLeaf;
import de.learnlib.optimalttt.dt.DecisionTree;
import de.learnlib.optimalttt.pt.PTNode;
import de.learnlib.optimalttt.pt.PrefixTree;
import de.learnlib.optimalttt.st.SuffixTrie;
import net.automatalib.words.Word;

import java.util.List;
import java.util.ArrayList;

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
    public boolean refineHypothesis(DefaultQuery<I, D> counterexample) {
        List<DefaultQuery<I, D>> ces = new ArrayList<>();
        ces.add(counterexample);
        boolean wasCe = false;
        for (DefaultQuery<I, D> ce : new ArrayList<>(ces)) {
            //System.out.println("Refine with ce: " +  ce);
            D hypOut = hypOutput(ce.getInput());
            if (hypOut.equals(ce.getOutput())) {
                continue;
                //return false;
            }
            do {
                wasCe = true;
                analyzeCounterexample(ce.getInput(), ce.getOutput(), ces);
                makeConsistent();
                hypOut = hypOutput(ce.getInput());
            } while (!hypOut.equals(ce.getOutput()));
        }
        return wasCe;
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
    
    private void analyzeCounterexample(Word<I> ce, D refOut, List<DefaultQuery<I, D>> ces) {
        PTNode ua = null;
        int upper = maxSearchIndex(ce.length());
        int lower = 0;
        //System.out.println("Hyp: " + hypOutput(ce));
        D hypOut = hypOutput(ce);
        while (upper - lower > 1) {
            int mid = (upper + lower) / 2;
            //System.out.println("Index: " + mid);
            Word<I> prefix = ce.prefix(mid);
            Word<I> suffix = ce.suffix(ce.length() - mid);
            //System.out.println(prefix + " . " + suffix);


            DTLeaf<I, D> q = getState(prefix);
            assert q != null;
            int asCount = q.getShortPrefixes().size();
            //System.out.println("===================================================================== AS COUNT: " + asCount);

            boolean stillCe = false;
            for (PTNode<I> u : q.getShortPrefixes()) {
                D sysOut = suffix(ceqs.answerQuery(u.word(), suffix), suffix.size());  // Fix Suffix Length ...
                //System.out.println("  Short prefix: " + u.word() + " : " + sysOut + " : [ref] " + suffix(refOut, suffix.size()) +
                //        " : [hyp] " + suffix(hypOutput(prefix.concat(suffix)), suffix.size()));
                if (!sysOut.equals(suffix(hypOut, suffix.size()))) {
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
        
        // add witnesses
        int mid = (upper + lower) / 2;
        Word<I> sprime = ce.suffix(ce.length() - (mid +1));
        DTLeaf<I, D> qnext = getState(ua.word());
        for (PTNode<I> uprime : qnext.getShortPrefixes()) {
            ces.add(new DefaultQuery<>(uprime.word().concat(sprime), ceqs.answerQuery(uprime.word(), sprime) ));
        }
        ces.add(new DefaultQuery<>(ua.word().concat(sprime), ceqs.answerQuery(ua.word(), sprime) ));

        //System.out.println("New short prefix (ce): " + ua.word());
        ua.makeShortPrefix();        
    }
}
