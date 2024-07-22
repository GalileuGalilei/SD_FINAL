import java.util.Scanner;
import StableMulticast.*;

public class StableMulticastTest implements IStableMulticast {

    private StableMulticast multicast;

    public StableMulticastTest(String ip, int port) throws Exception {
        multicast = new StableMulticast(ip, port, this);
    }

    @Override
    public void deliver(String msg) {
        System.out.println("Received message: " + msg);
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

            StableMulticastTest user = new StableMulticastTest(ip, port);

            System.out.println("StableMulticast user started.");
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
