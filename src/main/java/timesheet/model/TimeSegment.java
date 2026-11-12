package timesheet.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;


public record TimeSegment(ZonedDateTime start, ZonedDateTime end, TimeType type) {


    public TimeSegment {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(type);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Пустой отрезок");
        }
    }


    public Duration duration() {
        return Duration.between(start, end);
    }
}
