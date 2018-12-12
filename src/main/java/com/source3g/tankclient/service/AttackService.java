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
@SuppressWarnings("Duplicates")
@Service
public class AttackService {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    //默认攻击顺序
    private final List<String> tIdSorted = Arrays.asList("A1","C1","B1","C3","B3","C4","B4","C5","B5","C2","B2");

    /**
     * 计算能攻击到某坐标的坦克
     * @param params
     * @param currPos
     * @param enemyTanks
     * @return
     */
    public List<DiffPosition> beAttacked(GlobalValues params,Position currPos,List<Tank> enemyTanks){

        List<DiffPosition> result = new ArrayList<>();
        if (currPos == null)return result;

        for(Tank enemyTank : enemyTanks){
            List<Position> attackPos = new ArrayList<>();


            Position enemyPos = mapService.getPosition(params.getView(),enemyTank.getTId());
            if(enemyPos == null)continue;
            attackPos.add(enemyPos);

            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,-1,0));
            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,0,1));
            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,1,0));
            attackPos.addAll(buildAbleFirePos(params,enemyTank.getShecheng(),enemyPos,0,-1));

            if(attackPos.contains(currPos)){
                result.add(DiffPosition.builder()
                        .pos(enemyPos)
                        .tank(enemyTank)
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
        for(Tank tank : params.getEnemyTeam().getTanks()){
            Position tankPos = mapService.getPosition(params.getView(),tank.getTId());
            if(tankPos == null)continue;
            attackPos.add(tankPos);

            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,-1,0));
            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,0,1));
            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,1,0));
            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,0,-1));

        }

        return attackPos;
    }

    /**
     * 队伍的弹道
     * @param params
     * @return
     */
    public List<Position> attackLinePosList(GlobalValues params,List<Tank> tanks){
        List<Position> attackPos = new ArrayList<>();
        for(Tank tank : tanks){
            Position tankPos = mapService.getPosition(params.getView(),tank.getTId());
            if(tankPos == null)continue;
            attackPos.add(tankPos);

            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,-1,0));
            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,0,1));
            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,1,0));
            attackPos.addAll(buildAbleFirePos(params,tank.getShecheng(),tankPos,0,-1));

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
            if (params.getEnemyTeamTId().contains(mId) || !mapService.isBlock(mId) || params.getCurrTeamTId().contains(mId)){
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
     * 计算通过攻击到某个位置的空闲坐标
     * @param params
     * @param actionLen
     * @param currPos
     * @param diffR
     * @param diffC
     * @return
     */
    public List<Position> buildAbleAttackPos(GlobalValues params,int actionLen,Position currPos,int diffR,int diffC){
        List<Position> result = new ArrayList<>();
        for(int i=1;i<=actionLen;i++){
            Position itemPos = new Position(currPos.getRowIndex()+i*diffR,currPos.getColIndex()+i*diffC);

            if(!mapService.isPosition(params.getView(),itemPos)){
                break;
            }

            String mId = params.getView().get(itemPos.getRowIndex(),itemPos.getColIndex());
            if(mapService.isBlockTrue(mId)){
                break;
            }
            if(params.getBossTeam().getTanks().get(0).getTId().equals(mId) || params.getEnemyTeamTId().contains(mId) || params.getCurrTeamTId().contains(mId)){
                continue;
            }
            result.add(itemPos);
        }
        return result;
    }

    /**
     * 构建攻击前的站位
     * @param params
     * @param attackTarget
     * @param currTank
     * @param currPos
     */
    public Position buildPrepareAttackPos(GlobalValues params, TankPosition attackTarget, Tank currTank, Position currPos) {

        //如果有已方一个以上的其他队友锁定了这个敌人,并且当前坦克不是用来卡位的，那么攻击前的站位应该是可攻击位置
        List<DiffPosition> diffPos = this.beAttacked(params,attackTarget.getPos(),params.getCurrTeam().getTanks());
        diffPos = diffPos.stream().filter(item->!item.getTank().getTId().equals(currTank.getTId())).collect(Collectors.toList());
        if (diffPos.size() > 0){

            //可以攻击到目录的站位，优先从远到近
            List<Position> ableAttackPos = new ArrayList<>();
            ableAttackPos.addAll(buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),-1,0));
            ableAttackPos.addAll(buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),0,1));
            ableAttackPos.addAll(buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),1,0));
            ableAttackPos.addAll(buildAbleAttackPos(params,currTank.getShecheng(),attackTarget.getPos(),0,-1));

            TMap view = mapService.copyAttackLine(params,attackTarget.getTank().getTId());

            DiffPosition finalPos = ableAttackPos.stream().map(item->{
                AStar aStar = new AStar(view);
                int diff = aStar.countStep(currPos,item);
                return DiffPosition.builder().pos(item).diff(diff).build();
            }).min(Comparator.comparing(DiffPosition::getDiff)).orElse(null);
            return finalPos == null?null:finalPos.getPos();
        }

        Position enemyPos = attackTarget.getPos();

        TMap view = mapService.copyAttackLine(params);

        List<Position> preAttackPosList = mapService.findByMapEnum(view,enemyPos.getRowIndex()-1,enemyPos.getRowIndex()+1,enemyPos.getColIndex()-1,enemyPos.getColIndex()+1,MapEnum.M1,MapEnum.M2,MapEnum.M3);

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

        DiffPosition preFinalDiffPos = preAttackPosList.stream().map(item->{
            AStar aStar = new AStar(view);
            aStar.appendBlockList(currTank.getTId());
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
    public void attackTank(GlobalValues params,TMap view, Tank currTank, Action action, Position currPos, Position targetPos, Tank targetTank) {
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

        //生命值减血
        if (MapEnum.A1.name().equals(targetTank.getTId())){
            Tank bossTank = params.getBossTeam().getTanks().stream().filter(item->item.getTId().equals(targetTank.getTId())).findFirst().get();
            bossTank.setShengyushengming(bossTank.getShengyushengming()-currTank.getGongji());
        }else{
            Tank enemyTank = params.getEnemyTeam().getTanks().stream().filter(item->item.getTId().equals(targetTank.getTId())).findFirst().get();
            enemyTank.setShengyushengming(enemyTank.getShengyushengming()-currTank.getGongji());
        }

        //打死后地图上移除
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

        List<TankPosition> attackTarget = tIdSorted.stream().filter(item->params.getEnemyTeamTId().contains(item)).map(item->{
            Position pos = mapService.getPosition(params.getView(),item);
            Tank tank = params.getEnemyTeam().getTanks().stream().filter(item2->item2.getTId().equals(item)).findFirst().orElse(null);
            return TankPosition.builder().pos(pos).tank(tank).build();
        }).filter(item->item.getPos() != null).collect(Collectors.toList());

        ////寻找无路可走的敌方坦克
       /* TankPosition targetTankPos = ableMoveZero(params,attackTarget);
        if (targetTankPos != null){
            return targetTankPos;
        }*/

        //寻找落单的-周围半径1以上的落单者，倒序
        return buildMassPos(params,attackTarget);
    }

    //寻找无路可走的敌方坦克
    private TankPosition ableMoveZero(GlobalValues params, List<TankPosition> attackTarget) {
        List<Position> attackPos = this.attackLinePosList(params,params.getCurrTeam().getTanks());
        return attackTarget.stream().filter(item->{

            List<Position> ableMovePos = moveService.buildAbleMovePos(params,item.getTank(),item.getPos());
            ableMovePos.removeAll(attackPos);
            if (ableMovePos.size() <=1){
                return true;
            }
            return false;
        }).findFirst().orElse(null);
    }


    //寻找落单的-周围半径1以上的落单者，倒序
    private TankPosition buildMassPos(GlobalValues params, List<TankPosition> enemyPosList) {



        int scope = params.getView().getRowLen()/2;
        MapEnum[] enums = params.getEnemyTeamTId().stream().map(MapEnum::valueOf).toArray(MapEnum[]::new);

        TankPosition target = null;
        for(int i=scope;i>=0;i--){
            for(TankPosition currPos : enemyPosList){
                int startR = currPos.getPos().getRowIndex()-i;
                int endR = currPos.getPos().getRowIndex()+i;
                int startC = currPos.getPos().getColIndex()-i;
                int endC = currPos.getPos().getColIndex()+i;

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


    /* -----------------------------BOSS---------------------------------*/
    public TankPosition buildAbleAttackBossPos(GlobalValues params, Tank currTank, Position currPos) {
        TMap view = params.getView();

        List<Position> bossPosList = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,MapEnum.A1);

        if (bossPosList.isEmpty())return null;
        Position targetPos = bossPosList.get(0);

        List<Position> ableFirePos = new ArrayList<>();
        ableFirePos.addAll(buildBossAbleFirePos(params,currTank.getShecheng(),currPos,-1,0));
        ableFirePos.addAll(buildBossAbleFirePos(params,currTank.getShecheng(),currPos,0,1));
        ableFirePos.addAll(buildBossAbleFirePos(params,currTank.getShecheng(),currPos,1,0));
        ableFirePos.addAll(buildBossAbleFirePos(params,currTank.getShecheng(),currPos,0,-1));

        //未在弹道内
        if (!ableFirePos.contains(targetPos))return null;

        return TankPosition.builder().tank(params.getBossTeam().getTanks().get(0)).pos(targetPos).build();

    }

    private List<Position> buildBossAbleFirePos(GlobalValues params, int actionLen, Position currPos, int diffR, int diffC){
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
}
