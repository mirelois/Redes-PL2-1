import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

                synchronized (this.stream){
                    if(stream.streamId!=0){
                        this.stream.stream = packet.getData();
                    }
                }

                Sup stream = new Sup(packet);
                //int latency = stream.getTime_stamp() - Packet.getCurrTime();

                // Decidir Vizinho(s) mais adequado(s) para enviar stream(s)
                InetAddress vizinhomegafixe=null;
                synchronized (this.neighbourInfo) {
                    if(this.neighbourInfo.streamActiveLinks.get(stream.getStreamId()).iterator().hasNext())
                        vizinhomegafixe = this.neighbourInfo.streamActiveLinks.get(stream.getStreamId()).iterator().next();
                }

                if(vizinhomegafixe!=null) {
                    socket.send(new Sup(stream.getStreamId(), stream.getVideo_time_stamp(), stream.getSequence_number(),
                            vizinhomegafixe, this.port, stream.getPayloadSize(), stream.getPayload())
                            .toDatagramPacket());
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
