import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class Receiver implements Runnable {

    DatagramSocket socket;

    public Receiver(DatagramSocket socket){
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            
            DatagramPacket receivePacket;
            byte[] receiveBytes = new byte[1024];
            Packet packet;
            
            while (true) {
                receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);
                socket.receive(receivePacket);
                packet = new Packet(receivePacket.getData(), receivePacket.getLength());
                packet.printheader();
                System.out.println(new String(packet.getPayload(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
