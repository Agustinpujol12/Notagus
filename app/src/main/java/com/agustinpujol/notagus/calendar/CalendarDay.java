package com.agustinpujol.notagus.calendar;

public class CalendarDay {
    public int year;
    public int month; // 0-11
    public int dayOfMonth; // 1-31
    public boolean inCurrentMonth;
    public boolean isToday;
    public boolean hasTasks;

    public CalendarDay(int y, int m, int d, boolean inMonth, boolean today, boolean hasTasks) {
        year = y; month = m; dayOfMonth = d;
        inCurrentMonth = inMonth; isToday = today; hasTasks = hasTasks;
    }
}
