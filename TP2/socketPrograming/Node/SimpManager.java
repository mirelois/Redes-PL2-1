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
                byte[] streamName = simp.getPayload();

                synchronized (this.neighbourInfo) {
                    
                    streamId = this.neighbourInfo.fileNameToStreamId.get(new String(simp.getPayload()));
                    System.out.println("    StreamId do ficheiro pedido é: " + streamId);

                    this.neighbourInfo.clientRequest.add(simp.getAddress());

                }

                if (streamId == null || streamId == 0) {
                    //Stream nunca foi testada
                    System.out.println("    Não conheço a stream...");
                    synchronized(this.neighbourInfo) {

                        //Ainda não sei onde está o RP

                        System.out.println("    Enviar pedido para todos os seus vizinhos, não conheço o RP");
                        //Enviar para todos os vizinhos se não conhecer caminhos para o RP
                        for (InetAddress neighbour : this.neighbourInfo.overlayNeighbours) {
                            //Não envio para quem me enviou
                            if (!neighbour.equals(simp.getAddress()) && !neighbour.equals(clientIP) && !this.neighbourInfo.notRpAdjacent.contains(neighbour)) {

                                System.out.println("Enviado SIMP para " + neighbour.getHostName() + ", port " + Define.simpPort);

                                socket.send(new Simp(clientIP, neighbour, Define.simpPort, simp.getPayloadSize(), simp.getPayload()).toDatagramPacket());
                                //Adicionar pedido feito por Simp
                                this.neighbourInfo.rpRequest.add(neighbour);
                            }

                        }

                        if (this.neighbourInfo.rpRequest.isEmpty()) {
                            streamId = streamId == 0 ? 0 : 255;
                            System.out.println("Enviado SHRIMP para " + simp.getAddress().getHostName() + ", port " + Define.shrimpPort + 
                                                " com streamId: " + streamId);
                            this.neighbourInfo.fileNameToStreamId.put(new String(simp.getPayload()), 0);
                            socket.send(new Shrimp(Packet.getCurrTime(), clientIP, streamId, Define.shrimpPort, simp.getAddress(),
                            streamName.length, streamName).toDatagramPacket());
                        }
                    }
                    //255 significa que ainda não se sabe se a stream existe
                    this.neighbourInfo.fileNameToStreamId.put(new String(simp.getPayload()), 255);

                } else if (streamId != 255) {
                    //Stream existe (porque existe conexão)
                    synchronized (this.neighbourInfo) {
                        socket.send(new Shrimp((Packet.getCurrTime() - neighbourInfo.minNodeQueue.peek().latency)%60000, clientIP, this.neighbourInfo.fileNameToStreamId.get(new String(simp.getPayload())), Define.shrimpPort, simp.getAddress(), streamName.length, streamName).toDatagramPacket());
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
