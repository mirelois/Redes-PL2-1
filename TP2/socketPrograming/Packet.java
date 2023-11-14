import java.net.DatagramPacket;
import java.net.InetAddress;

public class Packet {

    public int payload_size;

    public byte[] payload;

    InetAddress address;
    
    int port;

    public Packet(int payload_size, byte[] payload, InetAddress address, int port){
		this.payload_size = payload_size;
		this.payload = payload;
		this.address = address;
		this.port = port;
    }

    public byte[] getPayload() {

        byte[] payload = new byte[payload_size];

        for (int i = 0; i < payload_size; i++) {
            payload[i] = this.payload[i];
        }

        return payload;
    }

    public int getPayloadSize() {
        return this.payload_size;
    }

    public int getPort() {
        return this.port;
    }

    public byte[] getPacket(byte[] header) {

        byte[] packet = new byte[header.length+payload_size];

        for (int i = 0; i < header.length; i++) {
            packet[i] = header[i];
        }
        for (int i = 0; i < payload_size; i++) {
            packet[i + header.length] = this.payload[i];
        }

        return packet;
    }

    public int getPacketLength(int header_size) {
        return header_size + this.payload_size;
    }
    
    public DatagramPacket toDatagramPacket(byte[] header, int header_size) {
        return new DatagramPacket(this.getPacket(header), this.getPacketLength(header_size), address, port);
    }

    public InetAddress getAddress() {
        return this.address;
    }

}
