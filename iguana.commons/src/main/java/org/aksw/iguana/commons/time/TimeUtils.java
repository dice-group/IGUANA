package org.aksw.iguana.commons.time;

import java.time.Duration;
import java.time.Instant;

public class TimeUtils {

	public static long getTimeInNanoseconds() {
		Instant now = Instant.now();
		return ((long)now.getNano() + now.getEpochSecond() * 1000000000 /*ns*/);
	}

	public static double getTimeInMilliseconds() {
		return getTimeInNanoseconds() / 1000000d /*ms*/;
	}

	public static double durationInMilliseconds(Instant start, Instant end) {
		Duration duration = Duration.between(start, end);
		return ((long)duration.getNano() + duration.getSeconds() * 1000000000 /*ns*/) / 1000000d /*ms*/;
	}
}
