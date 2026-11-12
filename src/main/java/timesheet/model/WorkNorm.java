package timesheet.model;

import java.time.Duration;
import java.time.YearMonth;


public sealed interface WorkNorm permits FullTimeNorm, PartTimeNorm {


    Duration dailyNorm();


    Duration monthlyOvertimeLimit();


    default Duration monthlyNorm(YearMonth month, HolidayCalendar calendar) {
        return dailyNorm().multipliedBy(calendar.workingDays(month));
    }
}
