package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import Protocols.Link;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class RPServerConectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    ServerInfo serverInfo;

    int streamId;

    String streamName;

    ServerInfo.StreamInfo streamInfo;

    public RPServerConectionManager(ServerInfo serverInfo) {

        // this.serverInfo = serverInfo;
        // this.streamId = streamId;
        // this.streamName = streamName;
        this.serverInfo = serverInfo;

    }

    public static void updateBestServer(ServerInfo.StreamInfo streamInfo, NeighbourInfo neighbourInfo, DatagramSocket socket)
            throws UnknownHostException { // TODO: currently this is never called stfu

        Integer streamId = streamInfo.streamId;

        synchronized (streamInfo.minServerQueue) {
            System.out.println("Current Priority Queue: ");
            for (ServerInfo.StreamInfo.Server server : streamInfo.minServerQueue) {
                System.out.println(server.address.getHostName() + ":" + server.getMetrics());
            }
        }

        if (neighbourInfo.streamActiveLinks.get(streamInfo.streamId).isEmpty()) {
            return;
        }

        if (streamInfo.connected != null) {
            streamInfo.updateLatency(streamInfo.connected);// bestServerLatency is the latency of the current best
        }
                                                               // server
        if (streamInfo.connectorThread == null) {
            System.out.println("Started connector thread.");
            streamInfo.connectorThread = new Thread(new Runnable() {

                public void run() {

                    ServerInfo.StreamInfo.Server connecting;

                    try {
                        while (true) {
                            connecting = null;
                            try {
                                streamInfo.connectingLock.lock();
                                while (streamInfo.connecting == null) {
                                    streamInfo.connectingEmpty.await();
                                }
                                connecting = streamInfo.getConnecting();// copy of the currentBestServer
                            } finally {
                                streamInfo.connectingLock.unlock();
                            }
                            
                            System.out.println("Enviado Link de ativação para " + connecting.address.getHostName() + " da stream " + streamId);
                            socket.send(new Link(
                                    false,
                                    true,
                                    false,
                                    streamId,
                                    connecting.address,
                                    Define.serverConnectionManagerPort,
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
            System.out.println("Started disconnector thread.");
            streamInfo.disconnectorThread = new Thread(new Runnable() {

                public void run() {

                    HashSet<ServerInfo.StreamInfo.Server> disconnecting;
                    HashSet<ServerInfo.StreamInfo.Server> deprecated;

                    try {
                        while (true) {
                            streamInfo.disconnectingDeprecatedLock.lock();
                            try {
                                while (streamInfo.disconnecting.isEmpty() && streamInfo.deprecated.isEmpty()) { // sleeps if there is nothing to remove
                                    streamInfo.disconnectingDeprecatedEmpty.await();
                                }

                                disconnecting = streamInfo.getDisconnecting();// copy of the disconnecting set
                                deprecated = streamInfo.getDeprecated();// copy of the deprecated set
                            } finally {
                                streamInfo.disconnectingDeprecatedLock.unlock();
                            }

                            for (ServerInfo.StreamInfo.Server server : disconnecting) { // sends disconect link to
                                System.out.println("Enviado Link de desativação para " + server.address + " da stream " + streamId);
                                socket.send(new Link(
                                        false,
                                        false,
                                        true,
                                        streamId,
                                        server.address,
                                        Define.serverConnectionManagerPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            for (ServerInfo.StreamInfo.Server server : deprecated) {
                                System.out.println("Enviado Link de desativação para " + server.address + " da stream " + streamId);
                                socket.send(new Link(
                                        false,
                                        true,
                                        false,
                                        streamId,
                                        server.address,
                                        Define.serverConnectionManagerPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            Thread.sleep(Define.streamingTimeout);

                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (!streamInfo.connectorThread.isAlive()) {
            streamInfo.connectorThread.start();
        }
        if (!streamInfo.disconnectorThread.isAlive()) {
            streamInfo.disconnectorThread.start();
        }

        streamInfo.connectingLock.lock();
        try {
            /*synchronized (streamInfo.disconnecting) { //not supposed to add connected to disconnecting here  
                if (streamInfo.connected != null) {
                    streamInfo.disconnecting.add(streamInfo.connected);                 
                }
            }*/
            streamInfo.disconnectingDeprecatedLock.lock();
            try {
                synchronized (streamInfo.minServerQueue) {
                    if (!streamInfo.minServerQueue.peek().equals(streamInfo.connected) &&
                        !streamInfo.minServerQueue.peek().equals(streamInfo.connecting)) {
                        streamInfo.disconnectingDeprecatedLock.lock();
                        try {
                            if (streamInfo.connecting != null) {
                                System.out.println("Connecting deprecado: " + streamInfo.connecting.address.getHostName());
                                streamInfo.deprecated.add(streamInfo.connecting); //add unactivated packet to the remove list
                                streamInfo.disconnectingDeprecatedEmpty.signal();
                            }
                        } finally {
                            streamInfo.disconnectingDeprecatedLock.unlock();
                        }
                        streamInfo.connecting = streamInfo.minServerQueue.peek();
                        if (streamInfo.connecting != null) {
                            System.out.println("Alterado connecting para " + streamInfo.connecting.address.getHostName());
                            streamInfo.connectingEmpty.signal();
                        }
                    }
                }
            } finally {
                streamInfo.disconnectingDeprecatedLock.unlock();
            }
        } finally {
            streamInfo.connectingLock.unlock();
        }

    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.RPConectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            //Links that RP receives here:
            //  Server Connection Acks (Connect && Disconnect)
            while (true) { // main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);

                Link link = new Link(packet);
                System.out.println("Recebido Link de " + link.getAddress() + " do tipo activate: " + link.isActivate());

                this.streamInfo = serverInfo.streamInfoMap.get(link.getStreamId());

                ServerInfo.StreamInfo.Server server = 
                    new ServerInfo.StreamInfo.Server(link.getAddress(), Integer.MAX_VALUE);

                if (link.isActivate()) { //this is a connection confirmation acknolegment
                    
                    //server.equals() só verifica o Address do Servidor
                    if (server.equals(streamInfo.connecting)) { //this checks if connection has been established

                        streamInfo.connectedLock.lock();
                        try {
                            streamInfo.connectingLock.lock();
                            try {
                                streamInfo.disconnectingDeprecatedLock.lock();
                                try {
                                    if (streamInfo.connecting != null && streamInfo.connecting.address.equals(server.address)) {
                                        if (streamInfo.connected != null && !streamInfo.connected.equals(streamInfo.connecting)) {
                                            streamInfo.disconnecting.add(streamInfo.connected);
                                            streamInfo.disconnectingDeprecatedEmpty.signal();
                                        }
                                        streamInfo.connected = streamInfo.connecting;
                                        streamInfo.connecting = null;
                                        System.out.println("Established Connection!\n   connected: " + 
                                        streamInfo.connected.address.getHostName());
                                    }
                                } finally {
                                    streamInfo.disconnectingDeprecatedLock.unlock();
                                }
                            } finally {
                                streamInfo.connectingLock.unlock();
                            }
                        } finally {
                            streamInfo.connectedLock.unlock();
                        }

                    } else if (streamInfo.deprecated.contains(server)) { //check if the server was deprecated

                        streamInfo.disconnectingDeprecatedLock.lock();
                        try {
                            streamInfo.deprecated.remove(server);
                            streamInfo.disconnecting.add(server);
                            streamInfo.disconnectingDeprecatedEmpty.signal();
                        } finally {
                            streamInfo.disconnectingDeprecatedLock.unlock();
                        }
                    }
                    
                } else if (link.isDeactivate()) { //this means a server acepted the disconnect request
                    
                    streamInfo.disconnectingDeprecatedLock.lock();
                    try {
                        streamInfo.disconnecting.remove(server);
                    } finally {
                        streamInfo.disconnectingDeprecatedLock.unlock();
                    }
                                                                        
                }

            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}
