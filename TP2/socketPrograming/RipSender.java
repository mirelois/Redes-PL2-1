import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RipSender implements Runnable{
    //Classe que reenvia tudo nos mapas de retransmissão
    public DatagramSocket socket;
    public ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public HashMap<Tuple<String, InetAddress>, DatagramPacket> simpPackets = new HashMap<>();
    public HashMap<Tuple<Integer, InetAddress>, DatagramPacket> shrimpPackets = new HashMap<>();
    public HashMap<Triple<Boolean, Boolean, InetAddress>, DatagramPacket> linkPackets = new HashMap<>();
    //Retransmissão de SUPs pode ser diferente
    //public HashMap<Tuple<>, DatagramPacket> supPackets = new HashMap<>();

    @Override
    public void run() {
        while(true) {
            try {
                
                Thread.sleep(Define.RetransTimeout);
                rwl.writeLock().lock();

                for (DatagramPacket packet : simpPackets.values()) {
                    socket.send(packet);
                }
                for (DatagramPacket packet : shrimpPackets.values()) {
                    socket.send(packet);
                }
                for (DatagramPacket packet : linkPackets.values()) {
                    socket.send(packet);
                }

            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            } finally {
                rwl.writeLock().unlock();
            }
        }
    }

    public RipSender(DatagramSocket socket) {
        this.socket = socket;
    }
}
