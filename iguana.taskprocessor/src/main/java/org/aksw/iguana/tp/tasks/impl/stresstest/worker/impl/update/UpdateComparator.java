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
		String[] fileName1 = arg0.getName().split("\\.");
		String[] fileName2 = arg1.getName().split("\\.");
		Integer number1 = Integer.parseInt(fileName1[0]);
		Integer number2 = Integer.parseInt(fileName2[0]);
		
		switch(updateStrategy) {
		case ALL_ADDING_FIRST:
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
		case ALL_REMOVING_FIRST:
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
		case ADD_REMOVE:
			//check if numbers are equal
			if(number1.equals(number2)) {
				//checks alphabetically (added smaller than removed)
				return fileName1[1].compareTo(fileName2[1]);
			}
			//different numbers, the numbers decide
			return number1.compareTo(number2);

		case REMOVE_ADD:
			//check if numbers are equal
			if(number1 == number2) {
				//checks alphabetically reversed (removed smaller than added)
				return fileName2[1].compareTo(fileName1[1]);
			}
			//different numbers, the numbers decide
			return number1.compareTo(number2);
		default:
			return 0;
		}
		
	}
	
}
