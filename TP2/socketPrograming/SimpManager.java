import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SimpManager implements Runnable{

    private final int port;

    private final int shrimpPort;

    private final NeighbourInfo neighbourInfo;

    public SimpManager(int myport, int shrimpPort, NeighbourInfo neighbourInfo){
        this.port = myport;
        this.neighbourInfo = neighbourInfo;
        this.shrimpPort = shrimpPort;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(this.port)){
            byte[] buf = new byte[1024]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true){
                socket.receive(packet);

                Simp simp = new Simp(packet);

                InetAddress clientIP = simp.getSourceAddress();

                boolean hasStream;
                synchronized (this.neighbourInfo) {
                    hasStream = this.neighbourInfo.nameHash.containsKey(new String(simp.getPayload()));
                }

                byte[] streamName = simp.getPayload();
                if(hasStream){

                    Set<InetAddress> adjacent;
                    synchronized (this.neighbourInfo) {
                        adjacent = this.neighbourInfo.clientAdjacent.getOrDefault(clientIP, new HashSet<>());
                        if(adjacent.isEmpty()){
                            adjacent.add(simp.getAddress());
                            this.neighbourInfo.clientAdjacent.put(clientIP, adjacent);
                        }
                    }
                    socket.send(new Shrimp(clientIP, 0, this.shrimpPort, simp.getAddress(), streamName.length, streamName).toDatagramPacket());
                }else{
                    socket.send(new Simp(clientIP, simp.getAddress(), this.port, simp.getPayloadSize(), simp.getPayload()).toDatagramPacket());
                }
            }

        } catch (SocketException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
