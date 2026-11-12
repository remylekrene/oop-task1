package timesheet.model;

import java.time.Duration;


public enum TimeType {


    REGULAR("Обычное") {
        @Override
        public long pay(Duration duration, long hourlyRate) {
            return payByPercent(duration.toMinutes(), hourlyRate, 100);
        }
    },


    OVERTIME("Сверхурочное") {
        @Override
        public long pay(Duration duration, long hourlyRate) {
            long minutes = duration.toMinutes();
            long firstTwoHours = Math.min(minutes, 120);
            long rest = minutes - firstTwoHours;
            return payByPercent(firstTwoHours, hourlyRate, 150) + payByPercent(rest, hourlyRate, 200);
        }
    },


    NIGHT("Ночное") {
        @Override
        public long pay(Duration duration, long hourlyRate) {
            return payByPercent(duration.toMinutes(), hourlyRate, 120);
        }
    },


    HOLIDAY("Праздничное") {
        @Override
        public long pay(Duration duration, long hourlyRate) {
            return payByPercent(duration.toMinutes(), hourlyRate, 200);
        }
    };

    private final String title;

    TimeType(String title) {
        this.title = title;
    }


    public String getTitle() {
        return title;
    }


    public abstract long pay(Duration duration, long hourlyRate);


    private static long payByPercent(long minutes, long hourlyRate, int percent) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Отрицательное время: " + minutes);
        }

        return (hourlyRate * minutes * percent + 3000) / 6000;
    }
}
