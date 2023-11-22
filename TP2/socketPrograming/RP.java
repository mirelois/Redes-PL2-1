import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RP implements Runnable {

    int port;

    ServerInfo serverInfo;

    public RP(int port) {
        this.port = port;
    }

    private InetAddress chooseBestServer(ServerInfo serverInfo){
        synchronized(serverInfo){
            return serverInfo.servers.get(0);
        }
    }

    @Override
    public void run() {

        while (true) {
            try (DatagramSocket socket = new DatagramSocket(this.port)) {

                byte[] buf = new byte[1024];

                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);

                Simp simp = new Simp(packet);
                //TODO tratar isto com shrinps

                Simp send_simp = new Simp(InetAddress.getByName("localhost"), chooseBestServer(serverInfo), port, 0, null);

                socket.send(send_simp.toDatagramPacket());

            } catch (Exception e) {
                // TODO: handle exception
            }
        }

    }

}
