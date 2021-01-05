package dds20.controller;

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
    public NodeGetDTO getNode(@RequestParam("session") String session) {
        Node node = nodeService.getNode(session);
        return DTOMapper.INSTANCE.convertEntityToNodeGetDTO(node);
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postSetup(@RequestParam("session") String session,
                          @RequestBody SetupPostDTO setupPostDTO) {
        nodeService.clearNode(session);
        dataService.clearData(session);

        Node node = DTOMapper.INSTANCE.convertSetupPostDTOtoEntity(setupPostDTO);
        node.setVote(true);
        node.setActive(true);
        node.setSession(session);
        nodeService.saveNode(node);
    }

    @PostMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestParam("session") String session,
                            @RequestBody SettingsPostDTO settingsPostDTO) {
        nodeService.updateSettings(session, settingsPostDTO);
    }
}
