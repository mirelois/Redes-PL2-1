import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RPConectionManager implements Runnable{

    ServerInfo serverInfo;

    int streamId;

    String streamName;

    StreamInfo streamInfo;

    public RPConectionManager(ServerInfo serverInfo, int streamId, String streamName){
        
        this.serverInfo = serverInfo;
        this.streamId = streamId;
        this.streamName = streamName;
        this.streamInfo = serverInfo.streamInfo.get(streamId);
        
    }

    public InetAddress chooseBestServer(int streamId) throws UnknownHostException{ // TODO: make this good
        return InetAddress.getLocalHost();
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Define.RPConectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { //main thread only listens for Shrimp it checks who sent the packet

                socket.receive(packet);
                
                Shrimp shrimp = new Shrimp(packet);

                Server server = new Server(shrimp.getAddress(), Packet.getLatency(shrimp.getTimeStamp()));
                
                if(streamInfo.toRemove.contains(server)){ 
                    //recebeu confirmação de remoção, o server
                    //vai parar de mandar cenas
                    streamInfo.toRemove.remove(server);
                    
                    
                }

            }
            
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
}
