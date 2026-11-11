package com.muhammaddaffa.playerprofiles.utils.nbt;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class NBTReader {

    public static NBTCompound readFile(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            byte type = in.readByte();
            if (type != 10) {
                throw new IOException("Root tag must be TAG_Compound (10), got " + type);
            }
            in.readUTF(); // Root tag name (usually empty)
            return readCompound(in);
        }
    }

    public static NBTCompound readStream(InputStream input) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(input)))) {
            byte type = in.readByte();
            if (type != 10) {
                throw new IOException("Root tag must be TAG_Compound (10), got " + type);
            }
            in.readUTF();
            return readCompound(in);
        }
    }

    private static NBTCompound readCompound(DataInputStream in) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            byte type = in.readByte();
            if (type == 0) break; // TAG_End
            String name = in.readUTF();
            Object payload = readPayload(in, type);
            map.put(name, payload);
        }
        return new NBTCompound(map);
    }

    private static Object readPayload(DataInputStream in, byte type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> {
                int len = in.readInt();
                byte[] payloadBytes = new byte[Math.max(0, len)];
                in.readFully(payloadBytes);
                yield payloadBytes;
            }
            case 8 -> in.readUTF();
            case 9 -> {
                byte elemType = in.readByte();
                int len = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, len));
                for (int i = 0; i < len; i++) {
                    list.add(readPayload(in, elemType));
                }
                yield list;
            }
            case 10 -> readCompound(in);
            case 11 -> {
                int len = in.readInt();
                int[] payloadInts = new int[Math.max(0, len)];
                for (int i = 0; i < len; i++) {
                    payloadInts[i] = in.readInt();
                }
                yield payloadInts;
            }
            case 12 -> {
                int len = in.readInt();
                long[] payloadLongs = new long[Math.max(0, len)];
                for (int i = 0; i < len; i++) {
                    payloadLongs[i] = in.readLong();
                }
                yield payloadLongs;
            }
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        };
    }
}
