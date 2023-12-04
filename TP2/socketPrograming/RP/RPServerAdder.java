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

    private int curr_streamID = 1;

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
                System.out.println("Recebido Shrimp de conexão do servidor " + shrimp.getAddress());
                int latency = Packet.getLatency(shrimp.getTimeStamp());
                
                String streamName = new String(shrimp.getPayload());
                Integer streamId;
                //Verificar se a stream já existe no RP
                synchronized (this.neighbourInfo) {
                    streamId = this.neighbourInfo.fileNameToStreamId.get(streamName);
                    ServerInfo.StreamInfo streamInfo;
                    synchronized(serverInfo) {
                        
                        if (streamId == null) { //Stream ainda não existe no RP
                            System.out.println("    Adicionado ficheiro " + streamName + "com o id" + 
                                               streamId + " para o servidor " + shrimp.getAddress().getHostName());
                            this.neighbourInfo.fileNameToStreamId.put(streamName, curr_streamID);
                            streamId = curr_streamID;
                            curr_streamID++;
                            streamInfo = new ServerInfo.StreamInfo(streamId);
                            this.serverInfo.streamInfoMap.put(streamId, streamInfo);

                        } else { //Stream já existe no RP
                            streamInfo = this.serverInfo.streamInfoMap.get(streamId);
                        }
                        
                        streamInfo.updateLatency(new ServerInfo.StreamInfo.Server(shrimp.getAddress(), latency));
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
