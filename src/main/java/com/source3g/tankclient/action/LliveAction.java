package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 存活处理
 */
@SuppressWarnings("Duplicates")
@Component
public class LliveAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        Position currPos = mapService.getPosition(params.getView(),action.getTId());

        //坦克未死亡
        if(currPos != null){
            return NodeType.Success;
        }

        if(params.getCurrTeam().getGlod() == 0){
            return NodeType.Failure;
        }

        params.getCurrTeam().setGlod(params.getCurrTeam().getGlod()-1);
        //死亡后使用复活币
        action.setUseGlod(true);
        return NodeType.Success;
    }

}
