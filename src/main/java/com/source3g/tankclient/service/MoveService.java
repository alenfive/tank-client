package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 移动
 */
@Service
public class MoveService {

    @Autowired
    private MapService mapService;

    public void buildLeaderAction(Leader leader, Position currPos, Position nextPos) {
        int rowDiff = nextPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = nextPos.getColIndex()-currPos.getColIndex();
        leader.setDirection(rowDiff>0?DirectionEnum.DOWN:rowDiff<0?DirectionEnum.UP:colDiff>0?DirectionEnum.RIGHT:colDiff<0?DirectionEnum.LEFT:DirectionEnum.WAIT);
        leader.setPos(new Position(nextPos.getRowIndex(),nextPos.getColIndex()));
    }

    public void buildAction(GlobalValues params,Action action, Position currPos, Position nextPos) {
        int rowDiff = nextPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = nextPos.getColIndex()-currPos.getColIndex();
        action.setDirection(rowDiff>0?DirectionEnum.DOWN:rowDiff<0?DirectionEnum.UP:colDiff>0?DirectionEnum.RIGHT:colDiff<0?DirectionEnum.LEFT:DirectionEnum.WAIT);
        action.setLength(Math.abs(rowDiff!=0?rowDiff:colDiff));
        action.setType(ActionTypeEnum.MOVE);
        action.setUsed(true);
        action.setSort(params.getSortNo());
        params.setSortNo(params.getSortNo()+1);

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

    public void buildLeader(GlobalValues params, List<Action> actions, Position targetPos) {

        mapService.buildBlank(params,targetPos);
        Position leaderPos = params.getSessionData().getLeader().getPos();
        int sourceRowIndex = leaderPos.getRowIndex();
        int sourceColIndex = leaderPos.getColIndex();

        AStar aStar = new AStar(params.getView());
        aStar.appendBlockList(params.getCurrTeamTId());
        aStar.findPath(leaderPos,targetPos);

        if(leaderPos == null || leaderPos.getParent() == null){
            params.getSessionData().getLeader().setDirection(DirectionEnum.UP);
            return;
        }

        Position nextPos = leaderPos.getParent();
        String mId = params.getView().getMap().get(nextPos.getRowIndex()).get(nextPos.getColIndex());
        this.buildLeaderAction(params.getSessionData().getLeader(),leaderPos,nextPos);

        //前进的路上有自己家的坦克，让他们先走
        if(params.getCurrTeamTId().contains(mId)){
            Action action = actions.stream().filter(item->item.getTId().equals(mId)).findFirst().orElse(null);
            this.buildMove(params,action);
        }


        //判断移动后队友是否能跟上，不能跟上就不动
        boolean flag = true;
        for(Action action : actions){
            int suffix = Integer.valueOf(action.getTId().substring(1,2));

            Position itemTagetPos = null;
            switch (suffix){
                case 1:itemTagetPos = byLeader1(params);break;
                case 2:itemTagetPos = byLeader2(params);break;
                case 3:itemTagetPos = params.getSessionData().isMass()?byLeader3(params):null;break;
                case 4:itemTagetPos = params.getSessionData().isMass()?byLeader4(params):null;break;
                case 5:itemTagetPos = byLeader5(params);break;
            }
            mapService.buildPosition(params,itemTagetPos);
            if(itemTagetPos == null || mapService.isBlock(params.getView().getMap().get(itemTagetPos.getRowIndex()).get(itemTagetPos.getColIndex()))){
                continue;
            }
            Position itemCurrPos = mapService.getPosition(params.getView(),action.getTId());

            if(itemCurrPos == null){
                continue;
            }

            aStar.clear();
            String targetMId = params.getView().get(itemTagetPos.getRowIndex(),itemTagetPos.getColIndex());
            aStar.appendBlockList(targetMId);
            Integer step = aStar.countStep(itemCurrPos,itemTagetPos);

            if(step <= 1){
                continue;
            }

            action.setTarget(itemTagetPos);

            flag = false;
        }

        if(!flag){
            params.getSessionData().getLeader().getPos().setRowIndex(sourceRowIndex);
            params.getSessionData().getLeader().getPos().setColIndex(sourceColIndex);
        }
    }

    public void buildMove(GlobalValues params, Action action) {
        if(action.isUsed())return;

        int suffix = Integer.valueOf(action.getTId().substring(1,2));
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

        Position targetPos = null;
        switch (suffix){
            case 1:targetPos = action.getTarget()!=null?action.getTarget():this.byLeader1(params);break;
            case 2:targetPos = action.getTarget()!=null?action.getTarget():this.byLeader2(params);break;
            case 3:targetPos = action.getTarget()!=null?action.getTarget():params.getSessionData().isMass()?this.byLeader3(params):quick(params,tank,currPos,1);break;
            case 4:targetPos = action.getTarget()!=null?action.getTarget():params.getSessionData().isMass()?this.byLeader4(params):quick(params,tank,currPos,-1);break;
            case 5:targetPos = action.getTarget()!=null?action.getTarget():buildConflict(this.byLeader5(params),params,tank);break;
        }

        if (targetPos == null)return;

        //找个空白点用于定位
        mapService.buildBlank(params,targetPos);

        //创建一个新视图避免走入弹路
        TMap attackLineMap = mapService.copyAttackLine(params.getEnemyTeam().getTanks(),params.getView());
        AStar aStar = new AStar(attackLineMap);
        mapService.buildPosition(params,targetPos);

        //获取最大行进路线
        Position nextPos = aStar.findPath(currPos,targetPos);

        //没有下一步就不动
        if(nextPos == null || nextPos.getParent() == null)return;

        nextPos = mapService.getMaxNext(tank,currPos,nextPos);

        params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
        params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

        //根据坐标，计算方位和步长
        this.buildAction(params,action,currPos,nextPos);
    }

    public Position byLeader5(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        switch (leader.getDirection()){
            case UP:
                rowIndex+=1;
                break;
            case RIGHT:
                colIndex-=1;
                break;
            case DOWN:
                rowIndex-=1;
                break;
            case LEFT:
                colIndex+=1;
                break;
            case WAIT:
                return null;
        }
        return new Position(rowIndex,colIndex);
    }

    public Position byLeader4(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        switch (leader.getDirection()){
            case UP:
                colIndex+=2;
                rowIndex-=1;
                break;
            case RIGHT:
                rowIndex+=2;
                colIndex+=1;
                break;
            case DOWN:
                colIndex+=2;
                rowIndex+=1;
                break;
            case LEFT:
                rowIndex+=2;
                colIndex-=1;
                break;
            case WAIT:
                return null;
        }
        return new Position(rowIndex,colIndex);
    }

    public Position byLeader3(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        switch (leader.getDirection()){
            case UP:
                colIndex-=2;
                rowIndex-=1;
                break;
            case RIGHT:
                rowIndex-=2;
                colIndex+=1;
                break;
            case DOWN:
                colIndex-=2;
                rowIndex+=1;
                break;
            case LEFT:
                rowIndex-=2;
                colIndex-=1;
                break;
            case WAIT:
                return null;
        }
        return new Position(rowIndex,colIndex);
    }

    public Position byLeader2(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        return new Position(rowIndex,colIndex);
    }

    public Position byLeader1(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        switch (leader.getDirection()){
            case UP:
                colIndex-=1;
                break;
            case RIGHT:
                rowIndex-=1;
                break;
            case DOWN:
                colIndex-=1;
                break;
            case LEFT:
                rowIndex-=1;
                break;
            case WAIT:
                return null;
        }
        return new Position(rowIndex,colIndex);
    }

    /**
     * 移动快的
     * @param params
     * @param tank
     */
    private Position quick(GlobalValues params, Tank tank,Position currPos,int scope) {

        List<Position> regions = buildRegions(tank,params.getView(),scope);

        Position targetPos = null;
        for(Position position : regions){
            Position finalPosition = findM3Position(position,params.getView(),tank);
            if(finalPosition != null){
                targetPos = finalPosition;
                break;
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

        mapService.flag(view,regions,"11");
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
