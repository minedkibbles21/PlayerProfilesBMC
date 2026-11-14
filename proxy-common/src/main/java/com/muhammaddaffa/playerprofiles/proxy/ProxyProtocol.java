package com.muhammaddaffa.playerprofiles.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ProxyProtocol {

    public static final String CHANNEL_IDENTIFIER = "bmc:profiles";
    public static final byte PACKET_MAGIC = (byte) 0xBC;
    public static final byte ACTION_FETCH_PROFILE = 0x01;
    public static final byte ACTION_DELIVER_PROFILE = 0x02;

    public static final byte STATUS_SUCCESS = 0x00;
    public static final byte STATUS_ABSENT = 0x01;
    public static final byte STATUS_FORBIDDEN = 0x02;

    public record InboundQuery(UUID queryIdentifier, UUID requestedPlayerUuid, boolean authenticated) {}

    public record InboundResponse(UUID queryIdentifier, byte statusIndicator, byte[] contentBytes, boolean authenticated) {}

    public static byte[] serializeQuery(UUID queryIdentifier, UUID requestedPlayerUuid, SecretManager secretHolder) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataOutputStream streamWriter = new DataOutputStream(byteBuffer);

        streamWriter.writeByte(PACKET_MAGIC);
        streamWriter.writeByte(ACTION_FETCH_PROFILE);
        streamWriter.writeLong(queryIdentifier.getMostSignificantBits());
        streamWriter.writeLong(queryIdentifier.getLeastSignificantBits());
        streamWriter.writeLong(requestedPlayerUuid.getMostSignificantBits());
        streamWriter.writeLong(requestedPlayerUuid.getLeastSignificantBits());
        streamWriter.writeLong(System.currentTimeMillis());
        streamWriter.flush();

        byte[] payloadBeforeSignature = byteBuffer.toByteArray();
        byte[] computedSignature = secretHolder.computeHmac(payloadBeforeSignature);
        streamWriter.write(computedSignature);
        streamWriter.flush();

        return byteBuffer.toByteArray();
    }

    public static InboundQuery parseQuery(byte[] rawPacketBytes, SecretManager secretHolder) {
        if (rawPacketBytes == null || rawPacketBytes.length < 58) {
            return null;
        }

        try (DataInputStream streamReader = new DataInputStream(new ByteArrayInputStream(rawPacketBytes))) {
            byte readMagic = streamReader.readByte();
            byte readAction = streamReader.readByte();
            if (readMagic != PACKET_MAGIC || readAction != ACTION_FETCH_PROFILE) {
                return null;
            }

            long queryMsb = streamReader.readLong();
            long queryLsb = streamReader.readLong();
            long playerMsb = streamReader.readLong();
            long playerLsb = streamReader.readLong();
            streamReader.readLong(); // Timestamp

            int payloadLength = 2 + 16 + 16 + 8; // 42 bytes
            byte[] signedSection = new byte[payloadLength];
            System.arraycopy(rawPacketBytes, 0, signedSection, 0, payloadLength);

            byte[] attachedSignature = new byte[32];
            streamReader.readFully(attachedSignature);

            boolean isSignatureValid = secretHolder.verifyHmac(signedSection, attachedSignature);
            UUID parsedQueryId = new UUID(queryMsb, queryLsb);
            UUID parsedPlayerUuid = new UUID(playerMsb, playerLsb);

            return new InboundQuery(parsedQueryId, parsedPlayerUuid, isSignatureValid);
        } catch (IOException streamException) {
            return null;
        }
    }

    public static byte[] serializeResponse(UUID queryIdentifier, byte statusIndicator, byte[] contentBytes, SecretManager secretHolder) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataOutputStream streamWriter = new DataOutputStream(byteBuffer);

        streamWriter.writeByte(PACKET_MAGIC);
        streamWriter.writeByte(ACTION_DELIVER_PROFILE);
        streamWriter.writeLong(queryIdentifier.getMostSignificantBits());
        streamWriter.writeLong(queryIdentifier.getLeastSignificantBits());
        streamWriter.writeByte(statusIndicator);

        int payloadSize = (contentBytes != null) ? contentBytes.length : 0;
        streamWriter.writeInt(payloadSize);
        if (contentBytes != null && payloadSize > 0) {
            streamWriter.write(contentBytes);
        }
        streamWriter.flush();

        byte[] payloadBeforeSignature = byteBuffer.toByteArray();
        byte[] computedSignature = secretHolder.computeHmac(payloadBeforeSignature);
        streamWriter.write(computedSignature);
        streamWriter.flush();

        return byteBuffer.toByteArray();
    }

    public static InboundResponse parseResponse(byte[] rawPacketBytes, SecretManager secretHolder) {
        if (rawPacketBytes == null || rawPacketBytes.length < 35) {
            return null;
        }

        try (DataInputStream streamReader = new DataInputStream(new ByteArrayInputStream(rawPacketBytes))) {
            byte readMagic = streamReader.readByte();
            byte readAction = streamReader.readByte();
            if (readMagic != PACKET_MAGIC || readAction != ACTION_DELIVER_PROFILE) {
                return null;
            }

            long queryMsb = streamReader.readLong();
            long queryLsb = streamReader.readLong();
            byte statusIndicator = streamReader.readByte();
            int declaredLength = streamReader.readInt();

            byte[] contentBytes = null;
            if (declaredLength > 0) {
                contentBytes = new byte[declaredLength];
                streamReader.readFully(contentBytes);
            }

            int signedSectionLength = 2 + 16 + 1 + 4 + (declaredLength > 0 ? declaredLength : 0);
            byte[] signedSection = new byte[signedSectionLength];
            System.arraycopy(rawPacketBytes, 0, signedSection, 0, signedSectionLength);

            byte[] attachedSignature = new byte[32];
            streamReader.readFully(attachedSignature);

            boolean isSignatureValid = secretHolder.verifyHmac(signedSection, attachedSignature);
            UUID parsedQueryId = new UUID(queryMsb, queryLsb);

            return new InboundResponse(parsedQueryId, statusIndicator, contentBytes, isSignatureValid);
        } catch (IOException streamException) {
            return null;
        }
    }
}
