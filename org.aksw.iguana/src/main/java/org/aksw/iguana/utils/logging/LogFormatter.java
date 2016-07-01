package org.aksw.iguana.utils.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Schönerer Formatter für Log Files
 * Format:
 * "[$LEVEL $DATE] $MESSAGE" 
 * 
 * @author Felix Conrads
 *
 */
public class LogFormatter extends Formatter {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("[").append(record.getLevel().getName()).append("  ").append(new Date().toString())
				.append("] ")
				.append(formatMessage(record))
				.append(LINE_SEPARATOR);

		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
				// ignore
			}
		}

		return sb.toString();
	}

}

