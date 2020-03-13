package org.kin.scheduler.admin.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public class DateUtils {
    private static Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        DATETIME_FORMAT.setLenient(false);
    }

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        return DATETIME_FORMAT.format(date);
    }

    public static String format(Date date, String patten) {
        DateFormat dateFormat = new SimpleDateFormat(patten);
        return dateFormat.format(date);
    }

    public static Date parseDate(String dateString) {
        return parse(dateString, DATE_FORMAT);
    }

    public static Date parseDateTime(String dateString) {
        return parse(dateString, DATETIME_FORMAT);
    }


    public static Date parse(String dateString, DateFormat dateFormat) {
        try {
            Date date = dateFormat.parse(dateString);
            return date;
        } catch (Exception e) {
            logger.warn("parse date error, dateString = {}, dateFormat={}; errorMsg = ", dateString, dateFormat, e.getMessage());
            return null;
        }
    }

    // ---------------------- add date ----------------------
    public static Date addDays(Date date, int amount) {
        return add(date, Calendar.DAY_OF_MONTH, amount);
    }

    public static Date addYears(Date date, int amount) {
        return add(date, Calendar.YEAR, amount);
    }

    public static Date addMonths(Date date, int amount) {
        return add(date, Calendar.MONTH, amount);
    }

    private static Date add(Date date, int calendarField, int amount) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }
}
