package generator;

import org.junit.Test;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 5/30/16
 * Time: 10:19 AM
 */
public class ModelTest {

    @org.junit.Test
    public void testAddSentence() throws Exception {
        Model model = new Model(4);
        model.addPrefixes(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 2});
        model.checkConsistency();
        model.calculateProbabilities();
        System.out.println(model.toDotString());
        int[] sampled = model.sampleSentence(100, false);
        StringBuilder sb = new StringBuilder();
        for (int c: sampled) {
            sb.append(c).append(" ");
        }
        System.out.println(sb.toString());
    }

    @Test
    public void test2() throws Exception {
        Model model = new Model(4);
        model.addPrefixes(new int[]{1, 2, 3, 4});
        model.checkConsistency();
        model.calculateProbabilities();
        System.out.println(model.toDotString());
        int[] sampled = model.sampleSentence(100, false);
        StringBuilder sb = new StringBuilder();
        for (int c: sampled) {
            sb.append(c).append(" ");
        }
        System.out.println(sb.toString());
    }

    @Test
    public void test3() throws Exception {
        Model model = new Model(4);
        model.addPrefixes(new int[]{5, 3, 2, 4, 5, 4, 6, 7, 3, 5, 2, 6, 7, 4, 5, 2, 3, 4, 3, 7, 6, 4, 5, 6, -1});
//        model.addPrefixes(new int[]{5, 3, 5, 3, 6});
        model.checkConsistency();
        model.calculateProbabilities();
        System.out.println(model.toDotString());
        int[] sampled = model.sampleSentence(100, false);
        StringBuilder sb = new StringBuilder();
        for (int c: sampled) {
            sb.append(c).append(" ");
        }
        System.out.println(sb.toString());
    }

    @Test
    public void test4() throws Exception {
        Model model = new Model(4);
        model.train("src/main/java/generator/Model.java");
        model.checkConsistency();
        model.calculateProbabilities();
        int[] sampled = model.sampleSentence(100, true);
        StringBuilder sb = new StringBuilder();
        for (int c: sampled) {
            sb.append((char)c);
        }
        System.out.println(sb.toString());

    }

    @Test
    public void test5() throws Exception {
        Model model = new Model(8);
        model.train("calc.original.js");
        model.checkConsistency();
        model.calculateProbabilities();
        for (int j=0; j<100; j++) {
            int[] sampled = model.sampleSentence(1000, true);
            StringBuilder sb = new StringBuilder();
            for (int c : sampled) {
                sb.append((char) c);
            }
            System.out.println(sb.toString());
        }
    }

}