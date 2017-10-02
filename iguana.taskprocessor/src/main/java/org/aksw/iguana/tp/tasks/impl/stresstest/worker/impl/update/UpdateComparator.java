package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update;

import java.io.File;
import java.util.Comparator;

public class UpdateComparator implements Comparator<File> {

	private UpdateStrategy updateStrategy;

	public UpdateComparator(UpdateStrategy updateStrategy) {
		this.updateStrategy = updateStrategy;
	}
	
	@Override
	public int compare(File arg0, File arg1) {
		//preparation for easy checking
		String[] fileName1 = arg0.getName().split("\\.");
		String[] fileName2 = arg1.getName().split("\\.");
		Integer number1 = Integer.parseInt(fileName1[0]);
		Integer number2 = Integer.parseInt(fileName2[0]);
		
		switch(updateStrategy) {
		case ALL_ADDING_FIRST:
			if(fileName1[1].equals("added")) {
				if(fileName2[1].equals("added")) {
					return number1.compareTo(number2);
				}
				return -1;
			}
			else if(fileName2[1].equals("added")) {
				return 1;
			}
			return number1.compareTo(number2);
		case ALL_REMOVING_FIRST:
			if(fileName1[1].equals("added")) {
				if(fileName2[1].equals("added")) {
					return number2.compareTo(number1);
				}
				return 1;
			}
			else if(fileName2[1].equals("added")) {
				return -1;
			}
			return number2.compareTo(number1);
		case ADD_REMOVE:
			//TODO
			return 0;

		case REMOVE_ADD:
			//TODO
			return 0;
		default:
			return 0;
		}
		
	}
	
}
