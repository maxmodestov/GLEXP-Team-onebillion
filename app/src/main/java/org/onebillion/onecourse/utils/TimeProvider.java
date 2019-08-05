package org.onebillion.onecourse.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.onebillion.onecourse.mainui.MainActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class TimeProvider {
    public static String PREFERENCES_CURRENT_TIMESTAMP = "currentTimestamp";
    public static String PREFERENCES_SYSTEM_TIME_ON_SETTING_TIMESTAMP = "systemTimeOnSettingTimestamp";

    static private long currentTimestamp;
    static private long systemTimeOnSettingTimestamp;
    static private boolean initialized = false;

    public static long currentTimeMillis() {
        initialize();
        long delta = systemTimeOnSettingTimestamp - System.currentTimeMillis();
        return currentTimestamp + delta;
    }

    public static void setTime(long timestamp) {
        initialized = true;
        currentTimestamp = timestamp;
        systemTimeOnSettingTimestamp = System.currentTimeMillis();
//        ((AlarmManager) MainActivity.mainActivity.getSystemService(Context.ALARM_SERVICE)).setTime(when);
    }

    public static void setDay(int dayNumber) {
        Log.d("TimeProvider", "setDay: " + dayNumber);
        long timestamp = getTimestampOfDay(dayNumber);
        Save(timestamp);
        setTime(timestamp);
    }

    public static long getTimestampOfDay(int dayNumber) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2017, Calendar.JANUARY, 1, 6, 0);
        calendar.add(Calendar.DATE, dayNumber);
        return calendar.getTimeInMillis();
    }

    public static int getCurrentDayNumber() {
        return getDayNumber(currentTimestamp);
    }

    public static int getDayNumber(long currentTimestamp) {
        long deltaTimestamp = currentTimestamp - getTimestampOfDay(0);
        long dayNumber = TimeUnit.DAYS.convert(deltaTimestamp, TimeUnit.MILLISECONDS);
        Log.d("TimeProvider", "getDayNumber: " + dayNumber);
        return (int)dayNumber;
    }

    private static void initialize() {
        if (initialized) return;

        setTime(getInitialTime());
    }

    private static long getInitialTime() {
        return Load();
//        return getTimestampOfDay(0);
    }

    private static long Load() {
        SharedPreferences sharedPref = MainActivity.mainActivity.getPreferences(Context.MODE_PRIVATE);
        long timestamp = sharedPref.getLong(PREFERENCES_CURRENT_TIMESTAMP, getTimestampOfDay(0));

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeInMillis(timestamp);
        Log.d("TimeProvider", "Load: Date " + calendar.toString());

        Log.d("TimeProvider", "Load: Day Number " + getDayNumber(timestamp));

        return timestamp;
    }

    private static void Save(long value) {
        SharedPreferences sharedPref = MainActivity.mainActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(PREFERENCES_CURRENT_TIMESTAMP, value);
        editor.apply();
    }
}
