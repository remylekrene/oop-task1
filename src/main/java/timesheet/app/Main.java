package timesheet.app;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import timesheet.model.Department;
import timesheet.model.Employee;
import timesheet.model.FullTimeNorm;
import timesheet.model.HolidayCalendar;
import timesheet.model.MonthTimeSheet;
import timesheet.model.OvertimeLimitExceededException;
import timesheet.model.PartTimeNorm;
import timesheet.model.Shift;
import timesheet.model.ShiftOverlapException;
import timesheet.model.TimeSegment;
import timesheet.model.TimeType;
import timesheet.model.TimesheetException;


public class Main {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final YearMonth MARCH = YearMonth.of(2025, Month.MARCH);

    public static void main(String[] args) {
        HolidayCalendar calendar = new HolidayCalendar(List.of(LocalDate.of(2025, 3, 8)));
        Department dept = new Department("Цех №1", MARCH, calendar);

        Employee ivanov = new Employee("T-001", "Иванов Иван", 450_00, new FullTimeNorm());
        Employee petrova = new Employee("T-002", "Петрова Анна", 520_00, new FullTimeNorm());
        Employee sidorov = new Employee("T-003", "Сидоров Пётр", 380_00, new FullTimeNorm());
        Employee kozlova = new Employee("T-004", "Козлова Мария", 600_00, new PartTimeNorm(50));
        Employee smirnov = new Employee("T-005", "Смирнов Олег", 300_00, new PartTimeNorm(25));
        for (Employee e : List.of(ivanov, petrova, sidorov, kozlova, smirnov)) {
            dept.addEmployee(e);
        }

        title("1. Сотрудники подразделения " + dept.getName());
        for (Employee e : dept.getEmployees()) {
            MonthTimeSheet sheet = dept.sheetOf(e);
            System.out.printf("%-22s ставка %s/ч, %s, норма на март %s, максимум %s%n",
                    e, Format.money(e.getHourlyRate()), Format.norm(e.getNorm()),
                    Format.duration(sheet.monthlyNorm()), Format.duration(sheet.maxAllowed()));
        }

        title("2. Заполняем табель");
        int added = 0;

        for (int day : new int[]{3, 4, 5, 6, 7, 10, 11}) {
            added += tryAdd(dept, ivanov, shift(day, 9, 0, day, 18, 0));
        }

        for (int day : new int[]{3, 5, 7, 12, 14}) {
            added += tryAdd(dept, petrova, shift(day, 18, 0, day + 1, 2, 0));
        }

        added += tryAdd(dept, petrova, shift(8, 10, 0, 8, 18, 0));

        for (int day : new int[]{3, 4, 10, 11, 17, 18}) {
            added += tryAdd(dept, sidorov, shift(day, 22, 0, day + 1, 8, 0));
        }

        for (int day : new int[]{3, 4, 5, 6, 7, 10, 11, 12}) {
            added += tryAdd(dept, kozlova, shift(day, 8, 0, day, 12, 0));
        }
        System.out.println("Добавлено смен: " + added);

        title("3. Ошибка: смена пересекается с уже записанной");
        tryAdd(dept, ivanov, shift(3, 17, 0, 3, 21, 0));

        title("4. Ошибка: переработка сверх лимита (Смирнов, 25% ставки, максимум "
                + Format.duration(dept.sheetOf(smirnov).maxAllowed()) + ")");
        for (int day : new int[]{3, 4, 5, 6, 7, 10}) {
            tryAdd(dept, smirnov, shift(day, 9, 0, day, 18, 0));
        }

        title("5. Разбиение смен на отрезки по типам времени");
        printSegments(dept.sheetOf(ivanov), dept.sheetOf(ivanov).getShifts().get(0));
        printSegments(dept.sheetOf(petrova), dept.sheetOf(petrova).getShifts().get(0));
        printSegments(dept.sheetOf(sidorov), dept.sheetOf(sidorov).getShifts().get(0));
        MonthTimeSheet petrovaSheet = dept.sheetOf(petrova);
        for (Shift s : petrovaSheet.getShifts()) {
            if (s.date().equals(LocalDate.of(2025, 3, 7))) {
                printSegments(petrovaSheet, s);
            }
        }

        title("6. Отработанное время за период");
        LocalDate from = LocalDate.of(2025, 3, 3);
        LocalDate to = LocalDate.of(2025, 3, 9);
        for (Employee e : dept.getEmployees()) {
            Duration week = dept.sheetOf(e).workedBetween(from, to);
            System.out.printf("%-22s с %s по %s: %s (Duration = %s)%n", e, from, to, Format.duration(week), week);
        }

        title("7. Отчёт за месяц");
        for (Employee e : dept.getEmployees()) {
            printReport(dept.sheetOf(e));
        }
        System.out.println("ИТОГО по подразделению к выплате: " + Format.money(dept.totalPay()));

        title("8. Поиск сотрудника по табельному номеру");
        System.out.println("T-003 -> " + dept.findEmployee("T-003"));
        System.out.println("T-999 -> " + dept.findEmployee("T-999"));

        title("9. Смены в ночь перевода часов (филиал в Берлине)");
        dstDemo(calendar);
    }


