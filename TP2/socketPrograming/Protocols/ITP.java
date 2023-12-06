package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class ITP extends Packet { // Idle Tick Protocol

    public static int HEADER_SIZE = 3;

    public boolean isNode;   // \
                             // |
    public boolean isServer; // |-> 1
                             // |
    public boolean isAck;    // /

    public int timeStamp;    // 2

    public ITP(boolean isServer, boolean isNode, boolean isAck, int timeStamp, InetAddress address, int port, int payload_size, byte[] payload) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.timeStamp = timeStamp;

        int flags = 0x00;

        if (isServer) { flags |= 0x01; }

        if (isNode) { flags |= 0x02; }

        if (isAck) { flags |= 0x04; }

        this.header[0] = (byte)(flags /* & 0xFF */);
        this.header[1] = (byte)(this.timeStamp >> 8 /* & 0xFF */);
        this.header[2] = (byte)(this.timeStamp /* & 0xFF */);

    }

    public ITP(DatagramPacket packet) throws PacketSizeException {

        super(packet, HEADER_SIZE);

        this.timeStamp = (Byte.toUnsignedInt(this.header[0]) << 8) |
                         Byte.toUnsignedInt(this.header[1]);

        int flags = Byte.toUnsignedInt(this.header[0]);

        this.isServer = (flags & 0x01) > 0;
        this.isNode   = (flags & 0x02) > 0;
        this.isAck    = (flags & 0x04) > 0;

    }

    public int getTimeStamp() {
        return timeStamp;
    }

}
