package timesheet.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;


public record Shift(ZonedDateTime start, ZonedDateTime end) {


    public static final Duration MAX_LENGTH = Duration.ofHours(24);


    public Shift {
        Objects.requireNonNull(start, "Начало смены не задано");
        Objects.requireNonNull(end, "Конец смены не задан");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Конец смены должен быть позже начала: " + start + " - " + end);
        }
        if (!start.getZone().equals(end.getZone())) {
            throw new IllegalArgumentException("Начало и конец смены в разных часовых поясах");
        }
        if (Duration.between(start, end).compareTo(MAX_LENGTH) > 0) {
            throw new IllegalArgumentException("Смена длиннее 24 часов");
        }
    }


    public static Shift of(LocalDateTime start, LocalDateTime end, ZoneId zone) {
        return new Shift(start.atZone(zone), end.atZone(zone));
    }


    public Duration duration() {
        return Duration.between(start, end);
    }


    public LocalDate date() {
        return start.toLocalDate();
    }


    public ZoneId zone() {
        return start.getZone();
    }


    public boolean overlaps(Shift other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
