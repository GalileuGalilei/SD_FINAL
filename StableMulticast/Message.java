package StableMulticast;

import java.io.*;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L; // Added serialVersionUID for better version control
    String content;
    int[] vectorClock;
    String sender;
    String senderName;

    Message(String content, int[] vectorClock, String sender, String senderName) {
        this.content = content;
        this.vectorClock = vectorClock;
        this.sender = sender;
        this.senderName = senderName;
    }

    byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.close();
        return baos.toByteArray();
    }

    static Message deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Message msg = (Message) ois.readObject();
        ois.close();
        return msg;
    }
}

