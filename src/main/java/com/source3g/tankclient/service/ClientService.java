package com.source3g.tankclient.service;

import com.source3g.tankclient.action.InitAction;
import com.source3g.tankclient.action.OnPatrolAction;
import com.source3g.tankclient.action.RandomAction;
import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.entity.GlobalValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private InitAction initAction;

    @Autowired
    private RandomAction randomAction;

    @Autowired
    private OnPatrolAction onPatrolAction;

    public void init(ClientParam clientParam) {

    }

    public List<Action> action(ClientParam clientParam) {

        GlobalValues globalValues = new GlobalValues();

        initAction.process(clientParam,globalValues);
        randomAction.process(globalValues, globalValues.getResultAction());

        for(Action action : globalValues.getResultAction()){
            try{

                //扫图
                onPatrolAction.process(globalValues,action);

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return globalValues.getResultAction();
    }

}
