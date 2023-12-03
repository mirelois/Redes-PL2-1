import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import BootStrapper.BootClient;
import BootStrapper.BootStrapper;
import Node.Client;
import Node.NodeConnectionManager;
import Node.ShrimpManager;
import Node.SimpManager;
import Node.Streaming;
import RP.RP;
import RP.RPServerAdder;
import Server.Server;
import Server.ServerConectionManager;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;
import SharedStructures.ServerInfo;

public class fullDuplex {
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 5) {
            System.out.println("Wrong Arguments!" +
                    "\nIP_Bootstrapper [-b] [-s] [-r]" +
                    "\n -b: Bootstrapper, -s: Server, -r: RP");
            return;
        }

        // Fase 0: Tratar argumentos
        StringBuilder arguments = new StringBuilder();
        for(int i=1; i<args.length; i++){
            arguments.append(" ").append(args[i]);
        }

        String filePath = null;
        boolean isRP = false;
        //cuidado, é preciso fazer -b*ESPAÇO*
        Pattern pattern = Pattern.compile("(?:(-b|-r) ?([^- \\n]*))");
        Matcher matcher = pattern.matcher(arguments.toString());
        while(matcher.find()){
            String flag = matcher.group(1);
            if(flag.equals("-b"))
                filePath = matcher.group(2);
            else if(flag.equals("-r"))
                isRP = true;
        }

        NeighbourInfo neighbours = new NeighbourInfo();
        InetAddress ip_bootstrapper;
        try {
            ip_bootstrapper = InetAddress.getByName(args[0]);

            // Setup Phase:
            if (filePath!=null) {
                new Thread(new BootStrapper( filePath)).start();
            }

            new Thread(new BootClient(ip_bootstrapper, neighbours)).start();

        } catch (UnknownHostException e) {
            System.out.println("The IP " + args[0] + " is Invalid for IP_Bootstrapper");
        }

        try {
            synchronized (neighbours) {
                neighbours.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        // Phase 2
        //Thread client = new Thread(new Client()); // criar um client a priori não funcional que só fazemos run quando o user pede,
        // dessa forma podiamos ter uma lógica fácil de propagar a stream para os próximos nodos e ver a stream porque também somos clientes
        // senão tinhamos de por o nome do cliente destino no packet, do be cringe sometimes
        System.out.println("Os meus Adjacentes:");
        for (InetAddress neighbour : neighbours.overlayNeighbours) {
            System.out.println(neighbour.getHostName());
        }

        Thread streaming = new Thread(new Streaming(neighbours));
        streaming.start();

        if (isRP){
            System.out.println("Começo de RP!");
            ServerInfo serverInfo = new ServerInfo();
            new Thread(new RP(serverInfo, neighbours)).start();
            new Thread(new RPServerAdder(serverInfo, neighbours)).start();
            new Thread(new NodeConnectionManager(neighbours)).start();
        }else {
            System.out.println("Começo de Nodo!");
            Thread simpManager = new Thread(new SimpManager(neighbours));
            simpManager.start();
            Thread shrimpManager = new Thread(new ShrimpManager(neighbours));
            shrimpManager.start();
        }
        boolean isClientAlive = false;
        boolean isServerAlive = false;
        boolean keepLooping = true;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while(keepLooping){
            String inputStr = input.readLine();
            if (inputStr.contains("client") && !isClientAlive) {

                String[] clientStrName = inputStr.split(" ", 2);
                new Client(clientStrName[1]);

            } else if (inputStr.contains("server") && !isServerAlive) {

                String[] file = inputStr.split(" ", 2);
                File folder = new File(file[2]);
                File[] listOfFiles = folder.listFiles();
                ArrayList<String> streams = new ArrayList<>();
                HashMap<Integer, String> streamIdToFileName = new HashMap<>();
                HashMap<Integer, Thread> serverSenderMap = new HashMap<>();

                for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
                    System.out.println("File " + listOfFiles[i].getName());
                    streams.add(file[2]+"/"+listOfFiles[i].getName());
                }
                try {
                    DatagramSocket RTPsocket = new DatagramSocket(Define.serverPort); //init RTP socket
                    new Thread(new Server(InetAddress.getByName(args[0]), streams, 
                                          streamIdToFileName, serverSenderMap, RTPsocket)).start();
    
                    new Thread(new ServerConectionManager(InetAddress.getByName(args[0]), 
                                                          serverSenderMap, streamIdToFileName, RTPsocket));
                    
                } catch (SocketException e) {
                    System.out.println("Servidor: erro no socket: " + e.getMessage());
                }
            }else if(inputStr.contains("kill"))
                keepLooping = false;
        }
    }
}
