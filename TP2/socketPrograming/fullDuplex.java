import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class fullDuplex {
    public static void main(String[] args){
        if(args.length!=1) {
            System.out.println("Wrong Arguments! You should give the port number");
            return;
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        byte[] receiveBuffer = new byte[1024];

        Thread sender;
        try (DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]))){
            sender = new Thread(() -> {
                int i=0;
                byte[] sendBuffer;
                String[] message;
                while(true) {

                    try {
                        message = input.readLine().split(",", 3); // Example: 192.168.56.101,3000,This is a random message
                        sendBuffer = message[2].getBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Packet packet = new Packet(i, i, sendBuffer, sendBuffer.length);
                    i++;
                    try {
                        socket.send(new DatagramPacket(packet.getPacket(), packet.getPacketLength(), InetAddress.getByName(message[0]), Integer.parseInt(message[1])));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            sender.start();
            DatagramPacket receivePacket;
            byte[] receiveBytes = new byte[1024];
            Packet packet;
            while(true){
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
