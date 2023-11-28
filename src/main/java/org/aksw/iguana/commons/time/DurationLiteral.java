package org.aksw.iguana.commons.time;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.vocabulary.XSD;

import java.time.Duration;

/**
 * This class is used to convert a Java Duration object to a typed RDF literal. The literal is typed as xsd:duration.<br/>
 * TODO: This class temporarily fixes an issue with Jena.
 */
public class DurationLiteral implements RDFDatatype {

    private final Duration duration;

    public DurationLiteral(Duration duration) {
        this.duration = duration;
    }

    public String getLexicalForm() {
        return duration.toString();
    }

    @Override
    public String getURI() {
        return XSD.getURI() + "duration";
    }

    @Override
    public String unparse(Object value) {
        return ((DurationLiteral) value).getLexicalForm();
    }

    @Override
    public Object parse(String lexicalForm) throws DatatypeFormatException {
        return new DurationLiteral(Duration.parse(lexicalForm));
    }

    @Override
    public boolean isValid(String lexicalForm) {
        try {
            Duration.parse(lexicalForm);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isValidValue(Object valueForm) {
        return valueForm instanceof DurationLiteral;
    }

    @Override
    public boolean isValidLiteral(LiteralLabel lit) {
        return lit.getDatatype() instanceof DurationLiteral;
    }

    @Override
    public boolean isEqual(LiteralLabel value1, LiteralLabel value2) {
        return value1.getDatatype() == value2.getDatatype() && value1.getValue().equals(value2.getValue());
    }

    @Override
    public int getHashCode(LiteralLabel lit) {
        return lit.getValue().hashCode();
    }

    @Override
    public Class<?> getJavaClass() {
        return DurationLiteral.class;
    }

    @Override
    public Object cannonicalise(Object value) {
        return value;
    }

    @Override
    public Object extendedTypeDefinition() {
        return null;
    }

    @Override
    public RDFDatatype normalizeSubType(Object value, RDFDatatype dt) {
        return dt;
    }
}
