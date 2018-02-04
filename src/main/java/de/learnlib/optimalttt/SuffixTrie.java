/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt;

import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
class SuffixTrie {
    
    static class Node {
        private final Node parent;
        private final Character symbol;
        private final Map<Character, Node> children = new HashMap<>();

        private Node(Node parent, Character symbol) {
            this.parent = parent;
            this.symbol = symbol;
        }        

        Word<Character> word() {
            return toWord( Word.epsilon() );
        }

        private Word<Character> toWord(Word<Character> prefix) {
            if (symbol == null) {
                return prefix;
            }
            return parent.toWord(prefix.append(symbol));
        }

        Node prepend(Character a) {
            Node n = children.get(a);
            if (n == null) {
                n = new Node(this, a);
                children.put(a, n);
            }
            return n;
        }
    }
        
    private final Node epsilon = new Node(null, null);
    
    Node root() {
        return epsilon;
    }
}
