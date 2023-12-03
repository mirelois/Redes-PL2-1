package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Shrimp;
import SharedStructures.Define;
import SharedStructures.*;

public class RPServerAdder implements Runnable{
    
    private final ServerInfo serverInfo;

    private final NeighbourInfo neighbourInfo;

    private int curr_streamID;

    public RPServerAdder(ServerInfo serverInfo, NeighbourInfo neighbourInfo){
        this.serverInfo = serverInfo;
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run(){

        byte[] buf = new byte[Define.infoBuffer];

        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while(true){
            try (DatagramSocket socket = new DatagramSocket(Define.RPServerAdderPort)) {

                socket.receive(packet);

                Shrimp shrimp = new Shrimp(packet); // servidor manda shrimps

                int latency = Packet.getLatency(shrimp.getTimeStamp());

                synchronized(this.serverInfo){
                    for (ServerInfo.StreamInfo streamInfo : serverInfo.streamInfoMap.values()) {
                        streamInfo.updateLatency(new ServerInfo.StreamInfo.Server(shrimp.getAddress(), latency));
                    }
                }
                
                System.out.println("Adicionado servidor de endereço " + shrimp.getAddress().getHostAddress());
                String streamName = new String(shrimp.getPayload());
                Integer streamId;
                synchronized (this.neighbourInfo) {

                    streamId = this.neighbourInfo.fileNameToStreamId.get(streamName);

                    if (streamId == null) {
                        this.neighbourInfo.fileNameToStreamId.put(streamName, curr_streamID);
                        streamId = curr_streamID;
                        curr_streamID++;
                    }

                }

                socket.send(new Shrimp(0, InetAddress.getByName("localhost"), streamId, Define.serverPort, shrimp.getAddress(), shrimp.getPayloadSize(), shrimp.getPayload()).toDatagramPacket());

                //TODO fazer check de perdas para nao dar barraco


            } catch (IOException | PacketSizeException e) {
                //TODO: handle exception
                e.printStackTrace();
            }
        }
        
    }
}
