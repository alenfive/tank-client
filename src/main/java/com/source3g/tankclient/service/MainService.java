package com.source3g.tankclient.service;

import com.source3g.tankclient.action.*;
import com.source3g.tankclient.action.Tank1Action.TankOneService;
import com.source3g.tankclient.action.Tank2Action.TankTwoService;
import com.source3g.tankclient.action.Tank3Action.TankThreeService;
import com.source3g.tankclient.action.Tank4Action.TankFourService;
import com.source3g.tankclient.action.Tank5Action.TankFiveService;
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
    private LiveAction liveAction;
    @Autowired
    private MapService mapService;
    @Autowired
    private TankOneService tankOneService;
    @Autowired
    private TankTwoService tanTwoService;
    @Autowired
    private TankThreeService tankThreeService;
    @Autowired
    private TankFourService tankFourService;
    @Autowired
    private TankFiveService tankFiveService;

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

        //计算一个攻击目标
        params.setAttackTarget(attackService.prepareAttackTarget(params));

        System.out.println(params.getAttackTarget()==null?null:params.getAttackTarget().getTank().getTId());


        //过淲无生命值的操作
        List<Action> actions = params.getResultAction().stream().filter(item->item.getTank().getShengyushengming()>0).collect(Collectors.toList());

        for (Action action : actions){
            int suffix = Integer.valueOf(action.getTId().substring(1,2));
            switch (suffix){
                case 1:tankOneService.action(params,action);break;
                case 2:tanTwoService.action(params,action);break;
                case 3:tankThreeService.action(params,action);break;
                case 4:tankFourService.action(params,action);break;
                case 5:tankFiveService.action(params,action);break;
            }
        }


        List<Action> sortedActions = params.getResultAction().stream().sorted(Comparator.comparing(Action::getSort)).collect(Collectors.toList());

        //更新最后的位置
        params.getSessionData().getTankLastPosList().forEach(item->{
            Position pos = mapService.getPosition(params.getView(),item.getTId());
            if (pos != null){
                item.setPos(pos);
            }
        });
        return sortedActions;
    }

}
