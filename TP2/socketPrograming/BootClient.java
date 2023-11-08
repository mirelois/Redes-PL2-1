import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class BootClient implements Runnable{
    private int bootStrapperPort, port, timeOut;

    private InetAddress bootStrapperIP;

    private ArrayList<InetAddress> neighbours;

    public BootClient(InetAddress bootStrapperIP, int bootStrapperPort, int port, int timeOut, ArrayList<InetAddress> neighbours){
        this.bootStrapperIP = bootStrapperIP;
        this.bootStrapperPort = bootStrapperPort;
        this.port = port;
        this.timeOut = timeOut;
        this.neighbours = neighbours;
    }
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
        
    }

    public void run() {
        try(DatagramSocket socket = new DatagramSocket(port)) {

            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(timeOut);
                    } catch (InterruptedException e) {
                        return;
                    }

                    // send neighbour request
                    Bop bop = new Bop(false, null, 0, bootStrapperIP, bootStrapperPort);

                    try {
                        socket.send(bop.toDatagramPacket());
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    // ---------
                }
            });
            t.start();

            for (int i = 0; i < 5; i++) {

                // receber os vizinhos
                byte[] buf = new byte[1024];

                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);

                Bop bopReceived = new Bop(packet);

                if (bopReceived.getChecksum() == 0) { // TODO checksum

                    // entra aqui quando receber o pacote bem
                    t.interrupt();

                    this.neighbours.addAll((ArrayList<InetAddress>) deserialize(bopReceived.getPayload()));
                    System.out.println(neighbours);
                    synchronized (neighbours) {
                        this.neighbours.notify();
                    }

                    Bop bop_ack = new Bop(true, null, 0, bootStrapperIP, bootStrapperPort);

                    socket.send(bop_ack.toDatagramPacket());
                    break; // senão ele fica a pedir 5 vezes
                }

            }
        } catch (SocketException e){
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
