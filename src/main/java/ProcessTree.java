import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Tree;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 6/18/16
 * Time: 10:41 AM
 */
public class ProcessTree {
    private int contextState = 0;
    List<String> ruleNames = null;
    String methodName;

    private void setRuleNames(Parser recog) {
        String[] ruleNames = recog != null ? recog.getRuleNames() : null;
        this.ruleNames = ruleNames != null ? Arrays.asList(ruleNames) : null;
    }

    private String getRuleName(Tree t) {
        int ruleIndex = ((RuleNode) t).getRuleContext().getRuleIndex();
        return ruleNames.get(ruleIndex);
    }

    private StringBuilder globalBuilder = null;

    public String getMethodText(RuleContext t) {
        if (t.getChildCount() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int n = t.getChildCount();
        if (n > 1) builder.append("(%\n");
        for (int i = 0; i < n; i++) {
            ParseTree tree = t.getChild(i);
            if (tree instanceof TerminalNodeImpl) {
                String s = tree.getText();
                builder.append(" ");
                builder.append(s);
                builder.append("\n");
            } else {
                String ruleName = getRuleName(tree);
                if (contextState == 0 && ruleName.equals("methodDeclaration")) contextState++;
                if (contextState == 2 && ruleName.equals("methodBody")) {
                    contextState++;
                    globalBuilder.append("-");
                    globalBuilder.append(methodName);
                    globalBuilder.append("\n");
                }
                String s = getMethodText((RuleContext) tree);
                builder.append(s);
                if (ruleName.equals("methodBody")) {
                    if (contextState == 3) globalBuilder.append(s);
                    contextState = 0;
                }
                if (ruleName.equals("methodDeclarator")) {
                    methodName = tree.getChild(0).getText();
                    if (contextState == 1 && methodName.startsWith("test")) contextState++;

                }
                if (contextState == 1 && s.equals("(%\n @\n Test\n%)\n")) contextState++;
            }
        }
        if (n > 1) builder.append("%)\n");

        return builder.toString();
    }

    public void parseFile(String f) {
        try {
            Lexer lexer = new Java8Lexer(new ANTLRFileStream(f));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Java8Parser parser = new Java8Parser(tokens);
            ParserRuleContext t = parser.compilationUnit();
            parser.setBuildParseTree(false);
            setRuleNames(parser);
            globalBuilder = new StringBuilder();
            getMethodText(t);
            System.out.println(globalBuilder.toString());
            try(  PrintWriter out = new PrintWriter(f+".tok")  ){
                out.print( globalBuilder.toString());
            }
            //System.out.println(t.toStringTree(parser));
        } catch (Exception e) {
            System.err.println("parser exception: " + e);
            e.printStackTrace();   // so we can get stack trace
        }
    }

    public static void main(String args[]) {
        ProcessTree p = new ProcessTree();
        p.parseFile(args[0]);
    }

}
