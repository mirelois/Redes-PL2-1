import java.net.DatagramPacket;
import java.net.InetAddress;

public class Bop {

    public int checksum(byte[] data){
        int sum = 0;
        for (byte b : data) {
            sum += b;
        }
        return ~sum;
    }

    static int HEADER_SIZE = 3;

    int header_size;

    // Header

    // int sequence_number; // 4

    int checksum; // 2

    boolean ack; // 1

    byte[] header;

    byte[] payload;

    int payload_size;

    InetAddress address;
    
    int port;

    public Bop(/* int sequence_number, */boolean ack, byte[] payload, int payload_size, InetAddress address, int port) {

        this.ack = ack;
        this.address = address;
        this.port = port;

        if (ack) {
            this.header_size = 1;

            this.header = new byte[1];

            this.header[0] = 0x01;
        } else {

            this.payload_size = payload_size;

            this.checksum = 0; // TODO checksum
                               // 
            this.header_size = HEADER_SIZE;

            this.header = new byte[HEADER_SIZE];
            this.header[0] = 0x00;
            this.header[1] = (byte) (checksum >> 8);
            this.header[2] = (byte) (checksum);

            if (payload_size > 0){

                this.payload = new byte[payload_size];

                for (int i = 0; i < payload.length; i++) {
                    this.payload[i] = payload[i];
                }
            } else {
                this.payload = null;
            }
            
        }

        // this.sequence_number = sequence_number;

        // this.header[0] = (byte) (sequence_number >> 24);
        // this.header[1] = (byte) (sequence_number >> 16);
        // this.header[2] = (byte) (sequence_number >> 8);
        // this.header[3] = (byte) (sequence_number);

        // this.header[0] = (byte) (checksum >> 8);
        // this.header[1] = (byte) (checksum);

    }

    public Bop(DatagramPacket udpPacket) {

        // TODO: check packet_size

        byte[] packet = udpPacket.getData();

        this.address = udpPacket.getAddress();
        this.port = udpPacket.getPort();
        this.ack = packet[0] != 0;
        

        if (ack) {
            this.header_size = 1;
            this.header = new byte[1];
        } else { 
            
            this.header_size = HEADER_SIZE;
            this.header = new byte[HEADER_SIZE];
            this.payload = new byte[packet.length - this.header_size];    
            this.payload_size = packet.length - this.header_size;
            for (int i = 0; i < packet.length - this.header_size; i++) {
                this.payload[i] = packet[header_size + i];
            }
            this.checksum = packet[2] | (packet[1] >> 8);
        }
        
        for (int i = 0; i < header_size; i++) {
            this.header[i] = packet[i];
        }
    }

    public int getheaderSize() {
        return header_size;
    }

    // public int getSequence_number() {
    // return sequence_number;
    // }
    
    public boolean getAck() {
        return this.ack;
    }

    public int getChecksum() {
        return checksum;
    }

    public byte[] getHeader() {

        byte[] header = new byte[header_size];

        for (int i = 0; i < header_size; i++) {
            header[i] = this.header[i];
        }

        return header;
    }

    public byte[] getPayload() {
        byte[] payload = new byte[payload_size];

        for (int i = 0; i < payload_size; i++) {
            payload[i] = this.payload[i];
        }

        return payload;
    }

    public int getPayload_size() {
        return payload_size;
    }

    public byte[] getPacket() {

        byte[] packet = new byte[this.header_size + this.payload_size];

        for (int i = 0; i < this.header_size; i++) {
            packet[i] = this.header[i];
        }

        for (int i = 0; i < this.payload_size; i++) {
            packet[i + this.header_size] = this.payload[i];
        }        

        return packet;
    }

    public int getPacketLength() {
        return this.header_size + this.payload_size;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public DatagramPacket toDatagramPacket() {
        return new DatagramPacket(this.getPacket(), this.getPacketLength(), address, port);
    }

    // public void printheader() {
    // System.out.print("[RTP-Header] ");
    // System.out.println("SequenceNumber: " + sequence_number
    // + ", Checksum: " + checksum);
    // }

}
