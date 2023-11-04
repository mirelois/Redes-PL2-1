import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;

public class BootStrapper{

    public static HashMap<InetAddress, ArrayList<InetAddress>> getTree(String filePath) {

        FileInputStream stream = null;

        HashMap<InetAddress, ArrayList<InetAddress>> tree = new HashMap<InetAddress, ArrayList<InetAddress>>();

        try {
            stream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String strLine;
        // ArrayList<String> alias = new ArrayList<String>();

        try {

            HashMap<String, InetAddress> map = new HashMap<String, InetAddress>();

            Pattern pattern = Pattern.compile("([^ ,]+?):((?:\\d+\\.\\d+\\.\\d+\\.\\d+)|localhost)"); // matches stuff
                                                                                                      // like n1:1.1.1.1
                                                                                                      // and
                                                                                                      // n2:localhost;

            Matcher matcher = pattern.matcher(reader.readLine());

            while (matcher.find()) {
                map.put(matcher.group(1), InetAddress.getByName(matcher.group(2)));
            }

            try {
                while ((strLine = reader.readLine()) != null) {
                    // n1:n2,n3,n4,n5

                    pattern = Pattern.compile("([^:]+):(.+)");

                    matcher = pattern.matcher(strLine);

                    if (!matcher.find()) {
                        // TODO error
                    }

                    String node = matcher.group(1);
                    String neighbours = matcher.group(2);

                    if (!map.containsKey(node)) {
                        // TODO error
                    }

                    tree.put(map.get(node), new ArrayList<InetAddress>());

                    pattern = Pattern.compile("[^ ,]+");

                    matcher = pattern.matcher(neighbours);

                    while (matcher.find()) {

                        String neighbour = matcher.group();

                        if (!map.containsKey(neighbour)) {
                            // TODO error
                        }
                        tree.get(map.get(node)).add(map.get(neighbour));

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tree;
    }

    public static void runBoot(int bootPort) {

        // NOTE using regular TCP for now

        // try {
        //     Socket socket;
        //
        //     try (ServerSocket serverSocket = new ServerSocket(1234)) {
        //         socket = serverSocket.accept();
        //
        //         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        //         BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        //
        //         while (true) {
        //             String msg = bufferedReader.readLine();
        //             System.out.println(msg);
        //             if (msg.equals("Tree")) {
        //                 bufferedWriter.write("ACK");
        //                 bufferedWriter.newLine();
        //                 bufferedWriter.flush();
        //             }
        //         }
        //     }
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

        byte[] buff = new byte[1024];

        try(DatagramSocket socket = new DatagramSocket(bootPort)){
            
            while(true){

                DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);

                try{
                 socket.receive(datagramPacket);
                }catch(IOException e){
                    e.printStackTrace();
                }


                InetAddress address = datagramPacket.getAddress();
                int port = datagramPacket.getPort();

                byte[] msg = "ACK".getBytes();

                Packet packet = new Packet(0, 0, msg, msg.length);
                    
                DatagramPacket datagramPacketSend = new DatagramPacket(packet.getPacket(), packet.getPacketLength(), address, port);

                try{
                    socket.send(datagramPacketSend);
                }catch(IOException e){
                    e.printStackTrace();
                }

            }
        }catch(SocketException e){
            e.printStackTrace();
        }
    }

}
