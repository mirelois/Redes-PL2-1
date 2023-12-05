import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import Protocols.ITP;
import Protocols.Packet;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class Idle implements Runnable{
    private final NeighbourInfo neighbourInfo;

    private ServerInfo serverInfo;

    private int port;

    Thread timeout;

    boolean isRP;

    public Idle(NeighbourInfo neighbourInfo, ServerInfo serverInfo, boolean isRP){
        this.port = Define.idlePort;
        this.neighbourInfo = neighbourInfo;
        this.serverInfo = serverInfo;
        this.isRP = isRP;
    }

    @Override
    public void run() {
        try(DatagramSocket socket = new DatagramSocket(Define.idlePort)) {    
            //Criar a thread que vai dar timeout e enviar os Idles para todos os adjacentes
            this.timeout = new Thread(
                ()->{
                    try {
                        while(true) {
                            Thread.sleep(Define.idleTimeout);
                            //Colecionar num conjunto todos os adjacentes em uso 
                            //Enviar para os adjacentes que não estão em uso
                            //TODO diminuir vida aos ajdacentes que foram enviados mensagens
                            for (InetAddress address : this.neighbourInfo.overlayNeighbours) {
                                socket.send(new ITP(false,
                                                    true,
                                                    false, 
                                                    this.port,
                                                    address, 
                                                    Packet.getCurrTime(), 
                                                    0, 
                                                    null).toDatagramPacket());
                            }
                            if(this.isRP){
                                synchronized (this.serverInfo.streamInfoMap) {
                                    for (ServerInfo.StreamInfo streamInfo : this.serverInfo.streamInfoMap.values()) {
                                        streamInfo.connectingLock.lock();
                                        try {
                                            socket.send(new ITP(false,
                                                    true,
                                                    false,
                                                    this.port,
                                                    streamInfo.connected.address,
                                                    Packet.getCurrTime(),
                                                    0,
                                                    null).toDatagramPacket());
                                        } finally {
                                            streamInfo.connectingLock.unlock();
                                        }

                                        streamInfo.disconnectingDeprecatedLock.lock();
                                        try {
                                            for (ServerInfo.StreamInfo.Server disco: streamInfo.disconnecting)
                                                socket.send(new ITP(false,
                                                        true,
                                                        false,
                                                        this.port,
                                                        disco.address,
                                                        Packet.getCurrTime(),
                                                        0,
                                                        null).toDatagramPacket());
                                            for(ServerInfo.StreamInfo.Server depre: streamInfo.deprecated)
                                                socket.send(new ITP(false,
                                                        true,
                                                        false,
                                                        this.port,
                                                        depre.address,
                                                        Packet.getCurrTime(),
                                                        0,
                                                        null).toDatagramPacket());
                                        } finally {
                                            streamInfo.disconnectingDeprecatedLock.unlock();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception E) {

                    }
                }
            );
            //Correr o timeout
            timeout.run();

            while (true){
                byte[] buf = new byte[Define.infoBuffer];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                ITP itp = new ITP(packet);

                if(itp.isNode){
                    neighbourInfo.updateLatency(new NeighbourInfo.Node(packet.getAddress(), Packet.getLatency(itp.timeStamp)));
                    //TODO adicionar "vida" aos nodos adjacentes (ainda estão vivos)
                    //Isto vai implicar uma lista dos nodos verdadeiramente vivos, e não só dos adjacentes (neighbours)
                } else {
                    for (ServerInfo.StreamInfo streamInfo : serverInfo.streamInfoMap.values()) {
                        streamInfo.updateLatency(new ServerInfo.StreamInfo.Server(itp.getAddress(), Packet.getLatency(itp.timeStamp)));
                    }
                }
                if (!itp.isAck) {
                    if (neighbourInfo.isConnectedToRP == 1) {
                        socket.send(new ITP(false, 
                                            true, 
                                            true,
                                            (Packet.getCurrTime() - neighbourInfo.minNodeQueue.peek().latency)%60000,
                                            itp.getAddress(), 
                                            itp.getPort(), 
                                            itp.getPayloadSize(), 
                                            itp.getPayload()).toDatagramPacket());
                    } else {
                        socket.send(new ITP(false, 
                                            true, 
                                            true,
                                            itp.getTimeStamp(),
                                            itp.getAddress(), 
                                            itp.getPort(), 
                                            itp.getPayloadSize(), 
                                            itp.getPayload()).toDatagramPacket());
                    }
                }
            }
        } catch (Exception e){
            //TODO
        }
    }
}
