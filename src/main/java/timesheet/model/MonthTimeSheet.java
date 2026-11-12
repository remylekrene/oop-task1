package timesheet.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MonthTimeSheet {

    private final Employee employee;
    private final YearMonth month;
    private final HolidayCalendar calendar;
    private final List<Shift> shifts = new ArrayList<>();


    public MonthTimeSheet(Employee employee, YearMonth month, HolidayCalendar calendar) {
        this.employee = Objects.requireNonNull(employee);
        this.month = Objects.requireNonNull(month);
        this.calendar = Objects.requireNonNull(calendar);
    }


    public void addShift(Shift shift) throws ShiftOverlapException, OvertimeLimitExceededException {
        Objects.requireNonNull(shift);
        if (!YearMonth.from(shift.start()).equals(month)) {
            throw new IllegalArgumentException("Смена " + shift.date() + " не из месяца " + month);
        }
        for (Shift existing : shifts) {
            if (existing.overlaps(shift)) {
                throw new ShiftOverlapException(shift, existing);
            }
        }
        Duration after = totalWorked().plus(shift.duration());
        Duration max = maxAllowed();
        if (after.compareTo(max) > 0) {
            throw new OvertimeLimitExceededException(employee, after, max);
        }
        shifts.add(shift);
        shifts.sort(Comparator.comparing(s -> s.start().toInstant()));
    }


    public boolean removeShift(Shift shift) {
        return shifts.remove(shift);
    }


    public List<Shift> getShifts() {
        return List.copyOf(shifts);
    }


    public Employee getEmployee() {
        return employee;
    }


    public YearMonth getMonth() {
        return month;
    }


    public Duration monthlyNorm() {
        return employee.getNorm().monthlyNorm(month, calendar);
    }


    public Duration maxAllowed() {
        return monthlyNorm().plus(employee.getNorm().monthlyOvertimeLimit());
    }


    public Duration totalWorked() {
        Duration sum = Duration.ZERO;
        for (Shift s : shifts) {
            sum = sum.plus(s.duration());
        }
        return sum;
    }


    public Duration workedBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Начало периода позже конца");
        }
        Duration sum = Duration.ZERO;
        for (Shift s : shifts) {
            ZonedDateTime periodStart = from.atStartOfDay(s.zone());
            ZonedDateTime periodEnd = to.plusDays(1).atStartOfDay(s.zone());
            ZonedDateTime a = s.start().isAfter(periodStart) ? s.start() : periodStart;
            ZonedDateTime b = s.end().isBefore(periodEnd) ? s.end() : periodEnd;
            if (b.isAfter(a)) {
                sum = sum.plus(Duration.between(a, b));
            }
        }
        return sum;
    }


    public List<TimeSegment> segments(Shift shift) {
        return ShiftSplitter.split(shift, calendar, employee.getNorm().dailyNorm());
    }


    public Map<TimeType, Duration> totalsByType() {
        Map<TimeType, Duration> result = new EnumMap<>(TimeType.class);
        for (TimeType t : TimeType.values()) {
            result.put(t, Duration.ZERO);
        }
        for (Shift s : shifts) {
            for (TimeSegment seg : segments(s)) {
                result.merge(seg.type(), seg.duration(), Duration::plus);
            }
        }
        return result;
    }


    public long payForShift(Shift shift) {
        long total = 0;
        for (Map.Entry<TimeType, Duration> e : byTypeInShift(shift).entrySet()) {
            total += e.getKey().pay(e.getValue(), employee.getHourlyRate());
        }
        return total;
    }


    public Map<TimeType, Long> payByType() {
        Map<TimeType, Long> result = new EnumMap<>(TimeType.class);
        for (TimeType t : TimeType.values()) {
            result.put(t, 0L);
        }
        for (Shift s : shifts) {
            for (Map.Entry<TimeType, Duration> e : byTypeInShift(s).entrySet()) {
                long pay = e.getKey().pay(e.getValue(), employee.getHourlyRate());
                result.merge(e.getKey(), pay, Long::sum);
            }
        }
        return result;
    }


    public long totalPay() {
        long sum = 0;
        for (Shift s : shifts) {
            sum += payForShift(s);
        }
        return sum;
    }

    private Map<TimeType, Duration> byTypeInShift(Shift shift) {
        Map<TimeType, Duration> map = new EnumMap<>(TimeType.class);
        for (TimeSegment seg : segments(shift)) {
            map.merge(seg.type(), seg.duration(), Duration::plus);
        }
        return map;
    }
}
