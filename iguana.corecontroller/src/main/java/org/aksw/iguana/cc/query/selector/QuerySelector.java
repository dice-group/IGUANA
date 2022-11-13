package org.aksw.iguana.cc.query.selector;

/**
 * The QuerySelector that is used to get the next query index.
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public interface QuerySelector {
    /**
     * This method gives the next query index that should be used.
     *
     * @return the next query index
     */
    int getNextIndex();
}
