package dds20.controller;

import dds20.entity.Node;
import dds20.rest.dto.*;
import dds20.rest.mapper.DTOMapper;
import dds20.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Node Controller
 * This class is responsible for handling all REST request that are related to the node.
 * The controller will receive the request and delegate the execution to the NodeService and finally return the result.
 */
@RestController
public class NodeController {

    private final NodeService nodeService;

    NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
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

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postStart() {
        //TODO: send prepare to all subordinates
    }

    @PostMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestBody SettingsPostDTO settingsPostDTO) {
        Node node = nodeService.getNode();
        node.setActive(settingsPostDTO.getActive());
        node.setDieAfter(settingsPostDTO.getDieAfter());
        nodeService.saveNode(node);
    }
}
