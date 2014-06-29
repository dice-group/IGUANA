package de.uni_leipzig.mosquito.utils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class EmailHandler {
	


	private static Email email;
	private static String aHostName;
	private static int port;
	private static DefaultAuthenticator da;
	private static String emailFrom;
	private static List<String> emailTo;
	
	public static void initEmail(String aHostName, int port, String user, String pwd, String emailFrom, List<String> emailTo) throws EmailException{
		EmailHandler.aHostName = aHostName;
		EmailHandler.port = port;
		EmailHandler.da = new DefaultAuthenticator(user, pwd);
		EmailHandler.emailFrom = emailFrom;
		EmailHandler.emailTo = emailTo;
		
	}
	
	private static void sendNews(String subject, String msg) throws EmailException{
		email = new SimpleEmail();
		email.setHostName(aHostName);
		email.setSmtpPort(port);
		email.setAuthenticator(da);
		email.setSSLOnConnect(true);
		email.setFrom(emailFrom);
		for(String em : emailTo){
			email.addTo(em);
		}
		email.setSubject(subject);
		email.setMsg(msg);
		email.send();
	}
	
	public static void sendGoodNews(String msg) throws EmailException{
		String news="Benchmark finished!";
		String msg2 = news+"\nHOOORAYYY\n"+msg;
		sendNews(news, msg2);
	}
	
	public static void sendBadNews(String msg) throws EmailException{
		String news="Benchmark doesn't feel good!";
		String msg2 = news+"\nTo find out why the Benchmark ended unexpected see the log Files\n\n"+msg;
		sendNews(news, msg2);
	}
	
	public static void sendGoodMail(Calendar start, Calendar end)
			throws EmailException {
		List<String> to = new LinkedList<String>();
		to.add("");
		String msg = "\n";
		msg += "Start: " + DateFormat.getInstance().format(start.getTime());
		msg += "\nEnd: " + DateFormat.getInstance().format(end.getTime());
		msg += "\n" + "Finished in: ";
		msg += getWellFormatDateDiff(start, end);
		EmailHandler.sendGoodNews(msg);
	}

	public static String getWellFormatDateDiff(Calendar start, Calendar end){
		int diff = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
		String msg = diff == 0 ? "" : String.valueOf(diff) + "y ";
		diff = end.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR);
		msg += diff == 0 ? "" : String.valueOf(diff) + "d ";
		diff = end.get(Calendar.HOUR_OF_DAY) - start.get(Calendar.HOUR_OF_DAY);
		msg += diff == 0 ? "" : String.valueOf(diff) + "h ";
		diff = end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE);
		msg += diff == 0 ? "" : String.valueOf(diff) + "m ";
		diff = end.get(Calendar.SECOND) - start.get(Calendar.SECOND);
		msg += diff == 0 ? "" : String.valueOf(diff) + "s ";
		diff = end.get(Calendar.MILLISECOND) - start.get(Calendar.MILLISECOND);
		msg += diff == 0 ? "" : String.valueOf(diff) + "ms";
		return msg;
	}
	
	public static void sendBadMail(Calendar start, Calendar end, Exception e)
			throws EmailException {
		String msg = "Problem at: ";
		msg += DateFormat.getInstance().format(new Date()) + "\n";
		msg += ExceptionUtils.getStackTrace(e);
		EmailHandler.sendBadNews(msg);
	}
}
