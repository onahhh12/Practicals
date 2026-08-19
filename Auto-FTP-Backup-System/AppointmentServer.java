import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class AppointmentServer {

    public static final String DB_FILE = "appointments.txt";
    public static final String IMAGE_DIR = "appointment_images";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java AppointmentServer 8000");
            return;
        }

        new File(IMAGE_DIR).mkdirs();

        int port = Integer.parseInt(args[0]);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            System.out.println("Open browser at http://localhost:" + port);
            while (true) {
                Socket client = server.accept();
                new Thread(new ClientSession(client)).start();
            }
        }
    }

    private static class ClientSession implements Runnable {
        private final Socket client;

        ClientSession(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (
                    OutputStream out = client.getOutputStream();
                    InputStream rawIn = client.getInputStream()) {
                byte[] requestBytes = readRawRequest(rawIn);
                String requestText = new String(requestBytes, StandardCharsets.ISO_8859_1);

                String requestLine = requestText.split("\r\n")[0];
                System.out.println("Request: " + requestLine);
                String[] parts = requestLine.split(" ");
                String method = parts[0];
                String path = parts[1];

                Map<String, String> headers = new HashMap<>();
                String[] lines = requestText.split("\r\n");
                int i = 1;
                while (i < lines.length && !lines[i].isEmpty()) {
                    String[] kv = lines[i].split(": ", 2);
                    if (kv.length == 2)
                        headers.put(kv[0].toLowerCase(), kv[1]);
                    i++;
                }

                int bodyStart = requestText.indexOf("\r\n\r\n") + 4;
                byte[] bodyBytes = Arrays.copyOfRange(requestBytes, bodyStart, requestBytes.length);

                String contentType = headers.getOrDefault("content-type", "");

                if (method.equals("GET") && path.equals("/")) {
                    sendHTML(out, buildHomePage());

                } else if (method.equals("GET") && path.equals("/list")) {
                    sendHTML(out, buildListPage());

                } else if (method.equals("POST") && path.equals("/add")) {
                    if (contentType.contains("multipart/form-data")) {
                        String boundary = contentType.split("boundary=")[1].trim();
                        Map<String, String> formData = parseMultipart(bodyBytes, boundary);

                        String appt = formData.getOrDefault("appointment", "").trim();
                        String hours = formData.getOrDefault("hours", "00");
                        String minutes = formData.getOrDefault("minutes", "00");
                        String ampm = formData.getOrDefault("ampm", "AM");
                        String imageFile = formData.getOrDefault("_savedImage", "");

                        if (!appt.isEmpty()) {
                            String fullAppt = appt + " at " + hours + ":" + minutes + " " + ampm;
                            addAppointment(fullAppt + "|" + imageFile);
                        }
                    }
                    sendRedirect(out, "/list");

                } else if (method.equals("GET") && path.startsWith("/images/")) {
                    serveImage(out, path.substring("/images/".length()));

                } else if (method.equals("GET") && path.startsWith("/search")) {
                    String keyword = "";
                    if (path.contains("?keyword=")) {
                        keyword = urlDecode(path.split("\\?keyword=")[1]);
                    }
                    sendHTML(out, buildSearchPage(keyword));

                } else if (method.equals("POST") && path.equals("/delete")) {
                    String body = new String(bodyBytes, StandardCharsets.UTF_8);
                    String idxStr = decodeForm(body).get("index");
                    if (idxStr != null)
                        deleteAppointment(Integer.parseInt(idxStr));
                    sendRedirect(out, "/list");

                } else if (method.equals("POST") && path.equals("/clear")) {
                    clearAppointments();
                    sendRedirect(out, "/list");

                } else {
                    send404(out);
                }

            } catch (IOException e) {
                System.out.println("Client error: " + e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static byte[] readRawRequest(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int available;

        do {
            available = in.read(chunk);
            if (available > 0)
                buffer.write(chunk, 0, available);
        } while (available == chunk.length || buffer.size() < 4);

        String partial = buffer.toString(StandardCharsets.ISO_8859_1);
        if (partial.toLowerCase().contains("content-length:")) {
            int clIdx = partial.toLowerCase().indexOf("content-length:") + 15;
            int clEnd = partial.indexOf("\r\n", clIdx);
            int contentLength = Integer.parseInt(partial.substring(clIdx, clEnd).trim());
            int headerEnd = partial.indexOf("\r\n\r\n") + 4;
            int bodyRead = buffer.size() - headerEnd;
            while (bodyRead < contentLength) {
                available = in.read(chunk, 0, Math.min(chunk.length, contentLength - bodyRead));
                if (available > 0) {
                    buffer.write(chunk, 0, available);
                    bodyRead += available;
                }
            }
        }
        return buffer.toByteArray();
    }

    private static Map<String, String> parseMultipart(byte[] body, String boundary) throws IOException {
        Map<String, String> result = new HashMap<>();
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        List<byte[]> parts = splitBytes(body, boundaryBytes);
        for (byte[] part : parts) {
            if (part.length < 4)
                continue;
            String partStr = new String(part, StandardCharsets.ISO_8859_1);
            int headerEnd = partStr.indexOf("\r\n\r\n");
            if (headerEnd < 0)
                continue;

            String partHeaders = partStr.substring(0, headerEnd);
            byte[] partBody = Arrays.copyOfRange(part, headerEnd + 4, part.length);
            if (partBody.length >= 2)
                partBody = Arrays.copyOfRange(partBody, 0, partBody.length - 2);

            if (partHeaders.contains("filename=")) {
                String filename = partHeaders.split("filename=\"")[1].split("\"")[0];
                if (!filename.isEmpty()) {
                    String ext = filename.contains(".")
                            ? filename.substring(filename.lastIndexOf("."))
                            : "";
                    String savedName = "img_" + System.currentTimeMillis() + ext;
                    writeFileBytes(new File(IMAGE_DIR, savedName), partBody);
                    result.put("_savedImage", savedName);
                }
            } else if (partHeaders.contains("name=")) {
                String fieldName = partHeaders.split("name=\"")[1].split("\"")[0];
                result.put(fieldName, new String(partBody, StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    private static List<byte[]> splitBytes(byte[] data, byte[] delimiter) {
        List<byte[]> parts = new ArrayList<>();
        int start = 0;
        outer: for (int i = 0; i <= data.length - delimiter.length; i++) {
            for (int j = 0; j < delimiter.length; j++) {
                if (data[i + j] != delimiter[j])
                    continue outer;
            }
            parts.add(Arrays.copyOfRange(data, start, i));
            start = i + delimiter.length;
        }
        parts.add(Arrays.copyOfRange(data, start, data.length));
        return parts;
    }

    private static byte[] readFileBytes(File file) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                result.write(buffer, 0, bytesRead);
            }
        }
        return result.toByteArray();
    }

    private static void writeFileBytes(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    private static String urlDecode(String s) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                String hex = s.substring(i + 1, i + 3);
                result.append((char) Integer.parseInt(hex, 16));
                i += 3;
            } else if (c == '+') {
                result.append(' ');
                i++;
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private static void serveImage(OutputStream out, String filename) throws IOException {
        File file = new File(IMAGE_DIR, filename);
        if (!file.exists()) {
            send404(out);
            return;
        }

        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        String mime;
        if (ext.equals("jpg") || ext.equals("jpeg")) {
            mime = "image/jpeg";
        } else if (ext.equals("png")) {
            mime = "image/png";
        } else if (ext.equals("gif")) {
            mime = "image/gif";
        } else if (ext.equals("webp")) {
            mime = "image/webp";
        } else {
            mime = "application/octet-stream";
        }

        byte[] imageBytes = readFileBytes(file);
        String header = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + mime + "\r\n"
                + "Content-Length: " + imageBytes.length + "\r\n"
                + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(imageBytes);
        out.flush();
    }

    private static void sendHTML(OutputStream out, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    private static void sendRedirect(OutputStream out, String location) throws IOException {
        String response = "HTTP/1.1 302 Found\r\n"
                + "Location: " + location + "\r\n"
                + "Content-Length: 0\r\n"
                + "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void send404(OutputStream out) throws IOException {
        String html = "<h1>404 Not Found</h1>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 404 Not Found\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    private static Map<String, String> decodeForm(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(urlDecode(kv[0]), urlDecode(kv[1]));
            }
        }
        return map;
    }

    private static String buildHomePage() {
        return "<!DOCTYPE html><html><head><title>Appointments</title>"
                + "<style>body{font-family:sans-serif;max-width:600px;margin:40px auto;}"
                + "a{display:block;margin:10px 0;font-size:18px;}"
                + "input,button{padding:8px;margin:5px;font-size:16px;}"
                + "</style></head><body>"
                + "<h1>Appointment Manager</h1>"
                + "<a href='/list'><button>View Appointments</button></a>"
                + "<h2>Add Appointment</h2>"
                + "<form method='POST' action='/add' enctype='multipart/form-data'>"
                + "<input type='text' name='appointment' placeholder='Enter appointment' size='40'>"
                + "<h1> </h1>"
                + "<h2><label for='meeting'>Select time:</label></h2>"
                + "<h1> </h1>"
                + "<select id='hours' name='hours'>"
                + "<option value='00'>00</option>"
                + "<option value='01'>01</option>"
                + "<option value='02'>02</option>"
                + "<option value='03'>03</option>"
                + "<option value='04'>04</option>"
                + "<option value='05'>05</option>"
                + "<option value='06'>06</option>"
                + "<option value='07'>07</option>"
                + "<option value='08'>08</option>"
                + "<option value='09'>09</option>"
                + "<option value='10'>10</option>"
                + "<option value='11'>11</option>"
                + "<option value='12'>12</option>"
                + "</select> : "
                + "<select id='minutes' name='minutes'>"
                + "<option value='00'>00</option>"
                + "<option value='15'>15</option>"
                + "<option value='30'>30</option>"
                + "<option value='45'>45</option>"
                + "</select>"
                + "<select id='ampm' name='ampm'>"
                + "<option value='AM'>AM</option>"
                + "<option value='PM'>PM</option>"
                + "</select>"
                + "<h1> </h1>"
                + "<label>Upload Picture: </label>"
                + "<input type='file' name='image' accept='image/*'>"
                + "<button type='submit'>Add</button>"
                + "</form>"
                + "<h2>Search</h2>"
                + "<form method='GET' action='/search'>"
                + "<input type='text' name='keyword' placeholder='Search keyword' size='30'>"
                + "<button type='submit'>Search</button>"
                + "</form>"
                + "</body></html>";
    }

    private static String buildListPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Appointments</title>"
                + "<style>body{font-family:sans-serif;max-width:600px;margin:40px auto;}"
                + "li{margin:12px 0;}button{padding:4px 8px;}"
                + "img{display:block;max-width:300px;margin-top:6px;border-radius:6px;}</style>"
                + "</head><body><h1>All Appointments</h1>"
                + "<button><a href='/'>Home</a></button><br><br>");

        try {
            List<String> appts = loadAll();
            if (appts.isEmpty()) {
                sb.append("<p>No appointments found.</p>");
            } else {
                sb.append("<ol>");
                for (int i = 0; i < appts.size(); i++) {
                    String[] entry = appts.get(i).split("\\|", 2);
                    String text = entry[0];
                    String image = entry.length > 1 ? entry[1] : "";

                    sb.append("<li><strong>").append(text).append("</strong>");
                    if (!image.isEmpty()) {
                        sb.append("<br><img src='/images/").append(image)
                                .append("' alt='appointment image'>");
                    }
                    sb.append(" <form style='display:inline' method='POST' action='/delete'>"
                            + "<input type='hidden' name='index' value='" + (i + 1) + "'>"
                            + "<button type='submit'>Delete</button></form></li>");
                }
                sb.append("</ol>");
                sb.append("<form method='POST' action='/clear'>"
                        + "<button type='submit' "
                        + "onclick='return confirm(\"Clear all?\")'>Clear All</button></form>");
            }
        } catch (IOException e) {
            sb.append("<p>Error loading appointments.</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String buildSearchPage(String keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Search</title>"
                + "<style>body{font-family:sans-serif;max-width:600px;margin:40px auto;}"
                + "img{max-width:300px;display:block;margin-top:6px;border-radius:6px;}</style>"
                + "</head><body><h1>Search Results for: ").append(keyword).append("</h1>"
                        + "<a href='/'>Home</a><br><br>");

        if (keyword.isEmpty()) {
            sb.append("<p>Please enter a keyword.</p>");
        } else {
            try {
                List<String> list = loadAll();
                String kw = keyword.toLowerCase();
                boolean found = false;
                sb.append("<ol>");
                for (String entry : list) {
                    String[] parts = entry.split("\\|", 2);
                    String text = parts[0];
                    String image = parts.length > 1 ? parts[1] : "";
                    if (text.toLowerCase().contains(kw)) {
                        sb.append("<li><strong>").append(text).append("</strong>");
                        if (!image.isEmpty()) {
                            sb.append("<br><img src='/images/").append(image)
                                    .append("' alt='image'>");
                        }
                        sb.append("</li>");
                        found = true;
                    }
                }
                sb.append("</ol>");
                if (!found)
                    sb.append("<p>No results found.</p>");
            } catch (IOException e) {
                sb.append("<p>Error searching.</p>");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static synchronized List<String> loadAll() throws IOException {
        File file = new File(DB_FILE);
        if (!file.exists())
            return new ArrayList<>();
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null)
                list.add(line);
        }
        return list;
    }

    private static synchronized void saveAll(List<String> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DB_FILE))) {
            for (String appt : list) {
                bw.write(appt);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving: " + e.getMessage());
        }
    }

    private static void addAppointment(String appt) throws IOException {
        List<String> list = loadAll();
        list.add(appt);
        saveAll(list);
    }

    private static boolean deleteAppointment(int idx) throws IOException {
        List<String> list = loadAll();
        if (idx < 1 || idx > list.size())
            return false;
        list.remove(idx - 1);
        saveAll(list);
        return true;
    }

    private static void clearAppointments() throws IOException {
        saveAll(new ArrayList<>());
    }
}