package com.pzx.raft.kv.service.entity;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateRegionResponse {

    private boolean success;

    private String message;

}
