package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻击
 */
@Service
public class AttackService {

    @Autowired
    private MapService mapService;

    //默认攻击顺序
    private final List<String> tIdSorted = Arrays.asList("A1","C1","B1","C3","B3","C4","B4","C5","B5","C2","B2");

    /**
     * 计算能攻击到某坐标的敌方坦克
     * @param params
     * @param currPos
     * @param enemyTanks
     * @return
     */
    public List<DiffPosition> beAttacked(GlobalValues params,Position currPos,List<Tank> enemyTanks){

        List<DiffPosition> result = new ArrayList<>();
        if (currPos == null)return result;

        for(Tank enemyTank : enemyTanks){

            Position enemyPos = mapService.getPosition(params.getView(),enemyTank.getTId());
            if(enemyPos == null)continue;

            int diff = Math.abs(enemyPos.getRowIndex()-currPos.getRowIndex())+Math.abs(enemyPos.getColIndex()-currPos.getColIndex());
            boolean ableAttack = (currPos.getRowIndex()==enemyPos.getRowIndex() || currPos.getColIndex() == enemyPos.getColIndex()) && diff<=enemyTank.getShecheng();
            if(ableAttack){
                result.add(DiffPosition.builder()
                        .pos(enemyPos)
                        .tank(enemyTank)
                        .diff(diff)
                        .build());
            }

        }
        return result;
    }

