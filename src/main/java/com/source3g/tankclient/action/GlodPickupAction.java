package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 复活币拾取
 */
@Component
public class GlodPickupAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());

        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

        int startR = currPos.getRowIndex()-tank.getShiye();
        int endR = currPos.getRowIndex()+tank.getShiye();
        int startC = currPos.getColIndex()-tank.getShiye();
        int endC = currPos.getColIndex()+tank.getShiye();

        //可视范围内存在复活币
        List<Position> positions = mapService.findByMapEnum(view,startR,endR,startC,endC,MapEnum.M2);

        if(positions.isEmpty()){
            return NodeType.Failure;
        }

        AStar aStar = new AStar(view);
        Position nextPos = aStar.findPath(currPos,positions.get(0));

        //计算移动的最大距离
        nextPos = mapService.getMaxNext(tank,currPos,nextPos);
        if(nextPos == null){
            return NodeType.Failure;
        }

        //允许下一步，替换地图
        params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
        params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

        //根据坐标，计算方位和步长
        moveService.buildAction(action,currPos,nextPos);
        return NodeType.Success;
    }

}
