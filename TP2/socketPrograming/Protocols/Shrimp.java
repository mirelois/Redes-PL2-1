package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Shrimp extends Packet { //Stream Hard Response Initiation Management Protocol

    static int HEADER_SIZE = 3;

    //Se estiver a 0, a stream não existe
    int streamId;  //1

    int timeStamp; //2;
                   
    public Shrimp(int timeStamp, int streamId, int port, InetAddress address, int payload_size, byte[] payload) {
        super(HEADER_SIZE, payload, payload_size, address, port);

        this.streamId = streamId;

        this.timeStamp = timeStamp;

        this.header[0] = (byte)(this.streamId /* & 0xFF */);

        this.header[1] = (byte)(this.timeStamp >> 8 /* & 0xFF */);
        this.header[2] = (byte)(this.timeStamp /* & 0xFF */);
    }

    public Shrimp(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException {

        super(packet, HEADER_SIZE);

        this.streamId = Byte.toUnsignedInt(this.header[0]);


        this.timeStamp = (Byte.toUnsignedInt(this.header[1]) << 8) |
                          Byte.toUnsignedInt(this.header[2]);

    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public int getStreamId() {
        return streamId;
    }
}
