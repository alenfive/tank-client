package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.service.LeaderService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 巡逻
 */
@Component
public class OnPatrolAction extends AbstractActiion<GlobalValues,List<Action>> {

    @Autowired
    private MoveService moveService;
    @Autowired
    private LeaderService leaderService;
    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        if(params.getSessionData().getLeader().getFinalPos() == null){
            //构建leader行动
            Position targetPos = leaderService.buildCenterBlank(params);
            mapService.buildBlank(params,params.getSessionData().getLeader().getCurrPos(),targetPos);

            Position leaderPos = params.getSessionData().getLeader().getCurrPos();

            AStar aStar = new AStar(params.getView());
            aStar.appendBlockList(params.getCurrTeamTId());
            aStar.findPath(leaderPos,targetPos);

            Position nextPos = null;
            if(leaderPos != null && leaderPos.getParent() != null){
                nextPos = leaderPos.getParent();
            }
            params.getSessionData().getLeader().setFinalPos(targetPos);

            leaderService.buildLeader(params,actions,nextPos);
        }

        for(Action action : actions){
            moveService.buildMove(params,action);
        }

        return NodeType.Success;
    }










}
