import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Streaming implements Runnable{
    private int port, timeOut;
    private Map<InetAddress, Set<InetAddress>> neighbours;

    private final ReadWriteLock destLock;

    public Streaming(int port, int timeOut, Map<InetAddress, Set<InetAddress>> neighbours){
        this.port = port;
        this.timeOut = timeOut;
        this.neighbours = neighbours;
        this.destLock = new ReentrantReadWriteLock();
    }

    private class Dest{
        public int port;
        public InetAddress ip;
    }

    @Override
    public void run(){
        Dest dest = new Dest();
        try(DatagramSocket socket = new DatagramSocket(this.port)){
            Thread t = new Thread(() -> {
                while (true) {
                    this.destLock.readLock().lock();
                    try {
                        try {
                            Thread.sleep(timeOut);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // send neighbour request
                        Bop bop = new Bop(false, null, 0, dest.ip, dest.port);

                        try {
                            socket.send(bop.toDatagramPacket());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        this.destLock.readLock().unlock();
                    }
                }
            });
            t.start();

            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length); // posso fazer isto cá fora
            int dest_ip, dest_port;
            while (true){
                socket.receive(packet);
                Sup stream = new Sup(packet);

                // TODO testar CheckSum

                // Decidir Vizinho mais adequado para enviar stream


                this.destLock.writeLock().lock();
                try {
                    //dest.ip = dest_ip;
                    //dest.port = dest_port;
                } finally {
                    this.destLock.writeLock().unlock();
                }


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
