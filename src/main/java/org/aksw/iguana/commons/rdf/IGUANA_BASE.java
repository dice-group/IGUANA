package org.aksw.iguana.commons.rdf;

import java.util.Map;

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

    public static Map<String, String> PREFIX_MAP = Map.of(
            IGUANA_BASE.PREFIX, IGUANA_BASE.NS,
            IONT.PREFIX, IONT.NS,
            IPROP.PREFIX, IPROP.NS,
            IRES.PREFIX, IRES.NS,
            "lsqr", "http://lsq.aksw.org/res/"
    );
}
