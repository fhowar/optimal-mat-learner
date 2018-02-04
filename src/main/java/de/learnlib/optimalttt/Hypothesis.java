/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt;

import java.util.Collection;
import net.automatalib.automata.fsa.DFA;

/**
 *
 * @author falk
 */
final class Hypothesis implements DFA<DecisionTree.LeafNode, Character> {
    
    private final PrefixTree ptree;
    
    private final DecisionTree dtree;

    public Hypothesis(PrefixTree ptree, DecisionTree dtree) {
        this.ptree = ptree;
        this.dtree = dtree;
    }
    
    @Override
    public DecisionTree.LeafNode getTransition(DecisionTree.LeafNode s, Character a) {
        PrefixTree.Node u = s.getShortPrefixes().get(0);
        assert u != null;
        PrefixTree.Node ua = u.succ(a);
        assert ua != null;
        DecisionTree.LeafNode dst = ua.state();
        assert dst != null;
        return dst;
    }

    @Override
    public boolean isAccepting(DecisionTree.LeafNode s) {
        assert s != null;
        return s.isAccepting();
    }

    @Override
    public DecisionTree.LeafNode getInitialState() {
        return ptree.root().state();
    }

    @Override
    public Collection<DecisionTree.LeafNode> getStates() {
        return dtree.leaves();
    }

    
}
