import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashSet;
import java.net.UnknownHostException;

public class NodeConnectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    NeighbourInfo neighbourInfo;

    int streamId;

    String streamName;

    NeighbourInfo.StreamInfo streamInfo;

    public NodeConnectionManager(NeighbourInfo neighbourInfo) {

        // this.serverInfo = serverInfo;
        // this.streamId = streamId;
        // this.streamName = streamName;
        this.neighbourInfo = neighbourInfo;

    }

    public static void updateBestServer( //TODO: fix args
        NeighbourInfo neighbourInfo,
        NeighbourInfo.StreamInfo streamInfo,
        Integer streamId,
        int bestServerLatency,
        DatagramSocket socket) throws UnknownHostException { // TODO: currently this is never called stfu

        neighbourInfo.updateLatency(streamInfo.connected);// bestServerLatency is the latency of the current best
                                                       // server
        if (streamInfo.connectorThread == null) {
            
            streamInfo.connectorThread = new Thread(new Runnable() {

                public void run() {

                    NeighbourInfo.Node connecting;

                    try {
                        while (true) {

                            synchronized (streamInfo.connecting) {
                                while (streamInfo.connecting != null) {
                                    streamInfo.connecting.wait();
                                }
                                connecting = streamInfo.getConnecting();// copy of the currentBestServer
                            }

                            socket.send(new Link(
                                    false,
                                    true,
                                    false,
                                    streamId,
                                    connecting.address,
                                    Define.serverPort,
                                    0,
                                    null).toDatagramPacket());

                            Thread.sleep(Define.RPTimeout);

                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (streamInfo.disconnectorThread == null) {
            //TODO Separar os Deprecated dos Disconnecting no futuro
            streamInfo.disconnectorThread = new Thread(new Runnable() {

                public void run() {

                    HashSet<NeighbourInfo.Node> disconnecting;
                    HashSet<NeighbourInfo.Node> deprecated;

                    try {
                        while (true) {
                            // streamInfo.disconnectingDeprecatedLock.lock();
                            try {
                                while (streamInfo.disconnecting.isEmpty() && streamInfo.deprecated.isEmpty()) { // sleeps if there is nothing to remove
                                    // streamInfo.disconnectingDeprecatedEmpty.await();
                                }

                                disconnecting = streamInfo.getDisconnecting();// copy of the disconnecting set
                                deprecated = streamInfo.getDeprecated();// copy of the deprecated set
                            } finally {
                                // streamInfo.disconnectingDeprecatedLock.unlock();
                            }

                            for (NeighbourInfo.Node node : disconnecting) { // sends disconect link to
                                                                                        // all servers in
                                socket.send(new Link(
                                        false,
                                        false,
                                        true,
                                        streamId,
                                        node.address,
                                        Define.serverPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            for (NeighbourInfo.Node node : deprecated) {
                                
                                socket.send(new Link(
                                        false,
                                        true,
                                        false,
                                        streamId,
                                        node.address,
                                        Define.serverPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            Thread.sleep(Define.RPTimeout);

                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
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
            synchronized (neighbourInfo.minNode) {
                if (streamInfo.connecting != null) {
                    streamInfo.disconnecting.add(streamInfo.connecting); //add unactivated packet to the remove list
                }
                streamInfo.connecting = neighbourInfo.minNode.peek(); // this operation has complexity O(1)
            }

            streamInfo.connecting.notify();
        }

    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.nodeConnectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { // main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);

                Link link = new Link(packet);

                this.streamInfo = neighbourInfo.streamIdToStreamInfo.get(link.getStreamId());

                NeighbourInfo.Node node = 
                    new NeighbourInfo.Node(link.getAddress(), Integer.MAX_VALUE);

                if (!link.isAck()) {

                    if (link.isActivate()) {
                        
                        synchronized(neighbourInfo.streamActiveLinks){
                            neighbourInfo.streamActiveLinks.get(link.streamId).add(link.getAddress());
                        }

                    } else if (link.isDeactivate()) {

                        //TODO lidar com pedidos de desconexão vindos de nodos dos clientes
                    }

                } else if (link.isActivate()) { //this is a connection confirmation acknolegment
                                         
                    if (node.equals(streamInfo.connecting)) { //this checks if connection has been established

                        // streamInfo.disconnectingDeprecatedLock.lock();
                        try {
                            streamInfo.disconnecting.add(streamInfo.connecting);
                            // streamInfo.disconnectingDeprecatedEmpty.notify();
                        } finally {
                            // streamInfo.disconnectingDeprecatedLock.unlock();
                        }

                        streamInfo.connected = streamInfo.connecting;

                        streamInfo.connecting = null;

                    } else if (streamInfo.deprecated.contains(node)) {
                        // streamInfo.disconnectingDeprecatedLock.lock();
                        try {
                            streamInfo.deprecated.remove(node);
                            streamInfo.disconnecting.add(node);
                            // streamInfo.disconnectingDeprecatedEmpty.notify();
                        } finally {
                            // streamInfo.disconnectingDeprecatedLock.unlock();
                        }
                    }
                    
                } else if (link.isDeactivate()) { //this means a server acepted the disconnect request
                    
                    synchronized(streamInfo.disconnecting){
                        streamInfo.disconnecting.remove(node);
                    }
                                                                        
                }

            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}
