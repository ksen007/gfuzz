package generator;

import ec.util.MersenneTwisterFast;
import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 5/30/16
 * Time: 12:32 AM
 */


class Edge {
    int edge;
    Node end;

    public Edge(int edge, Node end) {
        this.edge = edge;
        this.end = end;
    }
}

class Node {
    public static MersenneTwisterFast rand = new MersenneTwisterFast((new Date()).getTime());
    Node parent;
    Int2ObjectRBTreeMap<Node> children;
    Int2ObjectRBTreeMap<Node> next;
    Int2DoubleRBTreeMap probabilities;
    int count;
    int id;

    public Node(Model m) {
        count = 0;
        id = ++m.nNodes;
        m.allNodes.addLast(this);
    }

    public Int2DoubleRBTreeMap getProbabilities() {
        return probabilities;
    }

    public boolean hasNext() {
        return next == null;
    }

    public boolean isLeaf() {
        return children == null;
    }

    public boolean isTerminal() {
        return probabilities == null;
    }

    public Node transition(int rete) {
        Node tmp = this;
        while (!tmp.next.containsKey(rete)) {
            tmp = tmp.parent;
        }
        tmp = tmp.next.get(rete);
        if (tmp.isLeaf()) {
            tmp = tmp.parent;
        }
        return tmp;
    }

    public int sampleNextEdge() {
        double rv = rand.nextDouble();
        double sum = 0.0;
        int rete = 0;
        for (int e : probabilities.keySet()) {
            sum += probabilities.get(e);
            if (sum >= rv) {
                rete = e;
                break;
            }
        }
        return rete;
    }
}

public class Model {
    Node root;
    //ArrayList<Node> prevNodeList;
    //ArrayList<Node> currNodeList;
    public int ngram;
    int nNodes;
    LinkedList<Node> allNodes;

    private void init(int n) {
        nNodes = 0;
        allNodes = new LinkedList<Node>();
        root = new Node(this);
        ngram = n + 1;
        //prevNodeList = new ArrayList<Node>(ngram + 1);
        //currNodeList = new ArrayList<Node>(ngram + 1);
    }

    public Model(int n) {
        init(n);
    }

    private void addNext(Node node, int e) {
        Node next;
        if (node.next == null || !node.next.containsKey(e)) {
            if (node.next == null) {
                node.next = new Int2ObjectRBTreeMap<Node>();
            }
            next = new Node(this);
            next.parent = node;
            node.next.put(e, next);
        } else {
            next = node.next.get(e);
        }
        next.count++;
    }

    private Node add(Node node, int e) {
        Node child;
        if (node.children == null || !node.children.containsKey(e)) {
            if (node.children == null) {
                node.children = new Int2ObjectRBTreeMap<Node>();
            }
            child = new Node(this);
            child.parent = node;
            node.children.put(e, child);
        } else {
            child = node.children.get(e);
        }
        child.count++;
        return child;
    }

    private void addAll(int[] a, int idx) {
        Node curr = root;
        curr.count++;
        addNext(curr, a[idx]);
        for (int i = idx-1, k = 0; i >= 0 && k < ngram - 1; i--, k++) {
            curr = add(curr, a[i]);
            addNext(curr, a[idx]);
        }
    }

    public void addLast(int[] a) {
        addAll(a, a.length-1);
    }

    public void addPrefixes(int[] a) {
        for (int i = 0; i < a.length; i++) {
            addAll(a, i);
        }
    }

