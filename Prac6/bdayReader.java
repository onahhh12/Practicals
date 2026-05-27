import java.io.*;
import java.util.*;

public class bdayReader{

    public static class Events {
        public int day;
        public int month;
        public String info;
    
        public Events(int day, int month, String info) {
            this.day = day;
            this.month = month;
            this.info = info;
        }
    }
    
    //takes a filename as input and returns the list of birthday objects 
    public static List<Events> readEvents(String filename) throws IOException {
        List<Events> events = new ArrayList<>(); //FileReader
        

        //reads te file line, by line and parses each line and add a new event to the list 
        try(BufferedReader reader = new BufferedReader(new FileReader(filename))){
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; //skip empty lines
            
                String[] parts = line.split(" ", 2); 
                if (parts.length < 2) continue;
            
                String[] dateParts = parts[0].split("/");
                if (dateParts.length != 2) continue;
            
                int day = Integer.parseInt(dateParts[0].trim());
                int month = Integer.parseInt(dateParts[1].trim());
                String info = parts[1].trim();
            
                events.add(new Events(day, month, info));
            }
        }
        return events;
    }
}