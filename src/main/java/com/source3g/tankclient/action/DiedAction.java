package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.utils.MapUtils;
import org.springframework.stereotype.Component;

/**
 * 死亡处理
 */
@SuppressWarnings("Duplicates")
@Component
public class DiedAction extends AbstractActiion<GlobalValues,Action> {

    @Override
    public NodeType process(GlobalValues params, Action action) {

        Position currPos = MapUtils.getPosition(params.getView(),action.getTId());

        //坦克未死亡
        if(currPos != null){
            return NodeType.Failure;
        }

        return NodeType.Success;
    }

}
