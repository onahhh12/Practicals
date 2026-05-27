public class Main {
    public static void main(String[] args) throws Exception {

        FileMonitor monitor = new FileMonitor();

        Client ftp = new Client();
        ftp.connect("localhost", 2121);
        ftp.login("user", "pass");

        while (true) {
            
            FileChange change = monitor.checkForChanges(null);

            if (change != null) {
                System.out.println("Deleted: " + change.action + 
                    " - " + change.fileName +
                    " V" + change.version);
                ftp.uploadFile(change, "ftp_watch");
            }

            Thread.sleep(5000); 
        }
    }
    
}
