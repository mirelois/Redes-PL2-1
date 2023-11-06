import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class BootClient {

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
        
    }

    public static void askNeighbours(InetAddress bootStraperIP, int bootStraperPort, int port, int timeOut) {

        DatagramSocket socket = new DatagramSocket(port);

        Thread t = new Thread(() -> {
            while (true) {
                Thread.sleep(timeOut);

                // send neighbour request
                Bop bop = new Bop(false, null, 0, bootStraperIP, bootStraperPort);

                socket.send(bop.toDatagramPacket());
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

                ArrayList<InetAddress> neighbour = deserialize(bopReceived.getPayload());

                System.out.println(neighbour);

                Bop bop_ack = new Bop(true, null, 0, bootStraperIP, bootStraperPort);

                socket.send(bop_ack.toDatagramPacket());
            }

        }

    }

}
