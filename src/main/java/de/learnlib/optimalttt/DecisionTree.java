/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.optimalttt;

import de.learnlib.api.MembershipOracle.DFAMembershipOracle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.automatalib.words.Alphabet;

/**
 *
 * @author falk
 */
class DecisionTree {
        
    abstract class Node {
        final InnerNode parent;
        abstract void sift(PrefixTree.Node prefix);
        abstract void leaves(List<LeafNode> list);

        public Node(InnerNode parent) {
            this.parent = parent;
        }
        
        void path(List<Node> path) {
            path.add(0, this);
            if (this != root) {
                parent.path(path);
            }
        }
    }
    
    private class InnerNode extends Node {
        private final SuffixTrie.Node suffix;
        private Node trueChild;
        private Node falseChild;

        public InnerNode(InnerNode parent, SuffixTrie.Node suffix) {
            super(parent);
            this.suffix = suffix;
        }
                
        @Override
        void sift(PrefixTree.Node prefix) {
            boolean out = mqOracle.answerQuery(prefix.word(), suffix.word());
            Node succ = out ? trueChild : falseChild;
            if (succ != null) {
                succ.sift(prefix);
            }
            else {
                System.out.println("New short prefix (sifting): " + prefix.word());
                succ = new LeafNode(this, prefix);
                prefix.setState( (LeafNode) succ);
                if (out) {
                    trueChild = succ;
                }
                else {
                    falseChild = succ;
                }
                
                for (Character a : sigma) {
                    PrefixTree.Node ua = prefix.append(a);
                    System.out.println("Adding prefix: " + ua.word());
                    root.sift(ua);
                }
            }
        }
        
        boolean isInTrueSubTree(Node child, Node subRoot) {
            if (this == subRoot) {
                return child == trueChild;
            }
            else {
                return parent.isInTrueSubTree(this, subRoot);
            }
        }

        @Override
        void leaves(List<LeafNode> list) {
            if (trueChild != null) trueChild.leaves(list);
            if (falseChild != null) falseChild.leaves(list);
        }

        private void replace(LeafNode oldNode, InnerNode newNode) {
            if (trueChild == oldNode) {
                trueChild = newNode;
            }
            else {
                falseChild = newNode;
            }
        }

    }
    
    class LeafNode extends Node {
        
        private final List<PrefixTree.Node> shortPrefixes = new LinkedList<>();
        private final List<PrefixTree.Node> longPrefixes  = new LinkedList<>();
        
        private LeafNode(InnerNode parent, PrefixTree.Node u) {
            super(parent);
            shortPrefixes.add(u);
        }

        List<PrefixTree.Node> getShortPrefixes() {
            return shortPrefixes;
        }

        void addShortPrefix(PrefixTree.Node u) {
            shortPrefixes.add(u);
        }
        
        @Override
        void sift(PrefixTree.Node prefix) {
           prefix.setState(this);
           this.longPrefixes.add(prefix);
        }
        
        boolean isAccepting() {
            return parent.isInTrueSubTree(this, root);
        } 

        @Override
        void leaves(List<LeafNode> list) {
            list.add(this);
        }
        
        boolean refineIfPossible() {
            PrefixTree.Node ref = shortPrefixes.get(0);
            for (int i=1; i<shortPrefixes.size(); i++) {
                if (refineIfPossible(ref, shortPrefixes.get(i))) {
                    return true;
                }
            }
            return false;
        }
        
        boolean refineIfPossible(PrefixTree.Node u1, PrefixTree.Node u2) {
            for (Character a : sigma) {
                //System.out.println(u1.word() + " : " + u2.word());
                LeafNode ua1 = u1.succ(a).state();
                LeafNode ua2 = u2.succ(a).state();
                
                if (ua1 != ua2) {
                    split(u1, u2, a);
                    return true;
                }
            }
            return false;
        }
        
        void split(PrefixTree.Node u1, PrefixTree.Node u2, Character a) {
            System.out.println("Splitting " + u1.word() + " and " + u2.word());
            LeafNode ua1 = u1.succ(a).state();
            LeafNode ua2 = u2.succ(a).state();
            InnerNode n = (InnerNode) lca(ua1, ua2);
            SuffixTrie.Node av = n.suffix.prepend(a);
            boolean u1Out = ua1.parent.isInTrueSubTree(ua1, n);            
            
            InnerNode newInner = new InnerNode(parent, av);
            LeafNode trueLeaf  = new LeafNode(newInner, u1Out ? u1 : u2);
            LeafNode falseLeaf = new LeafNode(newInner, !u1Out ? u1 : u2);
            newInner.trueChild = trueLeaf;
            newInner.falseChild = falseLeaf;

            u1.setState(u1Out ? trueLeaf : falseLeaf);
            u2.setState(!u1Out ? trueLeaf : falseLeaf);
            
            shortPrefixes.remove(u1);
            shortPrefixes.remove(u2);
            
            for (PrefixTree.Node uOther : shortPrefixes) {
                if (mqOracle.answerQuery(uOther.word(), av.word())) {
                    trueLeaf.addShortPrefix(uOther);
                    uOther.setState(trueLeaf);
                }
                else {
                    falseLeaf.addShortPrefix(uOther);
                    uOther.setState(falseLeaf);
                }
            }
            
            for (PrefixTree.Node ua : longPrefixes) {
                newInner.sift(ua);
            }
            
            parent.replace(this, newInner);
        }

        void makeShortPrefix(PrefixTree.Node uNew) {
            longPrefixes.remove(uNew);
            shortPrefixes.add(uNew);
            
            for (Character a : sigma) {
                PrefixTree.Node ua = uNew.append(a);
                System.out.println("Adding prefix: " + ua.word());
                root.sift(ua);
            }
        }
    }
  
    private final DFAMembershipOracle<Character> mqOracle;

    private final Alphabet<Character> sigma;
    
    private final InnerNode root;

    DecisionTree(DFAMembershipOracle<Character> mqOracle, 
            Alphabet<Character> sigma, SuffixTrie suffixes) {
        
        this.mqOracle = mqOracle;
        this.sigma = sigma;        
        this.root = new InnerNode(null, suffixes.root());
    }
    
    void sift(PrefixTree.Node prefix) {
        root.sift(prefix);
    }
    
    List<LeafNode> leaves() {
        List<LeafNode> list = new LinkedList<>();
        root.leaves(list);
        return list;
    }

    private List<Node> path(LeafNode n) {
        List<Node> list = new LinkedList<>();
        n.path(list);
        return list;
    }
    
    private Node lca(LeafNode n1, LeafNode n2) {
        Iterator<Node> p1 = path(n1).iterator();
        Iterator<Node> p2 = path(n2).iterator();
        Node lca = null;
        while (p1.hasNext() && p2.hasNext()) {
            Node t1 = p1.next();
            Node t2 = p2.next();
            if (t1 != t2) {
                break;
            }
            lca = t1;
        }
        return lca;
    }
    
    boolean makeConsistent() {
        for (LeafNode n : leaves()) {
            if (n.refineIfPossible()) {
                return true;
            }
        }
        return false;
    }
    
}
