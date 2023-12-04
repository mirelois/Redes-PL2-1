package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;

import Node.NodeConnectionManager;
import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Sup;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class RPStreaming implements Runnable{
    
    private final NeighbourInfo neighbourInfo;

    private final ServerInfo serverInfo;

    public RPStreaming(ServerInfo serverInfo, NeighbourInfo neighbourInfo){
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(Define.streamingPort)){

            byte[] buf = new byte[Define.streamBuffer]; // 1024 is enough? no
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                Sup sup = new Sup(packet); //this came from a server
                System.out.println("Recebida Stream " + sup.getStreamId() + " de " + sup.getAddress());
                //calculatet and update server latencies

                ServerInfo.StreamInfo streamInfo;

                synchronized(serverInfo.streamInfoMap) {
                    streamInfo = serverInfo.streamInfoMap.get(sup.getStreamId());
                }

                Integer currLatency = Packet.getLatency(sup.getTime_stamp()), bestLatency;

                streamInfo.connectedLock.lock();
                try {
                    streamInfo.connected.latency = currLatency;
                } finally {
                    streamInfo.connectedLock.unlock();
                }
                
                synchronized(this.neighbourInfo.minNodeQueue) {
                    if (this.neighbourInfo.minNodeQueue.peek() != null)
                        bestLatency = this.neighbourInfo.minNodeQueue.peek().latency;
                    else
                        bestLatency = Integer.MAX_VALUE;
                }

                if (bestLatency < 0.95 * currLatency) {
                    RPServerConectionManager.updateBestServer(streamInfo, sup.getStreamId(), socket);
                }

                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(sup.getStreamId());
                    
                }

                for (InetAddress activeLink : streamActiveLinks) {
                    System.out.println("Enviada stream " + sup.getStreamId() + " para: " + activeLink +
                                       " com seq#: " + sup.getSequence_number());
                    socket.send(new Sup(
                            -1,
                            Packet.getCurrTime(),
                            sup.getVideo_time_stamp(),
                            sup.getFrameNumber(),
                            sup.getSequence_number(),
                            sup.getStreamId(),
                            activeLink,
                            Define.streamingPort,
                            sup.getPayloadSize(),
                            sup.getPayload()
                        ).toDatagramPacket());
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
