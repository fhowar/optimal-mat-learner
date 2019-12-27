package de.learnlib.optimalttt.mealy;

import de.learnlib.optimalttt.dt.Children;
import de.learnlib.optimalttt.dt.DTInnerNode;
import de.learnlib.optimalttt.dt.DTLeaf;
import de.learnlib.optimalttt.dt.DTNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

class ChildrenMealy<I, D> implements Children<I, D> {

    Map<D, DTNode<I, D>> children = new LinkedHashMap<>();

    @Override
    public DTNode<I, D> child(D out) {
        return children.get(out);
    }

    @Override
    public D key(DTNode<I, D> child) {
        for (Map.Entry<D, DTNode<I, D>> e : children.entrySet()) {
            if (e.getValue() == child) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public void addChild(D out, DTNode<I, D> child) {
        children.put(out, child);
    }

    @Override
    public void replace(DTLeaf<I, D> oldNode, DTInnerNode<I, D> newNode) {
        children.put(key(oldNode), newNode);
    }

    @Override
    public Collection<DTNode<I, D>> all() {
        return children.values();
    }
}
