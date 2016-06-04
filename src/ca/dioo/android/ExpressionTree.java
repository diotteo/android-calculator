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
		FLOAT_NB_PAT = Pattern.compile("(\\+|-)?(\\d+\\.\\d*)|(\\d*\\.\\d+)((E|e)(\\+|-)?\\d+)?");
		INT_NB_PAT = Pattern.compile("(\\+|-)?\\d+");
	}

	public ExpressionTree() {
		mRootNode = new Node();
		mCurNode = mRootNode;
	}


	public void buildTree(List<Token> tokList) {
		for (Token tok: tokList) {
			if (tok instanceof Value) {
				if (DEBUG) {
					System.out.println("Value:" + ((Value) tok).getVal());
				}
				mCurNode.setValue((Value) tok);
			} else if (tok instanceof Node) {
				Node n = (Node) tok;
				if (DEBUG) {
					System.out.println("Node:" + n.getType());
				}
				if (mCurNode instanceof PrivNode) {
					if (!((PrivNode) mCurNode).isClosed()) {
						PrivNode pn = (PrivNode) mCurNode;
						Token child = ((PrivNode) mCurNode).getChild();
						mCurNode = new Node(mCurNode, child, null, null);
						pn.setChild(mCurNode);
					}
				}

				if (mCurNode.getType() == null) {
					mCurNode.setType(n.getType());
				} else if (mCurNode instanceof Node && ((Node) mCurNode).getRight() == null) {
					throw new Error("Two consecutive Nodes in token list?");

				//n has higher priority to curNode. Insert above right leaf
				} else if (NodeBase.compare(mCurNode.getType(), n.getType()) > 0) {
					n = new Node(n.getType());
					((Node) mCurNode).insertRight(n);
					mCurNode = n;

				//n has equal or lower priority to curNode. Insert as parent
				//FIXME: need to keep going up until a missing right leaf is found
				} else {
					NodeBase nb = mCurNode.getParent();
					while (nb != null
							&& nb.getType() != NodeType.LEFT_PAREN
							&& NodeBase.compare(nb.getType(), n.getType()) <= 0) {
						mCurNode = nb;
						nb = nb.getParent();
					}
					NodeBase parent = nb;
					n = new Node(n.getType());
					NodeBase child = mCurNode;
					n.setLeft(child);
					n.setParent(parent);
					mCurNode = n;

					if (parent == null) {
						mRootNode = mCurNode;
					} else if (parent instanceof PrivNode) {
						((PrivNode) parent).setChild(mCurNode);
					} else {
						((Node) parent).setRight(mCurNode);
					}
				}
			} else if (tok instanceof PrivNode) {
				PrivNode pn = (PrivNode) tok;
				if (DEBUG) {
					System.out.println("PrivNode:" + pn.getType());
				}

				if (pn.getType() == NodeType.LEFT_PAREN) {
					Token child;
					if (mCurNode instanceof Node) {
						child = ((Node) mCurNode).getRight();
						pn = new PrivNode(null, pn.getType(), child);
						((Node) mCurNode).setRight(pn);
					} else {
						child = ((PrivNode) mCurNode).getChild();
						pn = new PrivNode(null, pn.getType(), child);
						((PrivNode) mCurNode).setChild(pn);
					}
					mCurNode = pn;

				} else if (pn.getType() == NodeType.RIGHT_PAREN) {
					if (mCurNode instanceof Node && ((Node) mCurNode).getRight() == null) {
						if (mCurNode.getType() != null) {
							throw new Error("Bogus closing parenthesis (follows value)");
						} else if (mCurNode.getParent() instanceof PrivNode) {
							Node n = (Node) mCurNode;
							mCurNode = n.getParent();
							((PrivNode) n.getParent()).setChild(n.getLeft());
						} else {
							throw new Error("?!?");
						}
					}

					NodeBase nb = mCurNode;
					boolean found = false;
					while (!(nb instanceof PrivNode)) {
						if (nb.getParent() == null) {
							throw new Error("extraneous closing parenthesis");
						} else {
							nb = nb.getParent();
						}
					}
					((PrivNode) nb).close();
					mCurNode = nb;
				}
			}
		}
	}


	public Number getResult() {
		return _getResult(mRootNode);
	}


	private Number _getResult(Token t) {
		if (t instanceof Value) {
			return ((Value) t).getVal();
		} else if (t instanceof PrivNode) {
			return _getResult(((PrivNode) t).getChild());
		} else if (t instanceof Node) {
			Node n = (Node) t;
			Number left = _getResult(n.getLeft());
			Number right = _getResult(n.getRight());
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
				throw new Error("Unimplemented operator: " + n.getType());
			}
		} else {
			throw new Error("?!?");
		}
	}

	private boolean hasFloat(Number left, Number right) {
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


	public int getDepth() {
		return _getDepth(mRootNode, 0);
	}


	private int _getDepth(Token tree, int depth) {
		if (tree instanceof Value || tree == null) {
			return depth+1;
		} else if (tree instanceof PrivNode) { 
			PrivNode pn = (PrivNode) tree;
			return _getDepth(pn.getChild(), depth + 1);
		} else if (tree instanceof Node) {
			Node n = (Node) tree;
			return Math.max(_getDepth(n.getLeft(), depth + 1), _getDepth(n.getRight(), depth + 1));
		} else {
			throw new Error("?!?");
		}
	}


	public void printTree() {
		printSubtree(mRootNode);
		//printSubtree(mRootNode, 0, getDepth(), 0);
	}

	/*
	private void printSubtree(Token t, int depth, int maxDepth, int nbSpaces) {
		if (t instanceof Node) {
			depth++;
			printSubTree(((Node) t).getLeft(), depth, maxDepth, nbSpaces + Math.pow(2, maxDepth - depth) + 1);
			System.out.println(repeat("", " ", nbSpaces) + '/' + ((Node) t));
			printSubTree(((Node) t).getRight(), depth, maxDepth, nbSpaces + Math.pow(2, maxDepth - depth) + 1);
		} else if (t instanceof PrivNode) {
			t = ((PrivNode) t).getChild();
		} else {
			System.out.println(repeat("", " ", depth
		}
	}
	*/

	private void printSubtree(NodeBase n) {
		System.out.println(n.getType());

		ArrayList<String> al = new ArrayList<String>();
		boolean isDeeper = true;
		for (int i = 1; isDeeper; i++) {
			if (n instanceof Node) {
				boolean l, r;
				l = getValueList(((Node) n).getLeft(), 1, i, al);
				r = getValueList(((Node) n).getRight(), 1, i, al);
				isDeeper = l || r;
			} else {
				isDeeper = getValueList(((PrivNode) n).getChild(), 1, i, al);
			}
			System.out.println(join(", ", al));
			al.clear();
		}
	}

	private String join(String del, Iterable<String> iter) {
		String out = "";

		String sep = "";
		for (String s: iter) {
			out += sep + s;
			sep = del;
		}

		return out;
	}

	private String repeat(String delim, String s, int n) {
		String out = "";
		int i = 0;

		if (n > 0) {
			out = s;
			i = 1;

			while (i * 2 < n) {
				i *= 2;
				out += delim + out;
			}
		}

		for (; i < n; i++) {
			out += delim + s;
		}
		return out;
	}

	private boolean getValueList(Token t, int curDepth, int targetDepth, List<String> valList) {
		if (curDepth < targetDepth) {
			if (t == null || t instanceof Value) {
				String s = "null";
				valList.add(repeat(", ", s, (int) Math.pow(2, targetDepth - curDepth)));
			} else if (t instanceof Node) {
				boolean l, r;

				l = getValueList(((Node) t).getLeft(), curDepth + 1, targetDepth, valList);
				r = getValueList(((Node) t).getRight(), curDepth + 1, targetDepth, valList);
				return l || r;
			} else {
				return getValueList(((PrivNode) t).getChild(), curDepth + 1, targetDepth, valList);
			}
		} else {
			if (t == null) {
				valList.add("null");
			} else if (t instanceof Value) {
				valList.add(((Value) t).toString());
			} else if (t instanceof Node) {
				Node n = (Node) t;
				valList.add(n.getType().toString());
				return n.getLeft() != null || n.getRight() != null;
			} else {
				valList.add(((PrivNode) t).getType().toString());
				return ((PrivNode) t).getChild() != null;
			}
		}
		return false;
	}



	public static List<Token> compressList(List<Token> tokList) {
		ArrayList<Token> al = new ArrayList<Token>();

		Token prevTok = null;
		Token tok = null;
		Token nextTok = null;
		for (Iterator<Token> it = tokList.iterator(); it.hasNext() || nextTok != null; ) {
			prevTok = tok;
			if (nextTok == null) {
				tok = it.next();
			} else {
				tok = nextTok;
				nextTok = null;
			}

			if ((prevTok == null || prevTok instanceof Node)
					&& tok instanceof Node) {
				nextTok = it.next();
				Node n = (Node) tok;
				if (nextTok instanceof Value) {
					switch (n.getType()) {
					case SUB:
						Value v = (Value) nextTok;
						if (v.getVal() instanceof Integer) {
							tok = new Value(new Integer(-v.getVal().intValue()));
						} else if (v.getVal() instanceof Float) {
							tok = new Value(new Float(-v.getVal().floatValue()));
						} else {
							throw new Error("Unknown value type:" + v.getVal().getClass().getName());
						}
						al.add(tok);
						break;
					case ADD:
						tok = nextTok;
						al.add(tok);
						break;
					default:
						throw new Error(n + " is not a unary operator");
					}
					nextTok = null;
				}
			} else if (tok instanceof Node && ((Node) tok).getType() == NodeType.MULT) {
				nextTok = it.next();
				if (nextTok instanceof Node && ((Node) nextTok).getType() == NodeType.MULT) {
					al.add(new Node(NodeType.POW));
				} else {
					al.add(tok);
				}
			} else {
				al.add(tok);
			}
		}

		return al;
	}


	public static List<Token> parse(String expr) {
		int tokStart = 0;
		List<Token> tokList = new ArrayList<Token>();

		for (int i = 0; i < expr.length(); i++) {
			int cp = expr.codePointAt(i);
			String cpstr = new String(Character.toChars(cp));

			if (tokStart == i) {
				if ("+-*/%".indexOf(cp) > -1) {
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
						type = NodeType.MULT;
						break;
					case '/':
						type = NodeType.DIV;
						break;
					case '%':
						type = NodeType.MOD;
						break;
					default:
						throw new Error("Unknown node type \"" + cpstr + "\"");
					}

					tokList.add(new Node(type));
				} else if (cp == '(') {
					tokList.add(new PrivNode(NodeType.LEFT_PAREN));
					tokStart = i + 1;
				} else if (cp == ')') {
					tokList.add(new PrivNode(NodeType.RIGHT_PAREN));
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
							Integer val = new Integer(valstr);
							if (DEBUG) {
								System.out.println("[int]:" + val.intValue());
							}
							tokStart = i + m.end();
							i = tokStart - 1;

							tokList.add(new Value(val));
						} else {
							throw new Error("Unknown token at index " + tokStart + ": " + sub);
						}
					}
				}
			}
		}

		return tokList;
	}


	public static abstract class Token {
		NodeBase mParent;

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
		public abstract void setType(NodeType type);
		public abstract void setValue(Value v);
		public abstract void insert(Token t);

		public static int compare(NodeType t1, NodeType t2) {
			return getPriority(t1) - getPriority(t2);
		}

		private static int getPriority(NodeType type) {
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
				throw new Error("Unknown type priority: " + type);
			}
		}
	}


	protected static class PrivNode extends NodeBase {
		Token mChild;
		NodeType mType;
		boolean closed;

		PrivNode(NodeType type) {
			this(null, type, null);
		}

		PrivNode(NodeBase parent, NodeType type, Token child) {
			super(parent);
			setType(type);
			setChild(child);
			closed = false;
		}

		@Override
		public void setType(NodeType type) {
			if (type == NodeType.LEFT_PAREN || type == NodeType.RIGHT_PAREN) {
				mType = type;
			} else {
				throw new Error("disallowed PrivNode type:" + type);
			}
		}

		public void close() {
			closed = true;
		}

		public boolean isClosed() {
			return closed;
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
		public void insert(Token t) {
			Token child = mChild;
			setChild(t);
			if (child != null) {
				if (!(t instanceof NodeBase)) {
					throw new Error("can't insert non-Node above non-null child");
				} else {
					((NodeBase) t).insert(child);
				}
			}
		}

		@Override
		public void setValue(Value v) {
			setChild(v);
		}

		@Override
		public String toString() {
			return mType.toString();
		}
	}

	public static class Node extends NodeBase {
		Token mLeftLeaf;
		Token mRightLeaf;
		NodeType mType;

		public Node() {
			this(null, null, null, null);
		}

		public Node(NodeType type) {
			this(null, null, null, type);
		}

		public Node(NodeBase parent, Token left, Token right, NodeType type) {
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
		public void setType(NodeType type) {
			if (type == NodeType.LEFT_PAREN || type == NodeType.RIGHT_PAREN) {
				throw new Error("disallowed PrivNode type:" + type);
			} else {
				mType = type;
			}
		}

		@Override
		public NodeType getType() {
			return mType;
		}

		public void insertLeft(Token t) {
			Token child = mLeftLeaf;
			if (child != null && !(t instanceof NodeBase)) {
				throw new Error("can't insert non-NodeBase as parent");
			} else {
				setLeft(t);
				if (child != null) {
					if (t instanceof PrivNode) {
						((PrivNode) t).setChild(child);
					} else if (t instanceof Node) {
						((Node) t).setLeft(child);
					} else {
						throw new Error("?!?");
					}
				}
			}
		}

		public void insertRight(Token t) {
			if (t instanceof Node) {
				((Node) t).setLeft(mRightLeaf);
			} else {
				((PrivNode) t).setChild(mRightLeaf);
			}
			setRight(t);
		}


		@Override
		public void insert(Token t) {
			insertLeft(t);
		}

		@Override
		public void setValue(Value v) {
			if (mLeftLeaf == null) {
				setLeft(v);
			} else if (mRightLeaf == null) {
				setRight(v);
			} else {
				//FIXME: Exception type
				throw new Error("Node is filled, should not happen for a Value");
			}
		}

		@Override
		public String toString() {
			return mType.toString();
		}
	}


	public static class Value extends Token {
		Number mVal;

		public Value(Number value) {
			this(null, value);
		}

		public Value(Node parent, Number value) {
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
				throw new Error("Unknown Value type: " + mVal.getClass().getName());
			}
		}
	}
}
