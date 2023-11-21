import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.LocalTime;

public class Packet {

    int header_size;

    public byte[] header;

    public int payload_size;

    public byte[] payload;

    InetAddress address;

    int port;

    public Packet(int header_size, byte[] payload, int payload_size, InetAddress address, int port) {

        this.header_size = header_size;
        this.header = new byte[header_size];
        this.payload_size = payload_size;
        this.payload = new byte[payload_size];

        this.address = address;
        this.port = port;

        if(payload_size == 0){
            this.payload = null;
        }else{
            this.address = address;
            
            for (int i = 0; i < payload_size; i++) {
                this.payload[i] = payload[i];
            }
        }

    }

    public Packet(DatagramPacket packet, int header_size) throws PacketSizeException{

        byte[] data = packet.getData();
        
        if(data.length < header_size){
            throw new PacketSizeException("Packet size to smol");
        }

        this.header_size = header_size;
        this.header = new byte[header_size];
        this.address = packet.getAddress();
        this.port = packet.getPort();

        this.payload_size = data.length - header_size;
        
        if(payload_size == 0){
            this.payload = null;
        }else{
            this.payload = new byte[this.payload_size]; // TODO: check data length

            for (int i = 0; i < data.length - this.header_size; i++) {
                this.payload[i] = data[header_size + i];
            }
        }

        for (int i = 0; i < header_size; i++) {
            this.header[i] = data[i];
        }

    }

    public byte[] getPayload() {

        if(this.payload_size == 0){
            return null;
        }

        byte[] payload = new byte[payload_size];

        for (int i = 0; i < payload_size; i++) {
            payload[i] = this.payload[i];
        }

        return payload;
    }

    public int getPayloadSize() {
        return this.payload_size;
    }

    public byte[] getHeader() {

        byte[] header = new byte[header_size];

        for (int i = 0; i < header_size; i++) {
            header[i] = this.header[i];
        }

        return header;
    }

    public int getHeaderSize() {
        return header_size;
    }

    public int getPort() {
        return this.port;
    }

    public byte[] getPacket() {

        byte[] packet = new byte[header_size + payload_size];

        for (int i = 0; i < header_size; i++) {
            packet[i] = this.header[i];
        }
            
        for (int i = 0; i < payload_size; i++) {
            packet[i + header_size] = this.payload[i];
        }

        return packet;
    }

    public int getPacketLength() {
        return header_size + this.payload_size;
    }

    public DatagramPacket toDatagramPacket() {
        return new DatagramPacket(this.getPacket(), this.getPacketLength(), address, port);
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public static int checksum(byte[] data){
        int sum = 0;
        for (byte b : data) {
            sum += b;
        }
        return ~sum;
    }
    
    public static int getCurrTime(){
        
        LocalTime now = LocalTime.now();
        
        return now.getSecond() * 1000 + now.getNano() / 1000000;
    }

}
