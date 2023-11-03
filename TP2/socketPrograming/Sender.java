import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * server
 */
public class Sender implements Runnable {

    DatagramSocket socket = null;

    public Sender(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        int i = 0;
        byte[] sendBuffer;
        String[] message;
        while (true) {

            try {
                message = input.readLine().split(",", 3); // Example: 192.168.56.101,3000,This is a random message;
                sendBuffer = message[2].getBytes();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Packet packet = new Packet(i, i, sendBuffer, sendBuffer.length);
            i++;
            try {
                this.socket.send(new DatagramPacket(packet.getPacket(), packet.getPacketLength(),
                        InetAddress.getByName(message[0]), Integer.parseInt(message[1])));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
