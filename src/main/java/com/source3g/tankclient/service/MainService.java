package com.source3g.tankclient.service;

import com.source3g.tankclient.action.*;
import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.entity.GlobalValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private LliveAction lliveAction;
    @Autowired
    private AttackEnemyAction attackEnemyAction;
    @Autowired
    private RetreatAction retreatAction;

    public void init(ClientParam clientParam) {

    }

    public List<Action> action(ClientParam clientParam) {

        GlobalValues globalValues = new GlobalValues();

        initAction.process(clientParam,globalValues);
        randomAction.process(globalValues, globalValues.getResultAction());

        for(Action action : globalValues.getResultAction()){
            try{

                //坦克已死亡
                NodeType diedType = lliveAction.process(globalValues,action);
                if(!NodeType.Success.equals(diedType)){
                    continue;
                }

                //捡复活币
                NodeType glodType = glodPickupAction.process(globalValues,action);
                if(NodeType.Success.equals(glodType)){
                    continue;
                }

                //攻打BOSS
                NodeType bossType = attackBossAction.process(globalValues,action);
                if(NodeType.Success.equals(bossType)){
                    continue;
                }

                //防御
                /*NodeType defenseType = defenseAction.process(globalValues,action);
                if(NodeType.Success.equals(defenseType)){
                    continue;
                }*/

                //撤退
                /*NodeType retreatType = retreatAction.process(globalValues,action);
                if(NodeType.Success.equals(retreatType)){
                    continue;
                }*/

                //攻击敌方坦克
                NodeType enemyType = attackEnemyAction.process(globalValues,action);
                if(NodeType.Success.equals(enemyType)){
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

        return globalValues.getResultAction();
    }

}
