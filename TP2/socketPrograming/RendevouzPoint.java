import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RendevouzPoint implements Runnable{
    private int port, timeOut;
    private final NeighbourInfo neighbourInfo;

    private Thread my_client;

    private byte[][] stream;
    private int streamN;

    private ReadWriteLock rwl;

    public RendevouzPoint(int port, int timeOut, NeighbourInfo neighbourInfo, Thread my_client, InetAddress Server){
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

        try (DatagramSocket socket = new DatagramSocket()) {
            
        } catch (Exception e) {
            //TODO: handle exception
        }
        
    }
}
