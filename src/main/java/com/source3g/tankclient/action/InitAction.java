package com.source3g.tankclient.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 初始化
 */
@Component
public class InitAction extends AbstractActiion<ClientParam,GlobalValues> {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(ClientParam params, GlobalValues globalValues) {

        globalValues.setClientParam(params);

        String teamId = params.getTeam();
        try {

            TMap view = objectMapper.readValue(objectMapper.writeValueAsString(params.getView()), TMap.class);
            TeamDetail currTeam = objectMapper.readValue(objectMapper.writeValueAsString("tB".equals(teamId)?params.getTB():params.getTC()), TeamDetail.class);
            TeamDetail bossTeam = objectMapper.readValue(objectMapper.writeValueAsString(params.getTA()), TeamDetail.class);
            TeamDetail enemyTeam = objectMapper.readValue(objectMapper.writeValueAsString(!"tB".equals(teamId)?params.getTB():params.getTC()), TeamDetail.class);
            List<String> currTeamTId = currTeam.getTanks().stream().map(Tank::getTId).collect(Collectors.toList());
            List<String> enemyTeamTId = enemyTeam.getTanks().stream().map(Tank::getTId).collect(Collectors.toList());

            List<Action> actions = currTeam.getTanks().stream().map(item-> {
                ActionTypeEnum typeEnum = ActionTypeEnum.FIRE;
                return Action.builder()
                        .length(100)
                        .tId(item.getTId())
                        .type(typeEnum)
                        .useGlod(false)
                        .tank(item)
                        .direction(DirectionEnum.getByIndex((int) Math.floor(Math.random()*4))).build();
            }).collect(Collectors.toList());

            //深拷贝一份
            globalValues.setResultAction(actions);
            globalValues.setView(view);
            globalValues.setCurrTeam(currTeam);
            globalValues.setBossTeam(bossTeam);
            globalValues.setEnemyTeam(enemyTeam);
            globalValues.setCurrTeamTId(currTeamTId);
            globalValues.setEnemyTeamTId(enemyTeamTId);
            globalValues.setSortNo(0);

            //会话参数
            globalValues.setSessionData(params.getSessionData());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return NodeType.Success;
    }
}
