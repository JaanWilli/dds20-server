package dds20.controller;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.rest.dto.DataGetDTO;
import dds20.rest.dto.InquiryPostDTO;
import dds20.rest.dto.MessagePostDTO;
import dds20.rest.mapper.DTOMapper;
import dds20.service.DataService;
import dds20.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Controller
 * This class is responsible for handling all REST request that are related to the data.
 * The controller will receive the request and delegate the execution to the DataService and finally return the result.
 */
@RestController
public class DataController {

    private final DataService dataService;
    private final NodeService nodeService;

    DataController(DataService dataService, NodeService nodeService) {
        this.dataService = dataService;
        this.nodeService = nodeService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postStart() {
        Data startMessage = new Data();
        startMessage.setIsStatus(true);
        startMessage.setMessage("Received start command from client");
        dataService.saveData(startMessage);
        //TODO: send prepare to all subordinates
    }

    @GetMapping("/info")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataGetDTO> getInfo() {
        List<Data> allData = dataService.getAllData();
        List<DataGetDTO> result = new ArrayList<>();
        for (Data data : allData) {
            result.add(DTOMapper.INSTANCE.convertEntityToDataGetDTO(data));
        }
        return result;
    }

    @PostMapping("/message")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestBody MessagePostDTO messagePostDTO) {
        if (nodeService.getNode().getActive()) {
            // handle message
            messagePostDTO.setIsStatus(false);
            Data data = DTOMapper.INSTANCE.convertMessagePostDTOtoEntity(messagePostDTO);
            dataService.saveData(data);
        }

        // die
        Node node = nodeService.getNode();
        if (node.getDieAfter().equals(messagePostDTO.getMessage())) {
            node.setActive(false);
            nodeService.saveNode(node);
        }
    }

    @PostMapping("/inquiry")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postInquiry(@RequestBody InquiryPostDTO inquiryPostDTO) {
        if (nodeService.getNode().getActive()) {
            Data data = dataService.getDataFromTransId(inquiryPostDTO.getTransId());
            dataService.sendMessage(inquiryPostDTO.getSender(), data.getMessage(), inquiryPostDTO.getTransId());
        }
    }
}
