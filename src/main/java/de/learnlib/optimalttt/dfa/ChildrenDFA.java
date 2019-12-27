package de.learnlib.optimalttt.dfa;

import de.learnlib.optimalttt.dt.Children;
import de.learnlib.optimalttt.dt.DTInnerNode;
import de.learnlib.optimalttt.dt.DTLeaf;
import de.learnlib.optimalttt.dt.DTNode;
import java.util.Collection;
import java.util.LinkedList;


class ChildrenDFA<I> implements Children<I, Boolean> {

    private DTNode<I, Boolean> trueChild;
    private DTNode<I, Boolean> falseChild;

    @Override
    public DTNode<I, Boolean> child(Boolean out) {
        return out ? trueChild : falseChild;
    }

    @Override
    public Boolean key(DTNode<I, Boolean> child) {
        if (child == trueChild) {
            return true;
        }
        else if (child == falseChild) {
            return false;
        }
        assert false;
        throw new RuntimeException("this should not be possible");
    }

    @Override
    public void addChild(Boolean out, DTNode<I, Boolean> child) {
        assert child(out) == null;
        if (out) {
            trueChild = child;
        }
        else {
            falseChild = child;
        }
    }

    @Override
    public void replace(DTLeaf<I, Boolean> oldNode, DTInnerNode<I, Boolean> newNode) {
        if (oldNode == trueChild) {
            trueChild = newNode;
        }
        else if (oldNode == falseChild) {
            falseChild = newNode;
        }
        else {
            assert false;
        }
    }

    @Override
    public Collection<DTNode<I, Boolean>> all() {
        LinkedList<DTNode<I, Boolean>> ret = new LinkedList<>();
        ret.add(trueChild);
        ret.add(falseChild);
        return ret;
    }

}
