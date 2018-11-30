package com.source3g.tankclient.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.TMap;
import com.source3g.tankclient.entity.TeamDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 初始化
 */
@Component
public class InitAction extends AbstractActiion<ClientParam,GlobalValues> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public NodeType process(ClientParam params, GlobalValues globalValues) {

        globalValues.setClientParam(params);

        String teamId = params.getTeam();
        globalValues.setResultAction(new ArrayList<>());
        try {

            TMap view = objectMapper.readValue(objectMapper.writeValueAsString(params.getView()), TMap.class);
            TeamDetail currTeam = objectMapper.readValue(objectMapper.writeValueAsString("tB".equals(teamId)?params.getTB():params.getTC()), TeamDetail.class);
            TeamDetail bossTeam = objectMapper.readValue(objectMapper.writeValueAsString(params.getTA()), TeamDetail.class);
            TeamDetail enemyTeam = objectMapper.readValue(objectMapper.writeValueAsString(!"tB".equals(teamId)?params.getTB():params.getTC()), TeamDetail.class);


            //深拷贝一份
            globalValues.setView(view);
            globalValues.setCurrTeam(currTeam);
            globalValues.setBossTeam(bossTeam);
            globalValues.setEnemyTeam(enemyTeam);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return NodeType.Success;
    }
}
