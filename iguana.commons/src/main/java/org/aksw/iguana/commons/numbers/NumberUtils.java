package org.aksw.iguana.commons.numbers;

/**
 * Utils class for everything with numbers
 *
 * @author f.conrads
 *
 */
public class NumberUtils {

	/**
	 * Returns either a long represantation of the String nm or null.
	 *
	 * @param nm String which should be parsed
	 * @return String as a long representation if String is a Long, otherwise null
	 */
	public static Long getLong(String nm) {
		try {
			Long ret = Long.parseLong(nm);
			return ret;
		}catch(NumberFormatException e) {}
		return null;
	}

	/**
	 * Returns either a double representation of the String nm or null.
	 *
	 * @param nm String which should be parsed
	 * @return String as a double representation if String is a double, otherwise null
	 */
	public static Double getDouble(String nm) {
		try {
			return Double.parseDouble(nm);
		} catch (NumberFormatException | NullPointerException ignored) {
		}
		return null;
	}

}
