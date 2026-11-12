package timesheet.model;

import java.time.Duration;


public record FullTimeNorm() implements WorkNorm {


    @Override
    public Duration dailyNorm() {
        return Duration.ofHours(8);
    }


    @Override
    public Duration monthlyOvertimeLimit() {
        return Duration.ofHours(16);
    }
}
