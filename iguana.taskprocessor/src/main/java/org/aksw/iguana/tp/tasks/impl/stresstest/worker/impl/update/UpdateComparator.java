package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update;

import java.io.File;
import java.util.Comparator;

/**
 * The Comparator to compare Iguana update files of the following kind <br/>
 * [0-9]+\.(added|removed)\.sparql
 * 
 * @author f.conrads
 *
 */
public class UpdateComparator implements Comparator<File> {

	private UpdateStrategy updateStrategy;
	private String[] fileName1;
	private String[] fileName2;
	private Integer number1;
	private Integer number2;

	/**
	 * Creates the UpdateComparator and sets the {@link UpdateStrategy} to use
	 * @param updateStrategy 
	 */
	public UpdateComparator(UpdateStrategy updateStrategy) {
		this.updateStrategy = updateStrategy;
	}
	
	/**
	 * Creates the UpdateComparator and sets the {@link UpdateStrategy} to use
	 * @param updateStrategy2 the UpdateStrategy as string representation
	 */
	public UpdateComparator(String updateStrategy2) {
		this.updateStrategy = UpdateStrategy.valueOf(updateStrategy2);
	}

	@Override
	public int compare(File arg0, File arg1) {
		if(updateStrategy.equals(UpdateStrategy.NONE)) {
			return 0;
		}
		
		//preparation for easy checking
		fileName1 = arg0.getName().split("\\.");
		fileName2 = arg1.getName().split("\\.");
		number1 = Integer.parseInt(fileName1[0]);
		number2 = Integer.parseInt(fileName2[0]);
		
		switch(updateStrategy) {
		case ALL_ADDING_FIRST:
			return allAddingFirst();
		case ALL_REMOVING_FIRST:
			return allRemovingFirst();
		case ADD_REMOVE:
			return addRemove();
		case REMOVE_ADD:
			return removeAdd();
		default:
			return 0;
		}
		
	}
	
	private int allAddingFirst() {
		//check if first file is add
		if(fileName1[1].equals("added")) {
			//check if  second file is also add
			if(fileName2[1].equals("added")) {
				//if so the number decides which will be 'smaller'
				return number1.compareTo(number2);
			}
			//otherwise the first file is 'smaller'
			return -1;
		}
		else if(fileName2[1].equals("added")) {
			//first file is remove, second file is add, second file is 'smaller'
			return 1;
		}
		//both are remove, let the number decide which is 'smaller'
		return number1.compareTo(number2);
	}
	
	private int allRemovingFirst() {
		//check if first file is add
		if(fileName1[1].equals("added")) {
			//check if  second file is also add
			if(fileName2[1].equals("added")) {
				//if so the number decides which will be 'smaller'
				return number1.compareTo(number2);
			}
			//otherwise the second file is 'smaller'
			return 1;
		}
		else if(fileName2[1].equals("added")) {
			//first file is remove, second file is add, first file is 'smaller'
			return -1;
		}
		//both are remove, let the number decide which is 'smaller'
		return number1.compareTo(number2);
	}
	
	private int addRemove() {
		//check if numbers are equal
		if(number1.equals(number2)) {
			//checks alphabetically (added smaller than removed)
			return fileName1[1].compareTo(fileName2[1]);
		}
		//different numbers, the numbers decide
		return number1.compareTo(number2);
	}
	
	private int removeAdd() {
		//check if numbers are equal
		if(number1.equals(number2)) {
			//checks alphabetically reversed (removed smaller than added)
			return fileName2[1].compareTo(fileName1[1]);
		}
		//different numbers, the numbers decide
		return number1.compareTo(number2);
	}
	
}
