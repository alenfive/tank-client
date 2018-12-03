package com.source3g.tankclient.service;

import com.source3g.tankclient.action.*;
import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.Position;
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

        List<Action> actions = globalValues.getResultAction();

        //使用复活币
        liveAction.process(globalValues,actions);


        //捡复活币
        glodPickupAction.process(globalValues,actions);


        actions = actions.stream().filter(item->!item.isUsed()).collect(Collectors.toList());

        //攻击敌方或者逃跑
        attackEnemyAction.process(globalValues,actions);

        //攻打BOSS
        attackBossAction.process(globalValues,actions);




        //扫图
        onPatrolAction.process(globalValues,actions);


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
