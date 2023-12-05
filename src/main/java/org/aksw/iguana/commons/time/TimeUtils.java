package org.aksw.iguana.commons.time;

import org.apache.jena.datatypes.xsd.impl.XSDDateTimeStampType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class related to the conversion of Java time objects to RDF literals.
 */
public class TimeUtils {

	public static Literal createTypedDurationLiteralInSeconds(Duration duration) {
		var seconds = "PT" + new BigDecimal(BigInteger.valueOf(duration.toNanos()), 9).toPlainString() + "S";

		// cut trailing zeros
		while (seconds.lastIndexOf("0") == seconds.length() - 2 /* The last character is S */) {
			seconds = seconds.substring(0, seconds.length() - 2) + "S";
		}

		if (seconds.endsWith(".S")) {
			seconds = seconds.substring(0, seconds.length() - 2) + "S";
		}

		return ResourceFactory.createTypedLiteral(seconds, new DurationLiteral(duration));
	}

	public static Literal createTypedDurationLiteral(Duration duration) {
		return ResourceFactory.createTypedLiteral(duration.toString(), new DurationLiteral(duration));
	}

	public static Literal createTypedInstantLiteral(Instant time) {
		return ResourceFactory.createTypedLiteral(new XSDDateTimeStampType(null).parse(time.toString()));
	}

	public static Literal createTypedZonedDateTimeLiteral(ZonedDateTime time) {
		return ResourceFactory.createTypedLiteral(new XSDDateTimeStampType(null).parse(time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
	}
}
