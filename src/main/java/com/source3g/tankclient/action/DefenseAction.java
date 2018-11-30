package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.utils.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 防御
 */
@SuppressWarnings("Duplicates")
@Component
public class DefenseAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        TMap view = params.getView();
        Position currPos = MapUtils.getPosition(params.getView(),action.getTId());

        //坦克已死亡
        if(currPos == null){
            return NodeType.Failure;
        }
        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);
        

        return NodeType.Success;
    }

}
