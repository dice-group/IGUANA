package org.aksw.iguana.commons.time;

import org.apache.jena.datatypes.xsd.XSDDuration;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeStampType;
import org.apache.jena.datatypes.xsd.impl.XSDDurationType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;

/**
 * Class related to the conversion of Java time objects to RDF literals.
 */
public class TimeUtils {

	public static XSDDuration toXSDDurationInSeconds(Duration duration) {
		return (XSDDuration) new XSDDurationType().parse("PT" + new BigDecimal(BigInteger.valueOf(duration.toNanos()), 9).toPlainString() + "S");
	}

	public static Literal createTypedDurationLiteral(Duration duration) {
		return ResourceFactory.createTypedLiteral(new XSDDurationType().parse(duration.toString()));
	}

	public static Literal createTypedInstantLiteral(Instant time) {
		return ResourceFactory.createTypedLiteral(new XSDDateTimeStampType(null).parse(time.toString()));
	}
}
