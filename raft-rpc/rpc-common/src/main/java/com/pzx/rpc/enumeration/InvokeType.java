package com.pzx.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum InvokeType {

    ONEWAY(0), SYNC(1), FUTURE(2), CALLBACK(3);

    private final int type;
}
