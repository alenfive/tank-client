package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 移动
 */
@SuppressWarnings("Duplicates")
@Service
public class MoveService {

    @Autowired
    private MapService mapService;

    @Autowired
    private AttackService attackService;


    public void buildAction(GlobalValues params,Action action, Position currPos, Position nextPos) {
        params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
        params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

        int rowDiff = nextPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = nextPos.getColIndex()-currPos.getColIndex();
        action.setDirection(rowDiff>0?DirectionEnum.DOWN:rowDiff<0?DirectionEnum.UP:colDiff>0?DirectionEnum.RIGHT:colDiff<0?DirectionEnum.LEFT:DirectionEnum.WAIT);
        action.setLength(Math.abs(rowDiff!=0?rowDiff:colDiff));
        action.setType(ActionTypeEnum.MOVE);
        action.setUsed(true);
        action.setSort(params.getSortNo());
        params.setSortNo(params.getSortNo()+1);


        //mapService.log(params.getView());
    }


    private Position buildConflict(Position target, GlobalValues params, Tank tank) {
        mapService.buildPosition(params,target);
        String mId = params.getView().getMap().get(target.getRowIndex()).get(target.getColIndex());
        if(!mapService.isBlock(mId)){
            return target;
        }

        List<Position> positions = mapService.findByMapEnum(params.getView(),target.getRowIndex()-1,target.getRowIndex()+1,target.getColIndex()-1,target.getColIndex()+1,MapEnum.M1);
        if(positions.isEmpty())
            return null;
        return positions.get(0);
    }

    //队友位置
    public Position buildCurrTeamPos(GlobalValues params, Tank tank) {
        Tank tank1 = params.getCurrTeam().getTanks().stream().filter(item->!item.getTId().equals(tank.getTId()) && item.getShengyushengming()>0).findFirst().orElse(null);
        if (tank1 == null)return null;
        return mapService.getPosition(params.getView(),tank1.getTId());
    }

    /**
     * 是否在敌方弹道上，构建逃离坐标
     * @param params
     * @param currTank
     * @param currPos
     * @return
     */
    public Position buildLeave(GlobalValues params, Tank currTank, Position currPos) {

        List<Position> attackPos = attackService.enemyAttackPosList(params);

        //未在敌方射程内
        if (!attackPos.contains(currPos)){
            return null;
        }

        Integer currShengmin = params.getCurrTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();
        Integer enemyShengmin = params.getEnemyTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();

        //大于对方三倍，直接上，不退
        if(currShengmin < enemyShengmin*3 && params.getEnemyTeam().getGlod() == 0){
            return null;
        }

        List<DiffPosition> beAttackeds = attackService.beAttacked(params,currPos,params.getEnemyTeam().getTanks()).stream().collect(Collectors.toList());

        if(beAttackeds.isEmpty())return null;

        if(beAttackeds.size() == 1){
            int diffCurr = (beAttackeds.get(0).getTank().getShengyushengming()+currTank.getGongji()-1)/currTank.getGongji();
            int diffEnemy = (currTank.getShengyushengming()+beAttackeds.get(0).getTank().getGongji()-1)/beAttackeds.get(0).getTank().getGongji();
            List<DiffPosition> diffPos = attackService.beAttacked(params,beAttackeds.get(0).getPos(),params.getCurrTeam().getTanks());

            if(diffPos.size() == 1 && diffCurr <= diffEnemy){ //1V1
                return null;
            }else if(diffPos.size() > 1){ //被我方二个及以上坦克锁定不撤退
                return null;
            }
            //将要被我方二个及以上锁定时不撤退

        }

        return buildLeavePos(params,currTank,currPos);

    }

    public List<Position> buildAbleMovePos(GlobalValues params,Tank currTank,Position currPos){
        List<Position> movePos = new ArrayList<>();
        movePos.add(currPos);
        //top
        movePos.addAll(buildAbleMovePos(params.getView(),currTank.getYidong(),currPos,-1,0));
        //right
        movePos.addAll(buildAbleMovePos(params.getView(),currTank.getYidong(),currPos,0,1));
        //bottom
        movePos.addAll(buildAbleMovePos(params.getView(),currTank.getYidong(),currPos,1,0));
        //left
        movePos.addAll(buildAbleMovePos(params.getView(),currTank.getYidong(),currPos,0,-1));
        return movePos;
    }

    private Position buildLeavePos(GlobalValues params,Tank currTank,Position currPos){

        //搜索逃离点
        List<Position> ableMovePos = buildAbleMovePos(params,currTank,currPos);

        if(ableMovePos.isEmpty()){
            return null;
        }

        List<DiffPosition> leavePos = ableMovePos.stream().map(item->{
            List<DiffPosition> diffAll = attackService.beAttacked(params,item,params.getEnemyTeam().getTanks());
            int diff = diffAll.stream().mapToInt(item2->item2.getTank().getGongji()).sum();
            return DiffPosition.builder().diff(diff).pos(item).build();
        }).sorted(Comparator.comparing(DiffPosition::getDiff)).collect(Collectors.toList());

        //这个坐标不会承受伤害，直接返回
        if(leavePos.get(0).getDiff() == 0){
            return leavePos.get(0).getPos();
        }

        //找一个可以攻击的位置
        for(DiffPosition diffPos : leavePos){
            TankPosition tp = attackService.ableAttackTop(params,currTank,diffPos.getPos());
            if (tp != null){
                return diffPos.getPos();
            }
        }

        return null;
    }

