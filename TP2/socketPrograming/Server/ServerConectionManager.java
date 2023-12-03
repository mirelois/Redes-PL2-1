package Server;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import Protocols.Link;
import Protocols.Packet;
import Protocols.Sup;
import Server.Server.ServerSender;
import SharedStructures.Define;

public class ServerConectionManager implements Runnable {

    
    /*class ServerSender extends JFrame implements Runnable {

        String VideoFileName; // video file to request to the server
        Integer streamId;
        InetAddress rpIPAddr; // RP IP address
        int imagenb = 0; // image nb of the image currently transmitted
        int VIDEO_LENGTH = 500; // length of the video in frames
        VideoStream video; // Server.VideoStream object used to access video frames
        
        public ServerSender(String VideoFileName, Integer streamId, InetAddress rpIPAddr) {
            this.VideoFileName = VideoFileName;
            this.streamId = streamId;
            this.rpIPAddr = rpIPAddr;
            try {
                System.out.println("Tentar abrir o ficheiro: " + VideoFileName);
                video = new VideoStream(VideoFileName); // init the Server.VideoStream object:
            } catch (Exception e) {
                System.out.println("Servidor: erro no video: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            while (true) {
                // update current imagenb
                this.imagenb = (this.imagenb + 1) % VIDEO_LENGTH;
                
                try {
                    // get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(sBuf);
                    
                    // Builds an RTPpacket object containing the frame
                    Sup rtp_packet = new Sup(this.streamId, imagenb * FRAME_PERIOD, imagenb, rpIPAddr, RTP_dest_port,
                    image_length, sBuf);
                    Sup sup = new Sup(
                        0, //dizer que é responsabilidade do RP
                        Packet.getCurrTime(),
                        imagenb * FRAME_PERIOD,
                        imagenb,
                        0,// sequence number, zero porque who cares
                        //TODO: get a streamId,
                        //TODO get an adress,
                        port,
                        image_length,
                        sbuf
                    )
                    // Sup rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD,
                    // sBuf, image_length);

                    // get to total length of the full rtp packet to send
                    // int packet_length = rtp_packet.getlength();

                    // retrieve the packet bitstream and store it in an array of bytes
                    // byte[] packet_bits = new byte[packet_length];
                    // byte[] packet_bits = rtp_packet.getPacket();
                    // rtp_packet.getpacket(packet_bits);
                    
                    // send the packet as a DatagramPacket over the UDP socket
                    // senddp = new DatagramPacket(packet_bits, packet_length, rpIPAddr,
                    // RTP_dest_port);
                    RTPsocket.send(rtp_packet.toDatagramPacket());

                    System.out.println("Send frame #" + imagenb);
                    // print the header bitstream
                    // rtp_packet.printheader();

                    // update GUI
                    // label.setText("Send frame #" + imagenb);
                } catch (Exception ex) {
                    System.out.println("Exception caught: " + ex);
                    System.exit(0);
                }
            }
        }
    }*/
    
    InetAddress rpIPAddr;
    HashMap<Integer, Thread> serverSenderMap;
    HashMap<Integer, String> videoNameToStreamId;
    DatagramSocket streamSocket;

    public ServerConectionManager(InetAddress rpIPAddr,
                                  HashMap<Integer, Thread> serverSenderMap, 
                                  HashMap<Integer, String> streamIdToVideoName,
                                  DatagramSocket streamSocket) {
        this.rpIPAddr = rpIPAddr;
        this.videoNameToStreamId = streamIdToVideoName;
        this.serverSenderMap = serverSenderMap;
        this.streamSocket = streamSocket;
    }

    @Override
    public void run() {
        
        try (DatagramSocket socket = new DatagramSocket(Define.serverConnectionManagerPort)) {
            
            byte[] buf = new byte[Define.infoBuffer];
            
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            
            socket.receive(packet);

            Link link = new Link(packet);

            System.out.println("Recebido Link de " + link.getAddress() + " da stream " + link.getStreamId() + 
                               " do tipo activate: " + link.isActivate());

            if (link.isActivate()) {// NOTE: como é que o server sabe o id da stream?
                if (!serverSenderMap.containsKey(link.getStreamId())){
                    String videoName;
                    synchronized (videoNameToStreamId) {
                        videoName = videoNameToStreamId.get(link.getStreamId());
                    }
                    serverSenderMap.put(link.getStreamId(), 
                                            new Thread(new ServerSender(videoName, 
                                                       link.getStreamId(), this.rpIPAddr, this.streamSocket)));
                    
                    if (!serverSenderMap.get(link.getStreamId()).isAlive()){

                        serverSenderMap.get(link.getStreamId()).start();
                        
                    }
                }
                socket.send(new Link(
                        true, // signifies that this is an acknolegment
                        true,
                        false,
                        link.getStreamId(),
                        link.getAddress(),
                        link.getPort(),
                        0,
                        null).toDatagramPacket());

            } else if (link.isDeactivate()) {
                
                if (serverSenderMap.get(link.getStreamId()).isAlive()){

                    serverSenderMap.get(link.getStreamId()).interrupt();
                    
                }

                socket.send(new Link(
                        true,
                        false,
                        true,
                        link.getStreamId(),
                        link.getAddress(),
                        link.getPort(),
                        0,
                        null).toDatagramPacket());
            }

        } catch (Exception e) {
            // TODO: handle exception
        }

    }

}
