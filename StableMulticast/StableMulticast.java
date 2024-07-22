package StableMulticast;

import java.net.*;
import java.io.*;
import java.util.*;

public class StableMulticast {
    private String ip;
    private int port;
    private IStableMulticast client;
    private DatagramSocket socket;
    private List<InetSocketAddress> groupMembers;
    private Map<String, int[]> vectorClocks;
    private Map<String, List<Message>> buffer;
    private int[] localClock;
    private String localId;

    //group
    private String multicastAddress = "230.0.0.0";
    private int multicastPort = 4446;

    public StableMulticast(String ip, Integer port, IStableMulticast client) throws Exception {
        this.ip = ip;
        this.port = port;
        this.client = client;
        this.socket = new DatagramSocket(port);
        this.groupMembers = new ArrayList<>();
        this.vectorClocks = new HashMap<>();
        this.buffer = new HashMap<>();
        this.localId = InetAddress.getLocalHost().getHostName() + ":" + port;
        this.localClock = new int[8]; // Presumindo um máximo de 8 processos

        // Iniciar a descoberta de grupo e a escuta de mensagens
        new Thread(this::discoverGroup).start();
        new Thread(this::receiveMessages).start();
    }

    public void msend(String msg, IStableMulticast client) throws Exception {
        Message message = new Message(msg, Arrays.copyOf(localClock, localClock.length), localId);
        localClock[getLocalIndex()]++;

        System.out.println("Send this message to all members of the group ? (Y/N)");
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.nextLine();
        while(!answer.equals("Y") && !answer.equals("N")){
            System.out.println("Invalid answer, please type Y or N");
            answer = scanner.nextLine();
        }

        if(answer.contains("Y"))
        {
            sendToAll(message);
        }
        else
        {
            sendOneByOne(message);
        }
    }

    private void sendToAll(Message msg) throws Exception {
        for (InetSocketAddress member : groupMembers) {
            sendUnicast(msg, member);
        }
    }

    private void sendOneByOne(Message msg) throws Exception {
        Scanner scanner = new Scanner(System.in);
        for (InetSocketAddress member : groupMembers) {
            System.out.println("Send this message to " + member + " now ? (Y)");
            String answer = scanner.nextLine();
            while(!answer.equals("Y")){
                System.out.println("Invalid answer, please type Y");
                answer = scanner.nextLine();
            }

            if(answer.contains("Y")){
                sendUnicast(msg, member);
            }
        }
    }

    private void sendUnicast(Message msg, InetSocketAddress member) throws Exception {
        byte[] data = msg.serialize();
        DatagramPacket packet = new DatagramPacket(data, data.length, member.getAddress(), member.getPort());
        socket.send(packet);
    }
    //multicastSocket.receive(packet);
    //String received = new String(packet.getData(), 0, packet.getLength());
    private void receiveMessages() {
        try {
            while (true) {
                byte[] buffer = new byte[2048]; // Increased buffer size
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Only read the actual data length from the packet
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

                Message msg = Message.deserialize(data);
                processMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Message msg) {
        String sender = msg.sender;
        int senderIndex = getIndex(sender);
    
        // Update the local vector clock for the sender
        vectorClocks.put(sender, msg.vectorClock);
        localClock[senderIndex]++;
    
        // Add the message to the buffer
        buffer.computeIfAbsent(sender, k -> new ArrayList<>()).add(msg);
    
        // Deliver stable messages
        deliverStableMessages();
    
        // Debugging output
        PrintClocks();
        PrintBuffer();
    }

    private void PrintClocks() 
    {
        for (Map.Entry<String, int[]> entry : vectorClocks.entrySet()) 
        {
            System.out.println(entry.getKey() + " : " + Arrays.toString(entry.getValue()));
        }
    }

    private void PrintBuffer() 
    {
        for (Map.Entry<String, List<Message>> entry : buffer.entrySet()) 
        {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    private void deliverStableMessages() {
        for (Map.Entry<String, List<Message>> entry : buffer.entrySet()) {
            String sender = entry.getKey();
            List<Message> messages = entry.getValue();
            int senderIndex = getIndex(sender);
    
            Iterator<Message> iterator = messages.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
    
                boolean stable = true;
                for (int[] clock : vectorClocks.values()) {
                    if (clock[senderIndex] < message.vectorClock[senderIndex]) {
                        stable = false;
                        break;
                    }
                }
    
                if (stable) 
                {
                    //deixa isso aqui por enquanto para debug
                    client.deliver(message.content);
                    // Remove the message from the buffer
                    iterator.remove();
                }
            }
        }
    }
    


      //  for (Iterator<Message> iterator = buffer.iterator(); iterator.hasNext();) {
    //        Message msg = iterator.next();
  //          boolean stable = true;
//
            //for (int[] clock : vectorClocks.values()) {
                //if (clock[getIndex(msg.sender)] < msg.vectorClock[getIndex(msg.sender)]) {
                  //  stable = false;
                  //  break;
                //}
            //}

            //if (stable) {
              //  client.deliver(msg.content);
            //    iterator.remove();
          //  }
        //}

    private void discoverGroup() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
            InetAddress group = InetAddress.getByName(multicastAddress);
            multicastSocket.joinGroup(group);

            // Thread para receber anúncios multicast
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(packet);

                        String received = new String(packet.getData(), 0, packet.getLength());
                        if (!received.equals(localId)) {
                            String[] parts = received.split(":");
                            String host = parts[0];
                            int port = Integer.parseInt(parts[1]);
                            InetSocketAddress member = new InetSocketAddress(InetAddress.getByName(host), port);

                            if (!groupMembers.contains(member)) {
                                groupMembers.add(member);
                                System.out.println("Discovered member: " + member);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Thread para enviar anúncios multicast
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = localId;
                        byte[] data = msg.getBytes();
                        DatagramPacket announcement = new DatagramPacket(data, data.length, group, multicastPort);
                        multicastSocket.send(announcement);
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getLocalIndex() 
    {
        return getIndex(localId);
    }

    private int getIndex(String id) 
    {
        int hash = (id.hashCode() % 8);
        return hash < 0 ? hash * -1 : hash;
    }
}
