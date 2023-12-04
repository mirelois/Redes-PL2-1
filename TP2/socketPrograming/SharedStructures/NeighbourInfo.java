package SharedStructures;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NeighbourInfo {
    
    public static class LossInfo {
        
        public int latestReceivedPacket = 0;
        public int totalReceivedPacket = 0;
        public double lossRate = -1;
        public int prevDiff = 0;
        public double jitter = -1;
        
    }

    public static class Node {
        public final InetAddress address;
        public int latency;
        public double lossRate = -1.;


        public Node(InetAddress address, int latency) {
            this.address = address;
            this.latency = latency;
        }

        public Node(InetAddress address, int latency, Double lossRate) {
            this.address = address;
            this.latency = latency;
            this.lossRate = lossRate;
        }

        public double getMetrics(){//NOTE: isto é de total responsabilidade do Lucena
            
            if (this.lossRate < 0){
                return this.latency;
            }else{
                return (0.45 * this.latency + 0.55 * this.lossRate*600);
            }
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
    
    public static class StreamInfo {
        
        //The order of the locks is connected->connecting->disconnecting
        public HashMap<InetAddress, Integer> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente
        public PriorityQueue<Node> minStreamNodeQ = new PriorityQueue<>((a, b) -> {
            if(clientAdjacent.get(a.address) > clientAdjacent.get(b.address)) return -1;

            return (a.getMetrics() - b.getMetrics()) > 0 ? 1 : -1;
        });
        public ReentrantLock connectedLock = new ReentrantLock();
        public NeighbourInfo.Node connected = null;
        public ReentrantLock connectingLock = new ReentrantLock();
        public Condition connectingEmpty = connectingLock.newCondition();
		public NeighbourInfo.Node connecting = null;

        public Lock disconnectingDeprecatedLock = new ReentrantLock();
        public Condition disconnectingDeprecatedEmpty = disconnectingDeprecatedLock.newCondition();
        public HashSet<NeighbourInfo.Node> disconnecting = new HashSet<>();
        public HashSet<NeighbourInfo.Node> deprecated = new HashSet<>();

        public Thread connectorThread;
        public Thread disconnectorThread;
    
        public NeighbourInfo.LossInfo lossInfo = new NeighbourInfo.LossInfo();
        
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
    
    public int isConnectedToRP = 255; // 255 significa que ainda não sabe, 0 no, 1 yes
    
    public List<InetAddress> overlayNeighbours = new ArrayList<>(); // lista de vizinhos
    public List<InetAddress> activeNeighbours = new ArrayList<>(); // lista de vizinhos vivos
    
    public Map<String, Integer> fileNameToStreamId = new HashMap<>(); // filenames to stream id
    
    public Map<Integer, StreamInfo> streamIdToStreamInfo = new HashMap<>();
    
    public PriorityQueue<Node> minNodeQueue = new PriorityQueue<>((a, b) -> (a.getMetrics() - b.getMetrics()) > 0 ? 1 : -1);
    
    public Map<Integer, Set<InetAddress>> streamActiveLinks = new HashMap<>(); // links para enviar a stream
    
    public Set<InetAddress> rpRequest = new HashSet<>(); // vizinhos onde foram enviados Simp

    public Set<InetAddress> clientRequest = new HashSet<>(); // vizinhos onde foram enviados Simp

    public Set<InetAddress> rpAdjacent = new HashSet<>(); // vizinhos que levam ao RP
    
    public void updateLatency(Node node) { //this method has O(log n) time complexity
        synchronized(this.minNodeQueue){
            this.minNodeQueue.remove(node);
            this.minNodeQueue.add(node);
        }
    }
}
