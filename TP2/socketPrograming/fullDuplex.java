import java.net.InetAddress;
import java.net.UnknownHostException;


public class fullDuplex {
    public static void main(String[] args){
        if(args.length<1 || args.length>3) {
            System.out.println("Wrong Arguments!" +
                    "\nIP_Bootstrapper [-b] [-s]" +
                    "\n -b: Bootstrapper, -s: Server");
            return;
        }

        NeighbourInfo neighbours = new NeighbourInfo();
        InetAddress ip_bootstrapper;
        try {
            ip_bootstrapper = InetAddress.getByName(args[0]);

            // Setup Phase:
            if((args.length == 2 && args[1].equals("-b")) || (args.length == 3 && args[2].equals("-b"))) {
                new Thread(new BootStrapper(2000, args[1], 1000)).start();
            }

            new Thread(new BootClient(ip_bootstrapper, 2000, 2001, 1000, neighbours)).start();

        } catch (UnknownHostException e){
            System.out.println("The IP " + args[0] + " is Invalid for IP_Bootstrapper");
        }

        try {
            synchronized (neighbours) {
                neighbours.wait();
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }



        // Phase 2
        //Thread client = new Thread(new Client()); // criar um client a priori não funcional que só fazemos run quando o user pede,
        // dessa forma podiamos ter uma lógica fácil de propagar a stream para os próximos nodos e ver a stream porque também somos clientes
        // senão tinhamos de por o nome do cliente destino no packet, do be cringe sometimes

        Thread client = new Thread(new Client());
        Thread streaming = new Thread(new Streaming(5000, 1000, neighbours, client));
        streaming.start();
        Thread simpManager = new Thread(new SimpManager(7000, 7001, neighbours));
        //if((args.length == 2 && args[1].equals("-s")) || (args.length == 3 && args[2].equals("-s"))){
            //new Thread(new Server(5000, 1000, neighbours))
        //}

    }
}
