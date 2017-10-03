package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update;

/**
 * The possible update strategies which can be used to sort the Iguana update files <br/>
 * <br/>
 * <ul>
 * <li>NONE: no sortation</li>
 * <li>ALL_ADDING_FIRST: all files which should be added will be executed before the files which removes. The added (resp. removed) files will be ordered according their numbers.</li>
 * <li>ALL_REMOVING_FIRST: same as ALL_ADDING_FIRST but first all removing files.</li>
 * <li>ADD_REMOVE: will be sorted by the numbers first, and the add file before the remove file. f.e. 1.added.sparql before 1.removed.sparql but latter one before 2.added.sparql</li>
 * <li>REMOVE_ADD: same as ADD_REMOVE but the remove file before the add file</li>
 * </ul>
 * <br/>
 * Be aware that all update files are not allowed to be just normal rdf files, they must be valid sparql update queries
 * <br/><br/> 
 * If you want to simply use your files as you ordered, use NONE as the updatestrategy (then they must not even be in the specified format)
 * 
 * @author f.conrads
 *
 */
public enum UpdateStrategy {
	NONE, ALL_ADDING_FIRST, ALL_REMOVING_FIRST, ADD_REMOVE, REMOVE_ADD
}
