package RP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import Protocols.Packet;
import Protocols.PacketSizeException;
import Protocols.Shrimp;
import SharedStructures.Define;
import SharedStructures.ServerInfo;

public class RPServerAdder implements Runnable{
    
    private final ServerInfo serverInfo;

    public RPServerAdder(ServerInfo serverInfo){
        this.serverInfo = serverInfo;
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
                    serverInfo.latencyMap.put(shrimp.getAddress(), latency);
                }
                
                System.out.println("Adicionado servidor de endereço " + shrimp.getAddress().getHostAddress());

                //TODO fazer check de perdas para nao dar barraco


            } catch (IOException | PacketSizeException e) {
                //TODO: handle exception
                e.printStackTrace();
            }
        }
        
    }
}
