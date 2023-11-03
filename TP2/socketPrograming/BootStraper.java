import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class BootStraper {

    public HashMap<InetAddress, ArrayList<InetAddress>> getTree(String filePath) {

        FileInputStream stream = null;

        HashMap<InetAddress, ArrayList<InetAddress>> tree = new HashMap<InetAddress, ArrayList<InetAddress>>();

        try {
            stream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String[] alias;
        String strLine;
        // ArrayList<String> alias = new ArrayList<String>();

        try {
            alias = reader.readLine().split(",");

            HashMap<String, InetAddress> map = new HashMap<String, InetAddress>();

            for (String string : alias) {
                ;
                // string -> n1:123.456.789.000
                String[] keyPair = string.split(":");
                try {
                    map.put(keyPair[0], InetAddress.getByName(keyPair[1]));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            try {
                while ((strLine = reader.readLine()) != null) {
                    // n1:n2,n3,n4,n5
                    String[] keyPair = strLine.split(":");

                    if (!map.containsKey(keyPair[0])) {
                        // TODO error
                    }

                    tree.put(map.get(keyPair[0]), new ArrayList<InetAddress>());

                    for (String string : keyPair[1].split(",")) {
                        if (!map.containsKey(string)) {
                            // TODO error
                        }
                        tree.get(map.get(keyPair[0])).add(map.get(string));
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

}
