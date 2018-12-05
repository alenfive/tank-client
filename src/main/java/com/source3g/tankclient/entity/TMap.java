package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TMap {
    private String name;
    private Integer rowLen;
    private Integer colLen;
    private List<List<String>> map;

    public String get(int r,int c){
        return map.get(r).get(c);
    }
}
