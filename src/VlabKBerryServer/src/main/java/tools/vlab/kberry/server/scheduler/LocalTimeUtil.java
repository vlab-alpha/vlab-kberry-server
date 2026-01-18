package tools.vlab.kberry.server.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record LocalTimeUtil() {

    public static boolean isSame(LocalDateTime a, DayOfWeek dayOfWeek, LocalTime localTime) {
        return a.getDayOfWeek() == dayOfWeek && isSame(a, localTime);
    }

    public static boolean isSame(LocalDateTime a, LocalTime b) {
        return a.getHour() == b.getHour() && a.getMinute() == b.getMinute() && a.getSecond() == b.getSecond();
    }

    public static boolean isWeekend(LocalDateTime localDateTime) {
        return localDateTime.getDayOfWeek() == DayOfWeek.SATURDAY || localDateTime.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static boolean isWeekday(LocalDateTime localDateTime) {
        return localDateTime.getDayOfWeek() != DayOfWeek.SATURDAY && localDateTime.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
