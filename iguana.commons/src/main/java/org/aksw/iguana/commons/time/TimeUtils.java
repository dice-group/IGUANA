package org.aksw.iguana.commons.time;

import java.time.Duration;
import java.time.Instant;

/**
 * Everythin related to time stuff
 */
public class TimeUtils {

	/**
	 * returns the current time in Nanoseconds as a long instead of a double
	 * @return current time in  nanoseconds as a long
	 */
	public static long getTimeInNanoseconds() {
		Instant now = Instant.now();
		return ((long)now.getNano() + now.getEpochSecond() * 1000000000 /*ns*/);
	}

	/**
	 * gets the current time in milliseconds
	 * @return the current time in ms
	 */
	public static double getTimeInMilliseconds() {
		return getTimeInNanoseconds() / 1000000d /*ms*/;
	}

	/**
	 * returns the duration in MS between two Time Instants
	 * @param start Start time
	 * @param end end time
	 * @return duration in ms between start and end
	 */
	public static double durationInMilliseconds(Instant start, Instant end) {
		Duration duration = Duration.between(start, end);
		return ((long)duration.getNano() + duration.getSeconds() * 1000000000 /*ns*/) / 1000000d /*ms*/;
	}
}
