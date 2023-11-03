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

        Thread t1, t2;

        try(DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]))){
            Sender sender = new Sender(socket);
            t1 = new Thread(sender);
            t1.start();
            
            Receiver receiver = new Receiver(socket);
            t2 = new Thread(receiver);
            t2.start();
            
            t1.join();
            t2.join();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
