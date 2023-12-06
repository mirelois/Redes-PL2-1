package RP;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import Protocols.Packet;
import Protocols.Rip;
import Protocols.Shrimp;
import Protocols.Simp;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class RP implements Runnable {

    int port, serverPort;

    ServerInfo serverInfo;

    private final NeighbourInfo neighbourInfo;

    public RP(ServerInfo serverInfo, NeighbourInfo neighbourInfo) {
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
        this.neighbourInfo.isConnectedToRP = 1;
    }

    @Override
    public void run() {


        try (DatagramSocket socket = new DatagramSocket(Define.simpPort)) {
            while (true) {
                byte[] buf = new byte[Define.infoBuffer];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                Simp simp = new Simp(packet); // from client

                socket.send(new Rip(0, simp.getAddress(), Define.ripPort).toDatagramPacket());

                System.out.println("Recebido SIMP de " + simp.getAddress().getHostAddress() +
                        " que pede Stream " + new String(simp.getPayload()));

                Integer streamId;
                String streamName = new String(simp.getPayload());

                synchronized (this.neighbourInfo) {
                    streamId = this.neighbourInfo.fileNameToStreamId.get(streamName); // senão existir cliente -> dumb
                }

                if (!serverInfo.streamInfoMap.containsKey(streamId)) {
                    System.out.println("    Recebido pedido da Stream " + streamId + "que ainda não existe servidor com o ficheiro");
                    //TODO não existe ainda a stream no RP
                    socket.send(new Shrimp(
                        Packet.getCurrTime(),
                        0,
                        Define.shrimpPort,
                        simp.getAddress(),
                        simp.getPayloadSize(),
                        simp.getPayload()).toDatagramPacket());

                        System.out.println("Enviado SHRIMP para " + simp.getAddress().getHostAddress() +
                            " da stream com id " + 0);
                } else {

                    ServerInfo.StreamInfo streamInfo = this.serverInfo.streamInfoMap.get(streamId);

                    synchronized(streamInfo.clientAdjacent) {
                        //Check if neighbour didn't ask for specific stream
                        if(!streamInfo.clientAdjacent.containsKey(simp.getAddress())) {
                            //Add asking neighbour
                            streamInfo.clientAdjacent.put(simp.getAddress(), 0);
                        }

                    }

                    socket.send(new Shrimp(
                            Packet.getCurrTime(),
                            streamId,
                            Define.shrimpPort,
                            simp.getAddress(),
                            simp.getPayloadSize(),
                            simp.getPayload()).toDatagramPacket());
    
                    System.out.println("Enviado SHRIMP para " + simp.getAddress().getHostAddress() +
                            " da stream com id " + streamId);
                }

            }
            } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }

}
