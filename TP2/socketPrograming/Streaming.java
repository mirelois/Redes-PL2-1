import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Streaming implements Runnable{
    private int port, timeOut;
    private NeighbourInfo neighbourInfo;

    private final Lock destLock;

    public Streaming(int port, int timeOut, NeighbourInfo neighbourInfo){
        this.port = port;
        this.timeOut = timeOut;
        this.neighbourInfo = this.neighbourInfo;
        this.destLock = new ReentrantLock();
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(this.port)){

            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length); // posso fazer isto cá fora

            while (true){
                socket.receive(packet);
                //int currTime = Packet.getCurrTime();
                Sup stream = new Sup(packet);
                //int latency = stream.getTime_stamp() - currTime;
                socket.send(new Sup(stream.getVideo_time_stamp(), stream.getSequence_number(),
                        InetAddress.getByName("deez"), this.port, stream.getPayloadSize(), stream.getPayload())
                        .toDatagramPacket());
                // TODO testar CheckSum

                // Decidir Vizinho mais adequado para enviar stream


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
