import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class smpt {
    public static void sendEmail(String from, String to) throws IOException {

        Socket socket = new Socket("localhost", 25);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        System.out.println("SMPT Server: " + in.readLine());

        in.readLine();

        out.write("HELO localhost\r\n");
        out.flush();
        System.out.println("HELO Response: " + in.readLine());

        out.write("MAIL FROM: <" + from + ".\r\n");
        out.flush();
        in.readLine();

        out.write("RCPT TO: <" + ">\r\n");
        out.flush();
        in.readLine();

        out.write("DATA\r\n");
        out.flush();
        System.out.println("DATA TO Response: " + in.readLine());

        out.write("Subject: Re: pac7\r\n");
        out.write("To: " + to + "\r\n");
        out.write("\r\n");
        out.write("Good day,\r\n");
        out.write("I am currently on vacation and will reply to your prac7 email soon.\r\n");
        out.write(".\r\n");
        out.flush();
        System.out.println("Message Result: " + in.readLine());

        out.write("QUIT\r\n");
        out.flush();
        socket.close();
    
    }
    
}
