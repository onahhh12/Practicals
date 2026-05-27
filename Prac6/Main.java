import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            List<bdayReader.Events> events =
                bdayReader.readEvents("events.txt");

            List<bdayReader.Events> upcoming =
                datecheck.getUpcomingEvents(events);

            if (!upcoming.isEmpty()) {
                client.sendEmail("onalenna@localhost", "onalenna@localhost", upcoming);
                System.out.println("Email sent!");
            } else {
                System.out.println("No upcoming birthdays found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
