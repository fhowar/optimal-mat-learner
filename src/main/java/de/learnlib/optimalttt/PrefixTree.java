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
class PrefixTree {
    
    static class Node {
        private final Node parent;
        private final Character symbol;
        private DecisionTree.LeafNode state;
        private final Map<Character, Node> children = new HashMap<>();

        public Node(Node parent, Character symbol) {
            this.parent = parent;
            this.symbol = symbol;
        }
                     
        Word<Character> word() {
            return toWord( Word.epsilon() );
        }

        Node append(Character a) {
            assert !children.containsKey(a);
            Node n = new Node(this, a);
            children.put(a, n);
            return n;
        }
        
        void setState(DecisionTree.LeafNode node) {
            this.state = node;
        }
        
        DecisionTree.LeafNode state() {
            return state;
        }
        
        boolean inL() {
            return state.isAccepting();
        } 
        
        private Word<Character> toWord(Word<Character> suffix) {
            if (symbol == null) {
                return suffix;
            }
            return parent.toWord(suffix.prepend(symbol));
        }
        
        Node succ(Character a) {
            return children.get(a);
        }

        void makeShortPrefix() {
            this.state.makeShortPrefix(this);
        }
    }  
    
    private final Node epsilon = new Node(null, null);
    
    Node root() {
        return epsilon;
    }
    
}
