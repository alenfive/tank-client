package com.source3g.tankclient.action.Tank5Action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.GlodService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Service
public class TankFiveService {

    @Autowired
    private MoveService moveService;
    @Autowired
    private MapService mapService;
    @Autowired
    private GlodService glodService;
    @Autowired
    private AttackService attackService;


    public void action(GlobalValues params, Action action){
        if(action.isUsed())return;

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank currTank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);


        //逃离
        Position leavePos = moveService.buildLeave(params,currTank,currPos);
        if(leavePos != null){
            moveService.buildMove(params,action,currTank,currPos,leavePos,false);
            return;
        }

        //攻击敌方
        TankPosition ableAttackPos = attackService.ableAttackTop(params,currTank,currPos);
        if (ableAttackPos != null){
            attackService.attackTank(view,currTank,action,currPos,ableAttackPos.getPos(),ableAttackPos.getTank());
            return;
        }

        //攻击boss
        TankPosition ableAttackBoss = attackService.buildAbleAttackBossPos(params,currTank,currPos);
        if (ableAttackBoss != null){
            attackService.attackTank(view,currTank,action,currPos,ableAttackBoss.getPos(),ableAttackBoss.getTank());
            return;
        }

        //存在攻击目标-向站位靠近
        if (params.getAttackTarget() != null){
            Position preTargetPos = buildPrepareAttackPos(params,params.getAttackTarget(),currTank,currPos);
            if (preTargetPos != null){
                moveService.buildMove(params,action,currTank,currPos,preTargetPos,true);
                return;
            }
        }

        //扫完图
        Position targetPos = buildCenterPos(params,currPos);
        moveService.buildMove(params,action,currTank,currPos,targetPos,true);
    }

    private Position buildCenterPos(GlobalValues params,Position currPos) {
        Position targetPos = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        mapService.buildBlank(params,currPos,targetPos);
        return targetPos;
    }


    /**
     * 构建攻击前的站位
     * @param params
     * @param attackTarget
     * @param currTank
     * @param currPos
     */
    public Position buildPrepareAttackPos(GlobalValues params, TankPosition attackTarget, Tank currTank, Position currPos) {

        //如果有已方一个以上的其他队友锁定了这个敌人，那么攻击前的站位应该是可攻击位置
        List<DiffPosition> diffPos = attackService.beAttacked(params,attackTarget.getPos(),params.getCurrTeam().getTanks());
        diffPos = diffPos.stream().filter(item->!item.getTank().getTId().equals(currTank.getTId())).collect(Collectors.toList());
        if (diffPos.size() > 0){

            //可以攻击到目录的站位，优先从远到近
            List<Position> ableAttackPos = new ArrayList<>();
            ableAttackPos.addAll(attackService.buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),-1,0));
            ableAttackPos.addAll(attackService.buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),0,1));
            ableAttackPos.addAll(attackService.buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),1,0));
            ableAttackPos.addAll(attackService.buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),0,-1));

            TMap view = mapService.copyAttackLine(params,attackTarget.getTank().getTId());

            DiffPosition finalPos = ableAttackPos.stream().map(item->{
                AStar aStar = new AStar(view);
                int diff = aStar.countStep(currPos,item);
                return DiffPosition.builder().pos(item).diff(diff).build();
            }).min(Comparator.comparing(DiffPosition::getDiff)).orElse(null);

            return finalPos == null?null:finalPos.getPos();
        }


        //可以攻击到目录的站位，优先从远到近-站在敌方的攻击范围外
        List<Position> ableAttackPos = new ArrayList<>();
        ableAttackPos.addAll(attackService.buildAbleAttackPos(params,attackTarget.getTank().getShecheng()+1,attackTarget.getPos(),-1,0));
        ableAttackPos.addAll(attackService.buildAbleAttackPos(params,attackTarget.getTank().getShecheng()+1,attackTarget.getPos(),0,1));
        ableAttackPos.addAll(attackService.buildAbleAttackPos(params,attackTarget.getTank().getShecheng()+1,attackTarget.getPos(),1,0));
        ableAttackPos.addAll(attackService.buildAbleAttackPos(params,attackTarget.getTank().getShecheng()+1,attackTarget.getPos(),0,-1));

        //移除敌方的弹道范围
        ableAttackPos.removeAll(attackService.enemyAttackPosList(params));

        TMap view = mapService.copyAttackLine(params);

        DiffPosition finalPos = ableAttackPos.stream().map(item->{
            AStar aStar = new AStar(view);
            int diff = aStar.countStep(currPos,item);
            return DiffPosition.builder().pos(item).diff(diff).build();
        }).min(Comparator.comparing(DiffPosition::getDiff)).orElse(null);

        return finalPos == null?null:finalPos.getPos();
    }


}
