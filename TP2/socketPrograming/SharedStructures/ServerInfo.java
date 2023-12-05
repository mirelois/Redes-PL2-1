package SharedStructures;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class ServerInfo { //NOTE: os gajos do java dizem que isto é melhor
    
    public static class StreamInfo {

        public static class Server {
            
            public final InetAddress address;
            public int latency;
            public double lossRate = -1.;
            public int jitter = -1;
            
            public Server(InetAddress address, int latency){
                this.address =  address;
                this.latency = latency;
                
            }

            public Server(InetAddress address, int latency, Double lossRate){
                this.address =  address;
                this.latency = latency;
                this.lossRate = lossRate;
            }
            
            public Server(InetAddress address, int latency, double lossRate, int jitter) {
                this.address = address;
                this.latency = latency;
                this.lossRate = lossRate;
                this.jitter = jitter;
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

            @Override
            public int hashCode() {
                return this.address.hashCode();
            }

            public double getMetrics(){
                
                if (this.lossRate < 0 && this.jitter < 0){
                    return this.latency;
                }else if (this.jitter < 0){
                    return 0.45 * this.latency + 0.55 * this.lossRate;
                }else if (this.lossRate < 0){
                    return 0.45 * this.latency + 0.55 * this.jitter;
                }else {
                    return 0.33 * this.latency + 0.33 * this.jitter + 0.34 * this.lossRate;
                }
            }
        }
                
        public NeighbourInfo.LossInfo lossInfo = new NeighbourInfo.LossInfo();

        public PriorityQueue<Server> minServerQueue = new PriorityQueue<>((a,b) -> a.latency - b.latency);
        public Integer activeServerLatency = Integer.MAX_VALUE;
        
        //locks for altering the connecting and connected variable
        //The order of the locks is connected->connecting->disconnecting
        public ReentrantLock connectedLock = new ReentrantLock();
        public Server connected;
        public ReentrantLock connectingLock = new ReentrantLock();
        public Condition connectingEmpty = connectingLock.newCondition();
		public Server connecting;

        public Lock disconnectingDeprecatedLock = new ReentrantLock();
        public Condition disconnectingDeprecatedEmpty = disconnectingDeprecatedLock.newCondition();
        public HashSet<Server> disconnecting = new HashSet<>();
        public HashSet<Server> deprecated    = new HashSet<>();

        public Integer streamId;

        public Thread connectorThread;
        
        public Thread disconnectorThread;

        public HashMap<InetAddress, Integer> clientAdjacent = new HashMap<>(); // vizinhos que levam ao cliente

        public StreamInfo(Integer streamId) {
            this.streamId = streamId;
        }

        public HashSet<Server> getDisconnecting() {
            HashSet<Server> disconnecting = new HashSet<>();
            disconnecting.addAll(this.disconnecting);
            return disconnecting;
        }
        
        public HashSet<Server> getDeprecated() {
            HashSet<Server> deprecated = new HashSet<>();
            deprecated.addAll(this.deprecated);
            return deprecated;
        }

        public Server getConnecting() {
            return new Server(this.connecting.address, this.connecting.latency);
        }

        public void updateLatency(Server server){//this method has O(log n) time complexity
            synchronized(this.minServerQueue){
                this.minServerQueue.remove(server);
                this.minServerQueue.add(server);
            }
        }

           
    }
    
    public Map<Integer, StreamInfo> streamInfoMap = new HashMap<>();
}

