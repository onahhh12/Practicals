public class main{
    public static void main(String[] args){
        try{
            while(true){
            System.out.println("Checking new mail...");

            String senderAddress = pop3.readEmail("localhost", "user", "password");

            if (senderAddress != null && !senderAddress.isEmpty()){
                smpt.sendEmail("vaction@localhost", senderAddress);
            }

            System.out.println("Sleeping for 60 seconds...");
            Thread.sleep(60000);
        }
    } catch(Exception e){
        System.out.println("An error occurred: " + e.getMessage());
    }

    }
}