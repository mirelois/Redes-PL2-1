import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Idle implements Runnable{
    private final NeighbourInfo neighbourInfo;

    private int port;

    public Idle(NeighbourInfo neighbourInfo){
        this.port = Define.idlePort;
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run() {
        try(DatagramSocket socket = new DatagramSocket(Define.RPPort)){
            while (true){
                byte[] buf = new byte[Define.infoBuffer];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);


            }
        } catch (Exception e){
            // Todo
        }
    }
}
