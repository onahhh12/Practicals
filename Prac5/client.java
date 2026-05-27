import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class client {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             Socket socket = new Socket("localhost", 389)) {

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            byte[] dnBytes = "cn=admin,dc=example,dc=com".getBytes("UTF-8");
            byte[] pwBytes = "admin123".getBytes("UTF-8");

            byte[] bindBody = concat(
                new byte[]{0x02, 0x01, 0x03},
                tlv(0x04, dnBytes),
                tlv((byte)0x80, pwBytes)
            );

            byte[] bindRequest = buildLdapMessage(1, (byte)0x60, bindBody);
            out.write(bindRequest);
            out.flush();

            byte[] bindResp = readMessage(in);
            int bindResult = parseResultCode(bindResp);
            if (bindResult != 0) {
                System.out.println("Bind failed with result code: " + bindResult);
                return;
            }
            System.out.println("Bind successful.");

            System.out.print("Please enter the car name: ");
            String carName = scanner.nextLine();

            byte[] baseDNBytes     = "ou=Automobiles,dc=example,dc=com".getBytes("UTF-8");
            byte[] filterAttrBytes = "cn".getBytes("UTF-8");
            byte[] filterValBytes  = carName.getBytes("UTF-8");
            byte[] attrBytes       = "description".getBytes("UTF-8");

            byte[] avBody = concat(
                tlv(0x04, filterAttrBytes),
                tlv(0x04, filterValBytes)
            );
            byte[] filter = tlv((byte)0xA3, avBody);

            byte[] attrList   = tlv(0x04, attrBytes);
            byte[] attributes = tlv(0x30, attrList);

            byte[] searchBody = concat(
                tlv(0x04, baseDNBytes),
                new byte[]{0x0A, 0x01, 0x02},
                new byte[]{0x0A, 0x01, 0x00},
                new byte[]{0x02, 0x01, 0x00},
                new byte[]{0x02, 0x01, 0x00},
                new byte[]{0x01, 0x01, 0x00},
                filter,
                attributes
            );

            byte[] searchMsg = buildLdapMessage(2, (byte)0x63, searchBody);
            out.write(searchMsg);
            out.flush();

            boolean found = false;

            while (true) {
                byte[] msg = readMessage(in);
                int[] off = {0};

                off[0]++;
                skipLength(msg, off);

                off[0]++;
                int idLen = readLength(msg, off);
                off[0] += idLen;

                int opTag = msg[off[0]++] & 0xFF;
                skipLength(msg, off);

                if (opTag == 0x64) {
                    off[0]++;
                    int dnLen = readLength(msg, off);
                    off[0] += dnLen;

                    off[0]++;
                    int palLen = readLength(msg, off);
                    int palEnd = off[0] + palLen;

                    while (off[0] < palEnd) {
                        off[0]++;
                        int paLen = readLength(msg, off);
                        int paEnd = off[0] + paLen;

                        off[0]++;
                        int typeLen = readLength(msg, off);
                        String attrType = new String(msg, off[0], typeLen, "UTF-8");
                        off[0] += typeLen;

                        off[0]++;
                        int setLen = readLength(msg, off);
                        int setEnd = off[0] + setLen;

                        while (off[0] < setEnd) {
                            off[0]++;
                            int valLen = readLength(msg, off);
                            String value = new String(msg, off[0], valLen, "UTF-8");
                            off[0] += valLen;

                            if (attrType.equalsIgnoreCase("description")) {
                                System.out.println("Maximum speed of " + carName + ": " + value + " km/h");
                                found = true;
                            }
                        }

                        off[0] = paEnd;
                    }

                } else if (opTag == 0x65) {
                    int resultCode = msg[off[0] + 2] & 0xFF;
                    if (!found) {
                        System.out.println("Car \"" + carName + "\" not found in directory.");
                    }
                    System.out.println("Search complete. Server result code: " + resultCode);
                    break;
                }
            }

            byte[] unbindMsg = buildLdapMessage(3, (byte)0x42, new byte[]{});
            out.write(unbindMsg);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static byte[] buildLdapMessage(int msgId, byte opTag, byte[] opBody) throws IOException {
        byte[] msgIdBytes = new byte[]{0x02, 0x01, (byte) msgId};
        byte[] operation  = tlv(opTag, opBody);
        byte[] inner      = concat(msgIdBytes, operation);
        return tlv((byte)0x30, inner);
    }

    static byte[] tlv(byte tag, byte[] value) throws IOException {
        byte[] lengthBytes = encodeLength(value.length);
        byte[] result = new byte[1 + lengthBytes.length + value.length];
        result[0] = tag;
        System.arraycopy(lengthBytes, 0, result, 1, lengthBytes.length);
        System.arraycopy(value, 0, result, 1 + lengthBytes.length, value.length);
        return result;
    }

    static byte[] tlv(int tag, byte[] value) throws IOException {
        return tlv((byte) tag, value);
    }

    static byte[] encodeLength(int length) {
        if (length < 0x80) {
            return new byte[]{ (byte) length };
        } else if (length < 0x100) {
            return new byte[]{ (byte)0x81, (byte) length };
        } else if (length < 0x10000) {
            return new byte[]{ (byte)0x82, (byte)(length >> 8), (byte)(length & 0xFF) };
        } else {
            throw new IllegalArgumentException("Length too large: " + length);
        }
    }

    static byte[] readMessage(InputStream in) throws IOException {
        int tag = in.read();
        if (tag < 0) throw new EOFException("Connection closed");

        int first = in.read();
        if (first < 0) throw new EOFException("Connection closed");

        byte[] lengthBytes;
        int length;

        if ((first & 0x80) == 0) {
            length = first;
            lengthBytes = new byte[]{ (byte) first };
        } else {
            int numBytes = first & 0x7F;
            byte[] lenBuf = new byte[numBytes];
            readFully(in, lenBuf, numBytes);
            length = 0;
            for (byte b : lenBuf) length = (length << 8) | (b & 0xFF);
            lengthBytes = new byte[1 + numBytes];
            lengthBytes[0] = (byte) first;
            System.arraycopy(lenBuf, 0, lengthBytes, 1, numBytes);
        }

        byte[] value = new byte[length];
        readFully(in, value, length);

        return concat(new byte[]{ (byte) tag }, lengthBytes, value);
    }

    static int parseResultCode(byte[] msg) {
        int[] off = {0};
        off[0]++; skipLength(msg, off);
        off[0]++; int idLen = readLength(msg, off); off[0] += idLen;
        off[0]++; skipLength(msg, off);
        off[0] += 2;
        return msg[off[0]] & 0xFF;
    }

    static int readLength(byte[] buf, int[] off) {
        int first = buf[off[0]++] & 0xFF;
        if ((first & 0x80) == 0) return first;
        int numBytes = first & 0x7F;
        int len = 0;
        for (int i = 0; i < numBytes; i++) len = (len << 8) | (buf[off[0]++] & 0xFF);
        return len;
    }

    static void skipLength(byte[] buf, int[] off) {
        readLength(buf, off);
    }

    static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, result, pos, p.length);
            pos += p.length;
        }
        return result;
    }

    static void readFully(InputStream in, byte[] buf, int length) throws IOException {
        int read = 0;
        while (read < length) {
            int n = in.read(buf, read, length - read);
            if (n < 0) throw new EOFException("Server closed connection early");
            read += n;
        }
    }
}