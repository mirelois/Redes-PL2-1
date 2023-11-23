/* ------------------
   Servidor
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar primeiro o cliente a correr, porque este dispara logo
   ---------------------- */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;


public class Server extends JFrame implements ActionListener, Runnable {

    //GUI:
    //----------------
    JLabel label;

    //RTP variables:
    //----------------
    DatagramPacket senddp; //UDP packet containing the video frames (to send)A
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    int rpAdderPort;
    int RTP_dest_port; //destination port for RTP packets
    InetAddress rpIPAddr; //RP IP address

    static String VideoFileName; //video file to request to the server

    //Video constants:
    //------------------
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    //static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer sTimer; //timer used to send the images at the video frame rate
    byte[] sBuf; //buffer used to store the images to send to the client

    public Map<Integer, Thread> senders = new HashMap<>(); // streams para Threads a enviá-las
    //--------------------------
    //Constructor
    //--------------------------
    public Server(InetAddress rpIPAddr, int port, int rpAdderPort, int streamPort) {
        //init Frame
        super("Servidor");

        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); //init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; //allocate memory for the sending buffer

        try {
            RTPsocket = new DatagramSocket(port); //init RTP socket
            this.rpIPAddr = rpIPAddr;
            this.rpAdderPort = rpAdderPort;
            this.RTP_dest_port = streamPort;
            //System.out.println("Servidor: socket " + ClientIPAddr);
            System.out.println("Servidor: vai enviar video da file " + VideoFileName);

        } catch (SocketException e) {
            System.out.println("Servidor: erro no socket: " + e.getMessage());
        }

        //Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                sTimer.stop();
                System.exit(0);
            }});

        //GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();
    }

    //------------------------------------
    //main/run
    //------------------------------------
    @Override
    public void run() {

        
        try {
            RTPsocket.send(new Simp(InetAddress.getByName("localhost"), rpIPAddr, this.rpAdderPort, 0, null).toDatagramPacket());
            while(true){
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                RTPsocket.receive(packet);
                Shrimp shrimp = new Shrimp(packet);
                if (senders.get(shrimp.getStreamId()) == null) {
                    Thread serverSender = new Thread(new ServerSender(shrimp.getPayload().toString(), shrimp.getStreamId(), this.rpIPAddr));
                    senders.put(shrimp.getStreamId(), serverSender);
                    serverSender.start();
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } catch (PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }

    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH)
        {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(sBuf);

                //Builds an RTPpacket object containing the frame
                Sup rtp_packet = new Sup(0,imagenb*FRAME_PERIOD, imagenb, rpIPAddr, RTP_dest_port, image_length, sBuf);
                //Sup rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, sBuf, image_length);

                //get to total length of the full rtp packet to send
                //int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                //byte[] packet_bits = new byte[packet_length];
                byte[] packet_bits = rtp_packet.getPacket();
                //rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket
                //senddp = new DatagramPacket(packet_bits, packet_length, rpIPAddr, RTP_dest_port);
                RTPsocket.send(senddp);

                System.out.println("Send frame #"+imagenb);
                //print the header bitstream
                //rtp_packet.printheader();

                //update GUI
                //label.setText("Send frame #" + imagenb);
            }
            catch(Exception ex)
            {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        else
        {
            //if we have reached the end of the video file, stop the timer
            sTimer.stop();
        }
    }

    class ServerSender extends JFrame implements Runnable {

        String VideoFileName; //video file to request to the server
        Integer streamId;
        InetAddress rpIPAddr; //RP IP address
        int imagenb = 0; //image nb of the image currently transmitted
        int VIDEO_LENGTH = 500; //length of the video in frames
        VideoStream video; //VideoStream object used to access video frames

        public ServerSender(String VideoFileName, Integer streamId, InetAddress rpIPAddr) {
            this.VideoFileName = VideoFileName;
            this.streamId = streamId;
            this.rpIPAddr = rpIPAddr;
            try {
                video = new VideoStream(VideoFileName); //init the VideoStream object:
            } catch (Exception e) {
                System.out.println("Servidor: erro no video: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            while(true) {
                //update current imagenb
                this.imagenb = (this.imagenb + 1) % VIDEO_LENGTH;

                try {
                    //get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(sBuf);

                    //Builds an RTPpacket object containing the frame
                    Sup rtp_packet = new Sup(this.streamId,imagenb*FRAME_PERIOD, imagenb, rpIPAddr, RTP_dest_port, image_length, sBuf);
                    //Sup rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, sBuf, image_length);

                    //get to total length of the full rtp packet to send
                    //int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    //byte[] packet_bits = new byte[packet_length];
                    //byte[] packet_bits = rtp_packet.getPacket();
                    //rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    //senddp = new DatagramPacket(packet_bits, packet_length, rpIPAddr, RTP_dest_port);
                    RTPsocket.send(rtp_packet.toDatagramPacket());

                    System.out.println("Send frame #"+imagenb);
                    //print the header bitstream
                    //rtp_packet.printheader();

                    //update GUI
                    //label.setText("Send frame #" + imagenb);
                }
                catch(Exception ex)
                {
                    System.out.println("Exception caught: "+ex);
                    System.exit(0);
                }
            }   
        }
    }
}
