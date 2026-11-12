package timesheet.app;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

import timesheet.model.FullTimeNorm;
import timesheet.model.PartTimeNorm;
import timesheet.model.WorkNorm;


public final class Format {


    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Format() {
    }


    public static String money(long kopecks) {
        return String.format("%,d,%02d руб.", kopecks / 100, kopecks % 100);
    }


    public static String duration(Duration d) {
        long minutes = d.toMinutes();
        if (minutes % 60 == 0) {
            return (minutes / 60) + " ч";
        }
        return (minutes / 60) + " ч " + (minutes % 60) + " мин";
    }


    public static String norm(WorkNorm norm) {
        return switch (norm) {
            case FullTimeNorm f -> "полная ставка (" + duration(f.dailyNorm()) + " в день)";
            case PartTimeNorm p -> "неполная ставка " + p.percent() + "% (" + duration(p.dailyNorm()) + " в день)";
        };
    }
}
