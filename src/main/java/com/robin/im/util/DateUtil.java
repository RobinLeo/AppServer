package com.robin.im.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Date Utility Class used to convert Strings to Dates and Timestamps
 *
 * @author <a href="mailto:bob@hodanet.com">Bob Huang</a>
 *  to correct time pattern. Minutes should be mm not MM (MM is month).
 */
public class DateUtil {
	private static Log log = LogFactory.getLog(DateUtil.class);
	private static final String TIME_PATTERN = "HH:mm";
	private static long MSEC_EVERYDAY = 86400000L; // 一天的微秒数
	private static int rawOffset = TimeZone.getDefault().getRawOffset();
	private static SimpleDateFormat oraDf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static long cacheNow = System.currentTimeMillis();

	/**
	 * Checkstyle rule: utility classes should not have public constructor
	 */
	private DateUtil() {
	}

	public static void main(String[] args) {
        System.out.println(new Date().getTime());
    }

	/**
	 * Return default datePattern (yyyy-MM-dd)
	 * @return a string representing the date pattern on the UI
	 */
	public static String getDatePattern() {
		return "yyyy-MM-dd";
	}

	public static String getDateTimePattern() {
		return DateUtil.getDatePattern() + " HH:mm:ss.S";
	}

	/**
	 * This method attempts to convert an Oracle-formatted date
	 * in the form dd-MMM-yyyy to yyyy-mm-dd.
	 *
	 * @param aDate date from database as a string
	 * @return formatted string for the ui
	 */
	public static String getDate(Date aDate) {
		SimpleDateFormat df;
		String returnValue = "";

		if (aDate != null) {
			df = new SimpleDateFormat(getDatePattern());
			returnValue = df.format(aDate);
		}

		return (returnValue);
	}

	public static void freshCacheNow(){
		cacheNow =System.currentTimeMillis();
	}

	public static long getCacheNow(){
		return cacheNow;
	}

	/**
	 * This method attempts to convert an Oracle-formatted date
	 * in the form dd-MMM-yyyy to yyyy-mm-dd HH24:mm:ss.
	 *
	 * @param aDate date from database as a string
	 * @return formatted string for the ui
	 */
	public static String getOraDateStr(Date aDate) {
		SimpleDateFormat df;
		String returnValue = "1900-01-01 00:00:00";

		if (aDate != null) {
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			returnValue = df.format(aDate);
		}
		return (returnValue);
	}

	/**
	 * This method attempts to convert an Oracle-formatted date
	 * in the form dd-MMM-yyyy to yyyy-mm-dd HH24:mm:ss.
	 *
	 * @param aDate date from database as a string
	 * @return formatted string for the ui
	 */
	public static String getOraNowStr() {
		return oraDf.format(new Date());
	}

	public static String getOraCacheNowStr() {
		return oraDf.format(cacheNow);
	}
	/**
	 * This method generates a string representation of a date/time
	 * in the format you specify on input
	 *
	 * @param aMask the date pattern the string is in
	 * @param strDate a string representation of a date
	 * @return a converted Date object
	 * @see java.text.SimpleDateFormat
	 * @throws java.text.ParseException when String doesn't match the expected format
	 */
	public static Date convertStringToDate(String aMask, String strDate)
			throws ParseException {
		SimpleDateFormat df;
		Date date;
		df = new SimpleDateFormat(aMask);

		if (log.isDebugEnabled()) {
			log.debug("converting '" + strDate + "' to date with mask '"
					+ aMask + "'");
		}

		try {
			date = df.parse(strDate);
		} catch (ParseException pe) {
			//log.error("ParseException: " + pe);
			throw new ParseException(pe.getMessage(), pe.getErrorOffset());
		}

		return (date);
	}

	/**
	 * This method returns the current date time in the format:
	 * yyyy/MM/dd HH:MM a
	 *
	 * @param theTime the current time
	 * @return the current date/time
	 */
	public static String getTimeNow(Date theTime) {
		return getDateTime(TIME_PATTERN, theTime);
	}

	/**
	 * This method returns the current date in the format: yyyy-MM-dd
	 *
	 * @return the current date
	 * @throws java.text.ParseException when String doesn't match the expected format
	 */
	public static Calendar getToday()  {
		Date today = new Date();
		SimpleDateFormat df = new SimpleDateFormat(getDatePattern());

		// This seems like quite a hack (date -> string -> date),
		// but it works ;-)
		String todayAsString = df.format(today);
		Calendar cal = new GregorianCalendar();
		try {
			cal.setTime(convertStringToDate(todayAsString));
		} catch (Exception e) {
			// TODO: handle exception
		}
		return cal;
	}




