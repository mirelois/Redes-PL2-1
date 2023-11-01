import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    public static void main(String[] args) throws Exception{
        if(args.length != 1)
            System.out.println("1 argumentos");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        DatagramSocket socket = new DatagramSocket();

        InetAddress ip = InetAddress.getByName(args[0]);

        byte[] receiveBytes = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(receiveBytes, receiveBytes.length);
        socket.receive(datagramPacket);

        Packet packet = new Packet(datagramPacket.getData(), datagramPacket.getLength());
        System.out.println(new String(packet.getHeader()));
    }
}
