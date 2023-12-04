package Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Set;

import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Sup;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;

public class Streaming implements Runnable{

    class lossInfo {
        
        int latestReceivedPacket = 0;
        int totalReceivedPacket = 0;
        double lossRate = -1;
        
    }
    HashMap<Integer, lossInfo> lossInfo = new HashMap<>();
    
    private final NeighbourInfo neighbourInfo;
    NeighbourInfo.StreamInfo streamInfo;

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

                lossInfo lossInfo = this.lossInfo.get(sup.getStreamId());

                if (sup.getFrameNumber() < lossInfo.latestReceivedPacket) {
                    continue;//Manda cu caralho
                }

                lossInfo.totalReceivedPacket++;

                lossInfo.lossRate = 1 - lossInfo.totalReceivedPacket/(double)sup.getFrameNumber();
                
                if (sup.getFrameNumber() < lossInfo.totalReceivedPacket) {
                    lossInfo.totalReceivedPacket = 0;
                    lossInfo.latestReceivedPacket = 0;
                    lossInfo.lossRate = -1.;
                }
                                
                lossInfo.latestReceivedPacket = sup.getFrameNumber();

                //neighbourInfo.updateLatency(new NeighbourInfo.Node(sup.getAddress(), Packet.getLatency(sup.getTime_stamp())));
                Integer currLatency = Packet.getLatency(sup.getTime_stamp());
                double currentMetrics = Double.MAX_VALUE, bestMetrics = Double.MAX_VALUE;
                
                this.streamInfo.connectedLock.lock();
                
                try {
                    if (streamInfo.connected != null) {
                        this.streamInfo.connected.latency = currLatency;
                        this.streamInfo.connected.lossRate = lossInfo.lossRate;
                        currentMetrics = this.streamInfo.connected.getMetrics();
                    }
                } finally {
                    this.streamInfo.connectedLock.unlock();
                }
                
                synchronized(this.neighbourInfo.minNodeQueue) {
                    if (this.neighbourInfo.minNodeQueue.peek() != null)
                        bestMetrics = this.neighbourInfo.minNodeQueue.peek().getMetrics();    
                }

                int timeStampToSend = sup.getTime_stamp();

                if (bestMetrics < 0.95 * currentMetrics) { //Mandar latencia melhor se isto fizer
                    NodeConnectionManager.updateBestNode(neighbourInfo, streamInfo, sup.getStreamId(), socket);
                    this.streamInfo.connectingLock.lock();
                    try{
                        timeStampToSend = (Packet.getCurrTime() - this.streamInfo.connecting.latency)%60000;
                    }finally{
                        this.streamInfo.connectingLock.unlock();
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
