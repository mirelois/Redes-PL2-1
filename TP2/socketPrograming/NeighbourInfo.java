import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeighbourInfo {
    public List<InetAddress> neighbours = new ArrayList<>(); // lista de vizinhos
    public Map<Integer, List<InetAddress>> streamClients = new HashMap<>();

    public Map<InetAddress, List<InetAddress>> clientAdjacent = new HashMap<>();

    public Map<Integer, List<InetAddress>> streamAdjacent = new HashMap<>();
}
