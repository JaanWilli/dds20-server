package dds20.controller;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.rest.dto.*;
import dds20.rest.mapper.DTOMapper;
import dds20.service.DataService;
import dds20.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Node Controller
 * This class is responsible for handling all REST request that are related to the node.
 * The controller will receive the request and delegate the execution to the NodeService and finally return the result.
 */
@RestController
public class NodeController {

    private final NodeService nodeService;
    private final DataService dataService;

    NodeController(NodeService nodeService, DataService dataService) {
        this.nodeService = nodeService;
        this.dataService = dataService;
    }

    @GetMapping("/status")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public NodeGetDTO getNode() {
        Node node = nodeService.getNode();
        return DTOMapper.INSTANCE.convertEntityToNodeGetDTO(node);
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postSetup(@RequestBody SetupPostDTO setupPostDTO) {
        Node node = DTOMapper.INSTANCE.convertSetupPostDTOtoEntity(setupPostDTO);
        nodeService.saveNode(node);
    }

    @PostMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestBody SettingsPostDTO settingsPostDTO) {
        // recovery process
        Node node = nodeService.getNode();
        if (!node.getActive() && settingsPostDTO.getActive()) {
            List<Data> dataList = dataService.getAllData();
            List<Data> dataListNotStatus = new ArrayList<>();
            for (Data data : dataList) {
                if (!data.getIsStatus()) {
                    dataListNotStatus.add(data);
                }
            }
            Data lastData = dataListNotStatus.get(0);
            for (Data data : dataListNotStatus) {
                if (data.getId() > lastData.getId()) {
                    lastData = data;
                }
            }
            String lastMessage = lastData.getMessage();
            if (lastMessage.equals("prepare")) {
                dataService.sendInquiry(node.getCoordinator(), lastData.getTransId());
            }
            if (lastMessage.equals("commit") || lastMessage.equals("abort")) {
                for (String sub : node.getSubordinates()) {
                    dataService.sendMessage(sub, lastMessage, lastData.getTransId());
                }
            }
        }

        // settings
        node.setActive(settingsPostDTO.getActive());
        node.setDieAfter(settingsPostDTO.getDieAfter());
        nodeService.saveNode(node);
    }
}
