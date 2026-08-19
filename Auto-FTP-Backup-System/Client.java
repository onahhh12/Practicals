import java.io.*;
import java.net.Socket;

public class Client {

    private Socket socket;
     private BufferedReader in;
     private PrintWriter out;

     public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("SERVER: "+ in.readLine());
    }

    public void login(String user, String password) throws IOException {
        out.println("USER" + user);
        System.out.println("SERVER: "+ in.readLine());

        out.println("PASS" + password);
        System.out.println("SERVER: "+ in.readLine());
    }

    public void uploadFile(FileChange change, String basePath) throws IOException {

        if (change.action == FileAction.DELETED) {
            out.println("DELE " + change.fileName);
            System.out.println("SERVER: "+ in.readLine());
            return;
        }

        File file = new File(basePath + "\\" + change.fileName);
        BufferedReader fileReader = new BufferedReader(new FileReader(file));

        String versionedName = change.fileName + "." + String.format("%03d", change.version);

        out.println("STOR " + versionedName);
        System.out.println("SERVER: "+ in.readLine()); //uploading the file

        String line;
        while ((line = fileReader.readLine()) != null) {
            out.println(line);
        }
        out.println("EOF");
        fileReader.close();

        System.out.println(in.readLine());
    }

    public void disconnect() throws IOException {
        out.println("QUIT");
        System.out.println("SERVER: " + in.readLine());
        socket.close();
    }
}