package de.learnlib.optimalttt.pt;

import de.learnlib.optimalttt.dt.DTLeaf;
import net.automatalib.words.Word;

public interface PTNode<I> {

    Word<I> word();

    PTNode<I> append(I a);

    void setState(DTLeaf node);

    DTLeaf state();

    PTNode<I> succ(I a);

    void makeShortPrefix();
}
