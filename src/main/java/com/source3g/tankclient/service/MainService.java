package com.source3g.tankclient.service;

import com.source3g.tankclient.action.*;
import com.source3g.tankclient.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainService {

    @Autowired
    private InitAction initAction;

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
    @Autowired
    private AttackService attackService;

    public void init(ClientParam clientParam) {

    }

    public List<Action> action(ClientParam clientParam) {

        GlobalValues globalValues = new GlobalValues();

        initAction.process(clientParam,globalValues);
        liveAction.process(globalValues,globalValues.getResultAction());

        //过淲无生命值的操作
        List<Action> actions = globalValues.getResultAction().stream().filter(item->item.getTank().getShengyushengming()>0).collect(Collectors.toList());

        //是否有复活币
        glodPickupAction.process(globalValues,actions);

        //有敌人
        attackEnemyAction.process(globalValues,actions);

        //攻击BOSS
        attackBossAction.process(globalValues,actions);

        //扫图
        onPatrolAction.process(globalValues,actions);

        List<Action> sortedActions = globalValues.getResultAction().stream().sorted(Comparator.comparing(Action::getSort)).collect(Collectors.toList());

        //更新最后的位置
        globalValues.getSessionData().getTankPositions().forEach(item->{
            Position pos = mapService.getPosition(globalValues.getView(),item.getTId());
            if (pos != null){
                item.setPosition(pos);
            }
        });
        return sortedActions;
    }

}
