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

                Integer streamId;
                Set<InetAddress> adjacent;
                byte[] streamName = simp.getPayload();


                synchronized (this.neighbourInfo) {
                    streamId = this.neighbourInfo.nameHash.get(new String(simp.getPayload()));

                    adjacent = this.neighbourInfo.clientAdjacent.get(clientIP);
                    if(adjacent == null){
                        adjacent = new HashSet<>();
                        this.neighbourInfo.clientAdjacent.put(clientIP, adjacent);
                    }

                    adjacent.add(simp.getAddress());

                    //Se isto falha, falha tudo, restruturar para ter em conta as streamID e ter uma lógica mais limpa
                    if (this.neighbourInfo.connectionToRP == 0) {
                        this.neighbourInfo.nameHash.put(new String(simp.getPayload()), 0);
                        socket.send(new Shrimp(clientIP, 0, this.shrimpPort, simp.getAddress(), streamName.length, streamName).toDatagramPacket());
                        continue;
                    }
                }

                if (streamId == null) {
                    //Stream nunca foi testada

                    synchronized(neighbourInfo) {
                        //if (this.neighbourInfo.streamAdjacent.values().isEmpty()) {
                            //Enviar para todos os vizinhos se não conhecer caminhos para o RP
                            for (InetAddress neighbour : this.neighbourInfo.neighbours) 
                                if (!neighbour.equals(simp.getAddress())) {
                                    socket.send(new Simp(clientIP, simp.getAddress(), this.port, simp.getPayloadSize(), simp.getPayload()).toDatagramPacket());
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
                    this.neighbourInfo.nameHash.put(new String(simp.getPayload()), -1);

                } else {
                    //Stream existe (porque existe conexão)
                    synchronized (this.neighbourInfo) {
                        socket.send(new Shrimp(clientIP, this.neighbourInfo.nameHash.get(new String(simp.getPayload())), this.shrimpPort, simp.getAddress(), streamName.length, streamName).toDatagramPacket());
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
