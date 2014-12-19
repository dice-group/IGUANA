package de.uni_leipzig.iguana.query;

import java.util.HashMap;
/**
 * User: Saud Aljaloud email: sza1g10@ecs.soton.ac.uk
 */

public class CopyOfPrefixes {

	public static final HashMap<String, String> prefixes = new HashMap<>();
	static {
		prefixes.put("a", "http://www.w3.org/2005/Atom");
		prefixes.put("address", "http://schemas.talis.com/2005/address/schema#");
		prefixes.put("admin", "http://webns.net/mvcb/");
		prefixes.put("atom", "http://atomowl.org/ontologies/atomrdf#");
		prefixes.put("aws", "http://soap.amazon.com/");
		prefixes.put("b3s", "http://b3s.openlinksw.com/");
		prefixes.put("batch", "http://schemas.google.com/gdata/batch");
		prefixes.put("bibo", "http://purl.org/ontology/bibo/");
		//		prefixes.put("bif", "bif:");
		prefixes.put("bugzilla", "http://www.openlinksw.com/schemas/bugzilla#");
		prefixes.put("c", "http://www.w3.org/2002/12/cal/icaltzd#");
		prefixes.put("category", "http://dbpedia.org/resource/Category:");
		prefixes.put("cb", "http://www.crunchbase.com/");
		prefixes.put("cc", "http://web.resource.org/cc/");
		prefixes.put("content", "http://purl.org/rss/1.0/modules/content/");
		prefixes.put("cv", "http://purl.org/captsolo/resume-rdf/0.2/cv#");
		prefixes.put("cvbase", "http://purl.org/captsolo/resume-rdf/0.2/base#");
		prefixes.put("dawgt",
				"http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#");
		prefixes.put("dbpedia", "http://dbpedia.org/resource/");
		prefixes.put("dbpedia-owl", "http://dbpedia.org/ontology/");
		prefixes.put("dbpprop", "http://dbpedia.org/property/");
		prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
		prefixes.put("dcterms", "http://purl.org/dc/terms/");
		prefixes.put("digg", "http://digg.com/docs/diggrss/");
		prefixes.put("ebay", "urn:ebay:apis:eBLBaseComponents");
		prefixes.put("enc", "http://purl.oclc.org/net/rss_2.0/enc#");
		prefixes.put("exif", "http://www.w3.org/2003/12/exif/ns/");
		prefixes.put("fb", "http://api.facebook.com/1.0/");
		prefixes.put("ff", "http://api.friendfeed.com/2008/03");
		prefixes.put("fn", "http://www.w3.org/2005/xpath-functions/#");
		prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		prefixes.put("freebase", "http://rdf.freebase.com/ns/");
		prefixes.put("g", "http://base.google.com/ns/1.0");
		prefixes.put("gb", "http://www.openlinksw.com/schemas/google-base#");
		prefixes.put("gd", "http://schemas.google.com/g/2005");
		prefixes.put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
		prefixes.put("geonames", "http://www.geonames.org/ontology#");
		prefixes.put("georss", "http://www.georss.org/georss");
		prefixes.put("gml", "http://www.opengis.net/gml");
		prefixes.put("go", "http://purl.org/obo/owl/GO#");
		prefixes.put("gr", "http://purl.org/goodrelations/v1#");
		prefixes.put("grs", "http://www.georss.org/georss/");
		prefixes.put("hlisting", "http://www.openlinksw.com/schemas/hlisting/");
		prefixes.put("hoovers", "http://wwww.hoovers.com/");
		prefixes.put("hrev", "http:/www.purl.org/stuff/hrev#");
		prefixes.put("ical", "http://www.w3.org/2002/12/cal/ical#");
		prefixes.put("ir", "http://web-semantics.org/ns/image-regions");
		prefixes.put("itunes", "http://www.itunes.com/DTDs/Podcast-1.0.dtd");
		prefixes.put("lgv", "http://linkedgeodata.org/ontology/");
		prefixes.put("link", "http://www.xbrl.org/2003/linkbase");
		prefixes.put("lod", "http://lod.openlinksw.com/");
		prefixes.put("math", "http://www.w3.org/2000/10/swap/math#");
		prefixes.put("media", "http://search.yahoo.com/mrss/");
		prefixes.put("mesh", "http://purl.org/commons/record/mesh/");
		prefixes.put("meta", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0");
		prefixes.put("mf",
				"http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#");
		prefixes.put("mmd", "http://musicbrainz.org/ns/mmd-1.0#");
		prefixes.put("mo", "http://purl.org/ontology/mo/");
		prefixes.put("mql", "http://www.freebase.com/");
		prefixes.put("nci",
				"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#");
		prefixes.put("nfo", "http://www.semanticdesktop.org/ontologies/nfo/#");
		prefixes.put("ng", "http://www.openlinksw.com/schemas/ning#");
		prefixes.put("nyt", "http://www.nytimes.com/");
		prefixes.put("oai", "http://www.openarchives.org/OAI/2.0/");
		prefixes.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
		prefixes.put("obo", "http://www.geneontology.org/formats/oboInOwl#");
		prefixes.put("office",
				"urn:oasis:names:tc:opendocument:xmlns:office:1.0");
		prefixes.put("oo", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0:");
		prefixes.put("openSearch", "http://a9.com/-/spec/opensearchrss/1.0/");
		prefixes.put("opencyc", "http://sw.opencyc.org/2008/06/10/concept/");
		prefixes.put("opl", "http://www.openlinksw.com/schema/attribution#");
		prefixes.put("opl-gs",
				"http://www.openlinksw.com/schemas/getsatisfaction/");
		prefixes.put("opl-meetup", "http://www.openlinksw.com/schemas/meetup/");
		prefixes.put("opl-xbrl", "http://www.openlinksw.com/schemas/xbrl/");
		prefixes.put("oplweb", "http://www.openlinksw.com/schemas/oplweb#");
		prefixes.put("ore", "http://www.openarchives.org/ore/terms/");
		prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
		prefixes.put("product", "http://www.buy.com/rss/module/productV2/");
		prefixes.put("protseq", "http://purl.org/science/protein/bysequence/");
		prefixes.put("r", "http://backend.userland.com/rss2");
		prefixes.put("radio", "http://www.radiopop.co.uk/");
		prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.put("rdfa", "http://www.w3.org/ns/rdfa#");
		prefixes.put("rdfdf", "http://www.openlinksw.com/virtrdf-data-formats#");
		prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixes.put("rev", "http://purl.org/stuff/rev#");
		prefixes.put("review", "http:/www.purl.org/stuff/rev#");
		prefixes.put("rss", "http://purl.org/rss/1.0/");
		prefixes.put("sc", "http://purl.org/science/owl/sciencecommons/");
		prefixes.put("scovo", "http://purl.org/NET/scovo#");
		prefixes.put("sd", "http://www.w3.org/ns/sparql-service-description#");
		prefixes.put("sf", "urn:sobject.enterprise.soap.sforce.com");
		prefixes.put("sioc", "http://rdfs.org/sioc/ns#");
		prefixes.put("sioct", "http://rdfs.org/sioc/types#");
		prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
		prefixes.put("slash", "http://purl.org/rss/1.0/modules/slash/");
		//		prefixes.put("sql", "sql:");
		prefixes.put("stock",
				"http://xbrlontology.com/ontology/finance/stock_market#");
		prefixes.put("twfy", "http://www.openlinksw.com/schemas/twfy#");
		prefixes.put("umbel", "http://umbel.org/umbel#");
		prefixes.put("umbel-ac", "http://umbel.org/umbel/ac/");
		prefixes.put("umbel-sc", "http://umbel.org/umbel/sc/");
		prefixes.put("uniprot", "http://purl.uniprot.org/");
		prefixes.put("units", "http://dbpedia.org/units/");
		prefixes.put("usc",
				"http://www.rdfabout.com/rdf/schema/uscensus/details/100pct/");
		prefixes.put("v", "http://www.openlinksw.com/xsltext/");
		prefixes.put("vcard", "http://www.w3.org/2001/vcard-rdf/3.0#");
		prefixes.put("vcard2006", "http://www.w3.org/2006/vcard/ns#");
		prefixes.put("vi", "http://www.openlinksw.com/virtuoso/xslt/");
		prefixes.put("virt", "http://www.openlinksw.com/virtuoso/xslt");
		prefixes.put("virtcxml", "http://www.openlinksw.com/schemas/virtcxml#");
		prefixes.put("virtrdf", "http://www.openlinksw.com/schemas/virtrdf#");
		prefixes.put("void", "http://rdfs.org/ns/void#");
		prefixes.put("wb", "http://www.worldbank.org/");
		prefixes.put("wdrs", "http://www.w3.org/2007/05/powder-s#");
		prefixes.put("wf", "http://www.w3.org/2005/01/wf/flow#");
		prefixes.put("wfw", "http://wellformedweb.org/CommentAPI/");
		prefixes.put("wikicompany",
				"http://dbpedia.openlinksw.com/wikicompany/");
		prefixes.put("xf", "http://www.w3.org/2004/07/xpath-functions");
		prefixes.put("xfn", "http://gmpg.org/xfn/11#");
		prefixes.put("xhtml", "http://www.w3.org/1999/xhtml");
		prefixes.put("xhv", "http://www.w3.org/1999/xhtml/vocab#");
		prefixes.put("xi", "http://www.xbrl.org/2003/instance");
		prefixes.put("xml", "http://www.w3.org/XML/1998/namespace");
		prefixes.put("xn", "http://www.ning.com/atom/1.0");
		prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		prefixes.put("xsl10", "http://www.w3.org/XSL/Transform/1.0");
		prefixes.put("xsl1999", "http://www.w3.org/1999/XSL/Transform");
		prefixes.put("xslwd", "http://www.w3.org/TR/WD-xsl");
		prefixes.put("y", "urn:yahoo:maps");
		prefixes.put("yago", "http://dbpedia.org/class/yago/");
		prefixes.put("yago-res", "http://mpii.de/yago/resource/");
		prefixes.put("yt", "http://gdata.youtube.com/schemas/2007");
		prefixes.put("zem", "http://s.zemanta.com/ns#");

	}

}
