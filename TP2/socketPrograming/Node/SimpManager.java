package Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Shrimp;
import Protocols.Simp;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.NeighbourInfo.StreamInfo;

public class SimpManager implements Runnable{

    private final NeighbourInfo neighbourInfo;

    public SimpManager(NeighbourInfo neighbourInfo){
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){
    try(DatagramSocket socket = new DatagramSocket(Define.simpPort)){
            byte[] buf = new byte[Define.infoBuffer]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true){
                socket.receive(packet);
                Simp simp = new Simp(packet);
                System.out.println("Recebeu Pedido de Stream de " + simp.getAddress().toString() +
                                   " com nome: " + new String(simp.getPayload()));

                InetAddress clientIP = simp.getSourceAddress();
                Integer streamId;
                Set<InetAddress> clientRequestStreamSet;
                String streamName = new String(simp.getPayload());
                byte[] streamNameBytes = simp.getPayload();

                synchronized (this.neighbourInfo) {
                    
                    streamId = this.neighbourInfo.fileNameToStreamId.get(new String(simp.getPayload()));
                    System.out.println("    StreamId do ficheiro pedido é: " + streamId);

                }

                if (streamId == null || streamId == 255) {

                    //Novo Pedido de Stream desconhecida
                    clientRequestStreamSet = this.neighbourInfo.streamNameToClientRequests.get(streamName);
                    if (clientRequestStreamSet == null) {
                        clientRequestStreamSet = new HashSet<>();
                        this.neighbourInfo.streamNameToClientRequests.put(streamName, clientRequestStreamSet);
                    }
                    synchronized(clientRequestStreamSet) {
                        clientRequestStreamSet.add(simp.getAddress());
                    }

                    if (streamId == null) {

                        //Ainda não fiz pedidos para este ficheiro
                        System.out.println("    Não pedi o ficheiro ainda...");
                        synchronized(this.neighbourInfo) {

                            System.out.println("    Enviar pedido para todos os vizinhos (exceto os que garantidamente não vão para o RP)");
                            //Enviar para todos os vizinhos se não conhecer caminhos para o RP
                            for (InetAddress neighbour : this.neighbourInfo.overlayNeighbours) {
                                //Não envio para quem me enviou nem para quem sei que não tem o RP
                                if (!neighbour.equals(simp.getAddress()) && !neighbour.equals(clientIP) && 
                                    !this.neighbourInfo.notRpAdjacent.contains(neighbour)) {

                                    System.out.println("Enviado SIMP para " + neighbour.getHostName() + ", port " + Define.simpPort);

                                    socket.send(new Simp(clientIP, neighbour, Define.simpPort, simp.getPayloadSize(), simp.getPayload()).toDatagramPacket());
                                    //Adicionar pedido feito por Simp
                                    this.neighbourInfo.rpRequest.add(neighbour);
                                }

                            }

                            if (this.neighbourInfo.rpRequest.isEmpty()) {
                                streamId = this.neighbourInfo.isConnectedToRP == 1 ? 0 : 255;
                                System.out.println("    Não há vizinhos para o RP: Enviado SHRIMP para " + simp.getAddress().getHostName() + " com streamId: " + streamId);
                                socket.send(new Shrimp(Packet.getCurrTime(), clientIP, streamId, Define.shrimpPort, simp.getAddress(),
                                                       streamNameBytes.length, streamNameBytes).toDatagramPacket());

                                synchronized(clientRequestStreamSet) {
                                    clientRequestStreamSet.remove(simp.getAddress());
                                }
                                continue;
                            }

                        }
                        //255 significa que já se pediu a stream mas não sabe se existe
                        this.neighbourInfo.fileNameToStreamId.put(new String(simp.getPayload()), 255);

                    }

                } else {
                    //Stream existe (porque existe conexão)
                    synchronized (this.neighbourInfo) {
                        socket.send(new Shrimp(Math.floorMod(Packet.getCurrTime() - neighbourInfo.minNodeQueue.peek().latency,60000), clientIP, this.neighbourInfo.fileNameToStreamId.get(new String(simp.getPayload())), Define.shrimpPort, simp.getAddress(), streamNameBytes.length, streamNameBytes).toDatagramPacket());
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
