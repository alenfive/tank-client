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
 * 攻击BOSS
 */
@SuppressWarnings("Duplicates")
@Component
public class AttackBossAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;

    @Autowired
    private FormationService formationService;
    @Autowired
    private MoveService moveService;
    @Autowired
    private AttackService attackService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());

        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);
        //发现BOSS
        List<Position> positions = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,MapEnum.A1);
        if(positions.isEmpty()){
            return NodeType.Failure;
        }

        //发现攻击目录，发起集结指令
        params.getSessionData().setMass(true);

        Tank boss = params.bossTeam.getTanks().stream().filter(item->MapEnum.A1.name().equals(item.getTId())).findFirst().orElse(null);

        Position targetPos = attackService.ableAttack(tank,currPos,positions);
        //在攻击范围内
        if(targetPos != null){
            attackService.attackTank(view,tank,action,currPos,targetPos,boss);
            return NodeType.Success;
        }

        targetPos = positions.get(0);

        //制定路线
        Position nextPos = formationService.random(params,tank,currPos,targetPos);
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
