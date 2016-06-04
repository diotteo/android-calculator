import java.util.List;
import java.util.Arrays;

import ca.dioo.android.dioo_calc.ExpressionTree;

public class Test {
	public static void main(String[] args) {
		List<String> exprList;
		if (true) {
			exprList = Arrays.asList(
					//"-20..+30**(45.2+*.5E-2)-36",
					"2**(2)",
					"200.+2",
					"1+2*3+5/4",
					"1+2*3/4-5+6",
					"0xff ** -(1.)",
					"-20+30*(45.2+-.5E-2)-36",
					"-20+30**(45.2+-.5E-2)-36");
		} else {
			exprList = Arrays.asList();
		}

		ExpressionTree.DEBUG = true;

		for (String expr: exprList) {
			System.out.println("expr:" + expr);
			List<ExpressionTree.Token> tokList = ExpressionTree.parse(expr);
			printList(tokList);
			System.out.println("");

			List<ExpressionTree.Token> optList = ExpressionTree.compressList(tokList);
			printList(optList);
			System.out.println("");

			ExpressionTree tree = ExpressionTree.buildTree(optList);
			tree.printTree();
			System.out.println("\nresult:" + tree.getResult());

			System.out.println("\n");
		}
	}


	public static void printList(List<ExpressionTree.Token> l) {
		String sep = "  ";
		for (ExpressionTree.Token t: l) {
			System.out.print(sep + t.toString());
			sep = ", ";
		}
	}
}
