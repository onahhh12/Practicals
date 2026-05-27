import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

public class client {
    public static void sendEmail(String from, String to, List<bdayReader.Events> events) throws IOException {

        Socket socket = new Socket("localhost", 25);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        in.readLine();

        out.write("HELO localhost\r\n");
        out.flush();
        in.readLine();

        out.write("MAIL FROM:<" + from + ">\r\n");
        out.flush();
        in.readLine();

        out.write("RCPT TO:<" + ">\r\n");
        out.flush();
        in.readLine();

        out.write("DATA\r\n");
        out.flush();
        in.readLine();

        out.write("From: " + from + "\r\n");
        out.write("To " + to + "\r\n");
        out.write("Subject: Birthday Reminder\r\n");
        out.write("\r\n");

        out.write("These are the birthdays happening in the next 6 days: ");
        out.write("\r\n");
        for (bdayReader.Events event : events) {
            out.write("- " + event.day + "/" + event.month + " : " + event.info + "\r\n");
        }

        out.flush();

        out.write(".\r\n");
        out.flush();
        in.readLine();

        out.write("QUIT\r\n");
        out.flush();
        in.readLine();

        socket.close();
    }
}
