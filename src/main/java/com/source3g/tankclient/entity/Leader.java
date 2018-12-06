package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Leader {
    //当前位置
    private Position currPos;

    //最终目标
    private Position finalPos;
}
