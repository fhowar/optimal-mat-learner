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

import java.util.LinkedHashSet;
import java.util.Set;

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

    abstract protected D hypOutput(Word<I> word, int length);

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

    protected abstract boolean canonic();

    @Override
    public boolean refineHypothesis(DefaultQuery<I, D> counterexample) {
        Set<DefaultQuery<I, D>> witnesses = new LinkedHashSet<>();
        witnesses.add(counterexample);
        boolean refined = refineWithWitness(counterexample, witnesses);
        if (!refined) {
            return false;
        }

        if (canonic()) {
            return true;
        }

        do {
            for (DefaultQuery<I, D> w : witnesses) {
                refined = refineWithWitness(w, witnesses);
                if (refined) {
                    break;
                }
            }

        } while (refined && !canonic());
        return true;
    }

    private boolean refineWithWitness(DefaultQuery<I, D> counterexample, Set<DefaultQuery<I, D>> witnesses) {
        if (counterexample.getOutput() == null) {
            counterexample.answer(ceqs.answerQuery(counterexample.getInput()));
        }
        D hypOut = hypOutput(counterexample.getInput(), counterexample.getSuffix().length());
        // System.out.println("refineWithWitness: " + counterexample.getPrefix() + " : " + counterexample.getSuffix());
        // System.out.println(counterexample.getOutput() + " <-> " + hypOut);
        if (hypOut.equals(counterexample.getOutput())) {
            return false;
        }
        do {
            analyzeCounterexample(counterexample.getInput(), counterexample.getOutput(), witnesses);
            makeConsistent();
            hypOut = hypOutput(counterexample.getInput(), counterexample.getSuffix().length());
        } while (!hypOut.equals(counterexample.getOutput()));
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

    private PTNode<I> longestPrefixOf(Word<I> ce) {
        PTNode cur = ptree.root();
        int i=0;
        do {
            cur = cur.succ(ce.getSymbol(i++));
        }
        while (cur.state().getShortPrefixes().contains(cur) && i < ce.length());
        assert !cur.state().getShortPrefixes().contains(cur);
        return cur;
    }

    private void analyzeCounterexample(Word<I> ce, D refOut, Set<DefaultQuery<I, D>> witnesses) {
        PTNode<I> lsp = longestPrefixOf(ce);
        PTNode ua = null;
        int upper = maxSearchIndex(ce.length());
        int lower = lsp.word().length() -1;
        //System.out.println("Hyp: " + hypOutput(ce));
        D hypOut = hypOutput(ce, ce.length());
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
            assert lower == lsp.word().length()-1;
            ua = lsp;
        }

        // add witnesses
        int mid = (upper + lower) / 2;
        Word<I> sprime = ce.suffix(ce.length() - (mid+1) );
        DTLeaf<I, D> qnext = getState(ua.word());
        for (PTNode<I> uprime : qnext.getShortPrefixes()) {
            witnesses.add(new DefaultQuery<>(uprime.word(), sprime ));
        }
        witnesses.add(new DefaultQuery<>(ua.word(), sprime));
        //System.out.println("New short prefix (ce): " + ua.word());
        ua.makeShortPrefix();        
    }
}
