package timesheet.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimesheetTest {

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private HolidayCalendar calendar;
    private Employee worker;
    private MonthTimeSheet sheet;

    @BeforeEach
    void setUp() {
        calendar = new HolidayCalendar(List.of(LocalDate.of(2025, 3, 8)));
        worker = new Employee("T-1", "Тестов Тест", 600_00, new FullTimeNorm());
        sheet = new MonthTimeSheet(worker, YearMonth.of(2025, 3), calendar);
    }

    private static Shift msk(String from, String to) {
        return Shift.of(LocalDateTime.parse(from), LocalDateTime.parse(to), MSK);
    }


    @Test
    void employeesWithSameNumberAreEqual() {
        Employee a = new Employee("T-1", "Иванов", 100, new FullTimeNorm());
        Employee b = new Employee("T-1", "Иванов И.И.", 200, new PartTimeNorm(50));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        Set<Employee> set = new HashSet<>(List.of(a, b));
        assertEquals(1, set.size());
    }

    @Test
    void employeesWithDifferentNumbersAreNotEqual() {
        Employee a = new Employee("T-1", "Иванов", 100, new FullTimeNorm());
        Employee b = new Employee("T-2", "Иванов", 100, new FullTimeNorm());
        assertNotEquals(a, b);
        assertNotEquals(a, null);
        assertNotEquals(a, "T-1");
        assertEquals(a, a);
    }

    @Test
    void employeeValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Employee(" ", "Имя", 100, new FullTimeNorm()));
        assertThrows(IllegalArgumentException.class, () -> new Employee("T-1", "Имя", 0, new FullTimeNorm()));
    }


    @Test
    void shiftEndMustBeAfterStart() {
        assertThrows(IllegalArgumentException.class, () -> msk("2025-03-03T10:00", "2025-03-03T10:00"));
        assertThrows(IllegalArgumentException.class, () -> msk("2025-03-03T10:00", "2025-03-03T09:00"));
    }

    @Test
    void shiftLongerThanDayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> msk("2025-03-03T08:00", "2025-03-04T08:01"));

        assertEquals(Duration.ofHours(24), msk("2025-03-03T08:00", "2025-03-04T08:00").duration());
    }

    @Test
    void shiftsBackToBackDoNotOverlap() {
        Shift a = msk("2025-03-03T08:00", "2025-03-03T12:00");
        Shift b = msk("2025-03-03T12:00", "2025-03-03T16:00");
        Shift c = msk("2025-03-03T11:59", "2025-03-03T13:00");
        assertFalse(a.overlaps(b));
        assertTrue(a.overlaps(c));
        assertTrue(c.overlaps(b));
    }


    @Test
    void dayShiftWithOneHourOvertime() {
        List<TimeSegment> segs = sheet.segments(msk("2025-03-03T09:00", "2025-03-03T18:00"));
        assertEquals(2, segs.size());
        assertEquals(TimeType.REGULAR, segs.get(0).type());
        assertEquals(Duration.ofHours(8), segs.get(0).duration());
        assertEquals(TimeType.OVERTIME, segs.get(1).type());
        assertEquals(Duration.ofHours(1), segs.get(1).duration());
    }

    @Test
    void eveningShiftThroughMidnight() {

        List<TimeSegment> segs = sheet.segments(msk("2025-03-03T18:00", "2025-03-04T02:00"));
        assertEquals(2, segs.size());
        assertEquals(TimeType.REGULAR, segs.get(0).type());
        assertEquals(Duration.ofHours(4), segs.get(0).duration());
        assertEquals(TimeType.NIGHT, segs.get(1).type());
        assertEquals(Duration.ofHours(4), segs.get(1).duration());
    }

    @Test
    void nightShiftWithOvertimeAndMorning() {

        List<TimeSegment> segs = sheet.segments(msk("2025-03-03T22:00", "2025-03-04T08:00"));
        assertEquals(List.of(TimeType.NIGHT, TimeType.OVERTIME), segs.stream().map(TimeSegment::type).toList());
        assertEquals(Duration.ofHours(2), segs.get(1).duration());
    }

    @Test
    void shiftIntoHolidayAfterMidnight() {

        Map<TimeType, Duration> byType = new java.util.EnumMap<>(TimeType.class);
        for (TimeSegment s : sheet.segments(msk("2025-03-07T20:00", "2025-03-08T03:00"))) {
            byType.merge(s.type(), s.duration(), Duration::plus);
        }
        assertEquals(Duration.ofHours(2), byType.get(TimeType.REGULAR));
        assertEquals(Duration.ofHours(2), byType.get(TimeType.NIGHT));
        assertEquals(Duration.ofHours(3), byType.get(TimeType.HOLIDAY));
    }

    @Test
    void nightBoundaries() {
        assertTrue(ShiftSplitter.isNight(java.time.LocalTime.of(22, 0)));
        assertTrue(ShiftSplitter.isNight(java.time.LocalTime.of(5, 59)));
        assertFalse(ShiftSplitter.isNight(java.time.LocalTime.of(6, 0)));
        assertFalse(ShiftSplitter.isNight(java.time.LocalTime.of(21, 59)));
    }


    @Test
    void springDstShiftIsOneHourShorter() {
        Shift s = Shift.of(LocalDateTime.of(2025, 3, 29, 22, 0), LocalDateTime.of(2025, 3, 30, 6, 0), BERLIN);
        assertEquals(Duration.ofHours(7), s.duration());
        List<TimeSegment> segs = sheet.segments(s);
        assertEquals(1, segs.size());
        assertEquals(TimeType.NIGHT, segs.get(0).type());
        assertEquals(Duration.ofHours(7), segs.get(0).duration());
    }

    @Test
    void autumnDstShiftIsOneHourLonger() {
        Shift s = Shift.of(LocalDateTime.of(2025, 10, 25, 22, 0), LocalDateTime.of(2025, 10, 26, 6, 0), BERLIN);
        assertEquals(Duration.ofHours(9), s.duration());

        List<TimeSegment> segs = ShiftSplitter.split(s, calendar, Duration.ofHours(8));
        assertEquals(List.of(TimeType.NIGHT, TimeType.OVERTIME), segs.stream().map(TimeSegment::type).toList());
        assertEquals(Duration.ofHours(1), segs.get(1).duration());
    }


    @Test
    void payCoefficients() {
        long rate = 600_00;
        assertEquals(600_00, TimeType.REGULAR.pay(Duration.ofHours(1), rate));
        assertEquals(720_00, TimeType.NIGHT.pay(Duration.ofHours(1), rate));
        assertEquals(1200_00, TimeType.HOLIDAY.pay(Duration.ofHours(1), rate));

        assertEquals(3000_00, TimeType.OVERTIME.pay(Duration.ofHours(3), rate));
        assertEquals(0, TimeType.REGULAR.pay(Duration.ZERO, rate));
    }

    @Test
    void payRoundsToKopeck() {

        assertEquals(2, TimeType.REGULAR.pay(Duration.ofMinutes(1), 100));

        assertEquals(1, TimeType.REGULAR.pay(Duration.ofMinutes(1), 30));
    }

    @Test
    void payForDayShift() throws TimesheetException {
        Shift s = msk("2025-03-03T09:00", "2025-03-03T18:00");
        sheet.addShift(s);

        assertEquals(5700_00, sheet.payForShift(s));
        assertEquals(5700_00, sheet.totalPay());
    }


    @Test
    void overlappingShiftThrows() throws TimesheetException {
        sheet.addShift(msk("2025-03-03T09:00", "2025-03-03T18:00"));
        ShiftOverlapException ex = assertThrows(ShiftOverlapException.class,
                () -> sheet.addShift(msk("2025-03-03T17:00", "2025-03-03T20:00")));
        assertEquals(LocalDate.of(2025, 3, 3), ex.getExistingShift().date());
        assertEquals(1, sheet.getShifts().size());
    }

    @Test
    void overtimeLimitThrows() throws TimesheetException {

        assertEquals(Duration.ofHours(168), sheet.monthlyNorm());
        for (int day = 1; day <= 7; day++) {
            sheet.addShift(msk(String.format("2025-03-%02dT00:00", day), String.format("2025-03-%02dT00:00", day + 1)));
        }

        sheet.addShift(msk("2025-03-10T08:00", "2025-03-11T00:00"));
        assertEquals(Duration.ofHours(184), sheet.totalWorked());
        OvertimeLimitExceededException ex = assertThrows(OvertimeLimitExceededException.class,
                () -> sheet.addShift(msk("2025-03-12T08:00", "2025-03-12T09:00")));
        assertEquals(Duration.ofHours(185), ex.getTotalAfter());
        assertEquals(Duration.ZERO, ex.remaining(sheet.totalWorked()));
    }

    @Test
    void shiftFromOtherMonthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> sheet.addShift(msk("2025-04-01T09:00", "2025-04-01T18:00")));
    }

    @Test
    void getShiftsReturnsCopy() throws TimesheetException {
        sheet.addShift(msk("2025-03-03T09:00", "2025-03-03T18:00"));
        List<Shift> list = sheet.getShifts();
        assertThrows(UnsupportedOperationException.class, () -> list.clear());
        assertEquals(1, sheet.getShifts().size());
    }

    @Test
    void workedBetweenCutsShiftsOnBorder() throws TimesheetException {
        sheet.addShift(msk("2025-03-03T18:00", "2025-03-04T02:00"));
        sheet.addShift(msk("2025-03-05T09:00", "2025-03-05T13:00"));
        assertEquals(Duration.ofHours(6), sheet.workedBetween(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 3)));
        assertEquals(Duration.ofHours(6), sheet.workedBetween(LocalDate.of(2025, 3, 4), LocalDate.of(2025, 3, 5)));
        assertEquals(Duration.ofHours(12), sheet.workedBetween(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)));
        assertThrows(IllegalArgumentException.class,
                () -> sheet.workedBetween(LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 1)));
    }

    @Test
    void totalsByTypeForMonth() throws TimesheetException {
        sheet.addShift(msk("2025-03-03T09:00", "2025-03-03T18:00"));
        sheet.addShift(msk("2025-03-04T22:00", "2025-03-05T06:00"));
        sheet.addShift(msk("2025-03-08T10:00", "2025-03-08T14:00"));
        Map<TimeType, Duration> t = sheet.totalsByType();
        assertEquals(Duration.ofHours(8), t.get(TimeType.REGULAR));
        assertEquals(Duration.ofHours(1), t.get(TimeType.OVERTIME));
        assertEquals(Duration.ofHours(8), t.get(TimeType.NIGHT));
        assertEquals(Duration.ofHours(4), t.get(TimeType.HOLIDAY));
        assertEquals(Duration.ofHours(21), sheet.totalWorked());
    }

    @Test
    void partTimeNorm() {
        assertEquals(Duration.ofHours(4), new PartTimeNorm(50).dailyNorm());
        assertEquals(Duration.ofMinutes(144), new PartTimeNorm(30).dailyNorm());
        assertThrows(IllegalArgumentException.class, () -> new PartTimeNorm(0));
        assertThrows(IllegalArgumentException.class, () -> new PartTimeNorm(100));
    }

    @Test
    void workingDaysInMonth() {
        assertEquals(21, calendar.workingDays(YearMonth.of(2025, 3)));
        calendar.addHoliday(LocalDate.of(2025, 3, 10));
        assertEquals(20, calendar.workingDays(YearMonth.of(2025, 3)));
        assertFalse(calendar.isWorkingDay(LocalDate.of(2025, 3, 9)));
    }
}
