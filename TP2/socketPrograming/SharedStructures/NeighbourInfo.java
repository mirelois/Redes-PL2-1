package SharedStructures;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NeighbourInfo {

    public static class LossInfo {

        public int latestReceivedPacket = 0;
        public int totalReceivedPacket  = 0;
        public double lossRate          = -1;
        public int prevDiff             = 0;
        public int jitter               = -1;

    }

    public static class Node {
        public final InetAddress address;
        public int latency;
        public double lossRate = -1.;
        public int jitter      = -1;


        public Node(InetAddress address, int latency) {
            this.address = address;
            this.latency = latency;
        }

        public Node(InetAddress address, int latency, double lossRate) {
            this.address  = address;
            this.latency  = latency;
            this.lossRate = lossRate;
        }

        public Node(InetAddress address, int latency, double lossRate, int jitter) {
            this.address  = address;
            this.latency  = latency;
            this.lossRate = lossRate;
            this.jitter   = jitter;
        }

        public double getMetrics() { //NOTE: jitter pode ser percentagem de latencia


            if ((this.lossRate < 0) && (this.jitter < 0)) {
                return this.latency;
            } else {

                //extraMetric does not fluctuate the same way
                double extraMetric = 0;

                double jitterVariance = this.latency == 0 ? 0 : this.jitter/this.latency;

                if ((this.lossRate < 0) && (this.jitter >= 0)) {
                    extraMetric = jitterVariance;
                } else if ((this.jitter < 0) && (this.lossRate >= 0))  {
                    extraMetric = this.lossRate/200;
                } else {
                    extraMetric = (Define.extraMetricsDelta)*jitterVariance + (1 - Define.extraMetricsDelta) * this.lossRate/200;
                }

                return (Define.mainDelta)*this.latency + (1 - Define.mainDelta) * (60000 * extraMetric);

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

            Node            s = (Node)o;

            return this.address.equals(s.address);
        }

        @Override
        public int hashCode() {
            return this.address.hashCode();
        }

    }

    public static class StreamInfo {

        public StreamInfo(Integer streamId) {
            this.streamId = streamId;
        }

        //The order of the locks is connected->connecting->disconnecting
        public HashMap<InetAddress, Integer> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente
        //
        public PriorityQueue<Node> minStreamNodeQ = new PriorityQueue<>((a, b) -> {
            if (clientAdjacent.get(a.address) > clientAdjacent.get(b.address)) { return -1; }

            return (a.getMetrics() - b.getMetrics()) > 0 ? 1 : -1;
        });

        public Integer streamId = 255;

        public ReentrantLock connectedLock   = new ReentrantLock();
        public NeighbourInfo.Node connected  = null;
        public ReentrantLock connectingLock  = new ReentrantLock();
        public Condition connectingEmpty     = connectingLock.newCondition();
        public NeighbourInfo.Node connecting = null;

        public Lock disconnectingDeprecatedLock          = new ReentrantLock();
        public Condition disconnectingDeprecatedEmpty    = disconnectingDeprecatedLock.newCondition();
        public HashSet<NeighbourInfo.Node> disconnecting = new HashSet<>();
        public HashSet<NeighbourInfo.Node> deprecated    = new HashSet<>();

        public Thread connectorThread;
        public Thread disconnectorThread;

        public NeighbourInfo.LossInfo lossInfo = new NeighbourInfo.LossInfo();

        public HashSet<NeighbourInfo.Node> getDisconnecting() {
            HashSet<Node>            disconnecting = new HashSet<>();
            disconnecting.addAll(this.disconnecting);
            return disconnecting;
        }

        public HashSet<Node> getDeprecated() {
            HashSet<Node>            deprecated = new HashSet<>();
            deprecated.addAll(this.deprecated);
            return deprecated;
        }

        public Node getConnecting() {
            return new Node(this.connecting.address, this.connecting.latency);
        }

    }

    public int isConnectedToRP = 255;                               // 255 significa que ainda não sabe, 0 no, 1 yes

    public List<InetAddress> overlayNeighbours = new ArrayList<>(); // lista de vizinhos
    public Map<InetAddress, Integer> neighBoursLifePoints = new HashMap<>(); // mapa vizinhos vivos -> life points (de 0 a 5)

    //0 means connection but no stream, 255 means doesn't know, otherwise stream
    public Map<String, Integer> fileNameToStreamId = new HashMap<>(); // filenames to stream id

    public Map<Integer, StreamInfo> streamIdToStreamInfo = new HashMap<>();

    public PriorityQueue<Node> minNodeQueue = new PriorityQueue<>((a, b) -> {
        double result = a.getMetrics() - b.getMetrics();
        if (result == 0) return 0;
        else return result > 0 ? 1 : -1;
    });

    public Map<Integer, Set<InetAddress> > streamActiveLinks = new HashMap<>();         // links para enviar a stream

    public Set<InetAddress> rpRequest = new HashSet<>();                                // vizinhos onde foram enviados Simp

    public Map<String, Set<InetAddress> > streamNameToClientRequests = new HashMap<>(); // vizinhos onde foram enviados Simp

    public Set<InetAddress> rpAdjacent = new HashSet<>();                               // vizinhos que levam ao RP

    public Set<InetAddress> notRpAdjacent = new HashSet<>();                            // vizinhos que não levam ao RP

    public void updateLatency(Node node) {                                              //this method has O(log n) time complexity
        synchronized (this.minNodeQueue) {
            this.minNodeQueue.remove(node);
            this.minNodeQueue.add(node);
        }
    }
}
