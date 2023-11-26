import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ServerInfo {
    public Map<Integer, InetAddress> providers = new HashMap<>();
    // public HashMap<InetAddress, Integer> latencyMap;
    // public TreeMap((a, b) -> latencyMap.get(a) - latencyMap.get(b);) latencyMap;
}

