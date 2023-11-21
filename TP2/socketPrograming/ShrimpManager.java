import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

public class ShrimpManager implements Runnable{

    private final int port;

    private final NeighbourInfo neighbourInfo;

    public ShrimpManager(int port, NeighbourInfo neighbourInfo){
        this.port = port;
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(this.port)){
            byte[] buf = new byte[1024]; // 1024 is enough?
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true){
                socket.receive(packet);

                Shrimp shrimp = new Shrimp(packet);

                Iterator<NetworkInterface> i = NetworkInterface.getNetworkInterfaces().asIterator();
                while(i.hasNext()){
                    NetworkInterface networkInterface = i.next();
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                        if(inetAddress.equals(shrimp.getSourceAddress())); // what to do now ?

                    }
                }

                if(InetAddress.getByName("localhost").equals(shrimp.getSourceAddress())){

                }

            }

        } catch (SocketException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
