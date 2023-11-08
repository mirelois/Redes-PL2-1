import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class fullDuplex {
    public static void main(String[] args){
        if(args.length<1 || args.length>2) {
            System.out.println("Wrong Arguments");
            return;
        }

        ArrayList<InetAddress> neighbours = new ArrayList<>(0);

        // Setup Phase:
        if(args.length == 1) {
            new Thread(new BootStrapper(2000, args[0], 1000)).start(); // no one will get this one #LMAO
        }
        else {
            try {
                new Thread(new BootClient(InetAddress.getByName(args[1]), 2000, 2000, 1000, neighbours)).start();
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
