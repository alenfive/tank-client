package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class GlobalValues {
    //原始参数
    public ClientParam clientParam;


    //自定义参数
    public TeamDetail currTeam;
    public TMap view;
    public List<Action> resultAction;
    public TeamDetail bossTeam;
    public TeamDetail enemyTeam;

    //会话参数
    private SessionData sessionData;
}
