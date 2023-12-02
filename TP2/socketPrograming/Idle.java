import java.net.DatagramPacket;
import java.net.DatagramSocket;

import NeighbourInfo.StreamInfo;

public class Idle implements Runnable{
    private final NeighbourInfo neighbourInfo;

    private ServerInfo serverInfo;

    private int port;

    public Idle(NeighbourInfo neighbourInfo, ServerInfo serverInfo){
        this.port = Define.idlePort;
        this.neighbourInfo = neighbourInfo;
        this.serverInfo = serverInfo;
    }

    @Override
    public void run() {
        try(DatagramSocket socket = new DatagramSocket(Define.RPPort)){
            while (true){
                byte[] buf = new byte[Define.infoBuffer];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                ITP itp = new ITP(packet);

                if(itp.isNode){
                    neighbourInfo.updateLatency(new NeighbourInfo.Node(packet.getAddress(), Packet.getLatency(itp.timeStamp)));
                } else {
                    for (ServerInfo.StreamInfo streamInfo : serverInfo.streamInfoMap.values()) {
                        streamInfo.updateLatency(new ServerInfo.ServerInfo.Server(itp.getAddress(), Packet.getLatency(itp.timeStamp)));
                    }
                }
            }
        } catch (Exception e){
            // Todo
        }
    }
}
