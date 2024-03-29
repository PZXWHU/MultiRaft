package com.pzx.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SerDeCode {

    KRYO((byte)0),
    JSON((byte)1),
    PROTOBUF((byte)2);

    private byte code;
}
