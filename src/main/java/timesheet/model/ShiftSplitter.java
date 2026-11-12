package timesheet.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class ShiftSplitter {


    public static final LocalTime NIGHT_START = LocalTime.of(22, 0);

    public static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    private ShiftSplitter() {

    }


    public static List<TimeSegment> split(Shift shift, HolidayCalendar calendar, Duration dailyNorm) {
        ZoneId zone = shift.zone();
        ZonedDateTime overtimeStart = shift.start().plus(dailyNorm);


        List<ZonedDateTime> points = new ArrayList<>();
        points.add(shift.start());
        points.add(shift.end());
        addIfInside(points, overtimeStart, shift);
        LocalDate day = shift.start().toLocalDate();
        while (!day.isAfter(shift.end().toLocalDate())) {
            addIfInside(points, day.atStartOfDay(zone), shift);
            addIfInside(points, day.atTime(NIGHT_END).atZone(zone), shift);
            addIfInside(points, day.atTime(NIGHT_START).atZone(zone), shift);
            day = day.plusDays(1);
        }
        points.sort(Comparator.comparing(ZonedDateTime::toInstant));

        List<TimeSegment> result = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i++) {
            ZonedDateTime from = points.get(i);
            ZonedDateTime to = points.get(i + 1);
            if (!to.isAfter(from)) {
                continue;
            }
            TimeType type = typeAt(from, overtimeStart, calendar);

            if (!result.isEmpty() && result.get(result.size() - 1).type() == type) {
                TimeSegment last = result.remove(result.size() - 1);
                result.add(new TimeSegment(last.start(), to, type));
            } else {
                result.add(new TimeSegment(from, to, type));
            }
        }
        return result;
    }


    public static boolean isNight(LocalTime time) {
        return !time.isBefore(NIGHT_START) || time.isBefore(NIGHT_END);
    }

    private static TimeType typeAt(ZonedDateTime moment, ZonedDateTime overtimeStart, HolidayCalendar calendar) {
        if (calendar.isHoliday(moment.toLocalDate())) {
            return TimeType.HOLIDAY;
        }
        if (!moment.isBefore(overtimeStart)) {
            return TimeType.OVERTIME;
        }
        if (isNight(moment.toLocalTime())) {
            return TimeType.NIGHT;
        }
        return TimeType.REGULAR;
    }

    private static void addIfInside(List<ZonedDateTime> points, ZonedDateTime p, Shift shift) {
        if (p.isAfter(shift.start()) && p.isBefore(shift.end())) {
            points.add(p);
        }
    }
}
