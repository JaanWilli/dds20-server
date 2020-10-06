package ch.uzh.ifi.seal.soprafs20.controller;

import ch.uzh.ifi.seal.soprafs20.entity.Data;
import ch.uzh.ifi.seal.soprafs20.rest.dto.DataGetDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.InquiryPostDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.MessagePostDTO;
import ch.uzh.ifi.seal.soprafs20.rest.mapper.DTOMapper;
import ch.uzh.ifi.seal.soprafs20.service.DataService;
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

    DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/info")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataGetDTO> getData() {
        List<Data> data = dataService.getData();
        List<DataGetDTO> dataGetDTOs = new ArrayList<>();

        for (Data dataItem : data) {
            dataGetDTOs.add(DTOMapper.INSTANCE.convertEntityToDataGetDTO(dataItem));
        }
        return dataGetDTOs;
    }

    @PostMapping("/message")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postMessage(@RequestBody MessagePostDTO messagePostDTO) {
        // handle Message here
    }

    @PostMapping("/inquiry")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void postInquiry(@RequestBody InquiryPostDTO inquiryPostDTO) {
        // handle Inquiry here
    }
}
