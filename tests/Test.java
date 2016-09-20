import java.util.List;
import java.util.Arrays;

import ca.dioo.android.dioo_calc.ExpressionTree;
import ca.dioo.android.dioo_calc.MalformedExpressionException;

public class Test {
	static class TestExpr {
		public String expr;
		public double result;
		public double relerr = 0.005;


		public TestExpr(String expr, double result) {
			this.expr = expr;
			this.result = result;
		}


		public boolean approxEqual(double result) {
			return (this.result - result) / result < this.relerr;
		}
	}


	public static void main(String[] args) throws MalformedExpressionException {
		List<TestExpr> exprList;
		if (true) {
			exprList = Arrays.asList(
					//"-20..+30**(45.2+*.5E-2)-36",
					new TestExpr("-+-(-2)", -+-(-2)),
					new TestExpr("2**(2)", Math.pow(2, 2)),
					new TestExpr("200.+2", 200.+2),
					new TestExpr("1+2*3+5/4", 1+2*3+5/4),
					new TestExpr("1+2*3/4-5+6", 1+2*3/4-5+6),
					new TestExpr("0xff ** -(1.)", Math.pow(255, -1)),
					new TestExpr("-2e-2+30", -2e-2+30),
					new TestExpr("-20+30*(45.2+-.5E-2)-36", -20+30*(45.2+-.5E-2)-36),
					new TestExpr("-20+30**(45.2+-.5E-2)-36", -20+Math.pow(30, (45.2+-.5E-2))-36)
					);
		} else {
			exprList = Arrays.asList();
		}

		ExpressionTree.DEBUG = false;

		for (TestExpr expr: exprList) {
			System.out.println("expr:" + expr.expr);
			List<ExpressionTree.Token> tokList = ExpressionTree.parse(expr.expr);
			printList(tokList);
			System.out.println("");

			List<ExpressionTree.Token> optList = ExpressionTree.compressList(tokList);
			printList(optList);
			System.out.println("");

			ExpressionTree tree = ExpressionTree.buildTree(optList);
			tree.printTree();

			Number n = tree.getResult();
			String s;
			if (expr.approxEqual(n.doubleValue())) {
				s = "PASSED";
			} else {
				s = "FAILED";
			}
			System.out.println("\n" + s + ": " + n + " = " + expr.result);
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
