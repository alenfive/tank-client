package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.utils.AStar;
import com.source3g.tankclient.utils.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡逻
 */
@SuppressWarnings("Duplicates")
@Component
public class OnPatrolAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(GlobalValues params, Action action) {
        int suffix = Integer.valueOf(action.getTId().substring(1,2));


        Position currPos = MapUtils.getPosition(params.getView(),action.getTId());

        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

        Position nextPos = null;
        switch (suffix){
            case 1:nextPos = quick(params,tank,currPos,1);break;
            case 2:nextPos = quick(params,tank,currPos,1);break;
            case 3:nextPos = quick(params,tank,currPos,1);break;
            case 4:nextPos = quick(params,tank,currPos,-1);break;
            case 5:nextPos = quick(params,tank,currPos,-1);break;
        }

        //获取最大行进路线
        nextPos = mapService.getMaxNext(tank,currPos,nextPos);

        //允许下一步，替换地图
        if(nextPos != null){
            params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
            params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

            //根据坐标，计算方位和步长
            buildAction(action,currPos,nextPos);
        }

        return NodeType.Success;
    }

    private void buildAction(Action action, Position currPos, Position nextPos) {
        int rowDiff = nextPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = nextPos.getColIndex()-currPos.getColIndex();
        action.setDirection(rowDiff>0?DirectionEnum.DOWN:rowDiff<0?DirectionEnum.UP:colDiff>0?DirectionEnum.RIGHT:colDiff<0?DirectionEnum.LEFT:DirectionEnum.WAIT);
        action.setLength(Math.abs(rowDiff!=0?rowDiff:colDiff));
        action.setType(ActionTypeEnum.MOVE);
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

        //无M3区域
        if(targetPos == null){
            return null;
        }

        AStar aStar = new AStar(params.getView());

        return aStar.findPath(currPos,targetPos);

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
