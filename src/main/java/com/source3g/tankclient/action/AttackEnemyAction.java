package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻击敌方坦克
 */
@Component
public class AttackEnemyAction extends AbstractActiion<GlobalValues,List<Action>> {

    @Autowired
    private MapService mapService;

    @Autowired
    private AttackService attackService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        //发现敌方坦克
        TMap view = params.getView();
        TeamDetail enemyTeam = params.getEnemyTeam();
        MapEnum[] enemyMaps = enemyTeam.getTanks().stream().map(item->MapEnum.valueOf(item.getTId())).toArray(MapEnum[]::new);
        List<Position> enemyPosList = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,enemyMaps);

        if(enemyPosList.isEmpty()){
            return NodeType.Failure;
        }

        //集结
        params.getSessionData().setMass(true);
        //获得集结点
        Position massPos = buildMassPos(params,enemyPosList);
        moveService.buildLeader(params,actions,massPos);

        Integer currShengmin = params.getCurrTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();
        Integer enemyShengmin = params.getEnemyTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();


        for (Action action : actions){

            if (action.isUsed())continue;

            Position currPos = mapService.getPosition(params.getView(),action.getTId());
            Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);


            //会被敌方攻击到，撤退
            if(currShengmin < enemyShengmin*2){
                List<DiffPosition> beAttackeds = attackService.beAttacked(params,tank,params.getEnemyTeam().getTanks()).stream().collect(Collectors.toList());
                if(!beAttackeds.isEmpty()){
                    if(beAttackeds.size() > 1 && retreat(params,action,params.getEnemyTeam().getTanks(),tank,currPos)){
                        continue;
                    }else if(beAttackeds.size() == 1){
                        int diffCurr = beAttackeds.get(0).getTank().getShengyushengming()/tank.getGongji();
                        int diffEnemy = tank.getShengming()/beAttackeds.get(0).getTank().getGongji();
                        List<DiffPosition> diffPos = attackService.beAttacked(params,beAttackeds.get(0).getTank(),params.getCurrTeam().getTanks());

                        if(diffPos.size() == 1 && diffCurr < diffEnemy && retreat(params,action,params.getEnemyTeam().getTanks(),tank,currPos)){
                            continue;
                        }else if(diffPos.size() < 2 && retreat(params,action,params.getEnemyTeam().getTanks(),tank,currPos)){
                            continue;
                        }

                    }
                }
            }



            //可攻击的坦克
            Position targetPos = attackService.ableAttackTop(view, tank,currPos,enemyPosList,params.getEnemyTeam());
            if(targetPos != null){
                String tId = view.getMap().get(targetPos.getRowIndex()).get(targetPos.getColIndex());
                Tank targetTank = enemyTeam.getTanks().stream().filter(item->item.getTId().equals(tId)).findFirst().orElse(null);
                attackService.attackTank(view,tank,action,currPos,targetPos,targetTank);
                continue;
            }

            //向敌方靠近
            mapService.buildBlank(params,enemyPosList.get(0));
            action.setTarget(enemyPosList.get(0));
        }

        return NodeType.Success;
    }

    /**
     * 撤退
     * @param action
     * @param tank
     * @param currPos
     */
    private boolean retreat(GlobalValues params,Action action, List<Tank> enemyTanks,Tank tank, Position currPos) {
        List<Position> ablePos = new ArrayList<>();
        for (int i=1;i<=tank.getYidong();i++){
            ablePos.addAll(Arrays.asList(
                    new Position(currPos.getRowIndex()-i,currPos.getColIndex()),
                    new Position(currPos.getRowIndex(),currPos.getColIndex()+i),
                    new Position(currPos.getRowIndex()+i,currPos.getColIndex()),
                    new Position(currPos.getRowIndex(),currPos.getColIndex()-i)
            ));
        }
        TMap view = params.getView();

        ablePos = ablePos.stream().filter(item->{
            boolean valid = item.getRowIndex()>=0 && item.getRowIndex()<view.getRowLen() && item.getColIndex()>=0 && item.getColIndex()<view.getColLen();
            if(!valid)return false;

            String mId = view.get(item.getRowIndex(),item.getColIndex());
            boolean block = mapService.isBlock(mId);
            if(block)return false;

            boolean able = attackService.isAbeAttacked(view,item,enemyTanks);
            return !able;
        }).collect(Collectors.toList());

        if (ablePos.isEmpty())return false;

        action.setTarget(ablePos.get(0));
        return true;
    }

    private Position buildMassPos(GlobalValues params, List<Position> enemyPosList) {
        return new Position(enemyPosList.get(0).getRowIndex(),enemyPosList.get(0).getColIndex());
    }


}
