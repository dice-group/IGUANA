/**
 * 
 */
package org.aksw.iguana.commons.script;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

/**
 * Class to execute Shell Scripts
 * 
 * @author f.conrads
 *
 */
public class ScriptExecutor {

	/**
	 * Will execute the given file with the provided arguments
	 * via Shell.
	 * 
	 * @param file file to execute
	 * @param args arguments to execute file with
	 * @throws ExecuteException
	 * @throws IOException
	 * @return Process return, 0 means everything worked fine
	 */
	public static int exec(String file, String[] args) throws ExecuteException, IOException{
		String fileName = new File(file).getAbsolutePath();
		CommandLine cmd = new CommandLine(fileName);
		for(String arg: args){
			cmd.addArgument(arg);
		}
		
		Executor exec = new DefaultExecutor();
		return exec.execute(cmd);
	}
	

}
