import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class RP implements Runnable {

    int port, shrimpPort, serverPort, next_stream = 1;

    ServerInfo serverInfo;

    private final NeighbourInfo neighbourInfo;

    public RP(int port, int shrimpPort, int serverPort, ServerInfo serverInfo, NeighbourInfo neighbourInfo) {
        this.port = port;
        this.shrimpPort = shrimpPort;
        this.serverPort = serverPort;
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
        this.neighbourInfo.connectionToRP = 1;
    }

    private InetAddress chooseBestServer(ServerInfo serverInfo){
        synchronized(serverInfo){
            return serverInfo.servers.get(0);
        }
    }

    @Override
    public void run() {

        while (true) {
            try (DatagramSocket socket = new DatagramSocket(this.port)) {

                byte[] buf = new byte[1024];

                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);

                Simp simp = new Simp(packet);
                //TODO tratar isto com shrimps

                System.out.println("Recebido SIMP de " + simp.getAddress().getHostAddress());
                Integer streamId;
                InetAddress clientIP = simp.getSourceAddress();
                String streamName = simp.getPayload().toString();
                Set<InetAddress> clientAdjacent, streamActiveLinks, streamClients;

                synchronized(this.neighbourInfo) {
                    streamId = this.neighbourInfo.nameHash.get(streamName);
                    if (streamId == null) {
                        this.neighbourInfo.nameHash.put(streamName, next_stream);
                    }
                }
                
                if (streamId == null) {
                    System.out.println("Nova Stream Pedida, dar novo ID");
                    streamId = next_stream;
                    next_stream++;
                    Shrimp serverShrimp = new Shrimp(InetAddress.getByName("localhost"), next_stream, this.serverPort, chooseBestServer(serverInfo), simp.getPayloadSize(), simp.getPayload());

                    socket.send(serverShrimp.toDatagramPacket());
                }
                
                synchronized(this.neighbourInfo) {
                    //Colocar o nome da Stream associado ao seu Id
                    clientAdjacent = this.neighbourInfo.clientAdjacent.get(clientIP);
                    streamClients = this.neighbourInfo.streamClients.get(streamId);
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(streamId);
                    
                    //Criar novas estruturas
                    if(clientAdjacent == null){
                        clientAdjacent = new HashSet<>();
                        this.neighbourInfo.clientAdjacent.put(clientIP, clientAdjacent);
                    }
                    if(streamActiveLinks == null) {
                        streamActiveLinks = new HashSet<>();
                        this.neighbourInfo.streamActiveLinks.put(streamId, streamActiveLinks);
                    }
                    if(streamClients == null){
                        streamClients = new HashSet<>();
                        this.neighbourInfo.streamClients.put(streamId, streamClients);
                    }
                    
                    //Adicionar caminho para Cliente
                    System.out.println("Adicionado caminho " + simp.getAddress().getHostAddress() + " para " + simp.getSourceAddress().getHostAddress());
                    clientAdjacent.add(simp.getAddress());

                    //TODO decidir qual dos links está ativo é dinâmico
                    //Se o cliente é novo, colocar o primeiro link para CLiente como o primeiro link
                    if (!streamClients.contains(clientIP)) 
                        streamActiveLinks.add(clientAdjacent.iterator().next());

                    //Adicionar Cliente à Stream (Porque existe!)
                    streamClients.add(clientIP);
                }

                Shrimp shrimp = new Shrimp(simp.getSourceAddress(), streamId, this.shrimpPort, simp.getAddress(), simp.getPayloadSize(), simp.getPayload());
                socket.send(shrimp.toDatagramPacket());

            } catch (Exception e) {
                // TODO: handle exception
            }
        }

    }

}
