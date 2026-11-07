import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/** A deterministic, single-process companion demo using real localhost sockets. */
public final class LoopbackCheck {
    private static final InetAddress LOCAL = InetAddress.getLoopbackAddress();
    private static final int BLOCK = 1024;

    public static void main(String[] args) throws IOException {
        byte[] input = new byte[1024 * BLOCK];
        new Random(42).nextBytes(input);
        tcp(input);
        udp(input);
    }

    private static void tcp(byte[] input) throws IOException {
        try (ServerSocket listener = new ServerSocket(0, 1, LOCAL);
             Socket writer = new Socket(LOCAL, listener.getLocalPort());
             Socket reader = listener.accept()) {
            reader.setSoTimeout(2000);
            var sink = new ByteArrayOutputStream(input.length);
            for (int offset = 0; offset < input.length; offset += BLOCK) {
                int bytes = Math.min(BLOCK, input.length - offset);
                writer.getOutputStream().write(input, offset, bytes);
                sink.writeBytes(reader.getInputStream().readNBytes(bytes));
            }
            writer.shutdownOutput();
            if (reader.getInputStream().read() != -1 || !Arrays.equals(input, sink.toByteArray())) {
                throw new IOException("TCP stream differed from the original");
            }
            System.out.printf("TCP: %,d bytes verified%n", sink.size());
        }
    }

    private static void udp(byte[] input) throws IOException {
        try (DatagramSocket sender = new DatagramSocket(0, LOCAL);
             DatagramSocket receiver = new DatagramSocket(0, LOCAL)) {
            sender.connect(LOCAL, receiver.getLocalPort());
            receiver.connect(LOCAL, sender.getLocalPort());
            sender.setSoTimeout(15);
            receiver.setSoTimeout(2000);
            var sink = new ByteArrayOutputStream(input.length);
            int next = 0, withheld = 0, retries = 0;
            for (int offset = 0, seq = 0; offset < input.length; offset += BLOCK, seq++) {
                int length = Math.min(BLOCK, input.length - offset);
                byte[] bytes = ByteBuffer.allocate(4 + length).putInt(seq).put(input, offset, length).array();
                for (int attempt = 1; ; attempt++) {
                    sender.send(new DatagramPacket(bytes, bytes.length));
                    var incoming = new DatagramPacket(new byte[4 + BLOCK], 4 + BLOCK);
                    receiver.receive(incoming);
                    int receivedSeq = ByteBuffer.wrap(incoming.getData()).getInt();
                    boolean fresh = receivedSeq == next;
                    if (fresh) {
                        sink.write(incoming.getData(), 4, incoming.getLength() - 4);
                        next++;
                    }
                    if (fresh && (receivedSeq + 1) % 5 == 0) {
                        withheld++;
                    } else {
                        byte[] ack = ByteBuffer.allocate(4).putInt(next - 1).array();
                        receiver.send(new DatagramPacket(ack, ack.length));
                    }
                    var ackPacket = new DatagramPacket(new byte[4], 4);
                    try {
                        sender.receive(ackPacket);
                        if (ByteBuffer.wrap(ackPacket.getData()).getInt() != seq) {
                            throw new IOException("Unexpected acknowledgment for block " + seq);
                        }
                        break;
                    } catch (SocketTimeoutException timeout) {
                        if (attempt == 4) throw new IOException("No acknowledgment for block " + seq, timeout);
                        retries++;
                    }
                }
            }
            if (withheld != 204 || retries < withheld || !Arrays.equals(input, sink.toByteArray())) {
                throw new IOException("UDP transfer did not reproduce the expected payload or loss pattern");
            }
            System.out.printf("UDP: %,d bytes verified, %d ACKs withheld, %d retransmissions%n",
                              sink.size(), withheld, retries);
        }
    }
}
