package Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import Protocols.Link;
import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Rip;
import Protocols.Shrimp;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;

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

                socket.send(new Rip(0, packet.getAddress(), Define.ripPort).toDatagramPacket());
                
                Set<InetAddress> rpAdjacent;
                NeighbourInfo.StreamInfo streamInfo;

                //Retirar streamId do pacote
                Integer streamId = shrimp.getStreamId();

                synchronized(this.neighbourInfo) {
                    //Remover pedido feito por Simp
                    this.neighbourInfo.rpRequest.remove(shrimp.getAddress());
                    
                    //Colocar o nome da Stream associado ao seu Id
                    streamInfo = this.neighbourInfo.streamIdToStreamInfo.get(streamId);
                    
                    //Existe Stream (conseguiu caminho até ao RP)
                    if (streamId != 0 && streamId != 255) {
                        System.out.println("Adicionado novo caminho para o RP: " + shrimp.getAddress());
                        this.neighbourInfo.isConnectedToRP = 1;

                        this.neighbourInfo.fileNameToStreamId.put(new String(shrimp.getPayload()), streamId);
                        rpAdjacent = this.neighbourInfo.rpAdjacent;

                        this.neighbourInfo.minNodeQueue.add(new NeighbourInfo.Node(shrimp.getAddress(), 
                                                            Packet.getLatency(shrimp.getTimeStamp())));

                        //Adicionar Novo Caminho para a Stream (Porque existe!)
                        rpAdjacent.add(shrimp.getAddress());

                        try {
                            if (clientIP.isAnyLocalAddress() ||
                                clientIP.isLoopbackAddress() || 
                                NetworkInterface.getByInetAddress(clientIP) != null) {
                                //TODO Avisar o Cliente anexo ao nodo de que a Stream chegou (ou não)!
                                System.out.println("    Shrimp veio para Cliente local - Enviado Link para local");
                                socket.send(new Link(false, 
                                                    true,
                                                    false,
                                                    shrimp.getStreamId(),
                                                    InetAddress.getByName("localhost"),
                                                    Define.nodeConnectionManagerPort,
                                                    0,
                                                    null).toDatagramPacket());     
                            }
                        } catch (SocketException e) {

                        }

                        //Avisar todos os caminhos de todo Cliente que pediu a Stream de que há Stream
                        for (InetAddress linkToClient : streamInfo.clientAdjacent.keySet()) {
                            if (!linkToClient.equals(InetAddress.getByName("localhost"))) {
                                System.out.println("Enviado SHRIMP para " + linkToClient.getHostAddress() +
                                                   " da stream com id " + streamId +
                                                   " pedida por " + clientIP.getHostAddress());
                                socket.send(new Shrimp(shrimp.getTimeStamp(), clientIP, streamId, shrimp.getPort(), 
                                                       linkToClient, shrimp.getPayloadSize(), shrimp.getPayload()).toDatagramPacket());
                            }
                        }

                    //Não existe stream/não tenho caminho até ao RP
                    } else if (streamId == 0) {
                        synchronized (neighbourInfo) {

                            if (this.neighbourInfo.rpRequest.isEmpty() && this.neighbourInfo.rpAdjacent.isEmpty()) {
                                
                                this.neighbourInfo.fileNameToStreamId.put(new String(shrimp.getPayload()), streamId);
                                System.out.println("Não existe conexão!");
                    
                                this.neighbourInfo.isConnectedToRP = 0; 
                                //Avisar todos os Clientes da falta de conexão
                                for (InetAddress linkToClient : streamInfo.clientAdjacent.keySet()) {
                                    socket.send(new Shrimp(shrimp.getTimeStamp(), clientIP, streamId, shrimp.getPort(), linkToClient, 
                                                shrimp.getPayloadSize(), shrimp.getPayload()).toDatagramPacket());
                                    streamInfo.clientAdjacent.remove(linkToClient);
                                }

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
