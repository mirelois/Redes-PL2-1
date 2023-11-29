import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;

class Server {
    InetAddress address;
    int latency;
    Server(InetAddress address, int latency){
        this.address =  address;
        this.latency = latency;
    }
}

class StreamInfo {
    public PriorityQueue<Server> minServer;
    public Server currentBestServer;
    public HashSet<Server> toRemove = new HashSet<Server>();
    public Server toAdd;
}

public class ServerInfo {
    public Map<Integer, StreamInfo> streamInfo = new HashMap<>();
}

