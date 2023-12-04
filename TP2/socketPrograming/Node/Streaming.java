package Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;

import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Sup;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.NeighbourInfo.Node;

public class Streaming implements Runnable{
    private final NeighbourInfo neighbourInfo;

    public Streaming(NeighbourInfo neighbourInfo){
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(Define.streamingPort)) {

            byte[] buf = new byte[Define.streamBuffer]; // 1024 is enough? no
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                Sup sup = new Sup(packet);
                                
                neighbourInfo.updateLatency(new NeighbourInfo.Node(sup.getAddress(), Packet.getLatency(sup.getTime_stamp())));

                Integer streamId = sup.getStreamId();
                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(streamId);
                }

                synchronized (streamActiveLinks) {
                    //TODO Como fazer os frame Numbers (o que são?)
                    for (InetAddress activeLink : streamActiveLinks) {
                        if (!activeLink.equals(sup.getAddress())) {
                            if (activeLink.equals(InetAddress.getByName("localhost"))) {
                                socket.send(new Sup(sup.getLossRate(), sup.getTime_stamp(), sup.getVideo_time_stamp(), sup.getFrameNumber(), sup.getSequence_number(),
                                     sup.getStreamId(), InetAddress.getByName("localhost"), 8389, sup.getPayloadSize(), sup.getPayload())
                                    .toDatagramPacket());
                            } else {
                                socket.send(new Sup(sup.getLossRate(), sup.getTime_stamp(), sup.getVideo_time_stamp(), sup.getFrameNumber(), sup.getSequence_number(),
                                     sup.getStreamId(), activeLink, Define.streamingPort, sup.getPayloadSize(), sup.getPayload())
                                    .toDatagramPacket());
                            }
                        }
                    }
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
