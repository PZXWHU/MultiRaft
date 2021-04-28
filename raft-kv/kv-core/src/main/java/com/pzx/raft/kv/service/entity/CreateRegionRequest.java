package com.pzx.raft.kv.service.entity;


import com.pzx.raft.kv.entity.Region;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegionRequest {

    Region region;

}
