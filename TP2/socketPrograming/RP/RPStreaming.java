package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Set;

import Node.NodeConnectionManager;
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
                    if (this.neighbourInfo.minNodeQueue.peek() != null)
                        bestMetrics = this.neighbourInfo.minNodeQueue.peek().getMetrics();
                }

                if (bestMetrics < 0.95 * currentMetrics) {
                    RPServerConectionManager.updateBestServer(streamInfo, sup.getStreamId(), socket);
                }

                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(sup.getStreamId());
                    
                }

                for (InetAddress activeLink : streamActiveLinks) {
                    System.out.println("Enviada stream " + sup.getStreamId() + " para: " + activeLink +
                                       " com seq#: " + sup.getSequenceNumber());
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
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
