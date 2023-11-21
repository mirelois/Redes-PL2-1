import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeighbourInfo {
    public List<InetAddress> neighbours = new ArrayList<>(); // lista de vizinhos

    public Map<Integer, List<InetAddress>> streamClients = new HashMap<>(); // clients daquela stream

    public Map<InetAddress, List<InetAddress>> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente

    public Map<Integer, List<InetAddress>> streamActiveLinks = new HashMap<>();
}
