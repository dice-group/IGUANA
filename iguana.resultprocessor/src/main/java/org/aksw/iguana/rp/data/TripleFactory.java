package org.aksw.iguana.rp.data;

public class TripleFactory {

    public Triple createTriple(String subject, String predicate, String object){
       return new Triple(subject, predicate, object);
    }

    public Triple createSuffixTriple(String subject, String predicate, String object){
        //TODO add namespaces
        return new Triple(subject, predicate, object);
    }

}
