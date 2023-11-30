import java.io.ObjectInputStream.GetField;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;



public class ServerInfo { //NOTE: os gajos do java dizem que isto é melhor
    
    static class StreamInfo {

        static class Server {
            
            final InetAddress address;
            int latency;
            
            Server(InetAddress address, int latency){
                this.address =  address;
                this.latency = latency;
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
        }

        public PriorityQueue<Server> minServer = new PriorityQueue<>((a,b) -> a.latency - b.latency);
        public Server currentBestServer;

		public HashSet<Server> disconnecting = new HashSet<Server>();
		public Server connecting;
        public HashSet<Server> deprecatedConnecting = new HashSet<>(); 

        public Integer streamId;

        public Thread connectorThread;
        
        public Thread disconnectorThread;

        public HashSet<Server> getDisconnecting() {
            HashSet<Server> disconnecting = new HashSet<>();
            disconnecting.addAll(this.disconnecting);
            return disconnecting;
        }

        public Server getConnecting() {
            return new Server(this.connecting.address, this.connecting.latency);
        }

        public void updateLatency(Server server){//this method has O(log n) time complexity
            synchronized(this.minServer){
                this.minServer.remove(server);
                this.minServer.add(server);
            }
        }
    }
    
    public Map<Integer, StreamInfo> streamInfo = new HashMap<>();
}

