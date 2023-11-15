import java.net.InetAddress;
import java.net.UnknownHostException;

public class Test {

    public static void main(String[] args) throws UnknownHostException {

        InetAddress address = InetAddress.getByName("localhost");
        
        Rip rip_source = new Rip(12, 13,address, 2000);

        Rip rip_dest = new Rip(rip_source.toDatagramPacket());

        assert(rip_dest.getLatency() == 12);
        assert(rip_dest.getThroughput() == 13);
        assert(rip_dest.getAddress().equals(address));
        assert(rip_dest.getPort() == 2000);

        //------------------------------------------------
        
        Simp simp_source = new Simp(address, address, 2000, 5, "hello".getBytes());

        Simp simp_dest = new Simp(simp_source.toDatagramPacket());

        System.out.println(simp_dest.getTime_stamp());
        assert(simp_dest.getSourceAddress().equals(address));
        assert(simp_dest.getChecksum() == 0);
        assert(simp_dest.getPort() == 2000);
        assert(simp_dest.getAddress().equals(address));
        assert(simp_dest.getPayload().equals("hello".getBytes()));

        //------------------------------------------------

        Sup sup_source = new Sup(1000, 1001, address, 2000, 5, "hello".getBytes());

        Sup sup_dest = new Sup(sup_source.toDatagramPacket());

        assert(sup_dest.getSequence_number() == 1000);
        assert(sup_dest.getAcknowledgment_number() == 1001);
        assert(sup_dest.getChecksum() == 0);
        assert(sup_dest.getPort() == 2000);
        assert(sup_dest.getAddress().equals(address));
        assert(sup_dest.getPayload().equals("hello".getBytes()));
        

    }
}
