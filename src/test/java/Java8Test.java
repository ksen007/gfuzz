import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Tree;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author alex.collins
 */
public class Java8Test {

    public void foo(int a, int b, int c, String s) {
        foo(a, b, c + 1, "Hello\nWorld!");
    }

    public static String getText(RuleContext t) {
        if (t.getChildCount() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int n = t.getChildCount();
        if (n > 1) builder.append("(%");
        for (int i = 0; i < n; i++) {
            ParseTree tree = t.getChild(i);
            if (tree instanceof TerminalNodeImpl) {
                if (i > 0) builder.append(" ");
                builder.append(tree.getText());
            } else {
                if (i > 0) builder.append(" ");
                builder.append(getText((RuleContext) tree));
            }
        }
        if (n > 1) builder.append("%)");

        return builder.toString();
    }

    private boolean inTestMethod = false;
    List<String> ruleNames = null;

    private void setRuleNames(Parser recog) {
        String[] ruleNames = recog != null ? recog.getRuleNames() : null;
        this.ruleNames = ruleNames != null ? Arrays.asList(ruleNames) : null;
    }

    private String getRuleName(Tree t) {
        int ruleIndex = ((RuleNode) t).getRuleContext().getRuleIndex();
        return ruleNames.get(ruleIndex);
    }

    public  String getMethodText(RuleContext t) {
        if (t.getChildCount() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int n = t.getChildCount();
        if (n > 1) builder.append("(%\n");
        for (int i = 0; i < n; i++) {
            ParseTree tree = t.getChild(i);
            if (tree instanceof TerminalNodeImpl) {
                builder.append(" ");
                builder.append(tree.getText());
                builder.append("\n");
            } else {
                builder.append(" ");
                builder.append(getMethodText((RuleContext) tree));
                builder.append("\n");
            }
        }
        if (n > 1) builder.append("%)\n");

        return builder.toString();
    }


    public static void parseFile(String f) {
        try {
            Lexer lexer = new Java8Lexer(new ANTLRFileStream(f));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Java8Parser parser = new Java8Parser(tokens);
            ParserRuleContext t = parser.compilationUnit();
            parser.setBuildParseTree(false);
//            t.getText();
            System.out.println(getText(t));
//            System.out.println(t.toStringTree(parser));
        } catch (Exception e) {
            System.err.println("parser exception: " + e);
            e.printStackTrace();   // so we can get stack trace
        }
    }

    @Test
    public void testExampleField() throws Exception {
        parseFile("src/test/java/Java8Test.java");
    }
}
