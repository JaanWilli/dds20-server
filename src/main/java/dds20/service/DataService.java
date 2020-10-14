package dds20.service;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.repository.DataRepository;
import dds20.repository.NodeRepository;
import dds20.rest.dto.InquiryPostDTO;
import dds20.rest.dto.MessagePostDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Data Service
 * This class is the "worker" and responsible for all functionality related to the data
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class DataService {

    private final Logger log = LoggerFactory.getLogger(DataService.class);

    private final DataRepository dataRepository;
    private final NodeRepository nodeRepository;

    @Autowired
    private final RestTemplate restTemplate;

    @Autowired
    public DataService(@Qualifier("dataRepository") DataRepository dataRepository,
                       @Qualifier("nodeRepository") NodeRepository nodeRepository,
                       RestTemplate restTemplate) {
        this.dataRepository = dataRepository;
        this.nodeRepository = nodeRepository;
        this.restTemplate = restTemplate;
    }

    public Data getData() {
        return this.dataRepository.findTopByOrderByIdDesc();
    }

    public void saveData(Data newData) {
        // saves the given entity but data is only persisted in the database once flush() is called
        newData = dataRepository.save(newData);
        dataRepository.flush();

        log.debug("Created Information for Data: {}", newData);
    }

    public Data getDataFromTransId(Integer transId) {
        return this.dataRepository.findByTransId(transId);
    }

    public void sendMessage(String recipient, String msg, int transId) {
        Node node = nodeRepository.findTopByOrderByIdDesc();

        MessagePostDTO message = new MessagePostDTO();
        message.setMessage(msg);
        message.setNode(node.getNode());
        message.setCoordinator(node.getCoordinator());
        message.setTransId(transId);

        restTemplate.postForObject(recipient, message, String.class);
    }

    public void sendInquiry(String recipient, int transId) {
        Node node = nodeRepository.findTopByOrderByIdDesc();
        InquiryPostDTO inquiry = new InquiryPostDTO();
        inquiry.setSender(node.getNode());
        inquiry.setTransId(transId);

        restTemplate.postForObject(recipient, inquiry, String.class);
    }
}