    /**
     * 计算某坐标范围可移动的点
     * @param actionLen
     * @param currPos
     * @param diffR
     * @param diffC
     * @return
     */
    private List<Position> buildAbleMovePos(TMap view, int actionLen, Position currPos, int diffR, int diffC){
        List<Position> movePos = new ArrayList<>();
        for(int i=1;i<=actionLen;i++){
            Position itemPos = new Position(currPos.getRowIndex()+i*diffR,currPos.getColIndex()+i*diffC);
            if (!mapService.isPosition(view,itemPos)){
                break;
            }
            String mId = view.get(itemPos.getRowIndex(),itemPos.getColIndex());
            if (mapService.isBlock(mId)){
                break;
            }
            movePos.add(itemPos);
        }

        return movePos;
    }

    public void buildMove(GlobalValues params, Action action,Tank currTank,Position currPos,Position targetPos,boolean attackLine) {
        if(action.isUsed())return;

        if (targetPos == null)return;

        //找个空白点用于定位
        mapService.buildBlank(params,currPos,targetPos);

        //创建一个新视图避免走入弹路
        TMap view = attackLine?mapService.copyAttackLine(params):params.getView();
        AStar aStar = new AStar(view);

        //获取最大行进路线
        Position nextPos = aStar.findPath(currPos,targetPos);

        //没有下一步就不动
        if(nextPos == null || nextPos.getParent() == null)return;

        nextPos = mapService.getMaxNext(currTank,currPos,nextPos);

        //根据坐标，计算方位和步长
        this.buildAction(params,action,currPos,nextPos);

        /*if(currTank.getYidong() == 1){
            //创建一个新视图避免走入弹路
            TMap view = attackLine?mapService.copyAttackLine(params):params.getView();
            AStar aStar = new AStar(view);

            //获取最大行进路线
            Position nextPos = aStar.findPath(currPos,targetPos);

            //没有下一步就不动
            if(nextPos == null || nextPos.getParent() == null)return;

            //根据坐标，计算方位和步长
            this.buildAction(params,action,currPos,nextPos.getParent());
            return;
        }else if(currTank.getYidong() == 2){
            AStar aStar = new AStar(params.getView());
            //获取最大行进路线
            Position nextPos = aStar.findPath(currPos,targetPos);

            //没有下一步就不动
            if(nextPos == null || nextPos.getParent() == null)return;

            if(attackLine){
                List<Position> attackLinePos = attackService.enemyAttackPosList(params);
                Position next1 = nextPos.getParent();
                Position next2 = nextPos.getParent().getParent();
                Position finalPos = next1;
                if(next2 != null && (next1.getRowIndex() == next2.getRowIndex() || next1.getColIndex() == next2.getColIndex()) && !attackLinePos.contains(next2)){
                    finalPos = next2;
                }

                if (attackLinePos.contains(finalPos)){
                    //创建一个新视图避免走入弹路
                    TMap view = mapService.copyAttackLine(params);
                    aStar = new AStar(view);
                    nextPos = aStar.findPath(currPos,targetPos);
                    //没有下一步就不动
                    if(nextPos == null || nextPos.getParent() == null)return;
                    finalPos = mapService.getMaxNext(currTank,currPos,nextPos);
                }

                //根据坐标，计算方位和步长
                this.buildAction(params,action,currPos,finalPos);
                return;
            }else{
                nextPos = mapService.getMaxNext(currTank,currPos,nextPos);
                //根据坐标，计算方位和步长
                this.buildAction(params,action,currPos,nextPos);
                return;
            }

        }*/

    }


    /**
     * 移动快的
     * @param params
     * @param tank
     */
    public Position scanMapNextPosition(GlobalValues params, Tank tank, int scope) {
        TMap view = mapService.copyAttackLine(params);
        List<Position> regions = buildRegions(tank,view,scope);
        Position targetPos = null;
        for(Position position : regions){
            Position finalPosition = findM3Position(position,view,tank);
            if(finalPosition != null){
                targetPos = finalPosition;
                break;
            }
        }

        //标记点完以后搜索剩下的
        if(targetPos == null){
            int centerRow = params.getView().getRowLen()/2;
            int startRow = scope>0?centerRow:0;
            int endRow = scope>0?params.getView().getRowLen()-1:centerRow-1;

            List<Position> m3Pos = mapService.findByMapEnum(view,startRow,endRow,0,params.getView().getColLen()-1,MapEnum.M3);
            if(!m3Pos.isEmpty()){
                return m3Pos.get(0);
            }
        }

        return targetPos;
    }

