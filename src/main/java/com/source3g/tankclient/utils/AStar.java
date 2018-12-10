package com.source3g.tankclient.utils;

import com.source3g.tankclient.entity.MapEnum;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.entity.TMap;

import java.util.*;

/**
 * Created by Administrator on 9/19/2017.
 */


public class AStar {

    private TMap view;

    public static final int STEP = 10;

    private ArrayList<Position> openList = new ArrayList<>();
    private ArrayList<Position> closeList = new ArrayList<>();
    private Set<String> blockList = new HashSet<>(Arrays.asList(MapEnum.M1.name(),MapEnum.M2.name(),MapEnum.M3.name()));

    public void appendBlockList(String ... arrs){
        blockList.addAll(new HashSet<>(Arrays.asList(arrs)));
    }
    public void appendBlockList(List<String> blocks){
        blockList.addAll(blocks);
    }

    public Position findMinFNodeInOpneList() {
        Position tempNode = openList.get(0);
        for (Position node : openList) {
            if (node.F < tempNode.F) {
                tempNode = node;
            }
        }
        return tempNode;
    }

    public void clear(){
        openList = new ArrayList<>();
        closeList = new ArrayList<>();
        blockList = new HashSet<>(Arrays.asList(MapEnum.M1.name(),MapEnum.M2.name(),MapEnum.M3.name()));
    }

    public AStar(TMap view){
        this.view = view;
    }

    public ArrayList<Position> findNeighborNodes(Position currentNode) {
        ArrayList<Position> arrayList = new ArrayList<>();

        // 只考虑上下左右，不考虑斜对角
        int topR = currentNode.rowIndex -1;
        int topC = currentNode.colIndex;
        if (canReach(topR, topC) && !exists(closeList, topR, topC)) {
            arrayList.add(new Position(topR, topC));
        }
        int bottomR = currentNode.rowIndex + 1;
        int bottomC = currentNode.colIndex;
        if (canReach(bottomR, bottomC) && !exists(closeList, bottomR, bottomC)) {
            arrayList.add(new Position(bottomR, bottomC));
        }
        int leftR = currentNode.rowIndex;
        int leftC = currentNode.colIndex - 1;
        if (canReach(leftR, leftC) && !exists(closeList, leftR, leftC)) {
            arrayList.add(new Position(leftR, leftC));
        }
        int rightR = currentNode.rowIndex;
        int rightC = currentNode.colIndex + 1;
        if (canReach(rightR, rightC) && !exists(closeList, rightR, rightC)) {
            arrayList.add(new Position(rightR, rightC));
        }
        return arrayList;
    }

    public boolean canReach(int rowIndex, int colIndex) {
        if (rowIndex >= 0 && rowIndex < view.getRowLen() && colIndex >= 0 && colIndex < view.getColLen()) {
            return blockList.contains(view.get(rowIndex,colIndex));
        }
        return false;
    }

    public Position findPath(Position startNode, Position endNode) {
        startNode.setParent(null);
        // 把起点加入 open list
        openList.add(startNode);

        while (openList.size() > 0) {
            // 遍历 open list ，查找 F值最小的节点，把它作为当前要处理的节点
            Position currentNode = findMinFNodeInOpneList();
            // 从open list中移除
            openList.remove(currentNode);
            // 把这个节点移到 close list
            closeList.add(currentNode);

            ArrayList<Position> neighborNodes = findNeighborNodes(currentNode);
            for (Position node : neighborNodes) {
                if (exists(openList, node)) {
                    foundPoint(currentNode, node);
                } else {
                    notFoundPoint(currentNode, endNode, node);
                }
            }
            if (find(openList, endNode) != null) {
                return revert(find(openList, endNode));
            }
        }

        return revert(find(openList, endNode));
    }

    /**
     * 路径反转
     * @return
     */
    private Position revert(Position head){

        if(head == null || head.getParent() == null){
            return head;
        }
        Position pre = revert(head.getParent());
        head.getParent().setParent(head);
        head.setParent(null);
        return pre;
    }

    private void foundPoint(Position tempStart, Position node) {
        int G = calcG(tempStart, node);
        if (G < node.G) {
            node.parent = tempStart;
            node.G = G;
            node.calcF();
        }
    }

    private void notFoundPoint(Position tempStart, Position end, Position node) {
        node.parent = tempStart;
        node.G = calcG(tempStart, node);
        node.H = calcH(end, node);
        node.calcF();
        openList.add(node);
    }

    private int calcG(Position start, Position node) {
        int G = STEP;
        int parentG = node.parent != null ? node.parent.G : 0;
        return G + parentG;
    }

    private int calcH(Position end, Position node) {
        int step = Math.abs(node.rowIndex - end.rowIndex) + Math.abs(node.colIndex - end.colIndex);
        return step * STEP;
    }


    public static Position find(List<Position> nodes, Position point) {
        for (Position n : nodes)
            if ((n.rowIndex == point.rowIndex) && (n.colIndex == point.colIndex)) {
                return n;
            }
        return null;
    }

    public static boolean exists(List<Position> nodes, Position node) {
        for (Position n : nodes) {
            if ((n.rowIndex == node.rowIndex) && (n.colIndex == node.colIndex)) {
                return true;
            }
        }
        return false;
    }

    public static boolean exists(List<Position> nodes, int rowIndex, int colIndex) {
        for (Position n : nodes) {
            if ((n.rowIndex == rowIndex) && (n.colIndex == colIndex)) {
                return true;
            }
        }
        return false;
    }

    public int countStep(Position currPos, Position targetPos) {
        Position head = findPath(currPos,targetPos);
        if (head == null)return 999999;
        int step = 0;
        if(head == null)return step;
        while (head.getParent() != null){
            head = head.getParent();
            step ++;
        }
        return step;
    }
}
