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
	 * @param nm
	 * @return
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
	 * @param nm
	 * @return
	 */
	public static Double getDouble(String nm) {
		try {
			return Double.parseDouble(nm);
		} catch (NumberFormatException | NullPointerException ignored) {
		}
		return null;
	}

}
