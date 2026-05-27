import java.io.*;
import java.net.*;
import java.util.regex.*;

public class SMTPProxy {

    private static int totalClients = 0;
    private static int emailsProcessed = 0;
    private static int blockedEmails = 0;
    private static int totalSubstitutions = 0;
    private static int activeConnections = 0;

    public static void main(String[] args) {

        int proxyPort = 55555;
        String smtpHost = "127.0.0.1";
        int smtpPort = 25;

        try (ServerSocket serverSocket = new ServerSocket(proxyPort)) {

            System.out.println("Proxy server is listening on port " + proxyPort);

            while (true) {

                Socket clientSocket = serverSocket.accept();
                totalClients++;
                activeConnections++;

                System.out.println("Client connected.");

                new Thread(() -> {
                    handleClient(clientSocket, smtpHost, smtpPort);
                }).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleClient(Socket clientSocket, String smtpHost, int smtpPort) {

        try (

                Socket serverSocket = new Socket(smtpHost, smtpPort);

                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);) {
            String response = serverIn.readLine();
            System.out.println("[SERVER] " + response);
            clientOut.println(response);

            String line;

            while ((line = clientIn.readLine()) != null) {

                System.out.println("[CLIENT] " + line);

                // Forward command to SMTP server
                serverOut.println(line);

                // Handle DATA command
                if (line.equalsIgnoreCase("DATA")) {

                    response = serverIn.readLine();

                    System.out.println("[SERVER] " + response);

                    clientOut.println(response);

                    printStats();

                    // Read email body until single dot
                    StringBuilder emailBody = new StringBuilder();
                    while (!(line = clientIn.readLine()).equals(".")) {

                        System.out.println("[BODY] " + line);
                        emailBody.append(line).append("\r\n");
                    }

                    String modifiedEmail = processEmail(emailBody.toString());
                    emailsProcessed++;

                    // Send terminating dot
                    serverOut.println(modifiedEmail);
                    serverOut.print("Please do not take anything in this email seriously :)\r\n");
                    serverOut.print(".\r\n");
                    serverOut.flush();

                    // Read final server response
                    response = serverIn.readLine();

                    System.out.println("[SERVER] " + response);

                    clientOut.println(response);

                    continue;
                }

                // Normal SMTP response
                response = serverIn.readLine();

                System.out.println("[SERVER] " + response);

                clientOut.println(response);

                if (line.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }

            clientSocket.close();
            activeConnections--;

            System.out.println("Client disconnected.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String processEmail(String text) {
        // Illuminati detection
        if (text.contains("Illuminati")) {
            blockedEmails++;
            return "Hello world\r\n";
        }

        text = replaceAndCount(text, "(?i)\\bvery good\\b", "plusgood");
        text = replaceAndCount(text, "(?i)\\bvery fast\\b", "pludfast");
        text = replaceAndCount(text, "(?i)\\bvery bad\\b", "plusungood");
        text = replaceAndCount(text, "(?i)\\bwarm\\b", "uncold");
        text = replaceAndCount(text, "(?i)\\bbad\\b", "ungood");
        text = replaceAndCount(text, "(?i)\\bfast\\b", "speedful");
        text = replaceAndCount(text, "(?i)\\brapid\\b", "speedful");
        text = replaceAndCount(text, "(?i)\\bquick\\b", "speedful");
        text = replaceAndCount(text, "(?i)\\bslow\\b", "unspeedful");
        text = replaceAndCount(text, "(?i)\\bran\\b", "runned");
        text = replaceAndCount(text, "(?i)\\bstole\\b", "stealed");
        text = replaceAndCount(text, "(?i)\\bbetter\\b", "gooder");
        text = replaceAndCount(text, "(?i)\\bbest\\b", "goodest");
        return text;
    }

    public static String replaceAndCount(String text, String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int count = 0;

        while (matcher.find()) {
            count++;
        }
        totalSubstitutions += count;
        return matcher.replaceAll(replacement);
    }

    public static void printStats() {
        System.out.println("Total clients connected: " + totalClients);
        System.out.println("Total emails processed: " + emailsProcessed);
        System.out.println("Blocked emails: " + blockedEmails);
        System.out.println("Total substitutions made: " + totalSubstitutions);
        System.out.println("Active connections: " + activeConnections);
    }
}