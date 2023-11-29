import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;

public class RPConectionManager implements Runnable{ //TODO: ver concorrencia e meter synchronized para ai

    ServerInfo serverInfo;

    int streamId;

    String streamName;

    ServerInfo.StreamInfo streamInfo;

    Thread connectorThread;

    Thread disconnectorThread;

    public RPConectionManager(ServerInfo serverInfo, int streamId, String streamName){
        
        this.serverInfo = serverInfo;
        this.streamId   = streamId;
        this.streamName = streamName;
        this.streamInfo = serverInfo.streamInfo.get(streamId);

    }

    public void updateBestServer(ServerInfo.StreamInfo streamInfo, int bestServerLatency, DatagramSocket socket) throws UnknownHostException{ // TODO: currently this is never called

        streamInfo.updateLatency(streamInfo.currentBestServer);//bestServerLatency is the latency of the current best server

        streamInfo.connecting = streamInfo.minServer.peek(); //this operation has complexity O(1)

        streamInfo.connecting.notify();
    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.RPConectionManagerPort)) {
            
            connectorThread = new Thread(() -> {
                while (true) {
                    while (streamInfo.currentBestServer != null) {
                        try {
							streamInfo.currentBestServer.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                    try {
                        socket.send(new Link(
                                    true,
                                    false,
                                    this.streamId,
                                    streamInfo.connecting.address,
                                    Define.serverPort,
                                    0,
                                    null
                                    ).toDatagramPacket());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(Define.RPTimeout);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });

            disconnectorThread = new Thread(() -> {
                while(true){
                    while (streamInfo.disconnecting.isEmpty()) { //sleeps if there is nothing to remove
                        try {
                            streamInfo.disconnecting.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    for (ServerInfo.StreamInfo.Server server : streamInfo.disconnecting) { //sends disconect link to all servers in disconnecting
                        try {
                            socket.send(new Link(
                                        false,
                                        true,
                                        this.streamId,
                                        server.address,
                                        Define.serverPort,
                                        0,
                                        null
                                        ).toDatagramPacket());
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            });

            connectorThread.start();
            
            disconnectorThread.start();
            

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { //main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);
                
                Shrimp shrimp = new Shrimp(packet);

                ServerInfo.StreamInfo.Server server = new ServerInfo.StreamInfo.Server(shrimp.getAddress(), Packet.getLatency(shrimp.getTimeStamp()));
                
                if(streamInfo.disconnecting.contains(server)){ 
                    //recebeu confirmação de remoção, o server
                    //vai parar de mandar cenas
                    streamInfo.disconnecting.remove(server);
                }
                if(streamInfo.connecting.equals(server)){ //recebeu confirmação de adição
                                                         //vai trocar o currentBest e remover o antigo

                    connectorThread.interrupt(); //TODO: podemos manter a thread viva e dar notify em cima

                    streamInfo.disconnecting.add(streamInfo.currentBestServer);

                    streamInfo.disconnecting.notify();

                    streamInfo.currentBestServer = streamInfo.connecting;

                    streamInfo.connecting = null;
                    
                }

            }
            
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
}
