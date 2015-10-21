package de.uni_leipzig.iguana.utils;

import java.util.Calendar;

public class CalendarHandler {

	
	public static String getFormattedTime(Calendar start){
		return start.get(Calendar.HOUR_OF_DAY)
				+":"+start.get(Calendar.MINUTE)
				+":"+start.get(Calendar.MILLISECOND)
				+" "+start.get(Calendar.DAY_OF_MONTH)
				+" "+start.get(Calendar.MONTH)
				+" "+start.get(Calendar.YEAR);
	}
	
	public static String getWellFormatDateDiff(Calendar start2, Calendar end2){
		long diff2 = end2.getTimeInMillis()-start2.getTimeInMillis();
		Calendar start = Calendar.getInstance();
		Calendar end = Calendar.getInstance();
		start.setTimeInMillis(0);
		end.setTimeInMillis(diff2);
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
}
