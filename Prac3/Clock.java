import com.sun.net.httpserver.*; //imporst HTTP server classes
import java.io.*; //input and output classes
import java.net.InetSocketAddress; //class for socket address (IP and port)
import java.time.ZoneId; //class for time zone IDs
import java.time.ZonedDateTime; //class for date and time with time zone
import java.time.format.DateTimeFormatter; //class for formatting date and time

public class Clock {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0); // creates an HTTP server on port 8000
        server.createContext("/", exchange -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String johannesburgTime = ZonedDateTime.now(ZoneId.of("Africa/Johannesburg")).format(formatter);
            String londonTime = ZonedDateTime.now(ZoneId.of("Europe/London")).format(formatter);
            String NYTime = ZonedDateTime.now(ZoneId.of("America/New_York")).format(formatter);
            String tokyoTime = ZonedDateTime.now(ZoneId.of("Asia/Tokyo")).format(formatter);
            String buenosAiresTime = ZonedDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).format(formatter);
            String sydneyTime = ZonedDateTime.now(ZoneId.of("Australia/Sydney")).format(formatter);

            String html = "<html><head><meta http-equiv='REFRESH' content='1'></head><body><h1 style= 'text-align: center;'>World Clock</h1>"
                    + // Heading
                    "<h1 style= 'text-align: center;'> This page displays the current time in different cities around the country </h1>"
                    +
                    "<table style='text-align: center; margin: auto; width: 50%; border: 10px solid blue; border-collapse:collapse;'>"
                    +
                    "<tr>" +
                    "<th style='border: 1px solid black; padding: 10px;'>City</th>" +
                    "<th style='border: 1px solid black; padding: 10px;'>Time</th>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;' ><a href='/city?name=Johannesburg'>Johannesburg</a></td>"
                    +
                    "<td style='border: 1px solid black; padding: 10px;'>" + johannesburgTime + "</td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=London'>London</a></td>" +
                    "<td style='border: 1px solid black; padding: 10px;'>" + londonTime + "</td> " +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=New_York'>New York</a></td>"
                    +
                    "<td style='border: 1px solid black; padding: 10px;'>" + NYTime + "</td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=Tokyo'>Tokyo</a></td>" +
                    "<td style='border: 1px solid black; padding: 10px;'>" + tokyoTime + "</td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=Buenos_Aires'>Buenos Aires</a></td>"
                    +
                    "<td style='border: 1px solid black; padding: 10px;'>" + buenosAiresTime + "</td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=Sydney'>Sydney</a></td>" +
                    "<td>" + sydneyTime + "</td>" +
                    "</tr>" +

                    "</table>" +
                    "</body></html>";

            byte[] response = html.getBytes(); // converts the HTML string to bytes
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        });

        server.createContext("/city", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String cityName = query.split("=")[1]; // extracts the city name from the query parameter
            String timezone;
            String currency;
            String fact;

            if (cityName.equals("Johannesburg")) {
                timezone = "Africa/Johannesburg";
                currency = "South African Rand (ZAR)";
                fact = "Johannesburg is nicknamed the 'City of Gold'!.";
            } else if (cityName.equals("London")) {
                timezone = "Europe/London";
                currency = "British Pound (GBP)";
                fact = "London is the capital and largest city of England and the United Kingdom.";
            } else if (cityName.equals("New_York")) {
                timezone = "America/New_York";
                currency = "US Dollar (USD)";
                fact = "New York is the most populous city in the United States.";
            } else if (cityName.equals("Tokyo")) {
                timezone = "Asia/Tokyo";
                currency = "Japanese Yen (JPY)";
                fact = "Tokyo is the capital and largest city of Japan.";
            } else if (cityName.equals("Buenos_Aires")) {
                timezone = "America/Argentina/Buenos_Aires";
                currency = "Argentine Peso (ARS)";
                fact = "Buenos Aires is the capital and largest city of Argentina.";
            } else if (cityName.equals("Sydney")) {
                timezone = "Australia/Sydney";
                currency = "Australian Dollar (AUD)";
                fact = "Sydney is the largest city in Australia.";
            } else {
                timezone = "Africa/Johannesburg"; // default timezone
                currency = "South African Rand (ZAR)"; // default currency
                fact = "Johannesburg is nicknamed the 'City of Gold'!."; //
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String cityFormattedTime = ZonedDateTime.now(ZoneId.of(timezone)).format(formatter);
            String saFormattedTime = ZonedDateTime.now(ZoneId.of("Africa/Johannesburg")).format(formatter);

            String html = "<html><head><meta http-equiv='REFRESH' content='1'></head><body>" +
                    "<h1 style= 'text-align: center; color: #333;'>Local Time: </h1>" +
                    "<h3 style='text-align: center;'>" + saFormattedTime + "</h3>" +
                    "<h1 style= 'text-align: center;'>City: " + cityName + "</h1>" +
                    "<h3 style= 'text-align: center;'>Time: " + cityFormattedTime + "</h3>" +

                    "<section style='width:50%; margin: 20px auto; padding:20px; border:2px solid #c1cee1;'>" +
                    "<h2 style='text-align: center;'>Details about " + cityName + "</h2>" +
                    "<p style='padding: 5px;'>Currency: " + currency + "</p>" +
                    "<p>Fun Fact: " + fact + "</p>" +
                    "</section>" +

                    "<table style='text-align: center; margin: 20px auto; width: 50%; border: 10px solid blue; border-collapse:collapse;'>"
                    +
                    "<tr>" +
                    "<th style='border: 1px solid black; padding: 10px;'>City</th>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;' ><a href='/city?name=Johannesburg'>Johannesburg</a></td>"
                    +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=London'>London</a></td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=New_York'>New York</a></td>"
                    +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=Tokyo'>Tokyo</a></td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=Buenos_Aires'>Buenos Aires</a></td>"
                    +
                    "</tr>" +

                    "<tr>" +
                    "<td style='border: 1px solid black; padding: 10px;'><a href='/city?name=Sydney'>Sydney</a></td>" +
                    "</tr>" +

                    "</table>" +
                    "<a href='/' style='display: block; width: 150px; margin: 20px auto; padding: 10px; text-align: center; background-color: blue; color: white; border-radius: 5px; text-decoration: none;'>Go Back</a>"
                    +
                    "</body></html>";

            byte[] response = html.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();

        });

        server.start();
        System.out.println("Server running in http://localhost:8000/");

    }
}