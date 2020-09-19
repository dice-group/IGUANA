/**
 * 
 */
package org.aksw.iguana.commons.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.commons.exec.ExecuteException;

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

		String[] shellCommand = new String[1 + (args == null ? 0 : args.length)];
		shellCommand[0] = fileName;

		if(args != null)
		{
			System.arraycopy(args, 0, shellCommand, 1, args.length);
		}

		return execute(shellCommand);
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

			int character;
			while ((character = reader.read()) != -1)
			{
				System.out.print((char)character);
			}

			exitVal = process.waitFor();

		} catch (IOException | InterruptedException e) {
			exitVal = -1;
		}

		return exitVal;
	}

}
