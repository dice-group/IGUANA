/**
 * 
 */
package org.aksw.iguana.dp.loader.impl;

import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.dp.loader.AbstractLoader;

/**
 * Will load with a provided script: </br>
 * user@host:~$ ./script datasetID connectionID
 * 
 * @author f.conrads
 *
 */
public class ScriptLoader extends AbstractLoader {

	private String script;

	/**
	 * 
	 */
	public ScriptLoader(String scriptFile) {
		this.script = scriptFile;
	}

	@Override
	public void upload(byte[] data) throws Exception {
		//set DatasetID, connectionID
		String[] args = new String[2];
		args[0] = datasetID;
		args[1] = connID;
		//exec script
		ScriptExecutor.exec(script, args);
	}
	
}
