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

import java.net.UnknownHostException;

public class NodeConnectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    static class Connector extends Thread {

        NeighbourInfo.StreamInfo streamInfo; 
        int streamId; 
        DatagramSocket socket;

        public Connector(NeighbourInfo.StreamInfo streamInfo, int streamId, DatagramSocket socket) {
            this.streamInfo = streamInfo;
            this.streamId = streamId;
            this.socket = socket;    
        }

        public void run() {

            NeighbourInfo.Node connecting;

            try {
                while (true) {

                    try {
                        streamInfo.connectingLock.lock();
                        while (streamInfo.connecting == null) {
                            streamInfo.connectingEmpty.wait();
                        }
                    } finally {
                        connecting = streamInfo.getConnecting();// copy of the currentBestServer
                        streamInfo.connectingLock.unlock();
                    }
                    System.out.println("Enviado Link de ativação para " + connecting.address + " da stream " + streamId);
                    socket.send(new Link(
                            false,
                            true,
                            false,
                            streamId,
                            connecting.address,
                            Define.linkPort,
                            0,
                            null).toDatagramPacket());

                    Thread.sleep(Define.RPTimeout);

                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class Disconnector extends Thread {

        NeighbourInfo.StreamInfo streamInfo; 
        int streamId; 
        DatagramSocket socket;

        public Disconnector(NeighbourInfo.StreamInfo streamInfo, int streamId, DatagramSocket socket) {
            this.streamInfo = streamInfo;
            this.streamId = streamId;
            this.socket = socket;    
        }

        //TODO Separar os Deprecated dos Disconnecting no futuro
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
                        System.out.println("Enviado Link de desativação para " + node.address + " da stream " + streamId);
                        socket.send(new Link(
                                false,
                                false,
                                true,
                                streamId,
                                node.address,
                                Define.linkPort,
                                0,
                                null).toDatagramPacket());
                    }

                    for (NeighbourInfo.Node node : deprecated) {
                        System.out.println("Enviado Link de desativação para " + node.address + " da stream " + streamId);
                        socket.send(new Link(
                                false,
                                true,
                                false,
                                streamId,
                                node.address,
                                Define.linkPort,
                                0,
                                null).toDatagramPacket());
                    }

                    Thread.sleep(Define.RPTimeout);

                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    NeighbourInfo neighbourInfo;

    NeighbourInfo.StreamInfo streamInfo;

    public NodeConnectionManager(NeighbourInfo neighbourInfo) {
        this.neighbourInfo = neighbourInfo;
    }

    //Called once at the start and everytime the Connected Node should change
    public static void updateBestNode( //TODO: fix args -> done?
        NeighbourInfo neighbourInfo,
        NeighbourInfo.StreamInfo streamInfo,
        Integer streamId,
        DatagramSocket socket) throws UnknownHostException { // TODO: called once, only in the first time it is needed

        if (streamInfo.connected != null) {
            neighbourInfo.updateLatency(streamInfo.connected);// bestServerLatency is the latency of the current best
        }
                                                       // server
        if (streamInfo.connectorThread == null) {
            
            streamInfo.connectorThread = new Connector(streamInfo, streamId, socket);
        }

        if (streamInfo.disconnectorThread == null) {
            streamInfo.disconnectorThread = new Disconnector(streamInfo, streamId, socket);
        }

        if (!streamInfo.connectorThread.isAlive()) {
            streamInfo.connectorThread.start();
        }
        if (!streamInfo.disconnectorThread.isAlive()) {
            streamInfo.disconnectorThread.start();
        }
        streamInfo.connectingLock.lock();
        try {
            /*synchronized (streamInfo.disconnecting) {
                if (streamInfo.connected != null) {
                    streamInfo.disconnecting.add(streamInfo.connected); //not supposed to add connected to disconnecting here
                }
            }*/
            streamInfo.disconnectingDeprecatedLock.lock();
            try {
                if (streamInfo.connecting != null) {
                    streamInfo.deprecated.add(streamInfo.connecting); //add unactivated packet to the remove list
                    streamInfo.disconnectingDeprecatedEmpty.signal();
                }
            } finally {
                streamInfo.disconnectingDeprecatedLock.unlock();
            }

            synchronized (neighbourInfo.minNodeQueue) {
                streamInfo.connecting = neighbourInfo.minNodeQueue.peek(); // this operation has complexity O(1)
                System.out.println("Alterado connecting para " + streamInfo.connecting);
            }
            streamInfo.connectingEmpty.signal();
        } finally {
            streamInfo.connectingLock.unlock();
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

                //RP never receives acks as their logic exists in RP
                if (!link.isAck()) {
                    Set<InetAddress> activeLinks;

                    if (link.isActivate()) {

                        boolean isActiveEmpty = false;
                        //TODO better locking mechanism to not stall the entire streamActiveLinks structure
                        synchronized(neighbourInfo.streamActiveLinks){

                            //Get the Set of active links (activated by clients)
                            activeLinks = neighbourInfo.streamActiveLinks.get(link.getStreamId());
                            isActiveEmpty = activeLinks.isEmpty();
                            activeLinks.add(link.getAddress());
                        }
                        
                        if (isActiveEmpty) {
                            //TODO Probably errors here! Don't know how this works fully
                            updateBestNode(neighbourInfo, streamInfo, link.getStreamId(), socket);
                        }
                        
                        socket.send(new Link(
                                        true,
                                        true,
                                        false,
                                        link.getStreamId(),
                                        link.getAddress(),
                                        Define.linkPort,
                                        0,
                                        null).toDatagramPacket());

                    } else if (link.isDeactivate()) {

                        synchronized(neighbourInfo.streamActiveLinks){

                            //Get the Set of active links (activated by clients)
                            activeLinks = neighbourInfo.streamActiveLinks.get(link.getStreamId());

                            //remove from the active links the one asked to be deactivated
                            activeLinks.remove(link.getAddress());

                            //if no more active links remain, then the stream doesn't have any more clients in that direction
                            if (activeLinks.isEmpty()) {
                                streamInfo.connectedLock.lock();
                                try {
                                    streamInfo.connectingLock.lock();
                                    try {
                                        streamInfo.disconnectingDeprecatedLock.lock();
                                        try {
                                            if (streamInfo.connected != null) {
                                                streamInfo.disconnecting.add(streamInfo.connected);
                                            }
                                            if (streamInfo.connecting != null) {
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

                        socket.send(new Link(
                                        true,
                                        false,
                                        true,
                                        link.getStreamId(),
                                        link.getAddress(),
                                        Define.linkPort,
                                        0,
                                        null).toDatagramPacket());
                    }  

                } else if (link.isActivate()) { //this is a connection confirmation acknolegment
                                         
                    if (node.equals(streamInfo.connecting)) { //this checks if connection has been established

                        streamInfo.connectedLock.lock();
                        try {
                            streamInfo.connectingLock.lock();
                            try {
                                streamInfo.disconnectingDeprecatedLock.lock();
                                try {
                                    streamInfo.disconnecting.add(streamInfo.connected);
                                    streamInfo.disconnectingDeprecatedEmpty.signal();
                                    streamInfo.connected = streamInfo.connecting;
                                    streamInfo.connecting = null;
                                } finally {
                                    streamInfo.disconnectingDeprecatedLock.unlock();
                                }
                            } finally {
                                streamInfo.connectingLock.unlock();
                            }
                        } finally {
                            streamInfo.connectedLock.unlock();
                        }

                    } else if (streamInfo.deprecated.contains(node)) {
                        
                        streamInfo.disconnectingDeprecatedLock.lock();
                        try {
                            streamInfo.deprecated.remove(node);
                            streamInfo.disconnecting.add(node);
                            streamInfo.disconnectingDeprecatedEmpty.signal();
                        } finally {
                            streamInfo.disconnectingDeprecatedLock.unlock();
                        }
                    }
                    
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
            // TODO: handle exception
        }
    }

}
