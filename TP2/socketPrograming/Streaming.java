import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Streaming implements Runnable{
    private final int port, timeOut;
    private final NeighbourInfo neighbourInfo;

    private final stream stream;

    public Streaming(int port, int timeOut, NeighbourInfo neighbourInfo, stream stream){
        this.port = port;
        this.timeOut = timeOut;
        this.neighbourInfo = neighbourInfo;
        this.stream = stream;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(this.port)){

            byte[] buf = new byte[1024]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                Sup stream = new Sup(packet);
                //int latency = stream.getTime_stamp() - Packet.getCurrTime();

                // Decidir Vizinho(s) mais adequado(s) para enviar stream(s)
                Integer streamId = stream.getStreamId();
                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(streamId);
                    if (streamActiveLinks.contains(InetAddress.getByName("localhost"))) {
                        socket.send(new Sup(stream.getStreamId(), stream.getVideo_time_stamp(), stream.getSequence_number(),
                            InetAddress.getByName("localhost"), 8389, stream.getPayloadSize(), stream.getPayload())
                            .toDatagramPacket());
                    }
                }

                for (InetAddress activeLink : streamActiveLinks) {
                    if (activeLink.equals(InetAddress.getByName("localhost"))) {
                        socket.send(new Sup(stream.getStreamId(), stream.getVideo_time_stamp(), stream.getSequence_number(),
                            InetAddress.getByName("localhost"), 8389, stream.getPayloadSize(), stream.getPayload())
                            .toDatagramPacket());
                    } else {
                        socket.send(new Sup(stream.getStreamId(), stream.getVideo_time_stamp(), stream.getSequence_number(),
                            activeLink, this.port, stream.getPayloadSize(), stream.getPayload())
                            .toDatagramPacket());
                    }
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
