package ca.dioo.android.dioo_calc;

public class StringUtils {
	private StringUtils() {}

	public static String join(String del, Iterable<String> iter) {
		String out = "";

		String sep = "";
		for (String s: iter) {
			out += sep + s;
			sep = del;
		}

		return out;
	}

	public static String repeat(String delim, String s, int n) {
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

}