	/**
	 * This method generates a string representation of a date's date/time
	 * in the format you specify on input
	 *
	 * @param aMask the date pattern the string is in
	 * @param aDate a date object
	 * @return a formatted string representation of the date
	 *
	 * @see java.text.SimpleDateFormat
	 */
	public static String getDateTime(String aMask, Date aDate) {
		SimpleDateFormat df = null;
		String returnValue = "";

		if (aDate == null) {
			log.error("aDate is null!");
		} else {
			df = new SimpleDateFormat(aMask);
			returnValue = df.format(aDate);
		}

		return (returnValue);
	}

	/**
	 * This method generates a string representation of a date based
	 * on the System Property 'dateFormat'
	 * in the format you specify on input
	 *
	 * @param aDate A date to convert
	 * @return a string representation of the date
	 */
	public static String convertDateToString(Date aDate) {
		return getDateTime(getDatePattern(), aDate);
	}

	/**
	 * This method converts a String to a date using the datePattern
	 *
	 * @param strDate the date to convert (in format yyyy-MM-dd)
	 * @return a date object
	 * @throws java.text.ParseException when String doesn't match the expected format
	 */
	public static Date convertStringToDate(String strDate)
			throws ParseException {
		Date aDate = null;

		try {
			if (log.isDebugEnabled()) {
				log.debug("converting date with pattern: " + getDatePattern());
			}

			aDate = convertStringToDate(getDatePattern(), strDate);
		} catch (ParseException pe) {
			log.error("Could not convert '" + strDate
					+ "' to a date, throwing exception");
			pe.printStackTrace();
			throw new ParseException(pe.getMessage(), pe.getErrorOffset());
		}

		return aDate;
	}

	/**
	 * 时间计算方法
	 *
	 * @param date java.util.Date类型的计算开始时间
	 * @param amount 要增加或减少的单位时间
	 * @param field   时间单位,可以是
	 * java.util.Calendar.DAY_OF_MONTH 日
	 * java.util.Calendar.MONTH 月
	 * java.util.Calendar.YEAR 年
	 * java.util.Calendar.SECOND 秒
	 * java.util.Calendar.MINUTE 分
	 * java.util.Calendar.HOUR_OF_DAY 时
	 * @return a date object
	 */
	public static Date add(Date date, int amount, int field) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(field, amount);
		return c.getTime();
	}



//	public static int getDaysAfter(Date aDate) {
//		int returnValue = 0;
//		if (aDate != null) {
//			Date today = new Date();
//			SimpleDateFormat df = new SimpleDateFormat(getDatePattern());
//			try {
//				long old = df.parse(df.format(aDate)).getTime();
//				long now = df.parse(df.format(today)).getTime();
//				returnValue = (int)((now-old)/MSEC_EVERYDAY);
//			} catch (Exception e) {
//				// TODO: handle exception
//			}
//		}
//		return (returnValue);
//	}

	public static int getDaysAfter(Date aDate) {
		if (null == aDate){
			return 0;
		}
		int today = (int) ((System.currentTimeMillis() + rawOffset) / MSEC_EVERYDAY);
		int thatDay = (int) ((aDate.getTime() + rawOffset) / MSEC_EVERYDAY);
		return (today-thatDay);
	}

	/**
     * 将日期转换为1970-01-01后的天数
     *
     * @param Date
     *            theDate 要计算天数的日期
     * @return int 所传入日期与1970-01-01相差的天数
     */
    public static int dateToDay(Date theDate) {
		if (null == theDate){
			return 0;
		}
    	return (int) ((theDate.getTime() + rawOffset) / MSEC_EVERYDAY);
    }

    /**
     * 将1970-01-01后的天数转换为日期
     *
     * @param int
     *            要取的日期与1970-01-01相差的天数
     * @return Date theDate 与1970-01-01相差相应天数的日期
     */
    public static Date dayToDate(int day) {
        return new Date(day * MSEC_EVERYDAY);
    }

    /**
     * 取今天与1970-01-01相差的天数
     *
     * @return int 取今天与1970-01-01相差的天数
     */
    public static int toDay() {
        return (int) ((System.currentTimeMillis() + rawOffset) / MSEC_EVERYDAY);
    }

    public static int get24Hour(){
    	int hour = 0;
    	Calendar cal = Calendar.getInstance();
    	hour = cal.get(Calendar.HOUR_OF_DAY);
    	return hour;
    }
}
