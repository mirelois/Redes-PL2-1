import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;

public class Streaming implements Runnable{
    private final NeighbourInfo neighbourInfo;

    private final stream stream;

    public Streaming(NeighbourInfo neighbourInfo, stream stream){
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

                Sup sup = new Sup(packet);
                
                //int latency = stream.getTime_stamp() - Packet.getCurrTime();

                // Decidir Vizinho(s) mais adequado(s) para enviar stream(s)
                Integer streamId = sup.getStreamId();
                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(streamId);
                    if (streamActiveLinks.contains(InetAddress.getByName("localhost"))) {
                        socket.send(new Sup(sup.getStreamId(), sup.getVideo_time_stamp(), sup.getSequence_number(),
                            InetAddress.getByName("localhost"), 8389, sup.getPayloadSize(), sup.getPayload())
                            .toDatagramPacket());
                    }
                }

                for (InetAddress activeLink : streamActiveLinks) {
                    if (activeLink.equals(InetAddress.getByName("localhost"))) {
                        socket.send(new Sup(sup.getStreamId(), sup.getVideo_time_stamp(), sup.getSequence_number(),
                            InetAddress.getByName("localhost"), 8389, sup.getPayloadSize(), sup.getPayload())
                            .toDatagramPacket());
                    } else {
                        socket.send(new Sup(sup.getStreamId(), sup.getVideo_time_stamp(), sup.getSequence_number(),
                            activeLink, Define.streamingPort, sup.getPayloadSize(), sup.getPayload())
                            .toDatagramPacket());
                    }
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
