import java.util.Scanner;
import StableMulticast.*;

public class StableMulticastTest implements IStableMulticast {

    private StableMulticast multicast;
    private String username;

    public StableMulticastTest(String ip, int port, String username) throws Exception {
        multicast = new StableMulticast(ip, port, username, this);
    }

    @Override
    public void deliver(String msg, String username) {
        System.out.println(username + " Sent: " + msg);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String setUsername(String username) {
        this.username = username;
        return username;
    }

    public void sendMessage(String msg) throws Exception {
        multicast.msend(msg, this);
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            // Configuração do IP e porta do usuário
            System.out.print("Enter your IP address: ");
            String ip = scanner.nextLine();

            System.out.print("Enter your port: ");
            int port = Integer.parseInt(scanner.nextLine());

            System.out.println("Enter your username: ");
            String username = scanner.nextLine();

            StableMulticastTest user = new StableMulticastTest(ip, port, username);

            System.out.println("StableMulticast user started.");
            System.out.println("To leave the chat, type 'exit'");
            System.out.println("Type your messages below:");

            // Loop para enviar mensagens
            while (true) {
                if(scanner.hasNextLine()) 
                {
                    String msg = scanner.nextLine();
                    user.sendMessage(msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
