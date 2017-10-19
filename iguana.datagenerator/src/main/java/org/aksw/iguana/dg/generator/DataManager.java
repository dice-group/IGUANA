/**
 * 
 */
package org.aksw.iguana.dg.generator;

import org.aksw.iguana.dp.loader.LoaderManager;

/**
 * Will manage a {@link DataGenerator}
 * 
 * @author f.conrads
 *
 */
public class DataManager {

	private DataGenerator generator;


	/**
	 * Will call the inner {@link DataGenerator} {@link DataGenerator#generateData} method
	 * 
	 * @throws Exception
	 */
	public void generate() throws Exception {
		this.generator.generateData();
	}

	/**
	 * Will set the {@link DataGenerator} to use
	 * 
	 * @param generator
	 */
	public void setDataGenerator(DataGenerator generator) {
		this.generator = generator;
	}

	/**
	 * Will set the {@link LoaderManager} of the inner {@link DataGenerator}
	 * @param lmanager
	 */
	public void setLoaderManager(LoaderManager lmanager) {
		this.generator.setLoaderManager(lmanager);
	}


}
