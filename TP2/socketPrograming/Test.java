import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) {
        // Thread t;

        Socket socket = null;

        // BootStraper bootStraper = new BootStraper(1233);
        // t = new Thread(bootStraper);

        // t.start();
        
        try {

            socket = new Socket("localhost", 1234);

            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner scanner = new Scanner(System.in);

            while (true) {

                String msg = scanner.nextLine();

                bufferedWriter.write(msg);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                String s = bufferedReader.readLine();
                System.out.println(s);
            }
                

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
