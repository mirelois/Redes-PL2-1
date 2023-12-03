import java.net.InetAddress;
import java.util.*;

public class NeighbourInfo {
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

    static class StreamInfo {

        public NeighbourInfo.Node connecting;

        public NeighbourInfo.Node connected;

        public HashSet<NeighbourInfo.Node> disconnecting = new HashSet<>();
        public HashSet<NeighbourInfo.Node> deprecated = new HashSet<>();

        public Thread connectorThread;
        public Thread disconnectorThread;

        public HashSet<NeighbourInfo.Node> getDisconnecting() {
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

    public List<InetAddress> overlayNeighbours = new ArrayList<>(); // lista de vizinhos
    public List<InetAddress> activeNeighbours = new ArrayList<>(); // lista de vizinhos vivos

    public Map<String, Integer> fileNameToStreamId = new HashMap<>(); // filenames to stream id

    public Map<Integer, StreamInfo> streamIdToStreamInfo = new HashMap<>();

    public PriorityQueue<Node> minNodeQueue = new PriorityQueue<>((a, b) -> a.latency - b.latency);

    public Map<Integer, Set<InetAddress>> streamActiveLinks = new HashMap<>(); // links para enviar a stream

    public Map<InetAddress, Set<InetAddress>> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente

    Set<InetAddress> rpRequest = new HashSet<>(); // vizinhos onde foram enviados Simp

    Set<InetAddress> rpAdjacent = new HashSet<>(); // vizinhos que levam ao RP
                             
    public void updateLatency(Node node) { //this method has O(log n) time complexity
        synchronized(this.minNodeQueue){
            this.minNodeQueue.remove(node);
            this.minNodeQueue.add(node);
        }
    }
}
