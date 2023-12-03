import Protocols.PacketSizeException;
import Protocols.Simp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Test {

    public static void main(String[] args) throws IOException, PacketSizeException {
        try (DatagramSocket socket = new DatagramSocket(2000)) {

            String file = "videoA.mp4";

            byte[] buf = new byte[1024];

            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);

            System.out.println(file.length());

            socket.send(new Simp(InetAddress.getByName("localhost"), InetAddress.getByName("localhost"), 2000, file.length(), file.getBytes(StandardCharsets.US_ASCII)).toDatagramPacket());

            socket.receive(datagramPacket);

            Simp simp = new Simp(datagramPacket);

            System.out.println(simp.getPacket().length);
            System.out.println(simp.getPacketLength());
            System.out.println(simp.getHeader().length);
            System.out.println(simp.getHeaderSize());
            System.out.println(simp.getPacket().length);
            System.out.println(simp.getPayloadSize());
            System.out.println(datagramPacket.getLength());

            String newfile = new String(simp.getPayload(), StandardCharsets.US_ASCII);

            System.out.println(newfile);

            System.out.println(file.equals(newfile));
            

            
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
