import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client implements Runnable{

    //GUI
    //----
    JFrame f = new JFrame("Cliente de Testes");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;

    //RTP variables:
    //----------------
    DatagramPacket rcvdp; //UDP packet received from the server (to receive)
    // DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets
    int simpPort = 7000;

    Timer cTimer; //timer used to receive data from the UDP socket
    byte[] cBuf; //buffer used to store data received from the server

    public Client(int port, stream stream, int simpPort) {

        this.simpPort = simpPort;
        //build GUI
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);

        // handlers... (so dois)
        playButton.addActionListener(new playButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390,370));
        f.setVisible(true);

        //init para a parte do cliente
        //--------------------------
        cTimer = new Timer(20, new clientTimerListener(port, stream, simpPort));
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server

        /*
        try {
            // socket e video
            RTPsocket = new DatagramSocket(RTP_RCV_PORT); //init RTP socket (o mesmo para o cliente e servidor)
            RTPsocket.setSoTimeout(5000); // setimeout to 5s
        } catch (SocketException e) {
            System.out.println("Cliente: erro no socket: " + e.getMessage());
        }
         */
    }


    // Run
    @Override
    public void run() {
        System.out.println("Do something"); // TODO fazer alguma coisa com o client tipo ler cenas
    }

    //------------------------------------
    //Handler for buttons
    //------------------------------------

    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            System.out.println("Play Button pressed !");
            //start the timers ...
            cTimer.start();
        }
    }

    //Handler for tear button
    //-----------------------
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            System.out.println("Teardown Button pressed !");
            //stop the timer
            cTimer.stop();
            //exit
            System.exit(0);
        }
    }

    //------------------------------------
    //Handler for timer (para cliente)
    //------------------------------------

    class clientTimerListener implements ActionListener {

        int port;
        int simpPort;
        stream stream;

        clientTimerListener(int port, stream stream, int simpPort){
            this.port = port;
            this.simpPort = simpPort;
            stream = new stream();
        }

        public void actionPerformed(ActionEvent e) {
            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);
            //receive the DP from the socket:
            try(DatagramSocket RTPsocket = new DatagramSocket(this.port)) {

                // TODO falta só configurar isto aqui, mandar o simp para o meu nodo aka o streaming de mim mesmo
                RTPsocket.send(new Simp(InetAddress.getByName("localhost"), InetAddress.getByName("localhost"), this.simpPort, stream.file.length(), stream.file.getBytes()).toDatagramPacket());

                while(true) {
                    RTPsocket.receive(rcvdp);

                    //create an RTPpacket object from the DP
                    Sup rtp_packet = new Sup(rcvdp);

                    //print important header fields of the RTP packet received:
                    //System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getSequenceNumber() +" TimeStamp "+rtp_packet.getTimeStamp());//+" ms, of type "+rtp_packet.getpayloadtype());

                    //print header bitstream:
                    //rtp_packet.printheader();

                    //get the payload bitstream from the RTPpacket object
                    byte[] payload = rtp_packet.getPayload();

                    //get an Image object from the payload bitstream
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    Image image = toolkit.createImage(payload, 0, payload.length);

                    //display the image as an ImageIcon object
                    icon = new ImageIcon(image);
                    iconLabel.setIcon(icon);
                }
            } catch (IOException eio){
                eio.printStackTrace();
            } catch (PacketSizeException ex) {
                throw new RuntimeException(ex);
            }

        }
    }
}
