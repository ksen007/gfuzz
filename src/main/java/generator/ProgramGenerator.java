package generator;

import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 6/19/16
 * Time: 12:18 AM
 */
public class ProgramGenerator {
    Model seq, tree;
    Object2IntRBTreeMap<String> wordToNum;
    ObjectArrayList<String> numToWord;
    String LB = "(%";
    String RB = "%)";
    String AT = "@";
    int at;
    int lb;
    int rb;

    ObjectSet<String> inputs;

    public ProgramGenerator(int m, int n) {
        seq = new Model(m);
        tree = new Model(n);
        wordToNum = new Object2IntRBTreeMap<>();
        numToWord = new ObjectArrayList<>();
        at = getNum(AT);
        lb = getNum(LB);
        rb = getNum(RB);
        inputs = new ObjectRBTreeSet<>();
    }

    private int getNum(String word) {
        if (wordToNum.containsKey(word)) {
            return wordToNum.get(word);
        }
        wordToNum.put(word, numToWord.size());
        numToWord.add(word);
        return numToWord.size() - 1;
    }

    private void collapseSubTree(IntArrayList tree) {
        if (tree.peekInt(0) == rb) {
            int i = tree.lastIndexOf(lb);
            tree.removeElements(i, tree.size());
            tree.push(at);
        }
    }

    private Int2DoubleRBTreeMap mergeProbabilitiesMean(Int2DoubleRBTreeMap p1, Int2DoubleRBTreeMap p2) {
        Int2DoubleRBTreeMap ret = new Int2DoubleRBTreeMap();
        for (int e : p1.keySet()) {
            ret.put(e, p1.get(e) / 2.0);
        }
        for (int e : p2.keySet()) {
            if (ret.containsKey(e)) {
                ret.put(e, ret.get(e) + p2.get(e) / 2.0);
            } else {
                ret.put(e, p2.get(e) / 2.0);
            }
        }
        return ret;
    }

    private Int2DoubleRBTreeMap mergeProbabilitiesMult(Int2DoubleRBTreeMap p1, Int2DoubleRBTreeMap p2) {
        Int2DoubleRBTreeMap ret = new Int2DoubleRBTreeMap();
        double sum = 0.0, tmp;
        for (int e : p1.keySet()) {
            if (p2.containsKey(e)) {
                ret.put(e, tmp = (p1.get(e) * p2.get(e)));
                sum += tmp;
            }
        }
        for (int e : ret.keySet()) {
            ret.put(e, ret.get(e) / sum);
        }
        return ret;
    }

    private int sampleEdge(Int2DoubleRBTreeMap probabilities) {
        double rv = Node.rand.nextDouble();
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

    public String sample(int max) {
        StringBuilder sb = new StringBuilder();
        IntArrayList seq = new IntArrayList();
        IntArrayList tree = new IntArrayList();
        for (int i = 0; i < this.seq.ngram - 1; i++) {
            seq.push(this.seq.START);
        }
        for (int i = 0; i < this.tree.ngram - 1; i++) {
            tree.push(this.tree.START);
        }
        int count = 0;
        while (count < max) {
            Node seqNode;
            Node treeNode = this.tree.transition(tree);
            int w = sampleEdge(treeNode.getProbabilities());
            if (w == lb || w == rb) {
                tree.push(w);
                collapseSubTree(tree);
            } else {
                seqNode = this.seq.transition(seq);
                Int2DoubleRBTreeMap seqProb = seqNode.getProbabilities();
                Int2DoubleRBTreeMap treeProb = treeNode.getProbabilities();
                Int2DoubleRBTreeMap mergedProb = mergeProbabilitiesMult(seqProb, treeProb);
                w = sampleEdge(mergedProb);
                if (w == lb || w == rb) {
                    tree.push(w);
                    collapseSubTree(tree);
                } else {
                    if (w == this.seq.END) {
                        return sb.toString();
                    } else {
                        if (w != this.seq.START) {
                            seq.push(w);
                            tree.push(w);
                            sb.append(numToWord.get(w));
                            count++;
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public void train(String file) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String word;
            IntArrayList seq = new IntArrayList();
            IntArrayList tree = new IntArrayList();
            boolean first = true;
            while ((word = br.readLine()) != null) {
                int w = getNum(word);
                if (word.startsWith("-")) {
                    if (first) {
                        first = false;
                    } else {
                        seq.push(this.seq.END);
                        this.seq.addLast(seq.toIntArray()); //@todo not efficient
                        tree.push(this.tree.END);
                        this.tree.addLast(tree.toIntArray()); //@todo not efficient
                        inputs.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    seq.clear();
                    tree.clear();
                    for (int i = 0; i < this.seq.ngram - 1; i++) {
                        seq.push(this.seq.START);
                        this.seq.addLast(seq.toIntArray()); //@todo not efficient
                    }
                    for (int i = 0; i < this.tree.ngram - 1; i++) {
                        tree.push(this.tree.START);
                        this.tree.addLast(tree.toIntArray()); //@todo not efficient
                    }
                } else if (w == lb || w == rb) {
                    tree.push(w);
                    this.tree.addLast(tree.toIntArray()); //@todo not efficient
                    collapseSubTree(tree);
                } else {
                    tree.push(w);
                    this.tree.addLast(tree.toIntArray()); //@todo not efficient
                    seq.push(w);
                    this.seq.addLast(seq.toIntArray()); //@todo not efficient
                    sb.append(word);
                }
            }
            seq.push(this.seq.END);
            this.seq.addLast(seq.toIntArray()); //@todo not efficient
            tree.push(this.tree.END);
            this.tree.addLast(tree.toIntArray()); //@todo not efficient
            inputs.add(sb.toString());
            this.seq.calculateProbabilities();
            this.tree.calculateProbabilities();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ProgramGenerator pg = new ProgramGenerator(10, 10);
        pg.train(args[0]);

        int count = 0;
        for(int i = 0; i<100; i++) {
            String input;
            System.out.println("Next input:");
            input = pg.sample(1000);
            String prefix = "import gnu.trove.TIntCollection; " +
                    "import gnu.trove.function.TIntFunction; " +
                    "import gnu.trove.iterator.TIntIterator; " +
                    "import gnu.trove.iterator.TObjectIntIterator; " +
                    "import gnu.trove.list.TIntList; " +
                    "import gnu.trove.list.array.TIntArrayList; " +
                    "import gnu.trove.map.TObjectIntMap; " +
                    "import gnu.trove.procedure.TIntProcedure; " +
                    "import gnu.trove.procedure.TObjectIntProcedure; " +
                    "import gnu.trove.procedure.TObjectProcedure; " +
                    "import junit.framework.TestCase; " +
                    "import java.io.ByteArrayInputStream; " +
                    "import java.io.ByteArrayOutputStream; " +
                    "import java.io.ObjectInputStream; " +
                    "import java.io.ObjectOutputStream; " +
                    "import java.util.*; " +
                    "public class TObjectPrimitiveHashMapTest"+i+" extends TestCase { " +
                    "    public void test"+i+"() ";
            System.out.print(prefix);
            System.out.print(input);
            System.out.println(" }");
            boolean isRedundant = pg.inputs.contains(input);
            if (!isRedundant) count++;
            System.out.println("Redundant input: "+isRedundant);
        }
        System.out.println("Non-redundant inputs = "+count);
//        System.out.println(pg.seq.toDotString());
//        System.out.println(pg.tree.toDotString());
    }

}
