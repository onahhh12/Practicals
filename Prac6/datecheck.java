import java.time.LocalDate;
import java.util.*;

public class datecheck {

    public static List<bdayReader.Events> getUpcomingEvents(List<bdayReader.Events> events){

        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(6);

        int targetDay = targetDate.getDayOfMonth();
        int targetMonth = targetDate.getMonthValue();

        List<bdayReader.Events> upcoming = new ArrayList<>();

        for (bdayReader.Events event : events){
            if (event.day == targetDay && event.month == targetMonth) {
                upcoming.add(event);
            }
            
        }

        return upcoming;

    }
    
}