    /**
     * 敌方弹道
     * @param params
     * @return
     */
    public List<Position> enemyAttackPosList(GlobalValues params){
        List<Position> attackPos = new ArrayList<>();
        for(Tank enemyTank : params.getEnemyTeam().getTanks()){
            Position enemyPos = mapService.getPosition(params.getView(),enemyTank.getTId());
            if(enemyPos == null)continue;
            attackPos.add(enemyPos);

            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,-1,0));
            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,0,1));
            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,1,0));
            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,0,-1));

        }

        return attackPos;
    }

    /**
     * 计算某坐标范围可攻击弹道
     * @param actionLen
     * @param currPos
     * @param diffR
     * @param diffC
     * @return
     */
    private List<Position> buildAbleFirePos(GlobalValues params, int actionLen, Position currPos, int diffR, int diffC){
        List<Position> leavePos = new ArrayList<>();
        for(int i=1;i<=actionLen;i++){
            Position itemPos = new Position(currPos.getRowIndex()+i*diffR,currPos.getColIndex()+i*diffC);

            if(!mapService.isPosition(params.getView(),itemPos)){
                break;
            }

            String mId = params.getView().get(itemPos.getRowIndex(),itemPos.getColIndex());
            if (params.getBossTeam().getTanks().get(0).getTId().equals(mId) || params.getEnemyTeamTId().contains(mId) || !mapService.isBlock(mId) || params.getCurrTeamTId().contains(mId)){
                leavePos.add(itemPos);
                continue;
            }
            break;
        }
        return leavePos;
    }

    /**
     * 是否会被攻击
     * @param view
     * @param currTank
     * @param enemyTank
     * @return
     */
    public boolean isAbeAttacked(TMap view, Tank currTank, Tank enemyTank){
        Position currPos = mapService.getPosition(view,currTank.getTId());
        Position enemyPos = mapService.getPosition(view,enemyTank.getTId());
        if(enemyPos == null || currPos == null)return false;
        int diff = Math.abs(enemyPos.getRowIndex()-currPos.getRowIndex())+Math.abs(enemyPos.getColIndex()-currPos.getColIndex());
        return (currPos.getRowIndex()==enemyPos.getRowIndex() || currPos.getColIndex() == enemyPos.getColIndex()) && diff<=enemyTank.getShecheng();
    }

    /**
     * 是否会被攻击
     * @param view
     * @param currPos
     * @param enemyTanks
     * @return
     */
    public boolean isAbeAttacked(TMap view, Position currPos, List<Tank> enemyTanks){

        for(Tank enemyTank : enemyTanks){
            Position enemyPos = mapService.getPosition(view,enemyTank.getTId());
            if(enemyPos == null || currPos == null)return false;
            int diff = Math.abs(enemyPos.getRowIndex()-currPos.getRowIndex())+Math.abs(enemyPos.getColIndex()-currPos.getColIndex());
            boolean able = (currPos.getRowIndex()==enemyPos.getRowIndex() || currPos.getColIndex() == enemyPos.getColIndex()) && diff<=enemyTank.getShecheng();
            if (able){
                return  true;
            }
        }

        return false;
    }

    /**
     * 构建攻击前的站位
     * @param params
     * @param attackTarget
     * @param currTank
     * @param currPos
     */
    public Position buildPrepareAttackPos(GlobalValues params, TankPosition attackTarget, Tank currTank, Position currPos) {
        Position enemyPos = attackTarget.getPos();

        List<Position> preAttackPosList = new ArrayList<>();
        for(int i=1;i<=currTank.getShecheng();i++){
            preAttackPosList.addAll(Arrays.asList(
                    new Position(enemyPos.getRowIndex()-i,enemyPos.getColIndex()),
                    new Position(enemyPos.getRowIndex(),enemyPos.getColIndex()+i),
                    new Position(enemyPos.getRowIndex()+i,enemyPos.getColIndex()),
                    new Position(enemyPos.getRowIndex(),enemyPos.getColIndex()-i)
            ));
        }

        List<Position> attackPos = this.enemyAttackPosList(params);

        //过淲无效位置
        preAttackPosList = preAttackPosList.stream().filter(item->{
            if(!mapService.isPosition(params.getView(),item)){
                return false;
            }
            String mId = params.getView().get(item.getRowIndex(),item.getColIndex());
            return !mapService.isBlock(mId) && !attackPos.contains(item);
        }).collect(Collectors.toList());

        if (preAttackPosList.isEmpty())return null;

        TMap view = mapService.copyAttackLine(params);

        DiffPosition preFinalDiffPos = preAttackPosList.stream().map(item->{
            AStar aStar = new AStar(view);
            int diff = aStar.countStep(currPos,item);
            return DiffPosition.builder().pos(item).diff(diff).build();
        }).min(Comparator.comparing(DiffPosition::getDiff)).orElse(null);

        return preFinalDiffPos == null?null:preFinalDiffPos.getPos();
    }

    /**
     * 返回可攻击敌方坐标
     *
     * @param params
     * @param currTank
     * @param currPos
     * @return
     */
    public TankPosition ableAttackTop(GlobalValues params, Tank currTank, Position currPos) {

        List<Position> ableFirePos = new ArrayList<>();
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,-1,0));
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,0,1));
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,1,0));
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,0,-1));

        List<TankPosition> ableFireTankPos = ableFirePos.stream().map(item->{
            String mId = params.getView().get(item.getRowIndex(),item.getColIndex());
            Tank tank = params.getEnemyTeam().getTanks().stream().filter(item2->item2.getTId().equals(mId)).findFirst().orElse(null);
            return TankPosition.builder().tank(tank).pos(item).tId(mId).build();
        }).filter(item->item.getTank()!=null).collect(Collectors.toList());

        //返回优先攻击对象
        //第一优先-一枪打死
        List<TankPosition> oneAttacks = new ArrayList<>();
        for(TankPosition tp : ableFireTankPos){
            if(tp.getTank().getShengyushengming() <= currTank.getGongji()){
                oneAttacks.add(tp);
            }
        }

        if(!oneAttacks.isEmpty()){
            for (String tId : tIdSorted){
                for (TankPosition tp : oneAttacks){
                    if (tId.equals(tp.getTId())){
                        return tp;
                    }
                }
            }
        }else{
            //第二优先-攻击顺序
            for (String tId : tIdSorted){
                for (TankPosition tp : ableFireTankPos){
                    if (tId.equals(tp.getTId())){
                        return tp;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 攻击
     * @param view
     * @param currTank
     * @param action
     * @param currPos
     * @param targetPos
     * @param targetTank
     */
    public void attackTank(TMap view, Tank currTank, Action action, Position currPos, Position targetPos, Tank targetTank) {
        DirectionEnum direct = DirectionEnum.UP;
        if(currPos.getRowIndex()<targetPos.getRowIndex()){
            direct = DirectionEnum.DOWN;
        }else if(currPos.getColIndex()<targetPos.getColIndex()){
            direct = DirectionEnum.RIGHT;
        }else if(currPos.getColIndex()>targetPos.getColIndex()){
            direct = DirectionEnum.LEFT;
        }
        int diff = Math.abs(targetPos.getRowIndex()-currPos.getRowIndex())+Math.abs(targetPos.getColIndex()-currPos.getColIndex());
        action.setDirection(direct);
        action.setType(ActionTypeEnum.FIRE);
        action.setLength(diff);
        action.setUsed(true);

        //打死BOSS后地图上移除
        if(targetTank.getShengyushengming()-currTank.getGongji() <=0){
            view.getMap().get(targetPos.getRowIndex()).set(targetPos.getColIndex(),MapEnum.M1.name());
        }

    }

    /**
     * 计算一个准备攻击的对象
     * @param params
     */
    public TankPosition prepareAttackTarget(GlobalValues params) {
        TMap view = params.getView();
        //发现BOSS
        List<Position> positions = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,MapEnum.A1);

        if(!positions.isEmpty()){
            return TankPosition.builder().pos(positions.get(0)).tank(params.getBossTeam().getTanks().get(0)).tId(params.getBossTeam().getTanks().get(0).getTId()).build();
        }

        DiffPosition attackTarget = tIdSorted.stream().filter(item->params.getEnemyTeamTId().contains(item)).map(item->{
            Position pos = mapService.getPosition(params.getView(),item);
            Tank tank = params.getEnemyTeam().getTanks().stream().filter(item2->item2.getTId().equals(item)).findFirst().orElse(null);
            return DiffPosition.builder().pos(pos).tank(tank).build();
        }).filter(item->{
            if (item.getPos() == null)return false;
            //路程或射程可达的坦克
            List<Tank> tanks = params.getCurrTeam().getTanks().stream().filter(item2->item2.getShengyushengming()>0).collect(Collectors.toList());

            Tank firstCurrTank = tanks.get((int) Math.floor(Math.random() * tanks.size()));
            Position firstCurrPos = mapService.getPosition(params.getView(),firstCurrTank.getTId());
            AStar aStar = new AStar(mapService.copyAttackLine(params,item.getTank().getTId()));
            aStar.appendBlockList(params.getCurrTeamTId());
            aStar.appendBlockList(item.getTank().getTId());
            aStar.findPath(firstCurrPos,item.getPos());
            return firstCurrPos != null && firstCurrPos.getParent()!=null;
        }).findFirst().orElse(null);

        if (attackTarget == null) return null;

        return TankPosition.builder().pos(attackTarget.getPos()).tank(attackTarget.getTank()).tId(attackTarget.getTank().getTId()).build();
    }



    /* -----------------------------BOSS---------------------------------*/
    public TankPosition buildAbleAttackBossPos(GlobalValues params, Tank currTank, Position currPos) {
        TMap view = params.getView();

        List<Position> bossPosList = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,MapEnum.A1);

        if (bossPosList.isEmpty())return null;
        Position targetPos = bossPosList.get(0);

        List<Position> ableFirePos = new ArrayList<>();
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,-1,0));
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,0,1));
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,1,0));
        ableFirePos.addAll(buildAbleFirePos(params,currTank.getShecheng(),currPos,0,-1));

        //未在弹道内
        if (!ableFirePos.contains(targetPos))return null;

        return TankPosition.builder().tank(params.getBossTeam().getTanks().get(0)).pos(targetPos).build();

    }
}
