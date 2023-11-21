import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Streaming implements Runnable{
    private int port, timeOut;
    private NeighbourInfo neighbourInfo;

    private Thread my_client;

    public Streaming(int port, int timeOut, NeighbourInfo neighbourInfo, Thread my_client){
        this.port = port;
        this.timeOut = timeOut;
        this.neighbourInfo = neighbourInfo;
        this.my_client = my_client;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(this.port)){

            byte[] buf = new byte[1024]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                if(this.my_client.isAlive()){
                    // mandar stream para mim através de concorrência :D
                }

                //int currTime = Packet.getCurrTime();
                Sup stream = new Sup(packet);
                //int latency = stream.getTime_stamp() - currTime;

                // Decidir Vizinho(s) mais adequado para enviar stream(s)
                socket.send(new Sup(0,stream.getVideo_time_stamp(), stream.getSequence_number(),
                        InetAddress.getByName("deez"), this.port, stream.getPayloadSize(), stream.getPayload())
                        .toDatagramPacket());




            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
