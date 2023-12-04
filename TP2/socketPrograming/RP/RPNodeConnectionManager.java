package RP;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import Protocols.Link;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class RPNodeConnectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    ServerInfo serverInfo;

    NeighbourInfo neighbourInfo;


    public RPNodeConnectionManager(ServerInfo serverInfo, NeighbourInfo neighbourInfo) {
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.nodeConnectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { // main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);

                Link link = new Link(packet);
                System.out.println("Recebido Link de " + link.getAddress() + " do tipo activate: " + link.isActivate());

                //TODO this.streamInfo = neighbourInfo.streamIdToStreamInfo.get(link.getStreamId());

                //Can be null if the stream hasn't yet been intrudeced, but that is checked by RP
                ServerInfo.StreamInfo streamInfo = this.serverInfo.streamInfoMap.get(link.getStreamId());

                Set<InetAddress> activeLinks;

                if (link.isActivate()) {

                    //TODO better locking mechanism to not stall the entire streamActiveLinks structure
                    synchronized(neighbourInfo.streamActiveLinks) {
                        //Get the Set of active links (activated by clients)
                        activeLinks = neighbourInfo.streamActiveLinks.get(link.getStreamId());
                        if (activeLinks == null) {
                            activeLinks = new HashSet<InetAddress>();
                            neighbourInfo.streamActiveLinks.put(link.getStreamId(), activeLinks);
                        }
                        activeLinks.add(link.getAddress());
                    }
                    
                    if (streamInfo.connected == null && streamInfo.connecting == null) {
                        System.out.println("    Recebido pedido da Stream " + link.getStreamId() + " que ainda não estava ativa");
                        RPServerConectionManager.updateBestServer(streamInfo, link.getStreamId(), socket);
                    }
                    
                    synchronized(streamInfo.clientAdjacent) {
                        streamInfo.clientAdjacent.put(link.getAddress(), streamInfo.clientAdjacent.get(link.getAddress()) + 1);
                    }

                    socket.send(new Link(
                                    true,
                                    true,
                                    false,
                                    link.getStreamId(),
                                    link.getAddress(),
                                    Define.nodeConnectionManagerPort,
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
    }

}
