package com.pzx.raft.core.utils;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Comparator;

public class ByteUtils {

    // A byte array comparator based on lexicograpic ordering.
    private static final ByteArrayComparator BYTES_LEXICO_COMPARATOR = new LexicographicByteArrayComparator();

    private ByteUtils(){

    }

    public static short bytesToShort(byte b1, byte b2){
        return (short) bytesToInteger(b1,b2,(byte)0,(byte)0);
        /*
        return (b2<<8&0xff00)|(b1&0xff);
         */

    }

    public static int bytesToUnsignedShort(byte b1, byte b2){
        return bytesToInteger(b1,b2,(byte)0,(byte)0);
    }

    public static int bytesToInteger(byte b1, byte b2, byte b3, byte b4){

        return (int)bytesToLong(b1,b2,b3,b4,(byte)0,(byte)0,(byte)0,(byte)0);
        /*
        return (b4<<24&0xff000000)|(b3<<16&0xff0000)|(b2<<8&0xff00)|(b1&0xff);

         */
    }

    public static long bytesToUnsignedInteger(byte b1, byte b2, byte b3, byte b4){
        return bytesToLong(b1,b2,b3,b4,(byte)0,(byte)0,(byte)0,(byte)0);
    }

    public static long bytesToLong(byte b1, byte b2, byte b3, byte b4,byte b5, byte b6, byte b7, byte b8){
        /*
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{b1,b2,b3,b4,b5,b6,b7,b8});
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getLong();

         */

        return ((long)b8<<56&(long)0xff<<56)|
                ((long)b7<<48&(long)0xff<<48)|
                ((long)b6<<40&(long)0xff<<40)|
                ((long)b5<<32&(long)0xff<<32)|
                ((long)b4<<24&(long)0xff<<24)|
                ((long)b3<<16&(long)0xff<<16)|
                ((long)b2<<8&(long)0xff<<8)|
                ((long)b1&(long)0xff);


    }


    public static float bytesToFloat(byte b1, byte b2, byte b3, byte b4){
        /*
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{b1,b2,b3,b4});
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat();

         */
        return Float.intBitsToFloat(bytesToInteger(b1, b2, b3, b4));

    }

    public static double bytesToDouble(byte b1, byte b2, byte b3, byte b4,byte b5, byte b6, byte b7, byte b8){

        long l = bytesToLong(b1, b2, b3, b4, b5, b6, b7, b8);

        return Double.longBitsToDouble(l);
        /*
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{b1,b2,b3,b4,b5,b6,b7,b8});
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();

         */
    }

    public static int bytesToInteger(byte[] bytes)throws IllegalArgumentException{
        if (bytes.length!=4){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        return bytesToInteger(bytes[0],bytes[1],bytes[2],bytes[3]);
    }


    public static short bytesToShort(byte[] bytes){
        if (bytes.length!=2){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        return bytesToShort(bytes[0],bytes[1]);
    }

    public static int bytesToUnsignedShort(byte[] bytes){
        if (bytes.length!=2){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        return bytesToUnsignedShort(bytes[0],bytes[1]);
    }

    public static float bytesToFloat(byte[] bytes){
        if (bytes.length!=4){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        return bytesToFloat(bytes[0],bytes[1],bytes[2],bytes[3]);
    }

    public static long bytesToLong(byte[] bytes){
        if (bytes.length!=8){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        return bytesToLong(bytes[0],bytes[1],bytes[2],bytes[3],bytes[4],bytes[5],bytes[6],bytes[7]);
    }


    public static long bytesToUnsignedInteger(byte[] bytes){
        if (bytes.length!=4){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        return bytesToUnsignedInteger(bytes[0],bytes[1],bytes[2],bytes[3]);
    }

    public static double bytesToDouble(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
    }

    public static byte[] shortToBytes(short data)
    {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        return bytes;
    }


    public static byte[] integerToBytes(int data)
    {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        bytes[2] = (byte) ((data & 0xff0000) >> 16);
        bytes[3] = (byte) ((data & 0xff000000) >> 24);
        return bytes;
    }

    public static byte[] longToBytes(long data)
    {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data >> 8) & 0xff);
        bytes[2] = (byte) ((data >> 16) & 0xff);
        bytes[3] = (byte) ((data >> 24) & 0xff);
        bytes[4] = (byte) ((data >> 32) & 0xff);
        bytes[5] = (byte) ((data >> 40) & 0xff);
        bytes[6] = (byte) ((data >> 48) & 0xff);
        bytes[7] = (byte) ((data >> 56) & 0xff);
        return bytes;
    }

    public static byte[] floatToBytes(float data)
    {
        int intBits = Float.floatToIntBits(data);
        return integerToBytes(intBits);
    }

    public static byte[] doubleToBytes(double d){
        long intBits = Double.doubleToLongBits(d);
        return longToBytes(intBits);
    }

    public static byte[] stringToBytes(String s){
        return s.getBytes(Charset.forName("UTF-8"));
    }

    public static String bytesToString(byte[] bytes){
        return new String(bytes, Charset.forName("UTF-8"));
    }

    public static byte[] merge(byte[] bt1, byte[] bt2){
        if(bt1 == null || bt2 == null) return bt1 == null ? bt2 : bt1;
        if (bt1.length == 0 || bt2.length == 0) return bt1.length == 0 ? bt2 : bt1;

        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    public static int compare(final byte[] a, final byte[] b) {
        return BYTES_LEXICO_COMPARATOR.compare(a, b);
    }

    public interface ByteArrayComparator extends Comparator<byte[]>, Serializable {

        int compare(final byte[] buffer1, final int offset1, final int length1, final byte[] buffer2,
                    final int offset2, final int length2);
    }

    private static class LexicographicByteArrayComparator implements ByteArrayComparator {

        private static final long serialVersionUID = -8623342242397267864L;

        @Override
        public int compare(final byte[] buffer1, final byte[] buffer2) {
            return compare(buffer1, 0, buffer1.length, buffer2, 0, buffer2.length);
        }

        @Override
        public int compare(final byte[] buffer1, final int offset1, final int length1, final byte[] buffer2,
                           final int offset2, final int length2) {
            // short circuit equal case
            if (buffer1 == buffer2 && offset1 == offset2 && length1 == length2) {
                return 0;
            }
            // similar to Arrays.compare() but considers offset and length
            final int end1 = offset1 + length1;
            final int end2 = offset2 + length2;
            for (int i = offset1, j = offset2; i < end1 && j < end2; i++, j++) {
                int a = buffer1[i] & 0xff;
                int b = buffer2[j] & 0xff;
                if (a != b) {
                    return a - b;
                }
            }
            return length1 - length2;
        }
    }

}
