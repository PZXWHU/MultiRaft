package com.pzx.raft.utils;

import com.google.common.base.Preconditions;

import java.net.InetSocketAddress;

public class AddressUtils {

    public static InetSocketAddress stringToInetSocketAddress(String address){
        Preconditions.checkArgument(checkStringAddress(address), "the address is not validate : " + address);

        String[] addressArray = address.split(":");
        InetSocketAddress inetSocketAddress = new InetSocketAddress(addressArray[1], Integer.parseInt(addressArray[1]));
        return inetSocketAddress;

    }

    public static boolean checkStringAddress(String stringAddress){
        String[] addressArray = stringAddress.split(":");
        if (addressArray.length != 2) return false;
        if (addressArray[0].split("\\.").length != 4) return false;
        return true;
    }

}
