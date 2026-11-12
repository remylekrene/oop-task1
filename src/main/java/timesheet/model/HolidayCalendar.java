package timesheet.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;


public class HolidayCalendar {

    private final Set<LocalDate> holidays;


    public HolidayCalendar(Collection<LocalDate> holidays) {
        this.holidays = new TreeSet<>(holidays);
    }


    public void addHoliday(LocalDate date) {
        holidays.add(date);
    }


    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }


    public boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !isHoliday(date);
    }


    public int workingDays(YearMonth month) {
        int count = 0;
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            if (isWorkingDay(month.atDay(d))) {
                count++;
            }
        }
        return count;
    }


    public Set<LocalDate> getHolidays() {
        return Set.copyOf(holidays);
    }
}
