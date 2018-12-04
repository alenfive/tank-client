package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.FormationService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 攻击敌方坦克
 */
@Component
public class AttackEnemyAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;

    @Autowired
    private FormationService formationService;

    @Autowired
    private AttackService attackService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);
        TeamDetail enemyTeam = params.getEnemyTeam();
        MapEnum[] enemyMaps = enemyTeam.getTanks().stream().map(item->MapEnum.valueOf(item.getTId())).toArray(MapEnum[]::new);
        //发现敌方坦克
        List<Position> positions = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,enemyMaps);
        if(positions.isEmpty()){
            return NodeType.Failure;
        }

        //集结
        params.getSessionData().setMass(true);

        //可攻击的坦克
        Position targetPos = attackService.ableAttack(view, tank,currPos,positions,params.getEnemyTeam());

        //攻击范围内存在敌方
        if(targetPos != null){
            String tId = view.getMap().get(targetPos.getRowIndex()).get(targetPos.getColIndex());
            Tank targetTank = enemyTeam.getTanks().stream().filter(item->item.getTId().equals(tId)).findFirst().orElse(null);
            attackService.attackTank(view,tank,action,currPos,targetPos,targetTank);
            return NodeType.Success;
        }

        //获得集结点
        //Position massPos = formationService.getMass(params);
        Position massPos = positions.get(0);
        if(massPos.getRowIndex() == currPos.getRowIndex() && massPos.getColIndex() == currPos.getColIndex()){
            return NodeType.Failure;
        }

        //制定路线
        Position nextPos = formationService.random(params,tank,currPos,massPos);
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
