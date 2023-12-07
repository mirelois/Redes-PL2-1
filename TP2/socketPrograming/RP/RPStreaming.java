package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;

import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Sup;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class RPStreaming implements Runnable{
    
    private final NeighbourInfo neighbourInfo;

    private final ServerInfo serverInfo;

    public RPStreaming(ServerInfo serverInfo, NeighbourInfo neighbourInfo){
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(Define.streamingPort)){

            System.out.println("Started chooser Thread");
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(Define.chooserThreadTimeOut);
                        synchronized (this.neighbourInfo.streamActiveLinks) {
                            /*while (this.neighbourInfo.streamActiveLinks.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()).isEmpty()) {
                                this.neighbourInfo.streamActiveLinks.wait();
                            }*/
                            for (ServerInfo.StreamInfo streamInfo : this.serverInfo.streamInfoMap.values()) {
                                System.out.println("Update de timeout à stream " + streamInfo.streamId);
                                RPServerConectionManager.updateBestServer(streamInfo, neighbourInfo, socket);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            byte[] buf = new byte[Define.streamBuffer]; // 1024 is enough? no
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                socket.receive(packet);

                Sup sup = new Sup(packet); //this came from a server
                   
                ServerInfo.StreamInfo streamInfo;

                synchronized(serverInfo.streamInfoMap) {
                    streamInfo = serverInfo.streamInfoMap.get(sup.getStreamId());
                }

                NeighbourInfo.LossInfo lossInfo = streamInfo.lossInfo;

                if (sup.getSequenceNumber() < lossInfo.latestReceivedPacket) {
                    continue;
                }

                int currentLatency = Packet.getLatency(sup.getTime_stamp());

                int arrival = Packet.getCurrTime();
                
                int timestap = Packet.getCurrTime();

                //see section 6.4.1 of rfc3550
                lossInfo.jitter = lossInfo.jitter + (Math.abs(lossInfo.prevDiff - (arrival - timestap)) - lossInfo.jitter)/16;

                lossInfo.prevDiff = (arrival - timestap);

                lossInfo.totalReceivedPacket++;

                lossInfo.lossRate = 1 - lossInfo.totalReceivedPacket/(double)sup.getSequenceNumber();
                
                if (sup.getSequenceNumber() < lossInfo.totalReceivedPacket) {
                    lossInfo.latestReceivedPacket = 0;
                    lossInfo.totalReceivedPacket = 0;
                    lossInfo.lossRate = -1;
                    lossInfo.jitter = -1;
                }
                                
                lossInfo.latestReceivedPacket = sup.getSequenceNumber();
                
                System.out.println("Recebida Stream " + sup.getStreamId() + " de " + sup.getAddress());
                //calculatet and update server latencies

                Integer currLatency = Packet.getLatency(sup.getTime_stamp());
                double currentMetrics = Double.MAX_VALUE, bestMetrics = Double.MAX_VALUE;
                 
                streamInfo.connectedLock.lock();
                
                try {
                    if (streamInfo.connected != null) {
                        streamInfo.connected.latency = currLatency;
                        streamInfo.connected.lossRate = lossInfo.lossRate;
                        currentMetrics = streamInfo.connected.getMetrics();
                    }
                } finally {
                    streamInfo.connectedLock.unlock();
                }
                
                synchronized(this.neighbourInfo.minNodeQueue) {
                    if (streamInfo.minServerQueue.peek() != null)
                        bestMetrics = streamInfo.minServerQueue.peek().getMetrics();
                }

                if (bestMetrics < 0.95 * currentMetrics) {
                    RPServerConectionManager.updateBestServer(streamInfo, neighbourInfo, socket);
                }

                Set<InetAddress> streamActiveLinks;
                
                System.out.println("\nMelhor: " + bestMetrics +
                                       " do server " + streamInfo.minServerQueue.peek().address.getHostName() +
                                       ": " + streamInfo.minServerQueue.peek().jitter +
                                       " , " + streamInfo.minServerQueue.peek().latency +
                                       " , " + streamInfo.minServerQueue.peek().lossRate);
    
                if (streamInfo.connected != null) {
                    System.out.println("\nCurrent: " + currentMetrics +
                                        " do server " + streamInfo.connected.address.getHostName() +
                                        ": " + streamInfo.connected.jitter +
                                        " , " + streamInfo.connected.latency +
                                        " , " + streamInfo.connected.lossRate);
                }

                synchronized (this.neighbourInfo) {
                    
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(sup.getStreamId());
                    
                }

                synchronized(streamActiveLinks) {
                    for (InetAddress activeLink : streamActiveLinks) {
                        synchronized(activeLink) {
                            //System.out.println("Enviada stream " + sup.getStreamId() + " para: " + activeLink +
                        //                   " com seq#: " + sup.getSequenceNumber());
                        socket.send(new Sup(
                                Packet.getCurrTime(),
                                sup.getFrameNumber(),
                                sup.getSequenceNumber(),
                                sup.getStreamId(),
                                activeLink,
                                Define.streamingPort,
                                sup.getPayloadSize(),
                                sup.getPayload()
                            ).toDatagramPacket());    
                        }
                        
                    }
                }
            }
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
