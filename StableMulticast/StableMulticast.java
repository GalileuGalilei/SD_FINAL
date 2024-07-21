package StableMulticast;
import java.net.*;
import java.util.*;

import java.io.*;




public class StableMulticast {
    private String ip;
    private int port;
    private IStableMulticast client;
    private DatagramSocket socket;
    private List<InetSocketAddress> groupMembers;
    private Map<String, int[]> vectorClocks;
    private List<Message> buffer;
    private int[] localClock;
    private String localId;

    public StableMulticast(String ip, Integer port, IStableMulticast client) throws Exception {
        this.ip = ip;
        this.port = port;
        this.client = client;
        this.socket = new DatagramSocket();
        this.groupMembers = new ArrayList<>();
        this.vectorClocks = new HashMap<>();
        this.buffer = new ArrayList<>();
        this.localId = InetAddress.getLocalHost().getHostName() + ":" + port;
        this.localClock = new int[256]; // Presumindo um máximo de 256 processos

        // Iniciar a descoberta de grupo e a escuta de mensagens
        new Thread(this::discoverGroup).start();
        new Thread(this::receiveMessages).start();
    }

    public void msend(String msg, IStableMulticast client) throws Exception {
        Message message = new Message(msg, Arrays.copyOf(localClock, localClock.length), localId);
        localClock[getLocalIndex()]++;

        for (InetSocketAddress member : groupMembers) {
            sendUnicast(message, member);
        }
    }

    private void sendUnicast(Message msg, InetSocketAddress member) throws Exception {
        byte[] data = msg.serialize();
        DatagramPacket packet = new DatagramPacket(data, data.length, member.getAddress(), member.getPort());
        socket.send(packet);
    }

    private void receiveMessages() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message msg = Message.deserialize(packet.getData());
                processMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Message msg) {
        int senderIndex = getIndex(msg.sender);

        // Atualizar o relógio vetorial local
        vectorClocks.put(msg.sender, msg.vectorClock);
        localClock[senderIndex]++;

        // Adicionar a mensagem ao buffer
        buffer.add(msg);

        // Entregar mensagens estáveis
        deliverStableMessages();
    }

    private void deliverStableMessages() {
        for (Iterator<Message> iterator = buffer.iterator(); iterator.hasNext();) {
            Message msg = iterator.next();
            boolean stable = true;

            for (int[] clock : vectorClocks.values()) {
                if (clock[getIndex(msg.sender)] < msg.vectorClock[getIndex(msg.sender)]) {
                    stable = false;
                    break;
                }
            }

            if (stable) {
                client.deliver(msg.content);
                iterator.remove();
            }
        }
    }

    private void discoverGroup() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(4446);
            InetAddress group = InetAddress.getByName("224.0.0.1");
            multicastSocket.joinGroup(group);
    
            while (true) {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);
    
                String received = new String(packet.getData(), 0, packet.getLength());
                if (!received.equals(localId)) {
                    groupMembers.add(new InetSocketAddress(packet.getAddress(), packet.getPort()));
                }
    
                // Enviar anúncio da própria presença
                String msg = localId;
                byte[] data = msg.getBytes();
                DatagramPacket announcement = new DatagramPacket(data, data.length, group, 4446);
                multicastSocket.send(announcement);
    
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private int getLocalIndex() {
        return getIndex(localId);
    }

    private int getIndex(String id) {
        return id.hashCode() % 256;
    }

    private static class Message {
        String content;
        int[] vectorClock;
        String sender;

        Message(String content, int[] vectorClock, String sender) {
            this.content = content;
            this.vectorClock = vectorClock;
            this.sender = sender;
        }

        public byte[] serialize() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
            return baos.toByteArray();
        }
        
        public static Message deserialize(byte[] data) throws IOException, ClassNotFoundException {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Message msg = (Message) ois.readObject();
            ois.close();
            return msg;
        }
    }
}