package BootStrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

import Protocols.Bop;
import Protocols.PacketSizeException;
import SharedStructures.Define;
import SharedStructures.NeighbourInfo;

public class BootClient implements Runnable{

    private InetAddress bootStrapperIP;

    private NeighbourInfo neighbours;

    public BootClient(InetAddress bootStrapperIP, NeighbourInfo neighbours){
        this.bootStrapperIP = bootStrapperIP;
        this.neighbours = neighbours;
    }
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
        
    }

    public void run() {
        try(DatagramSocket socket = new DatagramSocket(Define.bootClientPort)) {

            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(Define.bootClientTimeout);
                    } catch (InterruptedException e) {
                        return;
                    }

                    // send neighbour request
                    Bop bop = new Bop(null, 0, bootStrapperIP, Define.bootStraperPort);

                    try {
                        socket.send(bop.toDatagramPacket());
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    // ---------
                }
            });
            t.start();

            // receber os vizinhos
            byte[] buf = new byte[Define.infoBuffer];

            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true) {
                socket.receive(packet);

                Bop bopReceived = new Bop(packet);

                if (bopReceived.getChecksum() == 0) { // TODO checksum

                    // entra aqui quando receber o pacote bem
                    t.interrupt();

                    this.neighbours.overlayNeighbours = (List<InetAddress>) deserialize(bopReceived.getPayload());
                    for(InetAddress neighbour: this.neighbours.overlayNeighbours){
                        this.neighbours.neighBoursLifePoints.put(neighbour, 5); // Default 5 life points ao nascer
                    }

                    //System.out.println(neighbours);
                    synchronized (neighbours) {
                        this.neighbours.notify();
                    }

                    break;

                }
            }

        } catch (SocketException e){
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException | PacketSizeException e) {
            throw new RuntimeException(e);
        }
    }

}
