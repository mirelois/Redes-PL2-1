import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerConectionManager implements Runnable {

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.serverConnectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];

            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            socket.receive(packet);

            Link link = new Link(packet);

            if (link.isActivate()) {//NOTE: como é que o server sabe o id da stream?
                // if (link.getStreamId() server thread is not running
                    //TODO: start Server Thread
                socket.send(new Link(
                    true,
                    false,
                    link.getStreamId(),
                    link.getAddress(),
                    link.getPort(),
                    0,
                    null
                ).toDatagramPacket());
                
            } else if (link.isDeactivate()) {
                //TODO: interupt server thread
                socket.send(new Link(
                    false,
                    true,
                    link.getStreamId(),
                    link.getAddress(),
                    link.getPort(),
                    0,
                    null
                ).toDatagramPacket());
            }

        } catch (Exception e) {
            // TODO: handle exception
        }

    }

}
