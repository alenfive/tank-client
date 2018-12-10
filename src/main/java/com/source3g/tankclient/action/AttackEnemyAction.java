package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.LeaderService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    @Autowired
    private LeaderService leaderService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        //发现敌方坦克
        /*TMap view = params.getView();
        TeamDetail enemyTeam = params.getEnemyTeam();
        MapEnum[] enemyMaps = enemyTeam.getTanks().stream().map(item->MapEnum.valueOf(item.getTId())).toArray(MapEnum[]::new);


        Integer currShengmin = params.getCurrTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();
        Integer enemyShengmin = params.getEnemyTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();

        List<Position> enemyPosList = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,enemyMaps);
        if(enemyPosList.isEmpty())return NodeType.Failure;

        for (Action action : actions){
            if (action.isUsed())continue;

            enemyPosList = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,enemyMaps);

            if(enemyPosList.isEmpty()){
                return NodeType.Failure;
            }

            //集结
            params.getSessionData().setMass(true);



            Position currPos = mapService.getPosition(params.getView(),action.getTId());
            Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);


            //会被敌方攻击到，并且打不过，撤退
            if(currShengmin < enemyShengmin*3 && params.getEnemyTeam().getGlod() == 0){
                List<DiffPosition> beAttackeds = attackService.beAttacked(params,tank,params.getEnemyTeam().getTanks()).stream().collect(Collectors.toList());
                if(!beAttackeds.isEmpty()){
                    if(beAttackeds.size() > 1 && retreat(params,action,params.getEnemyTeam().getTanks(),tank,currPos)){
                        continue;
                    }else if(beAttackeds.size() == 1){
                        int diffCurr = beAttackeds.get(0).getTank().getShengyushengming()/tank.getGongji();
                        int diffEnemy = tank.getShengyushengming()/beAttackeds.get(0).getTank().getGongji();
                        List<DiffPosition> diffPos = attackService.beAttacked(params,beAttackeds.get(0).getTank(),params.getCurrTeam().getTanks());

                        if(diffPos.size() == 1 && diffCurr > diffEnemy && retreat(params,action,params.getEnemyTeam().getTanks(),tank,currPos)){
                            continue;
                        }else if(diffPos.size() < 2 && retreat(params,action,params.getEnemyTeam().getTanks(),tank,currPos)){
                            continue;
                        }

                    }
                }
            }*/



            //可攻击的坦克
            /*Position targetPos = attackService.ableAttackTop(view, tank,currPos,enemyPosList,params.getEnemyTeam());
            if(targetPos != null){
                String tId = view.getMap().get(targetPos.getRowIndex()).get(targetPos.getColIndex());
                Tank targetTank = enemyTeam.getTanks().stream().filter(item->item.getTId().equals(tId)).findFirst().orElse(null);
                attackService.attackTank(view,tank,action,currPos,targetPos,targetTank);
                continue;
            }

        }

       enemyPosList = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,enemyMaps);


        return NodeType.Success;*/
       return null;
    }

    private void buildEnforcement(GlobalValues params, List<Action> actions,Position finalPos) {
        for (Action action : actions) {
            if (action.isUsed()) continue;

            Position currPos = mapService.getPosition(params.getView(),action.getTId());
            Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

            int len = tank.getShecheng();

            List<Position> ableAttPos = new ArrayList<>();
            for(int i=len ==1?len:2;i<=len;i++){
                ableAttPos.addAll(Arrays.asList(
                        new Position(finalPos.getRowIndex()-i,finalPos.getColIndex()),
                        new Position(finalPos.getRowIndex(),finalPos.getColIndex()+i),
                        new Position(finalPos.getRowIndex()+i,finalPos.getColIndex()),
                        new Position(finalPos.getRowIndex(),finalPos.getColIndex()-i)
                ));
            }

            DiffPosition nextPosDiff = ableAttPos.stream().filter(item->{
                mapService.buildPosition(params,item);
                String mId = params.getView().get(item.getRowIndex(),item.getColIndex());
                return !mapService.isBlock(mId);
            }).map(item->{
                DiffPosition diffPosition = new DiffPosition();
                diffPosition.setPos(item);
                AStar aStar = new AStar(params.getView());
                diffPosition.setDiff(aStar.countStep(currPos,item));
                return diffPosition;
            }).min(Comparator.comparing(DiffPosition::getDiff)).orElse(null);

            Position nextPos = nextPosDiff.getPos();
            //没有下一步就不动
            if(nextPos == null || nextPos.getParent() == null)return;

            nextPos = mapService.getMaxNext(tank,currPos,nextPos);

            //根据坐标，计算方位和步长
            moveService.buildAction(params,action,currPos,nextPos);
        }
    }

    @SuppressWarnings("Duplicates")

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

    //寻找落单的-周围半径1以上的落单者，倒序
    private Position buildMassPos(GlobalValues params, List<Position> enemyPosList) {
        int scope = params.getView().getRowLen()/2;
        MapEnum[] enums = params.getEnemyTeamTId().stream().map(MapEnum::valueOf).toArray(MapEnum[]::new);

        Position target = null;
        for(int i=scope;i>=1;i--){
            for(Position currPos : enemyPosList){
                int startR = currPos.getRowIndex()-i;
                int endR = currPos.getRowIndex()+i;
                int startC = currPos.getColIndex()-i;
                int endC = currPos.getColIndex()+i;

                List<Position> enumsResut = mapService.findByMapEnum(params.getView(),startR,endR,startC,endC,enums);
                if(enumsResut.size() == 1){
                    target = currPos;
                    break;
                }
            }
            if(target != null)break;
        }

        return target;

    }


}
