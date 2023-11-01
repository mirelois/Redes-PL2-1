public class Packet {

    static int HEADER_SIZE = 6;

    public int sequence_number;
    public int time_stamp;

    public byte[] header;

    public int payload_size;

    public byte[] payload;

    public Packet(int sequence_number, int time_stamp, byte[] payload, int payload_size) {

        this.sequence_number = sequence_number;
        this.time_stamp = time_stamp;

        this.payload_size = payload_size;

        this.header = new byte[HEADER_SIZE];

        this.header[0] = (byte) (sequence_number >> 8 /* & 0xFF */);
        this.header[1] = (byte) (sequence_number /* & 0xFF */);

        this.header[2] = (byte) (time_stamp >> 24 /* & 0xFF */);
        this.header[3] = (byte) (time_stamp >> 16 /* & 0xFF */);
        this.header[4] = (byte) (time_stamp >> 8 /* & 0xFF */);
        this.header[5] = (byte) (time_stamp /* & 0xFF */);

        this.payload = new byte[payload_size];

        for (int i = 0; i < payload_size; i++) {
            this.payload[i] = payload[i];
        }
    }

    public Packet(byte[] packet, int packet_size) {

        // TODO: check packet_size

        this.header = new byte[HEADER_SIZE];

        for (int i = 0; i < HEADER_SIZE; i++) {
            this.header[i] = packet[i];
        }

        this.payload_size = packet_size - HEADER_SIZE;

        this.payload = new byte[this.payload_size];

        for (int i = HEADER_SIZE; i < packet_size; i++) {
            this.payload[i - HEADER_SIZE] = packet[i];
        }

        this.sequence_number = packet[1] + (packet[0] << 8);
        this.time_stamp = packet[5] + (packet[4] << 8) + (packet[3] << 16) + (packet[2] << 24);
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

    public byte[] getHeader() {

        byte[] header = new byte[HEADER_SIZE];

        for (int i = 0; i < HEADER_SIZE; i++) {
            header[i] = this.header[i];
        }

        return header;
    }

    public int getHeaderSize() {
        return HEADER_SIZE;
    }

    public int getTimeStamp() {
        return this.time_stamp;
    }

    public int getSequenceNumber() {
        return this.sequence_number;
    }

    public byte[] getPacket() {

        byte[] packet = new byte[HEADER_SIZE+payload_size];

        for (int i = 0; i < HEADER_SIZE; i++) {
            packet[i] = this.header[i];
        }
        for (int i = 0; i < payload_size; i++) {
            packet[i + HEADER_SIZE] = this.payload[i];
        }

        return packet;
    }

    public int getPacketLength() {
        return HEADER_SIZE + this.payload_size;
    }

    public void printheader() {
        System.out.print("[RTP-Header] ");
        System.out.println("SequenceNumber: " + sequence_number
                         + ", TimeStamp: " + time_stamp);
    }

}
