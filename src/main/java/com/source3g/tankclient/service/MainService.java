package com.source3g.tankclient.service;

import com.source3g.tankclient.action.*;
import com.source3g.tankclient.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainService {

    @Autowired
    private InitAction initAction;

    @Autowired
    private RandomAction randomAction;

    @Autowired
    private OnPatrolAction onPatrolAction;

    @Autowired
    private GlodPickupAction glodPickupAction;
    @Autowired
    private AttackBossAction attackBossAction;
    @Autowired
    private DefenseAction defenseAction;
    @Autowired
    private LiveAction liveAction;
    @Autowired
    private AttackEnemyAction attackEnemyAction;
    @Autowired
    private RetreatAction retreatAction;
    @Autowired
    private MapService mapService;

    public void init(ClientParam clientParam) {

    }

    public List<Action> action(ClientParam clientParam) {

        GlobalValues globalValues = new GlobalValues();

        initAction.process(clientParam,globalValues);
        randomAction.process(globalValues, globalValues.getResultAction());
        liveAction.process(globalValues,globalValues.getResultAction());

        //过淲无生命值的操作
        List<Action> actions = globalValues.getResultAction().stream().filter(item->{
            for(Tank tank : globalValues.getCurrTeam().getTanks()){
                if(tank.getTId().equals(item.getTId()) && tank.getShengyushengming()>0){
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());

        for(Action action : actions){
            try{

                //捡复活币
                NodeType glodType = glodPickupAction.process(globalValues,action);
                if(NodeType.Success.equals(glodType)){
                    continue;
                }

                //撤退
                /*NodeType retreatType = retreatAction.process(globalValues,action);
                if(NodeType.Success.equals(retreatType)){
                    continue;
                }*/

                //攻打BOSS
                NodeType bossType = attackBossAction.process(globalValues,action);
                if(NodeType.Success.equals(bossType)){
                    continue;
                }

                //攻击敌方坦克
                NodeType attackEnemy = attackEnemyAction.process(globalValues,action);
                if(NodeType.Success.equals(attackEnemy)){
                    continue;
                }

                //扫图
                NodeType onPatrolType = onPatrolAction.process(globalValues,action);
                if(NodeType.Success.equals(onPatrolType)){
                    continue;
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }


        //更新最后的位置
        globalValues.getSessionData().getTankPositions().forEach(item->{
            Position pos = mapService.getPosition(globalValues.getView(),item.getTId());
            if (pos != null){
                item.setPosition(pos);
            }
        });
        return globalValues.getResultAction();
    }

}
