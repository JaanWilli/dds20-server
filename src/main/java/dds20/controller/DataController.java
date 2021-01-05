package dds20.controller;

import dds20.entity.Data;
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
    public void postStart(@RequestParam("session") String session) {
        dataService.startTransaction(session);
    }

    @GetMapping("/info")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataGetDTO> getInfo(@RequestParam("session") String session) {
        List<DataGetDTO> result = new ArrayList<>();
        for (Data data : dataService.getAllData(session)) {
            result.add(DTOMapper.INSTANCE.convertEntityToDataGetDTO(data));
        }
        return result;
    }

    @PostMapping("/message")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestParam("session") String session,
                            @RequestBody MessagePostDTO messagePostDTO) {
        if (nodeService.isActive(session)) {
            // handle message
            Data data = DTOMapper.INSTANCE.convertMessagePostDTOtoEntity(messagePostDTO);
            data.setSession(session);
            dataService.receiveMessage(session, data);
        }
    }

    @PostMapping("/inquiry")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postInquiry(@RequestParam("session") String session,
                            @RequestBody InquiryPostDTO inquiryPostDTO) {
        if (nodeService.isActive(session)) {
            dataService.handleInquiry(session, inquiryPostDTO.getSender(), inquiryPostDTO.getTransId());
        }
    }
}