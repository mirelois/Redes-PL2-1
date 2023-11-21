import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BootStrapper implements Runnable {

    private int bootPort;
    private String filePath;

    public BootStrapper(int bootPort, String filePath, int timeout) {
        this.bootPort = bootPort;
        this.filePath = filePath;
    }

    private byte[] serialize(Object obj) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    private HashMap<InetAddress, Set<InetAddress>> getTree(String filePath) {

        FileInputStream stream = null;

        HashMap<InetAddress, Set<InetAddress>> tree = new HashMap<>();

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

                    tree.put(map.get(node), new HashSet<>());

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

    @Override
    public void run() {

        // buffer to receive datagramPacket
        byte[] buff = new byte[1024];

        // neighbour tree from file
        HashMap<InetAddress, Set<InetAddress>> tree = getTree(filePath);

        // open socket
        try (DatagramSocket socket = new DatagramSocket(bootPort)) {

            // start waiting for packets
            while (true) {

                // ------ get DatagramPacket from socket -------
                DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);

                socket.receive(datagramPacket);

                // unpack BOP packet
                Bop bop = new Bop(datagramPacket);
                
                byte[] payload = serialize(tree.get(bop.getAddress()));

                Bop send_bop = new Bop(payload, payload.length, bop.getAddress(), bop.getPort());
                // ------
                socket.send(send_bop.toDatagramPacket());

            }

        } catch (PacketSizeException | IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
