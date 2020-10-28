package dds20.controller;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.rest.dto.NodeGetDTO;
import dds20.rest.dto.SettingsPostDTO;
import dds20.rest.dto.SetupPostDTO;
import dds20.rest.mapper.DTOMapper;
import dds20.service.DataService;
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
        nodeService.clearNode();
        dataService.clearData();

        Node node = DTOMapper.INSTANCE.convertSetupPostDTOtoEntity(setupPostDTO);
        node.setVote(true);
        nodeService.saveNode(node);
    }

    @PostMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestBody SettingsPostDTO settingsPostDTO) {
        nodeService.updateSettings(settingsPostDTO);
    }
}
