package org.aksw.iguana.benchmark.processor;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.Config;
import org.aksw.iguana.utils.EmailHandler;
import org.aksw.iguana.utils.ZipUtils;
import org.apache.commons.mail.EmailException;
import org.bio_gene.wookie.utils.LogHandler;

/**
 * Processor to send Emails with attached results
 * 
 * @author Felix Conrads
 *
 */
public class EmailProcessor {

	private static final String SYS_EXIT_MSG = "Sys.exit";
	private static Logger log = Logger.getLogger(EmailProcessor.class
			.getSimpleName());

	/** 
	 * Init Logger with file
	 */
	static {
		LogHandler
				.initLogFileHandler(log, EmailProcessor.class.getSimpleName());
	}

	/**
	 * If the Config has an emailConfiguration send an email to them
	 * 
	 * @param attach Should the results be attached
	 * @param folder The results folder which should be attached
	 * @param start start Time of the suite
	 * @param end End time of the suite
	 */
	public static void send(Boolean attach, String folder, Calendar start, Calendar end){
		//If there is no email config there is no chance to send an email
		if(Config.emailConfiguration == null){
			return;
		}
		try {
			String attachment = null;
			if(attach){
				try {
					//try to attach the zip file
					attachment = ZipUtils.folderToZip(
							folder, 
							folder+".zip");
				}
				catch(IOException e1){
				}
			}
			//Send an Good Email
			EmailHandler.sendGoodMail(start, end,attachment);
		} catch (EmailException e) {
			log.warning("Couldn't send email due to: ");
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
	}
	
	/**
	 * Send an Email with an Excepetion if the Config has an emailConfiguration spec.
	 * 
	 * @param attach Should the results be attached
	 * @param folder The results folder which should be attached
	 * @param start start Time of the suite
	 * @param end End time of the suite
	 * @param e The Exception which occured
	 */
	public static void sendWithException(Boolean attach, String folder, Calendar start, Calendar end, Exception e){
		//If there is no email config there is no chance to send an email
		if(Config.emailConfiguration == null){
			return;
		}
		try {
			String attachment = null;
			if(attach){
				try {
					//try to attach the result folder as a zip file
					attachment = ZipUtils.folderToZip(
							folder, 
							folder+".zip");
				}
				catch(IOException e1){
				}
			}
			//Send a Bad Email
			EmailHandler.sendBadMail(start, end, e, attachment);
		} catch (EmailException e1) {
			log.warning("Couldn't send email due to: ");
			LogHandler.writeStackTrace(log, e1, Level.WARNING);
		}
	}
	

	/**
	 * Sends an Email if the Benchmark aborted and if the config has an email Configuration
	 * 
	 * @param attach Should the results be attached
	 * @param folder Folder in which the results are
	 */
	public static void sendAborted(Boolean attach, String folder) {
		//If there is no email config there is no chance to send an email
		if(Config.emailConfiguration == null){
			return;
		}
		try {
			String attachment = null;
			if (attach) {
				try {
					//try to attach the results as a zip file
					attachment = ZipUtils.folderToZip(folder, folder + ".zip");

				} catch (IOException e1) {
				}
			}
			//Send Bad news with a sys.exit message
			EmailHandler.sendBadNews(SYS_EXIT_MSG, attachment);
		} catch (Exception e) {
			log.warning("Couldn't send email due to: ");
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
	}

}
