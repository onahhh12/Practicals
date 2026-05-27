import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class pop3 {
    public static String readEmail(String from, String user, String password) throws IOException {

        Socket socket = new Socket("localhost", 110);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        System.out.println("Greeting: " + in.readLine());

        out.write("USER localhost\r\n");
        out.flush();
        String response = in.readLine();
        System.out.println("Server respose: " + response);

        out.write("PASS password\r\n");
        out.flush();
        String passResponse = in.readLine();
        System.out.println("Server respose: " + passResponse);

        out.write("RETR 1\r\n");
        out.flush();
        in.readLine();

        boolean shouldReply = false;
        String replyTo = null;
        String line;

        while (!(line = in.readLine()).equals(".")) {
            if (line.toLowerCase().contains("subject: prac7 ")) {
                shouldReply = true;
            }
            if (line.toLowerCase().startsWith("from: ")) {
                replyTo = line.substring(6).trim();
            }
        }

        out.write("QUIT\r\n");
        out.flush();
        socket.close();

        return shouldReply?replyTo: null;   
    }
}