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
            globalValues.setView(objectMapper.readValue(objectMapper.writeValueAsString(params.getView()), TMap.class));
            TeamDetail currTeam = "tB".equals(teamId)?params.getTB():params.getTC();
            globalValues.setCurrTeam(objectMapper.readValue(objectMapper.writeValueAsString(currTeam), TeamDetail.class));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return NodeType.Success;
    }
}
