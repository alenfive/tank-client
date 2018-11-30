package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 随机动作
 */
@Component
public class RandomAction extends AbstractActiion<GlobalValues,List<Action>> {

    @Override
    public NodeType process(GlobalValues params, List<Action> result) {

        result.addAll(params.getCurrTeam().getTanks().stream().map(item-> {
            ActionTypeEnum typeEnum = ActionTypeEnum.getByIndex((int) Math.floor(Math.random() * 2));

            return Action.builder()
                    .length(0)
                    .tId(item.getTId())
                    .type(typeEnum)
                    .useGlod(false)
                    .direction(DirectionEnum.getByIndex((int) Math.floor(Math.random()*4))).build();
        }).collect(Collectors.toList()));

        return NodeType.Success;
    }
}
