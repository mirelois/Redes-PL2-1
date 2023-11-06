import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.instrument.Instrumentation;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.*;

public class BootStrapper {
    
    public static byte[] serialize(Object obj) throws IOException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    private static HashMap<InetAddress, ArrayList<InetAddress>> getTree(String filePath) {

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

    public static void runBoot(int bootPort, String filePath, int timeout) {

        // buffer to receive datagramPacket
        byte[] buff = new byte[1024];

        // neighbour tree from file
        HashMap<InetAddress, ArrayList<InetAddress>> tree = getTree(filePath);

        // initialize map to store clients for whom wainting for ack
        HashMap<InetAddress, Thread> wait_map = new HashMap<InetAddress, Thread>();

        // open socket
        try (DatagramSocket socket = new DatagramSocket(bootPort)) {

            // start waiting for packets
            while (true) {

                // ------ get DatagramPacket from socket -------
                DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);

                try {
                    socket.receive(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // -------------

                // unpack BOP packet
                Bop bop = new Bop(datagramPacket);
                // ------

                if (bop.getAck()) {
                    wait_map.get(bop.getAddress()).interrupt();
                    wait_map.remove(bop.getAddress());
                } else {

                    // get neighbours from tree
                    // ArrayList<InetAddress> neighbours = tree.get(bop.getAddress());
                    // ------

                    // write neighbours into byte array payload
                    try {

                        byte[] payload = serialize(tree.get(bop.getAddress()));

                        // ------

                        Bop send_bop = new Bop(false, payload, payload.length, bop.getAddress(), bop.getPort());

                        // DatagramPacket send_datagram_packet = new
                        // DatagramPacket(send_bop.getPacket(), send_bop.getPacketLength(), address,
                        // port);

                        socket.send(send_bop.toDatagramPacket());

                        Thread t = new Thread(() -> {
                            while (true) {
                                try {
                                    try {
                                        Thread.sleep(timeout);
                                    } catch (InterruptedException e) {
                                        return;
                                    }
                                    socket.send(send_bop.toDatagramPacket());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        wait_map.put(bop.getAddress(), t);
                        t.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

}
