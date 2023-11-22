import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Streaming implements Runnable{
    private int port, timeOut;
    private final NeighbourInfo neighbourInfo;

    private Thread my_client;

    private byte[][] stream;
    private int streamN;

    private ReadWriteLock rwl;

    public Streaming(int port, int timeOut, NeighbourInfo neighbourInfo, Thread my_client){
        this.port = port;
        this.timeOut = timeOut;
        this.neighbourInfo = neighbourInfo;
        this.my_client = my_client;
        this.stream = new byte[10][]; // 10 frames?
        this.streamN = 0;
        this.rwl = new ReentrantReadWriteLock();
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(this.port)){

            byte[] buf = new byte[1024]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                if(this.my_client.isAlive()){
                    this.rwl.writeLock().lock();
                    try {
                        if(this.streamN < this.stream.length)
                            this.streamN = 0;
                        this.stream[this.streamN++] = packet.getData();
                    } finally {
                        this.rwl.writeLock().unlock();
                    }
                }

                Sup stream = new Sup(packet);
                //int latency = stream.getTime_stamp() - Packet.getCurrTime();

                // Decidir Vizinho(s) mais adequado(s) para enviar stream(s)
                InetAddress vizinhomegafixe=null;
                synchronized (this.neighbourInfo) {
                    if(this.neighbourInfo.streamActiveLinks.get(0).iterator().hasNext())
                        vizinhomegafixe = this.neighbourInfo.streamActiveLinks.get(0).iterator().next();
                }
                
                if(vizinhomegafixe!=null) {
                    socket.send(new Sup(0, stream.getVideo_time_stamp(), stream.getSequence_number(),
                            vizinhomegafixe, this.port, stream.getPayloadSize(), stream.getPayload())
                            .toDatagramPacket());
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
