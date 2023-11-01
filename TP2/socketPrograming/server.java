import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import Packet;

/**
 * server
 */
public class Server {

    static DatagramPacket datagram;
    static DatagramSocket socket;
    static int port = 3000;
    static InetAddress address;


    public static void main(String[] args) {
        
        address = InetAddress.getByName(args[0]);

        socket = new DatagramSocket(port, address);

        while (true) {
            
            Scanner scanner = new Scanner(System.in);
            String msg = scanner.nextLine();

            Packet packet = new Packet(0, 0, msg.getBytes(), msg.length());

            datagram = new DatagramPacket(packet.getPacket(), packet.getPacketLength());

            socket.send(datagram);

            
        }
        
    }
    
}
