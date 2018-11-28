package com.source3g.tankclient.controller;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.service.ClintService;
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
    private ClintService clintService;

    @Autowired
    private MapService mapService;

    @PostMapping("/init")
    public void init(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());
    }


    @PostMapping("/action")
    public List<Action> action(@RequestBody ClientParam clientParam) throws IOException {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());

        return clintService.action(clientParam);
    }


}
