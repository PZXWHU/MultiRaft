package com.pzx.raft.core.utils;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;

public class ByteUtils {

    // A byte array comparator based on lexicograpic ordering.
    public static final ByteArrayComparator BYTES_LEXICO_COMPARATOR = new LexicographicByteArrayComparator();

    public static final byte[] BYTE_ARRAY_MIN_VALUE = new byte[0];

    public static final byte[] BYTE_ARRAY_MAX_VALUE = null;

    private ByteUtils(){
    }


    public static int bytesToInteger(byte[] bytes)throws IllegalArgumentException{
        if (bytes.length!=4){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt();
    }


    public static short bytesToShort(byte[] bytes){
        if (bytes.length!=2){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getShort();
    }



    public static float bytesToFloat(byte[] bytes){
        if (bytes.length!=4){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getFloat();
    }

    public static long bytesToLong(byte[] bytes){
        if (bytes.length!=8){
            throw new IllegalArgumentException("字节数组长度不符合要求");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getLong();
    }


    public static double bytesToDouble(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getDouble();
    }

    public static byte[] shortToBytes(short data)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(data);
        return byteBuffer.array();
    }


    public static byte[] integerToBytes(int data)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(data);

        return byteBuffer.array();
    }

    public static void main(String[] args) {
        System.out.println(ByteUtils.bytesToInteger(ByteUtils.integerToBytes(4000)));
    }

    public static byte[] longToBytes(long data)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putLong(data);

        return byteBuffer.array();
    }

    public static byte[] floatToBytes(float data)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putFloat(data);
        return byteBuffer.array();

    }

    public static byte[] doubleToBytes(double data){
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putDouble(data);
        return byteBuffer.array();
    }

    public static byte[] stringToBytes(String s){
        return s.getBytes(Charset.forName("UTF-8"));
    }

    public static String bytesToString(byte[] bytes){
        if (bytes == null) return null;
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
            if (buffer1 == buffer2) return 0;

            if (buffer1 == ByteUtils.BYTE_ARRAY_MAX_VALUE || buffer2 == ByteUtils.BYTE_ARRAY_MAX_VALUE){
                return  buffer1 == ByteUtils.BYTE_ARRAY_MAX_VALUE ? 1 : -1;
            }
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
