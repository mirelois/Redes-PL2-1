public class Bop {

    static int HEADER_SIZE = 6;

    int sequence_number; // 4

    int checksum; // 2

    byte[] header;

    byte[] payload;

    int payload_size;

    public Bop(int sequence_number, int checksum, byte[] payload, int payload_size) {

        this.header = new byte[HEADER_SIZE];

        this.payload_size = payload_size;

        this.sequence_number = sequence_number;

        this.checksum = checksum;

        this.header[0] = (byte) (sequence_number >> 24);
        this.header[1] = (byte) (sequence_number >> 16);
        this.header[2] = (byte) (sequence_number >> 8);
        this.header[3] = (byte) (sequence_number);

        this.header[4] = (byte) (checksum >> 8);
        this.header[5] = (byte) (checksum);
        
        if(payload_size > 0){
            this.payload = new byte[payload_size];
            
            for (int i = 0; i < payload.length; i++) {
                this.payload[i] = payload[i];
            }
        }


    }

    public Bop(byte[] packet, int packet_size) {

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

        this.sequence_number = packet[3] | (packet[2] << 8) | (packet[1] << 16) | (packet[0] << 24);
        this.checksum = packet[5] | (packet[4] << 8);
    }

    public static int getheaderSize() {
        return HEADER_SIZE;
    }

    public int getSequence_number() {
        return sequence_number;
    }

    public int getChecksum() {
        return checksum;
    }

    public byte[] getHeader() {

        byte[] header = new byte[HEADER_SIZE];

        for (int i = 0; i < HEADER_SIZE; i++) {
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

        byte[] packet = new byte[HEADER_SIZE+payload_size];

        for (int i = 0; i < HEADER_SIZE; i++) {
            packet[i] = this.header[i];
        }
        
        if(this.payload_size > 0) {
            for (int i = 0; i < payload_size; i++) {
                packet[i + HEADER_SIZE] = this.payload[i];
            }
        }

        return packet;
    }
    
    public int getPacketLength() {
        return HEADER_SIZE + this.payload_size;
    }

    
    public void printheader() {
        System.out.print("[RTP-Header] ");
        System.out.println("SequenceNumber: " + sequence_number
                         + ", Checksum: " + checksum);
    }

}
