package org.aksw.iguana.cc.query.selector;

public interface QuerySelector {
    /**
     * This method gives the next query index that should be used.
     *
     * @return the next query index
     */
    int getNextIndex();
}
