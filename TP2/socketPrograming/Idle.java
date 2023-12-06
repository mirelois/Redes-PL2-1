import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import Protocols.ITP;
import Protocols.Packet;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class Idle implements Runnable {
    private final NeighbourInfo neighbourInfo;

    private ServerInfo serverInfo;

    private int port;

    Thread timeout;

    boolean isRP;

    InetAddress rpIp;

    public Idle(NeighbourInfo neighbourInfo, ServerInfo serverInfo, boolean isRP, fullDuplex.ServerRPHolder rpHolder) {

        this.port          = Define.idlePort;
        this.neighbourInfo = neighbourInfo;
        this.serverInfo    = serverInfo;
        this.isRP          = isRP;
        this.rpIp          = rpHolder.rpIP;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Define.idlePort)) {
            // Acabei de nascer, vou avisar todos ao meu redor
            //Criar a thread que vai dar timeout e enviar os Idles para todos os adjacentes
            this.timeout = new Thread(
                () -> {
                try {
                    // Acabei de saber os meus vizinhos pelo bootstrapper, vou testa-los para ver se tão
                    while (true) {
                        Thread.sleep(Define.idleTimeout);
                        //Colecionar num conjunto todos os adjacentes em uso
                        //Enviar para os adjacentes que não estão em uso
                        // diminuir vida aos ajdacentes que foram enviados mensagens
                        synchronized (this.neighbourInfo.neighBoursLifePoints){
                        Set<InetAddress> aux = new HashSet<>(this.neighbourInfo.neighBoursLifePoints.keySet());
                        for (InetAddress address : aux) {
                            int lifePoints = this.neighbourInfo.neighBoursLifePoints.get(address);
                            if (lifePoints > 0) {
                                this.neighbourInfo.neighBoursLifePoints.replace(address, lifePoints - 1);
                                try {
                                    socket.send(new ITP(address.equals(this.rpIp),
                                            true,
                                            false,
                                            this.port,
                                            address,
                                            Define.idlePort,
                                            0,
                                            null).toDatagramPacket());
                                } catch (SocketException e) {
                                    // Vizinho está morto? Será que esta exception faz o que queremos?
                                    this.neighbourInfo.neighBoursLifePoints.remove(address);
                                }

                            } else {
                                this.neighbourInfo.neighBoursLifePoints.remove(address);
                            }
                        }
                        }
                        if (this.isRP) {
                            synchronized (this.serverInfo.streamInfoMap) {
                                for (ServerInfo.StreamInfo streamInfo : this.serverInfo.streamInfoMap.values()) {
                                    streamInfo.connectingLock.lock();
                                    try {
                                        socket.send(new ITP(false,
                                                            true,
                                                            false,
                                                            this.port,
                                                            streamInfo.connected.address,
                                                            Define.idlePort,
                                                            0,
                                                            null).toDatagramPacket());
                                    }
                                    finally {
                                        streamInfo.connectingLock.unlock();
                                    }

                                    streamInfo.disconnectingDeprecatedLock.lock();
                                    try {
                                        for (ServerInfo.StreamInfo.Server disco: streamInfo.disconnecting) {
                                            socket.send(new ITP(false,
                                                                true,
                                                                false,
                                                                this.port,
                                                                disco.address,
                                                                Define.idlePort,
                                                                0,
                                                                null).toDatagramPacket());
                                        }
                                        for (ServerInfo.StreamInfo.Server depre: streamInfo.deprecated) {
                                            socket.send(new ITP(false,
                                                                true,
                                                                false,
                                                                this.port,
                                                                depre.address,
                                                                Define.idlePort,
                                                                0,
                                                                null).toDatagramPacket());
                                        }
                                    }
                                    finally {
                                        streamInfo.disconnectingDeprecatedLock.unlock();
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception E) {
                    E.printStackTrace();
                }
            }
                );
            //Correr o timeout
            timeout.start();

            while (true) {
                byte[]         buf    = new byte[Define.infoBuffer];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                ITP itp = new ITP(packet);

                System.out.println("Recebido Idle do " + itp.getAddress());

                if (itp.isNode) {
                    InetAddress address = packet.getAddress();
                    synchronized (this.neighbourInfo.neighBoursLifePoints) {
                        if (this.neighbourInfo.neighBoursLifePoints.get(address) != null) {
                            //adicionar "vida" aos nodos adjacentes (ainda estão vivos)
                            int lifePoints = this.neighbourInfo.neighBoursLifePoints.get(address);
                            System.out.println("    Tem vida " + lifePoints);
                            if (lifePoints < Define.maxLifePoints) {
                                this.neighbourInfo.neighBoursLifePoints.replace(address, lifePoints + 1);
                            }
                        } else { // vizinho "nasceu"
                            neighbourInfo.neighBoursLifePoints.put(packet.getAddress(), Define.maxLifePoints);
                        }
                    }
                }

                if(this.isRP && itp.isServer){
                    for (ServerInfo.StreamInfo streamInfo : serverInfo.streamInfoMap.values()) {
                        streamInfo.updateLatency(new ServerInfo.StreamInfo.Server(itp.getAddress(), Packet.getLatency(itp.timeStamp)));
                    }
                } else if (this.neighbourInfo.rpAdjacent.contains((itp.getAddress()))) {
                    this.neighbourInfo.updateLatency(new NeighbourInfo.Node(itp.getAddress(), Packet.getLatency(itp.timeStamp)));
                }

                if (!itp.isAck) {
                    synchronized (neighbourInfo.minNodeQueue) {
                        if (neighbourInfo.isConnectedToRP == 1 && neighbourInfo.minNodeQueue.peek() != null) {
                            socket.send(new ITP(false,
                                    true,
                                    true,
                                    Math.floorMod(Packet.getCurrTime() - neighbourInfo.minNodeQueue.peek().latency, 60000),
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
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