    private static int tryAdd(Department dept, Employee e, Shift s) {
        try {
            dept.addShift(e, s);
            return 1;
        } catch (ShiftOverlapException ex) {
            System.out.println("Не добавлено: " + ex.getMessage());
            System.out.println("  -> смену нужно перенести, табель не изменён");
        } catch (OvertimeLimitExceededException ex) {
            System.out.println("Не добавлено: " + ex.getMessage());

            MonthTimeSheet sheet = dept.sheetOf(e);
            Duration rest = ex.remaining(sheet.totalWorked());
            if (rest.isZero()) {
                System.out.println("  -> лимит исчерпан полностью, смена не ставится");
            } else {
                Shift shorter = new Shift(s.start(), s.start().plus(rest));
                try {
                    sheet.addShift(shorter);
                    System.out.println("  -> вместо неё поставлена укороченная смена на " + Format.duration(rest));
                    return 1;
                } catch (TimesheetException again) {
                    System.out.println("  -> укороченную смену тоже не удалось поставить: " + again.getMessage());
                }
            }
        } catch (TimesheetException ex) {
            System.out.println("Ошибка табеля: " + ex.getMessage());
        }
        return 0;
    }

    private static Shift shift(int d1, int h1, int m1, int d2, int h2, int m2) {
        return Shift.of(LocalDateTime.of(2025, 3, d1, h1, m1), LocalDateTime.of(2025, 3, d2, h2, m2), MOSCOW);
    }

    private static void printSegments(MonthTimeSheet sheet, Shift s) {
        System.out.println(sheet.getEmployee().getFullName() + ", смена "
                + s.start().format(Format.DATE_TIME) + " - " + s.end().format(Format.DATE_TIME)
                + " (" + Format.duration(s.duration()) + "):");
        for (TimeSegment seg : sheet.segments(s)) {
            System.out.printf("   %s - %s  %-13s %s%n", seg.start().format(Format.DATE_TIME),
                    seg.end().format(Format.DATE_TIME), seg.type().getTitle(), Format.duration(seg.duration()));
        }
        System.out.println("   оплата за смену: " + Format.money(sheet.payForShift(s)));
    }

    private static void printReport(MonthTimeSheet sheet) {
        Employee e = sheet.getEmployee();
        System.out.println("--- " + e + ", " + sheet.getMonth() + ", смен: " + sheet.getShifts().size());
        Map<TimeType, Duration> hours = sheet.totalsByType();
        Map<TimeType, Long> pay = sheet.payByType();
        for (TimeType t : TimeType.values()) {
            System.out.printf("   %-13s %-12s %s%n", t.getTitle(), Format.duration(hours.get(t)), Format.money(pay.get(t)));
        }
        System.out.printf("   %-13s %-12s %s%n", "Всего", Format.duration(sheet.totalWorked()), Format.money(sheet.totalPay()));
        Duration diff = sheet.totalWorked().minus(sheet.monthlyNorm());
        if (diff.isNegative()) {
            System.out.println("   до нормы не хватает " + Format.duration(diff.negated()));
        } else {
            System.out.println("   сверх нормы " + Format.duration(diff));
        }
    }

    private static void dstDemo(HolidayCalendar calendar) {
        Employee guard = new Employee("B-001", "Мюллер Ханс", 1500_00, new FullTimeNorm());
        MonthTimeSheet march = new MonthTimeSheet(guard, MARCH, calendar);

        Shift spring = Shift.of(LocalDateTime.of(2025, 3, 29, 22, 0), LocalDateTime.of(2025, 3, 30, 6, 0), BERLIN);

        Shift autumn = Shift.of(LocalDateTime.of(2025, 10, 25, 22, 0), LocalDateTime.of(2025, 10, 26, 6, 0), BERLIN);
        MonthTimeSheet october = new MonthTimeSheet(guard, YearMonth.of(2025, 10), calendar);
        try {
            march.addShift(spring);
            october.addShift(autumn);
        } catch (TimesheetException ex) {
            System.out.println("Ошибка: " + ex.getMessage());
            return;
        }
        System.out.println("По часам на стене обе смены 22:00-06:00, т.е. 8 часов, на самом деле:");
        printSegments(march, spring);
        printSegments(october, autumn);
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
