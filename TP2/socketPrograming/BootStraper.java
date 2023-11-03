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

            Pattern pattern = Pattern.compile("([^,]+?):(\\d+\\.\\d+\\.\\d+\\.\\d+)"); // matches stuff like n1:1.1.1.1

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

                    if (!map.containsKey(matcher.group(1))) {
                        // TODO error
                    }

                    tree.put(map.get(matcher.group(1)), new ArrayList<InetAddress>());

                    for (String string : matcher.group(2).split(",")) {
                        if (!map.containsKey(string)) {
                            // TODO error
                        }
                        tree.get(map.get(matcher.group(1))).add(map.get(string));
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
        
        // HashMap<InetAddress,ArrayList<InetAddress>> map = getTree("./tree.txt");
        //
        // String str = "n1:12.12.12.12,n2:13.13.13.13";
        //
        // Pattern pattern = Pattern.compile("([^,]+?):(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        //
        // Matcher matcher = pattern.matcher(str);
        //
        // while(matcher.find()){
        //     System.out.println(matcher.group(1));
        //     System.out.println(matcher.group(2));
        // }
        
        HashMap<InetAddress,ArrayList<InetAddress>> map = getTree("./tree.txt");

        HashMap<String,String> m = new HashMap<String,String>();

        m.put("k1", "V1");

        try{
            System.out.println(map.get(InetAddress.getByName("127.0.0.1")));
        }catch(UnknownHostException e){}
        System.out.println(m);

    }










    

}
