import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;

public class BootStraper {

    public static HashMap<InetAddress, ArrayList<InetAddress>> getTree(String filePath) {

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

            HashMap<String, InetAddress> map = new HashMap<String, InetAddress>();

            Pattern pattern = Pattern.compile("([^ ,]+?):((?:\\d+\\.\\d+\\.\\d+\\.\\d+)|localhost)"); // matches stuff like n1:1.1.1.1 and n2:localhost
            
            Matcher matcher = pattern.matcher(reader.readLine());

            while(matcher.find()){
                map.put(matcher.group(1), InetAddress.getByName(matcher.group(2)));
            }

            try {
                while ((strLine = reader.readLine()) != null) {
                    // n1:n2,n3,n4,n5
                    
                    pattern = Pattern.compile("([^:]+):(.+)");

                    matcher = pattern.matcher(strLine);


                    if(!matcher.find()){
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

    public static void main(String[] args) {
        
        HashMap<InetAddress, ArrayList<InetAddress>> map = getTree("./tree.txt");

        System.out.println(map);;
        

    }
    
}
