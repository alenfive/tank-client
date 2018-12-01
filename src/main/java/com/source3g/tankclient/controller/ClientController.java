package com.source3g.tankclient.controller;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.service.ClientService;
import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/player")
@Slf4j
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private MapService mapService;

    @PostMapping("/init")
    public void init(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());
        clientService.init(clientParam);
    }


    @PostMapping("/action")
    public synchronized List<Action> action(@RequestBody ClientParam clientParam) throws IOException {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());

        List<Action> actions = clientService.action(clientParam);

        actions.stream().filter(item->item.getLength()>0).forEach(item-> System.out.println(item.getTId()+":"+item.getLength()+":"+item.getDirection()));
        log.info("actions:{}",actions);
        return actions;
    }


}
