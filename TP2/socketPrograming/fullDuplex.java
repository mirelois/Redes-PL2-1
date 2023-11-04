import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class fullDuplex {
    public static void main(String[] args){
        if(args.length<1 || args.length>2) {
            System.out.println("Wrong Arguments");
            return;
        }

        // Setup Phase:
        if(args.length == 1) {
            HashMap<InetAddress, ArrayList<InetAddress>> tree = BootStrapper.getTree("/usr/bin/passwd");
            BootStrapper.runBoot(2000); // Falta mega ir removendo as entradas no dicionario quando receber ACKS do clientes de que têm os seus vizinhos
        }
        else
            BootClient.askNeighbours(); // falta return e guardar o estado dos vizinhos

        // Phase 2
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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
