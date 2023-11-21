import java.net.InetAddress;
import java.util.*;

public class NeighbourInfo {
    public List<InetAddress> neighbours = new ArrayList<>(); // lista de vizinhos

    public Map<String, Integer> nameHash = new HashMap<>();

    public Map<Integer, List<InetAddress>> streamClients = new HashMap<>(); // clients daquela stream

    public Map<InetAddress, Set<InetAddress>> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente

    public Map<Integer, List<InetAddress>> streamActiveLinks = new HashMap<>();
}