    public String toDotString() {
        StringBuilder ret = new StringBuilder();
        ret.append("digraph model {\n" +
                " node [shape=circle]");
        for (Node node : allNodes) {
            ret.append(node.id).append(" [label=\"").append(node.count).append("\"];\n");
        }
        for (Node node : allNodes) {
            if (node.children != null) {
                for (int e : node.children.keySet()) {
                    Node next = node.children.get(e);
                    ret.append(node.id).append(" -> ").append(next.id).append(" [label=\"").append(e).append("\"];\n");
                }
            }
        }
        ret.append("edge [color=red]\n");
        for (Node node : allNodes) {
            if (node.next != null) {
                for (int e : node.next.keySet()) {
                    Node next = node.next.get(e);
                    ret.append(node.id).append(" -> ").append(next.id).append(" [label=\"").append(e).append("\" fontcolor=Red];\n");
                }
            }
        }
        ret.append("edge [color=blue]\n");
        for (Node node : allNodes) {
            if (node.probabilities != null) {
                for (int e : node.probabilities.keySet()) {
                    double next = node.probabilities.get(e);
                    ret.append(node.id).append(" -> ").append(0).append(" [label=\"").append(e).append(":").append(next).append("\" fontcolor=Blue];\n");
                }
            }
        }
        ret.append("}\n");
        return ret.toString();
    }

    public void checkConsistency() {
        int sum = 0;
        for (Node node : allNodes) {
            if (node.next != null) {
                sum = 0;
                for (int e : node.next.keySet()) {
                    Node next = node.next.get(e);
                    sum += next.count;
                }
                if (sum != node.count) {
                    throw new RuntimeException("Count mismatch " + node.id + " " + node.count + " " + sum);
                }
            }
        }
    }

    private void calculateProbabilities(Node node) {
        if (node.hasNext()) return;
        node.probabilities = new Int2DoubleRBTreeMap();
        for (int e : node.next.keySet()) {
            Node next = node.next.get(e);
            node.probabilities.put(e, 1.0d * next.count / node.count);
        }
        if (node.parent != null) {
            Node parent = node.parent;
            double lambda = (1.0d * node.count) / (node.count + node.next.size());
            for (int e : parent.probabilities.keySet()) {
                double tmp;
                if (node.probabilities.containsKey(e)) {
                    tmp = node.probabilities.get(e);
                } else {
                    tmp = 0.0;
                }
                node.probabilities.put(e, lambda * tmp + (1 - lambda) * parent.probabilities.get(e));
            }
        }
        if (node.children != null) {
            for (int e : node.children.keySet()) {
                calculateProbabilities((node.children.get(e)));
            }
        }
    }

    public Node transition(IntArrayList prefix) {
        Node node = root;
        int i = prefix.size()-1;
        while(i >= 0) {
            int e = prefix.getInt(i);
            if (node.children == null || !node.children.containsKey(e)) {
                return node;
            } else {
                node = node.children.get(e);
            }
            i--;
        }
        return node;
    }

    public void calculateProbabilities() {
        calculateProbabilities(root);
    }


    public int START = -1;
    public int END = -2;

    public int[] sampleSentence(int max, boolean isPadding) {
        int edge;
        int count = 0;
        IntArrayList ret = new IntArrayList();
        Node curr = root;
        if (isPadding) {
            for (int i = 0; i < ngram - 1; i++) {
                ret.add(START);
            }
        }
        curr = transition(ret);
        while (true) {
            count++;
            if (count > max || curr.isTerminal()) {
                if (isPadding) {
                    ret.removeElements(0, ngram-1);
                }
                return ret.toIntArray();
            }
            edge = curr.sampleNextEdge();
            if (edge == END) {
                if (isPadding) {
                    ret.removeElements(0, ngram-1);
                }
                return ret.toIntArray();
            } else {
                if (edge != START)
                    ret.add(edge);
                curr = transition(ret);
            }
        }
    }

    public void train(String file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                char[] chars = line.toCharArray();
                int pass[] = new int[chars.length+ngram];
                int i;
                for (i = 0; i < ngram-1; i++) {
                    pass[i] = START;
                }
                for (i = 0; i < chars.length; i++) {
                    pass[i+ngram-1] = chars[i];
                }
                pass[chars.length+ngram-1] = END;
                addPrefixes(pass);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
