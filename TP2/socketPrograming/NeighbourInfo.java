import java.net.InetAddress;
import java.util.*;

public class NeighbourInfo {

    static class StreamInfo {

        static class Node {

            final InetAddress address;
            int latency;

            Node(InetAddress address, int latency) {
                this.address = address;
                this.latency = latency;
            }

            @Override
            public boolean equals(Object o) {

                if (o == this) {
                    return true;
                }

                /*
                 * Check if o is an instance of Complex or not
                 * "null instanceof [type]" also returns false
                 */
                if (!(o instanceof Node)) {
                    return false;
                }

                Node s = (Node) o;

                return this.address.equals(s.address);
            }

            @Override
            public int hashCode() {
                return this.address.hashCode();
            }
        }

        public Node connecting;
        
        public HashSet<Node> disconnecting = new HashSet<>();
        public HashSet<Node> deprecated    = new HashSet<>();

        public Thread connectorThread;
        public Thread disconnectorThread;
        
        public HashSet<Node> getDisconnecting() {
            HashSet<Node> disconnecting = new HashSet<>();
            disconnecting.addAll(this.disconnecting);
            return disconnecting;
        }
        
        public HashSet<Node> getDeprecated() {
            HashSet<Node> deprecated = new HashSet<>();
            deprecated.addAll(this.deprecated);
            return deprecated;
        }

        public Node getConnecting() {
            return new Node(this.connecting.address, this.connecting.latency);
        }

    }

    int isConnectedToRP = 255; // 255 significa que ainda não sabe, 0 no, 1 yes

    public List<InetAddress> neighbours = new ArrayList<>(); // lista de vizinhos

    // Uma stream só existe se estiver neste mapa
    public Map<String, Integer> fileNameToStreamId = new HashMap<>(); // nomes de ficheiros para streams

    // public Map<Integer, Set<InetAddress>> streamClients = new HashMap<>(); //
    // clients daquela stream

    public Map<Integer, Set<InetAddress>> streamActiveLinks = new HashMap<>(); // links para enviar a stream

    Set<InetAddress> rpRequest = new HashSet<>(); // vizinhos onde foram enviados Simp

    Set<InetAddress> rpAdjacent = new HashSet<>(); // vizinhos que levam ao RP

    public Map<InetAddress, Set<InetAddress>> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente
                                                                                
    public Map<Integer, StreamInfo> streamInfo = new HashMap<>();
}
