package timesheet.model;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Department {

    private final String name;
    private final YearMonth month;
    private final HolidayCalendar calendar;
    private final Map<Employee, MonthTimeSheet> sheets = new LinkedHashMap<>();


    public Department(String name, YearMonth month, HolidayCalendar calendar) {
        this.name = Objects.requireNonNull(name);
        this.month = Objects.requireNonNull(month);
        this.calendar = Objects.requireNonNull(calendar);
    }


    public MonthTimeSheet addEmployee(Employee employee) {
        return sheets.computeIfAbsent(employee, e -> new MonthTimeSheet(e, month, calendar));
    }


    public MonthTimeSheet sheetOf(Employee employee) {
        MonthTimeSheet sheet = sheets.get(employee);
        if (sheet == null) {
            throw new IllegalArgumentException("Сотрудника нет в подразделении: " + employee);
        }
        return sheet;
    }


    public Employee findEmployee(String personnelNumber) {
        for (Employee e : sheets.keySet()) {
            if (e.getPersonnelNumber().equals(personnelNumber)) {
                return e;
            }
        }
        return null;
    }


    public void addShift(Employee employee, Shift shift) throws TimesheetException {
        sheetOf(employee).addShift(shift);
    }


    public List<Employee> getEmployees() {
        return new ArrayList<>(sheets.keySet());
    }


    public long totalPay() {
        long sum = 0;
        for (MonthTimeSheet sheet : sheets.values()) {
            sum += sheet.totalPay();
        }
        return sum;
    }


    public String getName() {
        return name;
    }


    public YearMonth getMonth() {
        return month;
    }
}
