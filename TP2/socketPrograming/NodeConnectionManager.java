public class RPConectionManager implements Runnable { // TODO: ver concorrencia e meter synchronized para ai

    ServerInfo serverInfo;

    int streamId;

    String streamName;

    ServerInfo.StreamInfo streamInfo;

    public RPConectionManager(ServerInfo serverInfo) {

        // this.serverInfo = serverInfo;
        // this.streamId = streamId;
        // this.streamName = streamName;
        this.serverInfo = serverInfo;

    }

    public static void updateBestServer(ServerInfo.StreamInfo streamInfo, Integer streamId, int bestServerLatency, DatagramSocket socket)
            throws UnknownHostException { // TODO: currently this is never called stfu

        streamInfo.updateLatency(streamInfo.currentBestServer);// bestServerLatency is the latency of the current best
                                                               // server
        if (streamInfo.connectorThread == null) {
            
            streamInfo.connectorThread = new Thread(new Runnable() {

                public void run() {

                    ServerInfo.StreamInfo.Server connecting;

                    try {
                        while (true) {

                            synchronized (streamInfo.connecting) {
                                while (streamInfo.connecting != null) {
                                    streamInfo.connecting.wait();
                                }
                                connecting = streamInfo.getConnecting();// copy of the currentBestServer
                            }

                            socket.send(new Link(
                                    true,
                                    false,
                                    streamId,
                                    connecting.address,
                                    Define.serverPort,
                                    0,
                                    null).toDatagramPacket());

                            Thread.sleep(Define.RPTimeout);

                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (streamInfo.disconnectorThread == null) {
            
            streamInfo.disconnectorThread = new Thread(new Runnable() {

                public void run() {

                    HashSet<ServerInfo.StreamInfo.Server> disconnecting;
                    HashSet<ServerInfo.StreamInfo.Server> deprecated;

                    try {
                        while (true) {
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

                            for (ServerInfo.StreamInfo.Server server : disconnecting) { // sends disconect link to
                                                                                        // all servers in
                                socket.send(new Link(
                                        false,
                                        true,
                                        streamId,
                                        server.address,
                                        Define.serverPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            for (ServerInfo.StreamInfo.Server server : deprecated) {
                                
                                socket.send(new Link(
                                        true,
                                        false,
                                        streamId,
                                        server.address,
                                        Define.serverPort,
                                        0,
                                        null).toDatagramPacket());
                            }

                            Thread.sleep(Define.RPTimeout);

                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (streamInfo.connectorThread.isAlive()) {
            streamInfo.connectorThread.start();
        }
        if (streamInfo.disconnectorThread.isAlive()) {
            streamInfo.disconnectorThread.start();
        }

        synchronized (streamInfo.connecting) {
            synchronized (streamInfo.minServer) {
                if (streamInfo.connecting != null) {
                    streamInfo.disconnecting.add(streamInfo.connecting); //add unactivated packet to the remove list
                }
                streamInfo.connecting = streamInfo.minServer.peek(); // this operation has complexity O(1)
            }

            streamInfo.connecting.notify();
        }

    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(Define.RPConectionManagerPort)) {

            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) { // main thread only listens for Shrimp, it checks who sent the packet

                socket.receive(packet);

                Link link = new Link(packet);

                this.streamInfo = serverInfo.streamInfo.get(link.getStreamId());

                ServerInfo.StreamInfo.Server server = 
                    new ServerInfo.StreamInfo.Server(link.getAddress(), Integer.MAX_VALUE);

                if (link.isActivate()) { //this is a connection confirmation acknolegment
                                         
                    if (server.equals(streamInfo.connecting)) { //this checks if connection has been established

                        streamInfo.disconnectingDeprecatedLock.lock();
                        try{
                            streamInfo.disconnecting.add(streamInfo.currentBestServer);
                            streamInfo.disconnectingDeprecatedEmpty.notify();
                        } finally {
                            streamInfo.disconnectingDeprecatedLock.unlock();
                        }

                        streamInfo.currentBestServer = streamInfo.connecting;

                        streamInfo.connecting = null;

                    }else if (streamInfo.deprecated.contains(server)){
                        streamInfo.disconnectingDeprecatedLock.lock();
                        try{
                            streamInfo.disconnecting.add(server);
                            streamInfo.disconnectingDeprecatedEmpty.notify();
                        }finally {
                            streamInfo.disconnectingDeprecatedLock.unlock();
                        }
                    }
                    
                }else if (link.isDeactivate()) { //this means a server acepted the disconnect request
                    
                    synchronized(streamInfo.disconnecting){
                        streamInfo.disconnecting.remove(server);
                    }
                                                                        
                }

            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}
