package de.learnlib.optimalttt.dt;

import de.learnlib.api.MembershipOracle;
import de.learnlib.optimalttt.pt.PTNode;
import de.learnlib.optimalttt.st.STNode;
import net.automatalib.words.Alphabet;
import java.util.LinkedList;
import java.util.List;

public abstract class DecisionTree<I, D> {

    protected DTNode<I, D> root;

    protected final Alphabet<I> sigma;

    private final STNode<I> stRoot;

    protected final MembershipOracle<I, D> mqOracle;

    protected DecisionTree(MembershipOracle<I, D> mqOracle, Alphabet<I> sigma, STNode<I> stRoot) {

        this.mqOracle = mqOracle;
        this.sigma = sigma;
        this.stRoot = stRoot;
    }

    abstract protected Children<I, D> newChildren();

    abstract protected D query(PTNode<I> prefix, STNode<I> suffix);

    public void sift(PTNode<I> prefix) {
        root.sift(prefix);
    }

    public void setRoot(DTNode<I, D> newRoot) {
        this.root = newRoot;
    }

    public List<DTLeaf<I, D>> leaves() {
        List<DTLeaf<I, D>> list = new LinkedList<>();
        root.leaves(list);
        return list;
    }

    public boolean makeConsistent() {
        LinkedList<DTLeaf<I, D>> leaves = new LinkedList<>();
        root.leaves(leaves);
        for (DTLeaf<I, D> n : leaves) {
            if (n.refineIfPossible()) {
                return true;
            }
        }
        return false;
    }

    DTNode<I, D> root() {
        return root;
    }

    Alphabet<I> sigma() {
        return sigma;
    }

    STNode<I> newSuffix(I a) {
        return stRoot.prepend(a);
    }
}
