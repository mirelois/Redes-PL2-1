package Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import Protocols.PacketSizeException;
import Protocols.Shrimp;
import Protocols.Simp;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;

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
                                   " com payload " + new String(simp.getPayload()));

                InetAddress clientIP = simp.getSourceAddress();
                Integer streamId;
                Set<InetAddress> clientAdjacent;
                byte[] streamName = simp.getPayload();

                synchronized (this.neighbourInfo) {
                    streamId = this.neighbourInfo.fileNameToStreamId.get(new String(simp.getPayload()));

                    clientAdjacent = this.neighbourInfo.clientAdjacent.get(clientIP);
                    if(clientAdjacent == null){
                        clientAdjacent = new HashSet<>();
                        this.neighbourInfo.clientAdjacent.put(clientIP, clientAdjacent);
                    }

                    //Adicionar caminho para o cliente
                    clientAdjacent.add(simp.getAddress());
                    
                    //Se isto falha, falha tudo, restruturar para ter em conta as streamID e ter uma lógica mais limpa
                    if (this.neighbourInfo.isConnectedToRP == 0) {
                        this.neighbourInfo.fileNameToStreamId.put(new String(simp.getPayload()), 0);
                        socket.send(new Shrimp(clientIP, 0, Define.shrimpPort, simp.getAddress(), streamName.length, streamName).toDatagramPacket());
                        continue;
                    }
                }

                if (streamId == null) {
                    //Stream nunca foi testada

                    synchronized(neighbourInfo) {
                        //if (this.neighbourInfo.streamAdjacent.values().isEmpty()) {
                            //Enviar para todos os vizinhos se não conhecer caminhos para o RP
                            for (InetAddress neighbour : this.neighbourInfo.overlayNeighbours) 
                                if (!neighbour.equals(simp.getAddress()) && !neighbour.equals(simp.getSourceAddress())) {
                                    System.out.println("Enviado SIMP para " + neighbour.getHostName());
                                    socket.send(new Simp(clientIP, neighbour, Define.simpPort, simp.getPayloadSize(), simp.getPayload()).toDatagramPacket());
                                    //Adicionar pedido feito por Simp
                                    this.neighbourInfo.rpRequest.add(neighbour);
                                }
                        /*} else {
                            //Enviar apenas para os caminhos conhecidos do RP
                            for (Set<InetAddress> neighbourSet : this.neighbourInfo.streamAdjacent.values().) 
                                for (InetAddress neighbour : neighbourSet) {
                                    if (!neighbour.equals(simp.getAddress())) 
                                       socket.send(new Simp(clientIP, simp.getAddress(), this.port, simp.getPayloadSize(), simp.getPayload()).toDatagramPacket());    
                                }
                        }*/
                    }
                    //255 significa que ainda não se sabe se a stream existe
                    this.neighbourInfo.fileNameToStreamId.put(new String(simp.getPayload()), 255);

                } else {
                    //Stream existe (porque existe conexão)
                    synchronized (this.neighbourInfo) {
                        socket.send(new Shrimp(clientIP, this.neighbourInfo.fileNameToStreamId.get(new String(simp.getPayload())), Define.shrimpPort, simp.getAddress(), streamName.length, streamName).toDatagramPacket());
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
