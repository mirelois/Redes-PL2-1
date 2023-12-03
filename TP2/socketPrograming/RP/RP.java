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


        try (DatagramSocket socket = new DatagramSocket(Define.RPPort)) {
                while (true) {
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

                synchronized (this.neighbourInfo) {
                    streamId = this.neighbourInfo.fileNameToStreamId.get(streamName); // senão existir cliente -> dumb
                }

                if (!serverInfo.streamInfoMap.containsKey(streamId)) {

                    ServerInfo.StreamInfo streamInfo = new ServerInfo.StreamInfo();

                    serverInfo.streamInfoMap.put(streamId, streamInfo);

                    RPConectionManager.updateBestServer(streamInfo, streamId, Integer.MAX_VALUE, socket);

                    // System.out.println("Pedido de stream enviado ao servidor " +
                    // chooseBestServer(serverInfo) +
                    // " com payload " + new String(simp.getPayload()));
                }

                socket.send(new Shrimp(
                        Packet.getCurrTime(),
                        simp.getSourceAddress(),
                        streamId,
                        Define.shrimpPort,
                        simp.getAddress(),
                        simp.getPayloadSize(),
                        simp.getPayload()).toDatagramPacket());

                System.out.println("Enviado SHRIMP para " + simp.getAddress().getHostAddress() +
                        " da stream com id " + streamId +
                        " pedida por " + clientIP.getHostAddress());
            }
            } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }

}
