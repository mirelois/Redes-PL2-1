import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class fullDuplex {
    public static void main(String[] args){
        if(args.length<1 || args.length>2) {
            System.out.println("Wrong Arguments");
            return;
        }

        // Setup Phase:
        if(args.length == 2) {
            new Thread(new BootStrapper(2000, args[1], 1000)).start();
        }

        HashMap<InetAddress, Set<InetAddress>> neighbours = new HashMap<InetAddress, Set<InetAddress>>(0);

        try {
            new Thread(new BootClient(InetAddress.getByName(args[0]), 2000, 2001, 1000, neighbours)).start();
        } catch (UnknownHostException e){
            e.printStackTrace();
        }

        try {
            synchronized (neighbours) {
                neighbours.wait();
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        // Phase 2
        Thread t1, t2;
        try(DatagramSocket socket = new DatagramSocket(5000)){
            Sender sender = new Sender(socket);
            t1 = new Thread(sender);
            t1.start();
            
            Receiver receiver = new Receiver(socket);
            t2 = new Thread(receiver);
            t2.start();

            t1.join();
            t2.join();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
