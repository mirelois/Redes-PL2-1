import java.net.InetAddress;
import java.util.*;

public class NeighbourInfo {

    int connectionToRP = -1;

    public List<InetAddress> neighbours = new ArrayList<>(); // lista de vizinhos

    //Uma stream só existe se estiver neste mapa
    public Map<String, Integer> nameHash = new HashMap<>(); // nomes de ficheiros para streams

    public Map<Integer, Set<InetAddress>> streamClients = new HashMap<>(); // clients daquela stream

    public Map<Integer, Set<InetAddress>> streamActiveLinks = new HashMap<>(); // links para enviar a stream
    
    Set<InetAddress> rpRequest = new HashSet<>(); // vizinhos onde foram enviados Simp

    Set<InetAddress> rpAdjacent = new HashSet<>(); // vizinhos que levam ao RP
    
    public Map<InetAddress, Set<InetAddress>> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente
}
