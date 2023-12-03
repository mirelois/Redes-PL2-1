import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Protocols.Rip;
import SharedStructures.Define;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RipSender implements Runnable {
    //TODO Depois de tudo funcionar, voltar aqui
    //Classe que reenvia tudo nos mapas de retransmissão

    public class RipListener implements Runnable {
        //Classe para ouvir num dos Sockets dos RIPs e retirar elementos dos mapas de retransmissão
        public RetransmissionInfo retransInfo;

        public RipListener(RetransmissionInfo retransInfo) {
            this.retransInfo = retransInfo;
        }

        //TODO Escolher como responder com acks, vão todos ser muito parecidos com os próprios protocolos
        @Override
        public void run() {
            byte[] buf = new byte[Define.infoBuffer];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                retransInfo.socket.receive(packet);
                Rip rip = new Rip(packet);
                InetAddress address = rip.getAddress();
                switch (rip.getAcknowledgment()) {
                    case -1:
                        //Simp
                        String name = new String(rip.getPayload());
                        try {
                            retransInfo.lsimp.lock();
                            retransInfo.simpPackets.remove(new Tuple<InetAddress, String>(address, name));
                        } finally {
                            retransInfo.lsimp.unlock();
                        }
                        break;
                    case -2:
                        //Shrimp
                        //TODO Como tirar o streamId (do payload?)
                        //Integer streamId = rip.getPayload();
                        try {
                            //retransInfo.lsimp.lock();
                            //retransInfo.shrimpPackets.remove(new Tuple<InetAddress, Integer>(address, streamId));
                        } finally {
                            retransInfo.lsimp.unlock();
                        }
                        break;
                    case -3:
                        //Link
                        Boolean bool1, bool2;
                        try {
                            retransInfo.lsimp.lock();
                            //retransInfo.linkPackets.remove(new Triple<InetAddress, Boolean, Boolean>(address, bool1, bool2));
                        } finally {
                            retransInfo.lsimp.unlock();
                        }
                        break;
                    default:
                        //SUP? ou vamos ter outro listener
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public class RetransmissionInfo {
        //Socket de chegada dos RIPs e de Retransmissão
        public DatagramSocket socket;
        public ReentrantLock lsimp = new ReentrantLock();
        public ReentrantLock lshrimp = new ReentrantLock();
        public ReentrantLock llink = new ReentrantLock();

        //Simp: Nome da Stream && IP do link a enviar
        public HashMap<Tuple<InetAddress, String>, DatagramPacket> simpPackets = new HashMap<>();
        //Simp: Nome da Stream && IP do link a enviar
        public HashMap<Tuple<InetAddress, Integer>, DatagramPacket> shrimpPackets = new HashMap<>();
        //Simp: Nome da Stream && IP do link a enviar
        public HashMap<Triple<InetAddress, Boolean, Boolean>, DatagramPacket> linkPackets = new HashMap<>();

        //TODO: Retransmissão de SUPs pode ser diferente, mas pode ser feita aqui na mesma
        //public HashMap<Tuple<>, DatagramPacket> supPackets = new HashMap<>();
    }
    
    public RetransmissionInfo retransInfo = new RetransmissionInfo();
    public RipListener listener;

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Define.ripPort)) {
            //Preparar socket para receber os RIPs
            retransInfo.socket = socket;

            //Começar o RipListener
            listener = new RipListener(retransInfo);
            listener.run();

            //Ciclo de Retransmissão
            while(true) {
                //Wait
                Thread.sleep(Define.RetransTimeout);

                //Coletar os pacotes a reenviar
                ArrayList<DatagramPacket> packets = new ArrayList<>();

                //TODO: Ver se todos os locks são possíveis se fizer sobre a classe inteira do RetransmissionInfo
                try {
                    //lock simp
                    retransInfo.lsimp.lock();
                    try {
                        //lock shrimp
                        retransInfo.lshrimp.lock();
                        try {
                            //lock link
                            retransInfo.llink.lock();
                            packets.addAll(retransInfo.simpPackets.values());
                            packets.addAll(retransInfo.shrimpPackets.values());
                            packets.addAll(retransInfo.linkPackets.values());
                        } finally {
                            retransInfo.llink.unlock();
                        }
                    } finally {
                        retransInfo.lshrimp.unlock();
                    }
                } finally {
                    retransInfo.lsimp.unlock();
                }
                
                //Enviar os pacotes pelo socket
                for (DatagramPacket packet : packets) {
                    retransInfo.socket.send(packet);
                }

            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}
