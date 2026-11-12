package timesheet.model;

import java.time.Duration;


public record PartTimeNorm(int percent) implements WorkNorm {


    public PartTimeNorm {
        if (percent < 1 || percent > 99) {
            throw new IllegalArgumentException("Процент ставки должен быть от 1 до 99");
        }
    }


    @Override
    public Duration dailyNorm() {
        return Duration.ofMinutes(8 * 60L * percent / 100);
    }


    @Override
    public Duration monthlyOvertimeLimit() {
        return Duration.ofHours(8);
    }
}
