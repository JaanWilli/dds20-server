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
        NodeGetDTO nodeGetDTO = DTOMapper.INSTANCE.convertEntityToNodeGetDTO(node);
        return nodeGetDTO;
    }

    @PostMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestBody SettingsPostDTO settingsPostDTO) {
        Node userInput = DTOMapper.INSTANCE.convertSettingsPostDTOtoEntity(settingsPostDTO);
        nodeService.createNode(userInput);
    }
}
