/**
 * 
 */
package org.aksw.iguana.commons.script;

import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class to execute Shell Scripts
 * 
 * @author f.conrads
 *
 */
public class ScriptExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptExecutor.class);

	/**
	 * Will execute the given file with the provided arguments
	 * via Shell.
	 * 
	 * @param file file to execute
	 * @param args arguments to execute file with
	 * @throws ExecuteException if script can't be executed
	 * @throws IOException if file IO errors
	 * @return Process return, 0 means everything worked fine
	 */
	public static int exec(String file, String[] args) throws ExecuteException, IOException{
		String fileName = new File(file).getAbsolutePath();

		String[] shellCommand = new String[1 + (args == null ? 0 : args.length)];
		shellCommand[0] = fileName;

		if(args != null)
		{
			System.arraycopy(args, 0, shellCommand, 1, args.length);
		}

		return execute(shellCommand);
	}

	/**Checks if file contains arguments itself
	 *
	 * @param file file to execute
	 * @param args arguments to execute file with
	 * @return Process return, 0 means everything worked fine
	 * @throws ExecuteException if script can't be executed
	 * @throws IOException if file IO errors
	 */
	public static int execSafe(String file, String[] args) throws ExecuteException, IOException{
		String actualScript = file;
		String[] args2 = args;
		if(file.contains(" ")){

			String[] providedArguments = file.split("\\s+");
			args2 = new String[providedArguments.length-1+args.length];
			actualScript=providedArguments[0];
			int i=1;
			for(i=1;i<providedArguments.length;i++){
				args2[i-1]=providedArguments[i];
			}
			for(int j=0;j<args.length;j++){
				args2[j+i-1]=args[j];
			}

		}

		return exec(actualScript, args2);
	}

	private static int execute(String[] args)
	{
		ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);
		processBuilder.command(args);
		int exitVal;
		try {
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			StringBuilder out = new StringBuilder();
			int character;
			while (process.isAlive()&&(character = reader.read()) != -1)
			{

				out.append((char)character);
			}
			LOGGER.debug(out.toString());

			exitVal = process.waitFor();

		} catch (IOException | InterruptedException e) {
			LOGGER.error("Script had thrown an error. ", e);
			exitVal = -1;

		}

		return exitVal;
	}

}
