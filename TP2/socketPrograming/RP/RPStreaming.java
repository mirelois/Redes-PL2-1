package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;

import stream;
import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Sup;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class RPStreaming implements Runnable{
    
    private final NeighbourInfo neighbourInfo;

    private final ServerInfo serverInfo;

    private final stream stream;

    public RPStreaming(ServerInfo serverInfo, NeighbourInfo neighbourInfo, stream stream){
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
        this.stream = stream;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(Define.streamingPort)){

            byte[] buf = new byte[Define.streamBuffer]; // 1024 is enough? no
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                Sup sup = new Sup(packet); //this came from a server
                
                //calculatet and update server latencies
                int latency = Packet.getLatency(sup.getTime_stamp());

                synchronized(serverInfo){
                    serverInfo.latencyMap.put(sup.getAddress(), latency);
                }

                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(sup.getStreamId());
                    
                }

                for (InetAddress activeLink : streamActiveLinks) {
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
