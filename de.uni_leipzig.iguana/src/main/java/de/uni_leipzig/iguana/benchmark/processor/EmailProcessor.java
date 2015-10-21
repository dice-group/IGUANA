package de.uni_leipzig.iguana.benchmark.processor;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.mail.EmailException;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.utils.Config;
import de.uni_leipzig.iguana.utils.EmailHandler;
import de.uni_leipzig.iguana.utils.ZipUtils;

public class EmailProcessor {

	private static final String SYS_EXIT_MSG = "Sys.exit";
	private static Logger log = Logger.getLogger(EmailProcessor.class
			.getSimpleName());

	static {
		LogHandler
				.initLogFileHandler(log, EmailProcessor.class.getSimpleName());
	}

	public static void send(Boolean attach, String folder, Calendar start, Calendar end){
		if(Config.emailConfiguration == null){
			return;
		}
		try {
			String attachment = null;
			if(attach){
				try {
					attachment = ZipUtils.folderToZip(
							folder, 
							folder+".zip");
				}
				catch(IOException e1){
				}
			}
			EmailHandler.sendGoodMail(start, end,attachment);
		} catch (EmailException e) {
			log.warning("Couldn't send email due to: ");
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
	}
	
	public static void sendWithException(Boolean attach, String folder, Calendar start, Calendar end, Exception e){
		if(Config.emailConfiguration == null){
			return;
		}
		try {
			String attachment = null;
			if(attach){
				try {
					attachment = ZipUtils.folderToZip(
							folder, 
							folder+".zip");
				}
				catch(IOException e1){
				}
			}
			EmailHandler.sendBadMail(start, end, e, attachment);
		} catch (EmailException e1) {
			log.warning("Couldn't send email due to: ");
			LogHandler.writeStackTrace(log, e1, Level.WARNING);
		}
	}
	
	
	public static void sendAborted(Boolean attach, String folder) {
		if(Config.emailConfiguration == null){
			return;
		}
		try {
			String attachment = null;
			if (attach) {
				try {
					attachment = ZipUtils.folderToZip(folder, folder + ".zip");

				} catch (IOException e1) {
				}
			}
			EmailHandler.sendBadNews(SYS_EXIT_MSG, attachment);
		} catch (Exception e) {
			log.warning("Couldn't send email due to: ");
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
	}

}
