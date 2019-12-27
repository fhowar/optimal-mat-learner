package de.learnlib.optimalttt.mealy;

import de.learnlib.optimalttt.dt.DTLeaf;
import net.automatalib.words.Word;

public class MealyTransition<I, O> {

    final DTLeaf<I, Word<O>> source;

    final I input;

    MealyTransition(DTLeaf<I, Word<O>> source, I input) {
        this.source = source;
        this.input = input;
    }
}
