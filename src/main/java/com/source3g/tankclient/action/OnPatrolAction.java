package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡逻
 */
@Component
public class OnPatrolAction extends AbstractActiion<GlobalValues,List<Action>> {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        if(params.getSessionData().getLeader().getDirection() == null){
            Position leaderPos = params.getSessionData().getLeader().getPos();
            int sourceRowIndex = leaderPos.getRowIndex();
            int sourceColIndex = leaderPos.getColIndex();
            Position targetPos = buildCenterBlank(params);
            AStar aStar = new AStar(params.getView());
            aStar.findPath(leaderPos,targetPos);

            if(leaderPos != null && leaderPos.getParent() != null){
                Position nextPos = leaderPos.getParent();
                moveService.buildLeaderAction(params.getSessionData().getLeader(),leaderPos,nextPos);

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
                    mapService.buildPosition(params.getView(),itemTagetPos);
                    if(itemTagetPos == null || mapService.isBlock(params.getView().getMap().get(itemTagetPos.getRowIndex()).get(itemTagetPos.getColIndex()))){
                        continue;
                    }
                    Position itemCurrPos = mapService.getPosition(params.getView(),action.getTId());
                    aStar.clear();
                    String targetMId = params.getView().get(itemTagetPos.getRowIndex(),itemTagetPos.getColIndex());
                    aStar.appendBlockList(MapEnum.valueOf(targetMId));
                    Integer step = aStar.countStep(itemCurrPos,itemTagetPos);

                    if(step <= 1){
                        continue;
                    }
                    flag = false;
                    break;
                }

                if(!flag){
                    params.getSessionData().getLeader().getPos().setRowIndex(sourceRowIndex);
                    params.getSessionData().getLeader().getPos().setColIndex(sourceColIndex);
                }
            }

        }

        for(Action action : actions){
            if(action.isUsed())continue;

            int suffix = Integer.valueOf(action.getTId().substring(1,2));
            Position currPos = mapService.getPosition(params.getView(),action.getTId());
            Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

            Position targetPos = null;
            switch (suffix){
                case 1:targetPos = byLeader1(params);break;
                case 2:targetPos = byLeader2(params);break;
                case 3:targetPos = params.getSessionData().isMass()?byLeader3(params):quick(params,tank,currPos,1);break;
                case 4:targetPos = params.getSessionData().isMass()?byLeader4(params):quick(params,tank,currPos,-1);break;
                case 5:targetPos = byLeader5(params);break;
            }

            if(targetPos == null){
                targetPos = currPos;
            }

            AStar aStar = new AStar(params.getView());
            mapService.buildPosition(params.getView(),targetPos);
            String targetMId = params.getView().get(targetPos.getRowIndex(),targetPos.getColIndex());
            aStar.appendBlockList(MapEnum.valueOf(targetMId));

            //获取最大行进路线
            Position nextPos = aStar.findPath(currPos,targetPos);
            nextPos = mapService.getMaxNext(tank,currPos,nextPos);

            //没有下一步就不动
            if(nextPos == null){
                nextPos = currPos;
            }

            params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
            params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

            //根据坐标，计算方位和步长
            moveService.buildAction(action,currPos,nextPos);
        }

        /*if(action.isUsed()){
            return NodeType.Failure;
        }

        int suffix = Integer.valueOf(action.getTId().substring(1,2));






        Position nextPos = null;
        switch (suffix){
            case 1:nextPos = quick(params,tank,currPos,1);break;
            case 2:nextPos = quick(params,tank,currPos,1);break;
            case 3:nextPos = quick(params,tank,currPos,1);break;
            case 4:nextPos = quick(params,tank,currPos,-1);break;
            case 5:nextPos = quick(params,tank,currPos,1);break;
        }

        //获取最大行进路线
        nextPos = mapService.getMaxNext(tank,currPos,nextPos);

        //允许下一步，替换地图
        if(nextPos != null){
            params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
            params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

            //根据坐标，计算方位和步长
            moveService.buildAction(action,currPos,nextPos);
        }*/

        return NodeType.Success;
    }

    private Position byLeader5(GlobalValues params) {
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

    private Position byLeader4(GlobalValues params) {
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

    private Position byLeader3(GlobalValues params) {
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

    private Position byLeader2(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        return new Position(rowIndex,colIndex);
    }

    private Position byLeader1(GlobalValues params) {
        Leader leader = params.getSessionData().getLeader();
        int rowIndex = leader.getPos().getRowIndex();
        int colIndex = leader.getPos().getColIndex();
        switch (leader.getDirection()){
            case UP:
                colIndex-=2;
                break;
            case RIGHT:
                rowIndex-=2;
                break;
            case DOWN:
                colIndex-=2;
                break;
            case LEFT:
                rowIndex-=2;
                break;
            case WAIT:
                return null;
        }
        return new Position(rowIndex,colIndex);
    }

    //获得中心的某个空白点
    private Position buildCenterBlank(GlobalValues params) {
        Position target = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        String mId = params.getView().getMap().get(target.getRowIndex()).get(target.getColIndex());
        if(!mapService.isBlock(mId)){
            List<Position> m3Pos = mapService.findByMapEnum(params.getView(),0,params.getView().getRowLen()-1,0,params.getView().getColLen()-1,MapEnum.M3);
            if(!m3Pos.isEmpty()){
                return m3Pos.get(0);
            }

            List<Position> m1Pos = mapService.findByMapEnum(params.getView(),params.getView().getRowLen()/4,params.getView().getRowLen()-params.getView().getRowLen()/4,params.getView().getColLen()/4,params.getView().getColLen()-params.getView().getColLen()/4,MapEnum.M1);
            if (!m1Pos.isEmpty()){
                return m1Pos.get(0);
            }
        }
        return null;
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
