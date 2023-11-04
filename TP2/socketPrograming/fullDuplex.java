import java.io.IOException;
import java.net.DatagramSocket;

public class fullDuplex {
    public static void main(String[] args){
        if(args.length!=1) {
            System.out.println("Wrong Arguments! You should give the port number");
            return;
        }

        Thread t1, t2, t3;

        try(DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]))){
            Sender sender = new Sender(socket);
            t1 = new Thread(sender);
            t1.start();
            
            Receiver receiver = new Receiver(socket);
            t2 = new Thread(receiver);
            t2.start();

            BootStraper bootStraper = new BootStraper(1234);
            t3 = new Thread(bootStraper);
            t3.start();
            
            t1.join();
            t2.join();
            t3.join();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
