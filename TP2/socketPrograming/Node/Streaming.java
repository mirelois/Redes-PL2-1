package Node;
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

public class Streaming implements Runnable{
    
    private final NeighbourInfo neighbourInfo;

    public Streaming(NeighbourInfo neighbourInfo){
        
        this.neighbourInfo = neighbourInfo;
        
    }

    @Override
    public void run(){
        try(DatagramSocket socket = new DatagramSocket(Define.streamingPort)) {

            byte[] buf = new byte[Define.streamBuffer]; // 1024 is enough? no
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true){
                
                socket.receive(packet);

                Sup sup = new Sup(packet);

                NeighbourInfo.StreamInfo streamInfo;
                
                synchronized(neighbourInfo.streamIdToStreamInfo) {
                    streamInfo = neighbourInfo.streamIdToStreamInfo.get(sup.getStreamId());
                }
                
                System.out.println("Recebido SUP de " + sup.getAddress() + 
                                   "\n  Seq#: " + sup.getSequence_number());

                NeighbourInfo.LossInfo lossInfo = streamInfo.lossInfo;

                if (sup.getFrameNumber() < lossInfo.latestReceivedPacket) {
                    continue;//Manda cu caralho
                }

                int currentLatency = Packet.getLatency(sup.getTime_stamp());

                int arrival = Packet.getCurrTime();
                
                int timestap = Packet.getCurrTime();

                //see section 6.4.1 of rfc3550
                lossInfo.jitter = lossInfo.jitter + (Math.abs(lossInfo.prevDiff - (arrival - timestap)) - lossInfo.jitter)/16;

                lossInfo.prevDiff = (arrival - timestap);

                lossInfo.totalReceivedPacket++;

                lossInfo.lossRate = 1 - lossInfo.totalReceivedPacket/(double)sup.getFrameNumber();
                
                if (sup.getFrameNumber() < lossInfo.totalReceivedPacket) {
                    lossInfo.latestReceivedPacket = 0;
                    lossInfo.totalReceivedPacket = 0;
                    lossInfo.lossRate = -1;
                    lossInfo.jitter = -1;
                }
                                
                lossInfo.latestReceivedPacket = sup.getFrameNumber();

                //neighbourInfo.updateLatency(new NeighbourInfo.Node(sup.getAddress(), Packet.getLatency(sup.getTime_stamp())));
                double currentMetrics = Double.MAX_VALUE, bestMetrics = Double.MAX_VALUE;
                
                streamInfo.connectedLock.lock();
                
                try {
                    if (streamInfo.connected != null) {
                        streamInfo.connected.latency = currentLatency;
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

                int timeStampToSend = sup.getTime_stamp();

                System.out.println("\nMelhor: " + bestMetrics + 
                " do server " + this.neighbourInfo.minNodeQueue.peek().address.getHostName() + 
                ": " + this.neighbourInfo.minNodeQueue.peek().jitter +
                " , " + this.neighbourInfo.minNodeQueue.peek().latency +
                " , " + this.neighbourInfo.minNodeQueue.peek().lossRate);

                if (streamInfo.connected != null) {
                    System.out.println("\nCurrent: " + currentMetrics + 
                    " do server " + streamInfo.connected.address.getHostName() + 
                    ": " + streamInfo.connected.jitter +
                    " , " + streamInfo.connected.latency +
                    " , " + streamInfo.connected.lossRate);
                }

                if (bestMetrics < (0.95 * currentMetrics)) { //Mandar latencia melhor se isto fizer

                    NodeConnectionManager.updateBestNode(neighbourInfo, streamInfo, sup.getStreamId(), socket);
                    streamInfo.connectingLock.lock();
                    try{
                        timeStampToSend = (Packet.getCurrTime() - streamInfo.connecting.latency)%60000;
                    }finally{
                        streamInfo.connectingLock.unlock();
                    }
                }

                Integer streamId = sup.getStreamId();
                Set<InetAddress> streamActiveLinks;
                
                synchronized (this.neighbourInfo) {
                    streamActiveLinks = this.neighbourInfo.streamActiveLinks.get(streamId);
                }

                synchronized (streamActiveLinks) {
                    //TODO Como fazer os frame Numbers (o que são?)
                    for (InetAddress activeLink : streamActiveLinks) {
                        System.out.println("Enviando SUP da stream " + sup.getStreamId() + " para " + activeLink);
                        if (!activeLink.equals(sup.getAddress())) {
                            if (activeLink.equals(InetAddress.getByName("localhost"))) {
                                socket.send(new Sup(
                                    sup.getLossRate(),
                                    timeStampToSend,
                                    sup.getVideo_time_stamp(),
                                    sup.getFrameNumber(),
                                    sup.getSequence_number(),
                                    sup.getStreamId(),
                                    InetAddress.getByName("localhost"),//podia ser active link
                                    Define.clientPort,
                                    sup.getPayloadSize(),
                                    sup.getPayload()
                                ).toDatagramPacket());
                            } else {
                                socket.send(new Sup(
                                    sup.getLossRate(),
                                    timeStampToSend,
                                    sup.getVideo_time_stamp(),
                                    sup.getFrameNumber(),
                                    sup.getSequence_number(),
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
            }
            
        } catch (IOException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