    /**
     * 返回区域内合适的坐标
     * @param position
     * @param view
     * @param tank
     * @return
     */
    private Position findM3Position(Position position, TMap view, Tank tank) {

        if(MapEnum.M3.name().equals(view.getMap().get(position.getRowIndex()).get(position.getColIndex()))){
            return position;
        }
        int startR = position.getRowIndex()-tank.getShiye();
        int endR = position.getRowIndex()+tank.getShiye();
        int startC = position.getColIndex()-tank.getShiye();
        int endC = position.getColIndex()+tank.getShiye();

        startR = startR<0?0:startR;
        endR = endR>=view.getRowLen()?view.getRowLen()-1:endR;
        startC = startC<0?0:startC;
        endC = endC>=view.getColLen()?view.getColLen()-1:endC;
        boolean flag = false;
        Position resultPos = null;
        for(int i=startR;i<=endR;i++){
            for(int k=startC;k<=endC;k++){
                if(MapEnum.M3.name().equals(view.getMap().get(i).get(k))){
                    resultPos = new Position(i,k);
                    flag = true;
                    break;
                }
            }
            if(flag)break;
        }

        if(flag){
            if(MapEnum.M1.name().equals(view.getMap().get(position.getRowIndex()).get(position.getColIndex()))||
                    MapEnum.M2.name().equals(view.getMap().get(position.getRowIndex()).get(position.getColIndex()))){
                return position;
            }else{
                return resultPos;
            }
        }

        return null;
    }

    /**
     * 划分地图区域
     * @param view
     * @param scope
     * @return
     */
    private List<Position> buildRegions(Tank tank,TMap view, int scope) {
        int centerRow = view.getRowLen()/2;
        int centerCol = view.getColLen()/2;

        //划分上下部分
        int startRowIndex = scope>0?centerRow:0;
        int endRowIndex = scope>0?view.getRowLen()-1:centerRow-1;

        //扩散步长
        int stepSize = tank.getShiye()*2+1;

        int stepCount = (endRowIndex-startRowIndex+tank.getShiye()*4+1)/stepSize;
        List<Position> regions = new ArrayList<>();

        int rowIndex = centerRow + tank.getShiye()*scope;
        int colIndex = centerCol;

        regions.add(new Position(rowIndex,colIndex));

        int direct = 0;
        int step = tank.getShiye()*2+1;
        for(int i=1;i<stepCount;i++){

            if(direct % 2 == 0){
                //left
                regions.addAll(buildLeft(i,step,rowIndex,colIndex,-1,scope));
                //down
                regions.addAll(buildDown(i,step,rowIndex,colIndex,scope));
                //right
                regions.addAll(revert(buildLeft(i,step,rowIndex,colIndex,1,scope)));
            }else{
                //right
                regions.addAll(buildLeft(i,step,rowIndex,colIndex,1,scope));
                //down
                regions.addAll(revert(buildDown(i,step,rowIndex,colIndex,scope)));
                //left
                regions.addAll(revert(buildLeft(i,step,rowIndex,colIndex,-1,scope)));
            }

            direct ++;
        }

        //修正
        regions.forEach(item->{
            buildPosition(item,view,tank);
        });
        return regions;
    }

    private List<Position> revert(List<Position> positions) {
        List<Position> revertList = new ArrayList<>();
        for(int i=positions.size()-1;i>=0;i--){
            revertList.add(positions.get(i));
        }
        return revertList;
    }


    private List<Position> buildDown(int i, int step,int rowIndex,int colIndex,int topDown) {
        int startRow = i*topDown;
        int endRow = i*topDown;
        int startCol = i*-1;
        int endCol = i;

        List<Position> positions = new ArrayList<>();
        for(int r=startRow;r<=endRow;r++){
            for(int c = startCol;c<=endCol;c++){
                positions.add(new Position(rowIndex+r*step,colIndex+c*step));
            }
        }
        return positions;
    }

    //左边
    private List<Position> buildLeft(int i,int step,int rowIndex,int colIndex,int leftRight,int topDown) {
        int startRow = 0;
        int endRow = i;
        int startCol = i;
        int endCol = i;

        List<Position> positions = new ArrayList<>();
        for(int r=startRow;r<endRow;r++){
            for(int c = startCol;c<=endCol;c++){
                positions.add(new Position(rowIndex+r*topDown*step,colIndex+c*leftRight*step));
            }
        }
        return positions;
    }

    private void buildPosition(Position position, TMap view, Tank tank){
        if(position.getRowIndex()<tank.getShiye()){
            position.setRowIndex(tank.getShiye());
        }
        if(position.getRowIndex()>=view.getRowLen()-1-tank.getShiye()){
            position.setRowIndex(view.getRowLen()-1-tank.getShiye());
        }
        if(position.getColIndex()<tank.getShiye()){
            position.setColIndex(tank.getShiye());
        }
        if(position.getColIndex()>=view.getColLen()-1-tank.getShiye()){
            position.setColIndex(view.getColLen()-1-tank.getShiye());
        }
    }
}
