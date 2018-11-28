package com.source3g.tankclient.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tank {
    private String tId;
    private String name;
    private Integer gongji;
    private Integer shengming;
    private Integer shengyushengming;
    private Integer yidong;
    private Integer shecheng;
    private Integer shiye;
    private boolean mingzhong;
    private DirectionEnum direction;

    public Integer getShengyushengming() {
        if(shengyushengming == null){
            shengyushengming = shengming;
        }
        return shengyushengming;
    }

    public Tank setTId(String tId){
        this.tId = tId;
        return this;
    }

    @JsonProperty("tId")
    public String getTId() {
        return tId;
    }
}
