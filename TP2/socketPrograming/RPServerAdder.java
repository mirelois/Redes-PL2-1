import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RPServerAdder implements Runnable{
    
    private int port;

    private final ServerInfo serverInfo;

    public RPServerAdder(int port, ServerInfo serverInfo){
        this.port = port;
        this.serverInfo = serverInfo;
    }

    @Override
    public void run(){

        byte[] buf = new byte[1024];

        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while(true){
            try (DatagramSocket socket = new DatagramSocket(this.port)) {

                socket.receive(packet);

                Simp simp = new Simp(packet);

                // int latency = Packet.getCurrTime() - simp.getTime_stamp();

                synchronized(this.serverInfo){
                    this.serverInfo.servers.add(simp.getSourceAddress());
                }


                //TODO fazer check de perdas para nao dar barraco


            } catch (IOException | PacketSizeException e) {
                //TODO: handle exception
                e.printStackTrace();
            }
        }
        
    }
}
