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
    @Override
    public boolean equals(Object o) {
        
        if (o == this) {
            return true;
        }
 
        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Server)) {
            return false;
        }

        Server s = (Server) o;

        return this.address.equals(s.address);
    }
}

class StreamInfo {
    
    public PriorityQueue<Server> minServer = new PriorityQueue<>((a,b) -> a.latency - b.latency);
    public Server currentBestServer;
    public HashSet<Server> disconnecting = new HashSet<Server>();
    public Server connecting;
    
    public void updateLatency(Server server){//this method has O(log n) time complexity
        this.minServer.remove(server);
        this.minServer.add(server);
    }
}

public class ServerInfo {
    public Map<Integer, StreamInfo> streamInfo = new HashMap<>();
}

