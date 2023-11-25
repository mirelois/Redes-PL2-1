import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class ShrimpManager implements Runnable{

    private final NeighbourInfo neighbourInfo;

    public ShrimpManager( NeighbourInfo neighbourInfo){
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(Define.shrimpPort)){
            
            byte[] buf = new byte[Define.infoBuffer]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true){
                socket.receive(packet);

                Shrimp shrimp = new Shrimp(packet);
                InetAddress clientIP = shrimp.getSourceAddress();
                System.out.println( "Recebido SHRIMP de " + clientIP.toString() + 
                                    " vindo de " + shrimp.getAddress().getHostAddress() +
                                    " com streamId " + shrimp.getStreamId());

                synchronized(this.neighbourInfo) {
                    //Remover pedido feito por Simp
                    this.neighbourInfo.rpRequest.remove(shrimp.getAddress());
                }

                try {
                    if (clientIP.isAnyLocalAddress() ||
                        clientIP.isLoopbackAddress() || 
                        NetworkInterface.getByInetAddress(clientIP) != null) {
                        //TODO Avisar o Cliente anexo ao nodo de que a Stream chegou (ou não)!
                        //continue;
                    }
                } catch (SocketException e) {

                }

                Set<InetAddress> clientAdjacent, streamActiveLinks, streamClients, rpAdjacent;
                Integer streamId = shrimp.getStreamId();
                synchronized(this.neighbourInfo) {

                    //Colocar o nome da Stream associado ao seu Id
                    clientAdjacent = this.neighbourInfo.clientAdjacent.get(clientIP);
                    
                    //Existe Stream (conseguiu caminho até ao RP)
                    if (streamId != 0 && streamId != 255) {
                        this.neighbourInfo.connectionToRP = 1;

                        this.neighbourInfo.nameHash.put(new String(shrimp.getPayload()), streamId);
                        streamClients = this.neighbourInfo.streamClients.get(streamId);
                        streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(streamId);
                        rpAdjacent = this.neighbourInfo.rpAdjacent;
                        
                        //Criar novas estruturas
                        if(streamActiveLinks == null) {
                            streamActiveLinks = new HashSet<>();
                            this.neighbourInfo.streamActiveLinks.put(streamId, streamActiveLinks);
                        }
                        if(streamClients == null){
                            streamClients = new HashSet<>();
                            this.neighbourInfo.streamClients.put(streamId, streamClients);
                        }

                        //TODO decidir qual dos links está ativo é dinâmico
                        //Se o cliente é novo, colocar o primeiro link para CLiente como o primeiro link
                        if (!streamClients.contains(clientIP)) 
                            streamActiveLinks.add(clientAdjacent.iterator().next());
                        
                        //Adicionar Cliente à Stream (Porque existe!)
                        streamClients.add(clientIP);

                        //Adicionar Novo Caminho para a Stream (Porque existe!)
                        rpAdjacent.add(shrimp.getAddress());
                        //Avisar todos os caminhos de todo Cliente que pediu a Stream de que há Stream
                        for (InetAddress linkToClient : clientAdjacent) {
                            if (!linkToClient.equals(InetAddress.getByName("localhost"))) {
                                System.out.println("Enviado SHRIMP para " + linkToClient.getHostAddress() +
                                                   " da stream com id " + streamId +
                                                   " pedida por " + clientIP.getHostAddress());
                                socket.send(new Shrimp(shrimp.getTimeStamp(), clientIP, streamId, shrimp.getPort(), linkToClient, shrimp.getPayloadSize(), shrimp.getPayload()).toDatagramPacket());
                            }
                        }

                    } else if (streamId == 0) {
                        synchronized (neighbourInfo) {
                            if (this.neighbourInfo.rpRequest.isEmpty() && this.neighbourInfo.rpAdjacent.isEmpty()) {
                                this.neighbourInfo.nameHash.put(new String(shrimp.getPayload()), streamId);
                                System.out.println("Não existe conexão!");
                                this.neighbourInfo.connectionToRP = 0;
                                //Avisar todos os Clientes da falta de conexão
                                //TODO escolher o melhor link para avisar
                                for (Set<InetAddress> linksToClient : this.neighbourInfo.clientAdjacent.values())
                                    socket.send(new Shrimp(shrimp.getTimeStamp(), clientIP, streamId, shrimp.getPort(), linksToClient.iterator().next(), shrimp.getPayloadSize(), shrimp.getPayload()).toDatagramPacket());
                                
                            }
                        }
                    }
                }
            }

        } catch (SocketException | PacketSizeException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
		}
    }
}
