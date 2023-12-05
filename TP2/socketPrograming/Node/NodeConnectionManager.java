package Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import Protocols.Link;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

import java.net.UnknownHostException;

public class NodeConnectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    NeighbourInfo neighbourInfo;

    public NodeConnectionManager(NeighbourInfo neighbourInfo) {

        this.neighbourInfo = neighbourInfo;
    }

    //Called once at the start and everytime the Connected Node should change
    public static void updateBestNode( //TODO: fix args -> done? nao era isso lmao
        NeighbourInfo neighbourInfo,
        NeighbourInfo.StreamInfo streamInfo,
        DatagramSocket socket) throws UnknownHostException { // TODO: called once, only in the first time it is needed
        System.out.println("Update ao best Node da stream " + streamInfo.streamId);

        if (streamInfo.connected != null) {
            neighbourInfo.updateLatency(streamInfo.connected);// bestServerLatency is the latency of the current best
        }

        if (streamInfo.connectorThread == null) {
            System.out.println("Started connector thread.");
            streamInfo.connectorThread = new Thread(new Runnable() {

                public void run() {

                    NeighbourInfo.Node connecting;

                    try {
                        while (true) {

                            try {
                                streamInfo.connectingLock.lock();
                                while (streamInfo.connecting == null) {
                                    streamInfo.connectingEmpty.await();
                                }
                                connecting = streamInfo.getConnecting();// copy of the currentBestServer
                            } finally {
                                streamInfo.connectingLock.unlock();
                            }
                            System.out.println("Enviado Link de ativação para " + connecting.address.getHostName() + 
                                               " da stream " + streamInfo.streamId);
                            socket.send(new Link(
                                    false,
                                    true,
                                    false,
                                    streamInfo.streamId,
                                    connecting.address,
                                    Define.nodeConnectionManagerPort,
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
            System.out.println("Started disconnector thread.");
            streamInfo.disconnectorThread = new Thread(new Runnable() {

                public void run() {

                    HashSet<NeighbourInfo.Node> disconnecting;
                    HashSet<NeighbourInfo.Node> deprecated;

                    try {
                        while (true) {
                            disconnecting = null;
                            deprecated = null;
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

                            for (NeighbourInfo.Node node : disconnecting) { // sends disconect link to
                                System.out.println("Enviado Link de desativação para " + node.address + " da stream " + streamInfo.streamId);
                                socket.send(new Link(
                                        false,
                                        false,
                                        true,
                                        streamInfo.streamId,
                                        node.address,
                                        Define.nodeConnectionManagerPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            for (NeighbourInfo.Node node : deprecated) {
                                System.out.println("Enviado Link de desativação para " + node.address + " da stream " + streamInfo.streamId);
                                socket.send(new Link(
                                        false,
                                        true,
                                        false,
                                        streamInfo.streamId,
                                        node.address,
                                        Define.nodeConnectionManagerPort,
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

        if (!streamInfo.connectorThread.isAlive()) {
            streamInfo.connectorThread.start();
        }
        if (!streamInfo.disconnectorThread.isAlive()) {
            streamInfo.disconnectorThread.start();
        }

        streamInfo.connectedLock.lock();
        try {
            streamInfo.connectingLock.lock();
            try {
                /*synchronized (streamInfo.disconnecting) {
                    if (streamInfo.connected != null) {
                        streamInfo.disconnecting.add(streamInfo.connected); //not supposed to add connected to disconnecting here
                    }
                }*/
                synchronized (neighbourInfo.minNodeQueue) {
                    if (!neighbourInfo.minNodeQueue.peek().equals(streamInfo.connected) &&
                        !neighbourInfo.minNodeQueue.peek().equals(streamInfo.connecting)) {
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
            
                        streamInfo.connecting = neighbourInfo.minNodeQueue.peek(); // this operation has complexity O(1)
                        System.out.println("Alterado connecting para " + streamInfo.connecting.address.getHostName());
                        streamInfo.connectingEmpty.signal();
                    }
                }
            } finally {
                streamInfo.connectingLock.unlock();
            }
        } finally {
            streamInfo.connectedLock.unlock();
        }

    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.nodeConnectionManagerPort)) {
            
            System.out.println("Started chooser Thread");
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(Define.chooserThreadTimeOut);
                        for (NeighbourInfo.StreamInfo streamInfo : this.neighbourInfo.streamIdToStreamInfo.values()) {
                            updateBestNode(neighbourInfo, streamInfo, socket);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { // main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);

                Link link = new Link(packet);
                System.out.println("Recebido Link de " + link.getAddress() + "\n    do tipo activate: " + link.isActivate() + 
                                   "\n  do tipo ack: " + link.isAck());
                NeighbourInfo.StreamInfo streamInfo = this.neighbourInfo.streamIdToStreamInfo.get(link.getStreamId());
                //TODO this.streamInfo = neighbourInfo.streamIdToStreamInfo.get(link.getStreamId());

                NeighbourInfo.Node node = 
                    new NeighbourInfo.Node(link.getAddress(), Integer.MAX_VALUE);

                //RP never receives acks as their logic exists in RP
                if (!link.isAck()) {
                    Set<InetAddress> activeLinks;

                    if (link.isActivate()) {

                        boolean isActiveEmpty = false;
                        //TODO better locking mechanism to not stall the entire streamActiveLinks structure
                        synchronized(neighbourInfo.streamActiveLinks) {
                            
                            //Get the Set of active links (activated by clients)
                            activeLinks = neighbourInfo.streamActiveLinks.get(link.getStreamId());
                            if (activeLinks == null) {
                                activeLinks = new HashSet<InetAddress>();
                                neighbourInfo.streamActiveLinks.put(link.getStreamId(), activeLinks);
                            }
                            isActiveEmpty = activeLinks.isEmpty();
                            //Tem de ter o active link local ou não manda para o streaming
                            activeLinks.add(link.getAddress());
                        }
                        
                        if (isActiveEmpty) {
                            System.out.println("    Novo active link, update ao melhor nodo");
                            updateBestNode(this.neighbourInfo, streamInfo, socket);
                        }

                        //Evitar loops de links ao apenas considerar respostas a não locais
                        if (!link.getAddress().equals(InetAddress.getByName("localhost"))) {
                            synchronized(streamInfo.clientAdjacent) {
                                Integer n = streamInfo.clientAdjacent.get(link.getAddress());
                                if (n == null) {
                                    streamInfo.clientAdjacent.put(link.getAddress(), 1);
                                } else {
                                    streamInfo.clientAdjacent.put(link.getAddress(), n + 1);
                                }
                            }
                            System.out.println("Enviada resposta de Link de ativação para " + link.getAddress());
                            socket.send(new Link(
                                            true,
                                            true,
                                            false,
                                            link.getStreamId(),
                                            link.getAddress(),
                                            Define.nodeConnectionManagerPort,
                                            0,
                                            null).toDatagramPacket());
                        }

                    } else if (link.isDeactivate()) {

                        synchronized(neighbourInfo.streamActiveLinks){

                            //Get the Set of active links (activated by clients)
                            activeLinks = neighbourInfo.streamActiveLinks.get(link.getStreamId());

                            //remove from the active links the one asked to be deactivated
                            activeLinks.remove(link.getAddress());

                            //if no more active links remain, then the stream doesn't have any more clients in that direction
                            if (activeLinks.isEmpty()) {
                                System.out.println("    Active links became empty!");
                                streamInfo.connectedLock.lock();
                                try {
                                    streamInfo.connectingLock.lock();
                                    try {
                                        streamInfo.disconnectingDeprecatedLock.lock();
                                        try {
                                            if (streamInfo.connected != null) {
                                                System.out.println("    Added to the disconnecting: " + streamInfo.connected);
                                                streamInfo.disconnecting.add(streamInfo.connected);
                                            }
                                            if (streamInfo.connecting != null) {
                                                System.out.println("    Added to the deprecated: " + streamInfo.connecting);
                                                streamInfo.deprecated.add(streamInfo.connecting);
                                            }
                                            streamInfo.connecting = null;
                                            streamInfo.connected = null;
                                            streamInfo.disconnectingDeprecatedEmpty.signal();
                                        } finally {
                                            streamInfo.disconnectingDeprecatedLock.unlock();
                                        }
                                    } finally {
                                        streamInfo.connectingLock.unlock();
                                    }
                                } finally {
                                    streamInfo.connectedLock.unlock();
                                }
                            }

                        }

                        synchronized(streamInfo.clientAdjacent) {
                            streamInfo.clientAdjacent.put(link.getAddress(), streamInfo.clientAdjacent.get(link.getAddress()) - 1);
                        }

                        socket.send(new Link(
                                        true,
                                        false,
                                        true,
                                        link.getStreamId(),
                                        link.getAddress(),
                                        Define.nodeConnectionManagerPort,
                                        0,
                                        null).toDatagramPacket());
                    }  

                } else if (link.isActivate()) { //this is a connection confirmation acknolegment
                    //who cares
                    
                } else if (link.isDeactivate()) { //this means a node acepted the disconnect request
                    
                    streamInfo.disconnectingDeprecatedLock.lock();
                    try {
                        streamInfo.disconnecting.remove(node);
                    } finally {
                        streamInfo.disconnectingDeprecatedLock.unlock();
                    }
                                                                        
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
    }

}
