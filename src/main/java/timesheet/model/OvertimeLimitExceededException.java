package timesheet.model;

import java.time.Duration;


public class OvertimeLimitExceededException extends TimesheetException {

    private final Duration totalAfter;
    private final Duration maxAllowed;


    public OvertimeLimitExceededException(Employee employee, Duration totalAfter, Duration maxAllowed) {
        super("Переработка у " + employee.getFullName() + ": было бы " + totalAfter.toHours()
                + " ч при максимуме " + maxAllowed.toHours() + " ч");
        this.totalAfter = totalAfter;
        this.maxAllowed = maxAllowed;
    }


    public Duration getTotalAfter() {
        return totalAfter;
    }


    public Duration getMaxAllowed() {
        return maxAllowed;
    }


    public Duration remaining(Duration alreadyWorked) {
        Duration rest = maxAllowed.minus(alreadyWorked);
        return rest.isNegative() ? Duration.ZERO : rest;
    }
}
