package org.aksw.iguana.commons.rdf;

public class IGUANA_BASE {
    public static final String NS = "http://iguana-benchmark.eu" + "/";
    public static final String PREFIX = "iguana";

    private IGUANA_BASE() {
    }

    /**
     * The RDF-friendly version of the IGUANA namespace
     * with trailing / character.
     */
    public static String getURI() {
        return NS;
    }
}
