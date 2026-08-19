import java.io.*;
import java.net.*;
import java.util.*;

public class AppointmentServer {

    private static final String ESC = "\u001B";
    private static final String CLEAR = ESC + "[2J";
    private static final String HOME = ESC + "[H";

    private static final String DB_FILE = "appointments.txt";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java AppointmentServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket client = server.accept();
                Thread t = new Thread(new ClientSession(client));
                t.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Client Session
    private static class ClientSession implements Runnable {
        private final Socket client;

        ClientSession(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (
                    BufferedInputStream in = new BufferedInputStream(client.getInputStream());
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                // Loading DB
                boolean running = true;

                clearScreen(out);
                out.println("Welcome to the Appointment Server");
                out.println("---------------------------------");
                out.println("Tip: If your telnet is not showing what you type, turn localecho on.");
                out.println("");

                while (running) {
                    showMenu(out);
                    out.print("Choose (1-6): ");
                    out.flush();

                    String choice = readLineEchoOnEnter(in, out);
                    if (choice == null)
                        break;

                    choice = choice.trim();

                    if (choice.equals("1")) {
                        clearScreen(out);
                        listAppointments(out);
                        pause(out, in);

                    } else if (choice.equals("2")) {
                        clearScreen(out);
                        out.println("ADD appointment (example: 2026-03-01 14:00 Meeting with Thabo)");
                        out.print("Enter appointment: ");
                        out.flush();
                        String appt = readLineEchoOnEnter(in, out);
                        if (appt != null && appt.trim().length() > 0) {
                            addAppointment(appt.trim());
                            out.println("\nSaved.");
                        } else {
                            out.println("\nNothing saved.");
                        }
                        pause(out, in);

                    } else if (choice.equals("3")) {
                        clearScreen(out);
                        out.print("SEARCH keyword: ");
                        out.flush();
                        String kw = readLineEchoOnEnter(in, out);
                        clearScreen(out);
                        if (kw != null)
                            searchAppointments(out, kw.trim());
                        pause(out, in);

                    } else if (choice.equals("4")) {
                        clearScreen(out);
                        listAppointments(out);
                        out.println("");
                        out.print("DELETE by number (e.g. 2): ");
                        out.flush();
                        String num = readLineEchoOnEnter(in, out);
                        if (num != null) {
                            try {
                                int idx = Integer.parseInt(num.trim());
                                boolean ok = deleteAppointment(idx);
                                out.println(ok ? "\nDeleted." : "\nInvalid number.");
                            } catch (NumberFormatException nfe) {
                                out.println("\nNot a number.");
                            }
                        }
                        pause(out, in);

                    } else if (choice.equals("5")) {
                        clearScreen(out);
                        out.println("CLEAR ALL appointments?");
                        out.print("Type YES to confirm: ");
                        out.flush();
                        String ans = readLineEchoOnEnter(in, out);
                        if (ans != null && ans.trim().equalsIgnoreCase("YES")) {
                            clearAll();
                            out.println("\nAll appointments cleared.");
                        } else {
                            out.println("\nCancelled.");
                        }
                        pause(out, in);

                    } else if (choice.equals("6")) {
                        out.println("\nGoodbye!");
                        running = false;

                    } else {
                        out.println("\nInvalid option.");
                        pause(out, in);
                    }
                }

            } catch (IOException e) {

            } finally {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void showMenu(PrintWriter out) {
            out.println(CLEAR + HOME);
            out.println("Appointment Server Menu");
            out.println("-----------------------");
            out.println("1) List appointments");
            out.println("2) Add appointment");
            out.println("3) Search appointments");
            out.println("4) Delete appointment");
            out.println("5) Clear all");
            out.println("6) Quit");
            out.println("");
        }

        private void clearScreen(PrintWriter out) {
            out.print(CLEAR);
            out.print(HOME);
            out.flush();
        }

        private void pause(PrintWriter out, BufferedInputStream in) throws IOException {
            out.println("\nPress ENTER to continue...");
            out.flush();
            readLineEchoOnEnter(in, out);
        }
    }

    private static synchronized List<String> loadAll() throws IOException {
        File f = new File(DB_FILE);
        if (!f.exists())
            return new ArrayList<>();

        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0)
                    list.add(line);
            }
        }
        return list;
    }

    private static synchronized void saveAll(List<String> list) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DB_FILE, false))) {
            for (String s : list)
                pw.println(s);
        }
    }

    private static void listAppointments(PrintWriter out) throws IOException {
        List<String> list = loadAll();
        out.println("Appointments");
        out.println("------------");
        if (list.isEmpty()) {
            out.println("(none)");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            out.println((i + 1) + ") " + list.get(i));
        }
    }

    private static void addAppointment(String appt) throws IOException {
        List<String> list = loadAll();
        list.add(appt);
        saveAll(list);
    }

    private static void searchAppointments(PrintWriter out, String keyword) throws IOException {
        List<String> list = loadAll();
        out.println("Search results for: " + keyword);
        out.println("-------------------------");
        if (keyword.length() == 0) {
            out.println("Keyword empty.");
            return;
        }

        boolean found = false;
        String kw = keyword.toLowerCase();

        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            if (line.toLowerCase().contains(kw)) {
                out.println((i + 1) + ") " + line);
                found = true;
            }
        }
        if (!found)
            out.println("(no matches)");
    }

    private static boolean deleteAppointment(int number) throws IOException {
        List<String> list = loadAll();
        int idx = number - 1;
        if (idx < 0 || idx >= list.size())
            return false;
        list.remove(idx);
        saveAll(list);
        return true;
    }

    private static void clearAll() throws IOException {
        saveAll(new ArrayList<>());
    }

    private static String readLineEchoOnEnter(BufferedInputStream in, PrintWriter out) throws IOException {
        StringBuilder sb = new StringBuilder();

        while (true) {
            int b = in.read();
            if (b == -1)
                return null;

            if (b == 255) {
                in.read();
                in.read();
                continue;
            }

            if (b == 13) {
                in.mark(1);
                int next = in.read();
                if (next != 10)
                    in.reset();
                break;
            }
            if (b == 10)
                break;

            if (b == 8 || b == 127) {
                if (sb.length() > 0)
                    sb.deleteCharAt(sb.length() - 1);
                continue;
            }

            sb.append((char) b);
        }

        String line = sb.toString();

        out.println(line);
        out.flush();

        return line;
    }
}