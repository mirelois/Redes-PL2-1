import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class BootClient {

    public static void askNeighbours(InetAddress bootStraperIP, int bootStraperPort, int port, int timeOut) {

        DatagramSocket socket = new DatagramSocket(port);

        Thread t = new Thread(() -> {
            while (true) {
                Thread.sleep(timeOut);

                // send neighbour request
                Bop bop = new Bop(false, null, 0);

                socket.send(bop.getDatagramPacket());
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

            if (bopReceived.getChecksum() == Checksum(packet)){
                //entra aqui quando receber o pacote bem
                t.interrupt();

                Bop bop_ack = new Bop(true, null, 0);

                socket.send(bop_ack.getDatagramPacket());
            }

        }

    }

}
