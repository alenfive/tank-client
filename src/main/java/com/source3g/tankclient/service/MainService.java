package com.source3g.tankclient.service;

import com.source3g.tankclient.action.*;
import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.Position;
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
    private MapService mapService;
    @Autowired
    private AttackService attackService;

    public void init(ClientParam clientParam) {

    }

    public List<Action> action(ClientParam clientParam) {

        GlobalValues params = new GlobalValues();

        //数据初始化
        initAction.process(clientParam,params);

        //复活币使用策略
        liveAction.process(params,params.getResultAction());



        //过淲无生命值的操作
        List<Action> actions = params.getResultAction().stream().filter(item->item.getTank().getShengyushengming()>0).collect(Collectors.toList());

        //是否有复活币
        glodPickupAction.process(params,actions);

        //有敌人
        attackEnemyAction.process(params,actions);

        //攻击BOSS
        attackBossAction.process(params,actions);

        //扫图
        onPatrolAction.process(params,actions);

        List<Action> sortedActions = params.getResultAction().stream().sorted(Comparator.comparing(Action::getSort)).collect(Collectors.toList());

        //更新最后的位置
        params.getSessionData().getTankPositions().forEach(item->{
            Position pos = mapService.getPosition(params.getView(),item.getTId());
            if (pos != null){
                item.setPosition(pos);
            }
        });
        return sortedActions;
    }

}
