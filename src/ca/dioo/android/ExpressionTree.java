package ca.dioo.android.dioo_calc;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class ExpressionTree {
	public static boolean DEBUG = false;

	private NodeBase mRootNode;
	private NodeBase mCurNode;
	private List<Token> tokList;

	private static Pattern FLOAT_NB_PAT;
	private static Pattern INT_NB_PAT;

	static {
		FLOAT_NB_PAT = Pattern.compile("(\\+|-)?((((\\d+\\.\\d*)|(\\d*\\.\\d+))((E|e)(\\+|-)?\\d+)?)|\\d+(E|e)(-)?\\d+)");
		INT_NB_PAT = Pattern.compile("((\\+|-)?[1-9]\\d*)|(0x\\p{XDigit}+)");
	}

	public ExpressionTree() throws MalformedExpressionException {
		mRootNode = new BinaryNode();
		mCurNode = mRootNode;
	}


	public static ExpressionTree fromExpr(String expr) throws MalformedExpressionException {
		List<Token> tokList = parse(expr);
		tokList = compressList(tokList);
		ExpressionTree tree = buildTree(tokList);
		return tree;
	}

	public static Number getResultFromExpr(String expr) throws MalformedExpressionException {
		return fromExpr(expr).getResult();
	}


	public static ExpressionTree buildTree(List<Token> tokList) throws MalformedExpressionException {
		ExpressionTree tree = new ExpressionTree();

		for (Token tok: tokList) {
			if (tok instanceof Value) {
				if (DEBUG) {
					System.out.println("Value:" + ((Value) tok).getVal());
				}
				tree.mCurNode.setValue((Value) tok);
			} else if (tok instanceof BinaryNode) {
				BinaryNode n = (BinaryNode) tok;
				if (DEBUG) {
					System.out.println("BinaryNode:" + n.getType());
				}
				if (tree.mCurNode instanceof UnaryNode) {
					UnaryNode pn = (UnaryNode) tree.mCurNode;
					if (!pn.isClosed()) {
						Token child = pn.getChild();
						tree.mCurNode = new BinaryNode(tree.mCurNode, child, null, null);
						pn.setChild(tree.mCurNode);
					}
				}

				if (tree.mCurNode.getType() == null) {
					tree.mCurNode.setType(n.getType());
				} else if (tree.mCurNode instanceof BinaryNode && ((BinaryNode) tree.mCurNode).getRight() == null) {
					throw new MalformedExpressionException("Two consecutive Nodes in token list?");

				//n has higher priority to curNode. Insert above right leaf
				} else if (NodeBase.compare(tree.mCurNode.getType(), n.getType()) > 0) {
					n = new BinaryNode(n.getType());
					((BinaryNode) tree.mCurNode).insertRight(n);
					tree.mCurNode = n;

				//n has equal or lower priority to curNode. Insert as parent
				//FIXME: need to keep going up until a missing right leaf is found
				} else {
					NodeBase nb = tree.mCurNode.getParent();
					while (nb != null
							&& nb.getType() != NodeType.LEFT_PAREN
							&& NodeBase.compare(nb.getType(), n.getType()) <= 0) {
						tree.mCurNode = nb;
						nb = nb.getParent();
					}
					NodeBase parent = nb;
					n = new BinaryNode(n.getType());
					NodeBase child = tree.mCurNode;
					n.setLeft(child);
					n.setParent(parent);
					tree.mCurNode = n;

					if (parent == null) {
						tree.mRootNode = tree.mCurNode;
					} else if (parent instanceof UnaryNode) {
						((UnaryNode) parent).setChild(tree.mCurNode);
					} else {
						((BinaryNode) parent).setRight(tree.mCurNode);
					}
				}
			} else if (tok instanceof UnaryNode) {
				UnaryNode pn = (UnaryNode) tok;
				if (DEBUG) {
					System.out.println("UnaryNode:" + pn);
				}

				if (pn.getType() == NodeType.LEFT_PAREN) {
					Token child;
					if (tree.mCurNode instanceof BinaryNode) {
						BinaryNode n = (BinaryNode) tree.mCurNode;
						child = n.getRight();
						boolean isNegative = pn.isNegative();
						pn = new UnaryNode(null, pn.getType(), child);
						pn.setNegative(isNegative);

						if (n.getType() == null) {
							if (n.getParent() == null) {
								tree.mRootNode = pn;
							} else {
								throw new MalformedExpressionException("?!?");
							}
							pn.setParent(n.getParent());
						} else {
							n.setRight(pn);
						}
					} else {
						child = ((UnaryNode) tree.mCurNode).getChild();
						pn = new UnaryNode(null, pn.getType(), child);
						((UnaryNode) tree.mCurNode).setChild(pn);
					}
					tree.mCurNode = pn;

				} else if (pn.getType() == NodeType.RIGHT_PAREN) {
					if (tree.mCurNode instanceof BinaryNode && ((BinaryNode) tree.mCurNode).getRight() == null) {
						if (tree.mCurNode.getType() != null) {
							throw new MalformedExpressionException("Bogus closing parenthesis (follows operator)");
						} else if (tree.mCurNode.getParent() instanceof UnaryNode) {
							BinaryNode n = (BinaryNode) tree.mCurNode;
							tree.mCurNode = n.getParent();
							((UnaryNode) n.getParent()).setChild(n.getLeft());
						} else {
							throw new MalformedExpressionException("?!?");
						}
					}

					NodeBase nb = tree.mCurNode;
					boolean found = false;
					while (!(nb instanceof UnaryNode)) {
						if (nb.getParent() == null) {
							throw new MalformedExpressionException("extraneous closing parenthesis");
						} else {
							nb = nb.getParent();
						}
					}
					((UnaryNode) nb).close();
					tree.mCurNode = nb;
				}
			}
		}
		return tree;
	}


	public Number getResult() throws MalformedExpressionException {
		return _getResult(mRootNode);
	}


	private Number _getResult(Token t) throws MalformedExpressionException {
		if (t == null) {
			return null;
		} else if (t instanceof Value) {
			return ((Value) t).getVal();
		} else if (t instanceof UnaryNode) {
			UnaryNode pn = (UnaryNode) t;
			Number num = _getResult(pn.getChild());
			if (num == null) {
				return null;
			} else if (pn.isNegative()) {
				if (hasFloat(num, null)) {
					num = new Double(-num.doubleValue());
				} else {
					num = new Long(-num.longValue());
				}
			}
			return num;
		} else if (t instanceof BinaryNode) {
			BinaryNode n = (BinaryNode) t;
			Number left = _getResult(n.getLeft());
			if (left == null) {
				return null;
			}

			//Handle special case where expression is just a number
			if (n == mRootNode && n.getRight() == null && n.getType() == null) {
				return left;
			}
			Number right = _getResult(n.getRight());
			if (right == null) {
				return null;
			}
			switch (n.getType()) {
			case ADD:
				return add(left, right);
			case SUB:
				return sub(left, right);
			case MULT:
				return mult(left, right);
			case DIV:
				return div(left, right);
			case MOD:
				return mod(left, right);
			case POW:
				return pow(left, right);
			default:
				throw new MalformedExpressionException("Unimplemented operator: " + n.getType());
			}
		} else {
			throw new MalformedExpressionException("?!?:" + t);
		}
	}

	private static boolean hasFloat(Number left, Number right) {
		return (left instanceof Float
				|| left instanceof Double
				|| right instanceof Float
				|| right instanceof Double);
	}

	private Number add(Number left, Number right) {
		if (hasFloat(left, right)) {
			return new Double(left.doubleValue() + right.doubleValue());
		} else {
			return new Long(left.longValue() + right.longValue());
		}
	}

	private Number sub(Number left, Number right) {
		if (hasFloat(left, right)) {
			return new Double(left.doubleValue() - right.doubleValue());
		} else {
			return new Long(left.longValue() - right.longValue());
		}
	}

	private Number mult(Number left, Number right) {
		if (hasFloat(left, right)) {
			return new Double(left.doubleValue() * right.doubleValue());
		} else {
			return new Long(left.longValue() * right.longValue());
		}
	}

	private Number div(Number left, Number right) {
		if (hasFloat(left, right)) {
			return new Double(left.doubleValue() / right.doubleValue());
		} else {
			return new Long(left.longValue() / right.longValue());
		}
	}

	private Number mod(Number left, Number right) {
		if (hasFloat(left, right)) {
			return new Double(left.doubleValue() % right.doubleValue());
		} else {
			return new Long(left.longValue() % right.longValue());
		}
	}

	private Number pow(Number left, Number right) {
		if (hasFloat(left, right)) {
			return new Double(Math.pow(left.doubleValue(), right.doubleValue()));
		} else {
			return new Long((long)Math.pow(left.doubleValue(), right.doubleValue()));
		}
	}


	public int getDepth() throws MalformedExpressionException {
		return _getDepth(mRootNode, 0);
	}


	private int _getDepth(Token tree, int depth) throws MalformedExpressionException {
		if (tree instanceof Value || tree == null) {
			return depth+1;
		} else if (tree instanceof UnaryNode) {
			UnaryNode pn = (UnaryNode) tree;
			return _getDepth(pn.getChild(), depth + 1);
		} else if (tree instanceof BinaryNode) {
			BinaryNode n = (BinaryNode) tree;
			return Math.max(_getDepth(n.getLeft(), depth + 1), _getDepth(n.getRight(), depth + 1));
		} else {
			throw new MalformedExpressionException("?!?");
		}
	}


	public void printTree() {
		printSubtree(mRootNode);
		//printSubtree(mRootNode, 0, getDepth(), 0);
	}

	/*
	private void printSubtree(Token t, int depth, int maxDepth, int nbSpaces) {
		if (t instanceof BinaryNode) {
			depth++;
			printSubTree(((BinaryNode) t).getLeft(), depth, maxDepth, nbSpaces + Math.pow(2, maxDepth - depth) + 1);
			System.out.println(StringUtils.repeat("", " ", nbSpaces) + '/' + ((BinaryNode) t));
			printSubTree(((BinaryNode) t).getRight(), depth, maxDepth, nbSpaces + Math.pow(2, maxDepth - depth) + 1);
		} else if (t instanceof UnaryNode) {
			t = ((UnaryNode) t).getChild();
		} else {
			System.out.println(StringUtils.repeat("", " ", depth
		}
	}
	*/

	private void printSubtree(NodeBase n) {
		System.out.println(n.getType());

		ArrayList<String> al = new ArrayList<String>();
		boolean isDeeper = true;
		for (int i = 1; isDeeper; i++) {
			if (n instanceof BinaryNode) {
				boolean l, r;
				l = getValueList(((BinaryNode) n).getLeft(), 1, i, al);
				r = getValueList(((BinaryNode) n).getRight(), 1, i, al);
				isDeeper = l || r;
			} else {
				isDeeper = getValueList(((UnaryNode) n).getChild(), 1, i, al);
			}
			System.out.println(StringUtils.join(", ", al));
			al.clear();
		}
	}

	private boolean getValueList(Token t, int curDepth, int targetDepth, List<String> valList) {
		if (curDepth < targetDepth) {
			if (t == null || t instanceof Value) {
				String s = "null";
				valList.add(StringUtils.repeat(", ", s, (int) Math.pow(2, targetDepth - curDepth)));
			} else if (t instanceof BinaryNode) {
				boolean l, r;

				l = getValueList(((BinaryNode) t).getLeft(), curDepth + 1, targetDepth, valList);
				r = getValueList(((BinaryNode) t).getRight(), curDepth + 1, targetDepth, valList);
				return l || r;
			} else {
				return getValueList(((UnaryNode) t).getChild(), curDepth + 1, targetDepth, valList);
			}
		} else {
			if (t == null) {
				valList.add("null");
			} else if (t instanceof Value) {
				valList.add(((Value) t).toString());
			} else if (t instanceof BinaryNode) {
				BinaryNode n = (BinaryNode) t;
				valList.add(n.getType().toString());
				return n.getLeft() != null || n.getRight() != null;
			} else {
				valList.add(((UnaryNode) t).toString());
				return ((UnaryNode) t).getChild() != null;
			}
		}
		return false;
	}



	public static List<Token> compressList(List<Token> tokList) throws MalformedExpressionException {
		ArrayList<Token> al = new ArrayList<Token>();

		Token prevTok = null;
		Token tok = null;
		Token nextTok = null;
		for (Iterator<Token> it = tokList.iterator(); it.hasNext() || nextTok != null; ) {
			prevTok = tok;
			if (nextTok == null) {
				if (!it.hasNext()) {
					throw new MalformedExpressionException("Unterminated expression");
				}
				tok = it.next();
			} else {
				tok = nextTok;
				nextTok = null;
			}

			if ((
					prevTok == null || prevTok instanceof BinaryNode
					||
						(
						prevTok instanceof UnaryNode
						&& ((UnaryNode) prevTok).getType() == NodeType.LEFT_PAREN
						)
					)
					&& tok instanceof BinaryNode) {
				if (!it.hasNext()) {
					throw new MalformedExpressionException("Unterminated expression");
				}
				nextTok = it.next();
				BinaryNode n = (BinaryNode) tok;

				if (nextTok instanceof BinaryNode) {
					BinaryNode nxt = (BinaryNode) nextTok;
					if ((nxt.getType() == NodeType.SUB || nxt.getType() == NodeType.ADD)
							&& (n.getType() == NodeType.SUB || n.getType() == NodeType.ADD)) {
						if (n.getType() == NodeType.ADD && nxt.getType() == NodeType.ADD) {
							tok = prevTok;
							nextTok = new BinaryNode(NodeType.ADD);
						} else if (n.getType() == NodeType.SUB && nxt.getType() == NodeType.SUB) {
							tok = prevTok;
							nextTok = new BinaryNode(NodeType.ADD);
						} else {
							tok = prevTok;
							nextTok = new BinaryNode(NodeType.SUB);
						}
					} else {
						throw new MalformedExpressionException("one of " + n + " or " + nxt + " is not a unary operator");
					}
				} else if (nextTok instanceof UnaryNode) {
					switch (n.getType()) {
					case ADD:
						tok = prevTok;
						break;
					case SUB:
						((UnaryNode) nextTok).setNegative(true);
						tok = prevTok;
						break;
					default:
						throw new MalformedExpressionException(n + " is not a unary operator");
					}
				} else if (nextTok instanceof Value) {
					switch (n.getType()) {
					case SUB:
						Value v = (Value) nextTok;
						if (v.getVal() instanceof Integer) {
							tok = new Value(new Integer(-v.getVal().intValue()));
						} else if (v.getVal() instanceof Float) {
							tok = new Value(new Float(-v.getVal().floatValue()));
						} else {
							throw new MalformedExpressionException("Unknown value type:" + v.getVal().getClass().getName());
						}
						al.add(tok);
						break;
					case ADD:
						tok = nextTok;
						al.add(tok);
						break;
					case MULT:
						if (prevTok instanceof BinaryNode && ((BinaryNode) prevTok).getType() == NodeType.MULT) {
							//Pass
						} else {
							throw new MalformedExpressionException(n + " is not a unary operator");
						}
						break;
					default:
						throw new MalformedExpressionException(n + " is not a unary operator");
					}
					nextTok = null;
				}
			} else if (tok instanceof BinaryNode && ((BinaryNode) tok).getType() == NodeType.MULT) {
				if (!it.hasNext()) {
					throw new MalformedExpressionException("Unterminated expression");
				}
				nextTok = it.next();
				if (nextTok instanceof BinaryNode && ((BinaryNode) nextTok).getType() == NodeType.MULT) {
					tok = new BinaryNode(NodeType.POW);
					nextTok = null;
					al.add(tok);
				} else {
					al.add(tok);
				}
			} else {
				al.add(tok);
			}
		}

		return al;
	}


	public static List<Token> parse(String expr) throws MalformedExpressionException {
		int tokStart = 0;
		List<Token> tokList = new ArrayList<Token>();

		for (int i = 0; i < expr.length(); i++) {
			int cp = expr.codePointAt(i);
			String cpstr = new String(Character.toChars(cp));

			if (tokStart == i) {
				if ("+-*×/÷%".indexOf(cp) > -1) {
					NodeType type = null;
					if (DEBUG) {
						System.out.println("[operator]:" + cpstr);
					}
					tokStart = i + 1;

					switch (cp) {
					case '+':
						type = NodeType.ADD;
						break;
					case '-':
						type = NodeType.SUB;
						break;
					case '*':
					case '×':
						type = NodeType.MULT;
						break;
					case '/':
					case '÷':
						type = NodeType.DIV;
						break;
					case '%':
						type = NodeType.MOD;
						break;
					default:
						throw new MalformedExpressionException("Unknown node type \"" + cpstr + "\"");
					}

					tokList.add(new BinaryNode(type));
				} else if (cp == '(') {
					tokList.add(new UnaryNode(NodeType.LEFT_PAREN));
					tokStart = i + 1;
				} else if (cp == ')') {
					tokList.add(new UnaryNode(NodeType.RIGHT_PAREN));
					tokStart = i + 1;
				} else {
					String sub = expr.substring(tokStart);
					Matcher m = FLOAT_NB_PAT.matcher(sub);
					if (m.find() && m.start() == 0) {
						String valstr = sub.substring(0, m.end());
						Float val = new Float(valstr);
						if (DEBUG) {
							System.out.println("[float]:" + val.floatValue());
						}
						tokStart = i + m.end();
						i = tokStart - 1;

						tokList.add(new Value(val));
					} else {
						m = INT_NB_PAT.matcher(sub);
						if (m.find() && m.start() == 0) {
							String valstr = sub.substring(0, m.end());
							Integer val = Integer.decode(valstr);
							if (DEBUG) {
								System.out.println("[int]:" + val.intValue());
							}
							tokStart = i + m.end();
							i = tokStart - 1;

							tokList.add(new Value(val));
						} else if (Character.isWhitespace(cp)) {
							tokStart = i + 1;
						} else {
							throw new MalformedExpressionException("Unknown token at index " + tokStart + ": " + sub);
						}
					}
				}
			}
		}

		return tokList;
	}


	public static abstract class Token {
		private NodeBase mParent;

		public Token(NodeBase parent) {
			setParent(parent);
		}

		public void setParent(NodeBase parent) {
			mParent = parent;
		}

		public NodeBase getParent() {
			return mParent;
		}

		public abstract String toString();
	}


	public enum NodeType {
		ADD,
		SUB,
		MULT,
		DIV,
		MOD,
		POW,
		LEFT_PAREN,
		RIGHT_PAREN,
	}

	public static abstract class NodeBase extends Token {
		NodeBase(NodeBase parent) {
			super(parent);
		}

		public abstract NodeType getType();
		public abstract void setType(NodeType type) throws MalformedExpressionException;
		public abstract void setValue(Value v) throws MalformedExpressionException;
		public abstract void insert(Token t) throws MalformedExpressionException;

		public static int compare(NodeType t1, NodeType t2) throws MalformedExpressionException {
			return getPriority(t1) - getPriority(t2);
		}

		private static int getPriority(NodeType type) throws MalformedExpressionException {
			switch (type) {
			case LEFT_PAREN:
			case RIGHT_PAREN:
				return 0;
			case POW:
				return 1;
			case MULT:
			case DIV:
			case MOD:
				return 2;
			case ADD:
			case SUB:
				return 3;
			default:
				throw new MalformedExpressionException("Unknown type priority: " + type);
			}
		}
	}


	public static class UnaryNode extends NodeBase {
		private Token mChild;
		private NodeType mType;
		private boolean mClosed;
		private boolean mNegative;

		UnaryNode(NodeType type) throws MalformedExpressionException {
			this(null, type, null);
		}

		UnaryNode(NodeBase parent, NodeType type, Token child) throws MalformedExpressionException {
			super(parent);
			setType(type);
			setChild(child);
			mClosed = false;
			mNegative = false;
		}

		@Override
		public void setType(NodeType type) throws MalformedExpressionException {
			if (type == NodeType.LEFT_PAREN || type == NodeType.RIGHT_PAREN) {
				mType = type;
			} else {
				throw new MalformedExpressionException("disallowed UnaryNode type:" + type);
			}
		}

		public void close() {
			mClosed = true;
		}

		public boolean isClosed() {
			return mClosed;
		}

		public void setNegative(boolean negative) {
			mNegative = negative;
		}

		public boolean isNegative() {
			return mNegative;
		}

		@Override
		public NodeType getType() {
			return mType;
		}

		public void setChild(Token child) {
			mChild = child;
			if (child != null) {
				child.setParent(this);
			}
		}

		public Token getChild() {
			return mChild;
		}

		@Override
		public void insert(Token t) throws MalformedExpressionException {
			Token child = mChild;
			setChild(t);
			if (child != null) {
				if (!(t instanceof NodeBase)) {
					throw new MalformedExpressionException("can't insert Value above non-null child");
				} else {
					((NodeBase) t).insert(child);
				}
			}
		}

		@Override
		public void setValue(Value v) throws MalformedExpressionException {
			setChild(v);
		}

		@Override
		public String toString() {
			return (mNegative ? "-" : "") + mType.toString();
		}
	}

	public static class BinaryNode extends NodeBase {
		private Token mLeftLeaf;
		private Token mRightLeaf;
		private NodeType mType;

		public BinaryNode() throws MalformedExpressionException {
			this(null, null, null, null);
		}

		public BinaryNode(NodeType type) throws MalformedExpressionException {
			this(null, null, null, type);
		}

		public BinaryNode(NodeBase parent, Token left, Token right, NodeType type) throws MalformedExpressionException {
			super(parent);
			setLeft(left);
			setRight(right);
			setType(type);
		}


		public void setLeft(Token left) {
			mLeftLeaf = left;
			if (left != null) {
				left.setParent(this);
			}
		}

		public Token getLeft() {
			return mLeftLeaf;
		}

		public void setRight(Token right) {
			mRightLeaf = right;
			if (right != null) {
				right.setParent(this);
			}
		}

		public Token getRight() {
			return mRightLeaf;
		}

		@Override
		public void setType(NodeType type) throws MalformedExpressionException {
			if (type == NodeType.LEFT_PAREN || type == NodeType.RIGHT_PAREN) {
				throw new MalformedExpressionException("disallowed UnaryNode type:" + type);
			} else {
				mType = type;
			}
		}

		@Override
		public NodeType getType() {
			return mType;
		}

		public void insertLeft(Token t) throws MalformedExpressionException {
			Token child = mLeftLeaf;
			if (child != null && !(t instanceof NodeBase)) {
				throw new MalformedExpressionException("can't insert non-NodeBase as parent");
			} else {
				setLeft(t);
				if (child != null) {
					if (t instanceof UnaryNode) {
						((UnaryNode) t).setChild(child);
					} else if (t instanceof BinaryNode) {
						((BinaryNode) t).setLeft(child);
					} else {
						throw new MalformedExpressionException("?!?");
					}
				}
			}
		}

		public void insertRight(Token t) {
			if (t instanceof BinaryNode) {
				((BinaryNode) t).setLeft(mRightLeaf);
			} else {
				((UnaryNode) t).setChild(mRightLeaf);
			}
			setRight(t);
		}


		@Override
		public void insert(Token t) throws MalformedExpressionException {
			insertLeft(t);
		}

		@Override
		public void setValue(Value v) throws MalformedExpressionException {
			if (mLeftLeaf == null) {
				setLeft(v);
			} else if (mRightLeaf == null) {
				setRight(v);
			} else {
				//FIXME: Exception type
				throw new MalformedExpressionException("BinaryNode is filled, should not happen for a Value");
			}
		}

		@Override
		public String toString() {
			return mType.toString();
		}
	}


	public static class Value extends Token {
		private Number mVal;

		public Value(Number value) {
			this(null, value);
		}

		public Value(BinaryNode parent, Number value) {
			super(parent);

			mVal = value;
		}

		public Number getVal() {
			return mVal;
		}

		@Override
		public String toString() {
			if (mVal instanceof Float) {
				return mVal.toString(); //floatValue();
			} else if (mVal instanceof Integer) {
				return mVal.toString(); //intValue();
			} else {
				//throw new Error("Unknown Value type: " + mVal.getClass().getName());
				return null;
			}
		}
	}
}
