package org.aksw.iguana.commons.rdf;

public class IRES {
    public static final String NS = IGUANA_BASE.NS + "resource" + "/";
    public static final String PREFIX = "ires";

    private IRES() {
    }

    /**
     * The RDF-friendly version of the IRES namespace
     * with trailing / character.
     */
    public static String getURI() {
        return NS;
    }


}
