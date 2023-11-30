import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

public class RPConectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    ServerInfo serverInfo;

    int streamId;

    String streamName;

    ServerInfo.StreamInfo streamInfo;

    public RPConectionManager(ServerInfo serverInfo) {

        // this.serverInfo = serverInfo;
        // this.streamId = streamId;
        // this.streamName = streamName;
        this.serverInfo = serverInfo;

    }

    public void updateBestServer(ServerInfo.StreamInfo streamInfo, Integer streamId, int bestServerLatency, DatagramSocket socket)
            throws UnknownHostException { // TODO: currently this is never called stfu

        streamInfo.updateLatency(streamInfo.currentBestServer);// bestServerLatency is the latency of the current best
                                                               // server
        if (streamInfo.connectorThread == null) {
            
            streamInfo.connectorThread = new Thread(new Runnable() {

                public void run() {

                    ServerInfo.StreamInfo.Server connecting;

                    while (true) {
                        try {

                            synchronized (streamInfo.connecting) {
                                while (streamInfo.connecting != null) {
                                    streamInfo.connecting.wait();
                                }
                                connecting = streamInfo.getConnecting();// copy of the currentBestServer
                            }

                            socket.send(new Link(
                                    true,
                                    false,
                                    streamId,
                                    connecting.address,
                                    Define.serverPort,
                                    0,
                                    null).toDatagramPacket());

                            Thread.sleep(Define.RPTimeout);

                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        if (streamInfo.disconnectorThread == null) {
            
            streamInfo.disconnectorThread = new Thread(new Runnable() {

                public void run() {

                    HashSet<ServerInfo.StreamInfo.Server> disconnecting;

                    while (true) {
                        try {
                            synchronized (streamInfo.disconnecting) {
                                while (streamInfo.disconnecting.isEmpty()) { // sleeps if there is nothing to remove
                                    streamInfo.disconnecting.wait();
                                }
                                disconnecting = streamInfo.getDisconnecting();// copy of the disconnecting set
                            }

                            for (ServerInfo.StreamInfo.Server server : disconnecting) { // sends disconect link to
                                                                                        // all servers in
                                socket.send(new Link(
                                        false,
                                        true,
                                        streamId,
                                        server.address,
                                        Define.serverPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        if (streamInfo.connectorThread.isAlive()) {
            streamInfo.connectorThread.start();
        }
        if (streamInfo.disconnectorThread.isAlive()) {
            streamInfo.disconnectorThread.start();
        }

        synchronized (streamInfo.connecting) {
            synchronized (streamInfo.minServer) {
                if (streamInfo.connecting != null) {
                    streamInfo.deprecatedConnecting.add(streamInfo.connecting);
                }
                streamInfo.connecting = streamInfo.minServer.peek(); // this operation has complexity O(1)
            }

            streamInfo.connecting.notify();
        }

    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.RPConectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { // main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);

                Shrimp shrimp = new Shrimp(packet);

                this.streamInfo = serverInfo.streamInfo.get(shrimp.getStreamId());

                ServerInfo.StreamInfo.Server server = 
                    new ServerInfo.StreamInfo.Server(shrimp.getAddress(), Packet.getLatency(shrimp.getTimeStamp()));

                if (streamInfo.disconnecting.contains(server)) {
                    // recebeu confirmação de remoção, o server
                    // vai parar de mandar cenas
                    if (Byte.toUnsignedInt(shrimp.getPayload()[0]) == 0) { //isto é um disconnect acknolegment
                                                                           //caso este if falhar ele recebeu um connect
                       streamInfo.disconnecting.remove(server);            //e so ignora
                        
                    }
                }
                else if (streamInfo.connecting.equals(server)) { // recebeu confirmação de adição
                                                                 // vai trocar o currentBest e remover o antigo
                    streamInfo.disconnecting.add(streamInfo.currentBestServer);

                    streamInfo.disconnecting.notify();

                    streamInfo.currentBestServer = streamInfo.connecting;

                    streamInfo.connecting = null;

                }
                if (streamInfo.deprecatedConnecting.contains(server)) { // recebeu confirmação de ligação de uma stream
                                                                        // de que ja
                   streamInfo.disconnecting.add(server);                // nao quer saber, lmao manda po lixo

                    streamInfo.disconnecting.notify();

                }

            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}
