package StableMulticast;
public interface IStableMulticast {
    void deliver(String msg, String username);
    String getUsername();
    String setUsername(String username);
}
