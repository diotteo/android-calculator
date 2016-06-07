package ca.dioo.android.dioo_calc;

public class MalformedExpressionException extends Exception {
	public MalformedExpressionException(String message) {
		this(message, null);
	}

	public MalformedExpressionException(String message, Throwable cause) {
		super(message, cause);
	}
}
