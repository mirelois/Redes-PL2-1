import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set ;

public class RP implements Runnable {

    int port, serverPort, next_stream = 1;

    ServerInfo serverInfo;

    private final NeighbourInfo neighbourInfo;

    public RP(ServerInfo serverInfo, NeighbourInfo neighbourInfo) {
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
        this.neighbourInfo.connectionToRP = 1;
    }

    @Override
    public void run() {

        while (true) {
            try (DatagramSocket socket = new DatagramSocket(Define.RPPort)) {

                byte[] buf = new byte[Define.infoBuffer];

                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);

                Simp simp = new Simp(packet); // from client

                socket.send(new Rip(0, simp.getAddress(), simp.getPort()).toDatagramPacket());

                System.out.println("Recebido SIMP de " + simp.getAddress().getHostAddress() +
                                   " Pede Stream " + new String(simp.getPayload()));
                
                Integer streamId;
                InetAddress clientIP = simp.getSourceAddress();
                String streamName = new String(simp.getPayload());
                Set<InetAddress> clientAdjacent, streamActiveLinks, streamClients;

                synchronized(this.neighbourInfo) {
                    streamId = this.neighbourInfo.nameHash.get(streamName);
                    if (streamId == null) {
                        this.neighbourInfo.nameHash.put(streamName, next_stream);
                        streamId = next_stream;
                        next_stream++;
                    }
                }
                
                if (this.serverInfo.providers.get(streamId) == null) {
                    
                    Thread t = new Thread(new RPConectionManager(serverInfo, streamId, streamName));
                    t.start();
                    
                    // System.out.println("Pedido de stream enviado ao servidor " + chooseBestServer(serverInfo) +
                                       // " com payload " + new String(simp.getPayload()));
            }

                Shrimp shrimp = new Shrimp(
                    Packet.getCurrTime(),
                    simp.getSourceAddress(),
                    streamId,
                    Define.shrimpPort,
                    simp.getAddress(),
                    simp.getPayloadSize(),
                    simp.getPayload()
                );
                socket.send(shrimp.toDatagramPacket());
                System.out.println("Enviado SHRIMP para " + simp.getAddress().getHostAddress() +
                                               " da stream com id " + streamId +
                                               " pedida por " + clientIP.getHostAddress());
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

    }

}
