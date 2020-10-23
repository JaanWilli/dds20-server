package dds20.service;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.repository.DataRepository;
import dds20.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

    private static final String PREPARE = "PREPARE";
    private static final String COMMIT = "COMMIT";
    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String ACK = "ACK";

    private final List<String> yesVotes = new ArrayList<>();

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

    public List<Data> getAllData() {
        return this.dataRepository.findAll();
    }

    public Data getLastDataEntry() {
        return this.dataRepository.findTopByIsStatusFalseOrderByIdDesc();
    }

    public Data getDataFromTransId(Integer transId) {
        return this.dataRepository.findByTransId(transId);
    }

    public synchronized void saveData(Data newData) {
        dataRepository.saveAndFlush(newData);
    }

    private Node getNode() {
        return this.nodeRepository.findTopByOrderByIdDesc();
    }

    private List<String> getSubordinates() {
        return nodeRepository.findTopByOrderByIdDesc().getSubordinates();
    }

    public void startTransaction() {
        for (String s : getSubordinates()) {
            Data log = new Data();
            log.setIsStatus(true);
            log.setMessage(String.format("Sending \"%s\" to %s", PREPARE, s));
            saveData(log);
            sendMessage(s, PREPARE, 1);
        }
    }

    private void handlePrepare() {
        Node node = getNode();

        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Sending \"%s\" to %s", YES, node.getCoordinator()));
        saveData(log);

        sendMessage(node.getCoordinator(), YES, 1);
    }

    private void handleYes(Data data) {
        yesVotes.add(data.getNode());
        Node node = getNode();

        if (node.getSubordinates().size() == yesVotes.size()) {
            for (String s : node.getSubordinates()) {
                Data log = new Data();
                log.setIsStatus(true);
                log.setMessage(String.format("Sending \"%s\" to %s", COMMIT, s));
                saveData(log);

                sendMessage(s, COMMIT, 1);
            }
        }
    }

    private void handleNo() {
        // TODO later
    }

    private void handleCommit() {
        Node node = getNode();

        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Sending \"%s\" to %s", ACK, node.getCoordinator()));
        saveData(log);

        sendMessage(node.getCoordinator(), ACK, 1);
    }

    private void handleAbort() {
        // TODO later
    }

    public void handleMessage(Data data) {

        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Received \"%s\" from %s", data.getMessage(), data.getNode()));
        saveData(log);

        data.setIsStatus(false);
        saveData(data);

        switch (data.getMessage().toLowerCase()) {
            case "prepare":
                handlePrepare();
                break;
            case "yes":
                handleYes(data);
                break;
            case "no":
                handleNo();
                break;
            case "commit":
                handleCommit();
                break;
            case "abort":
                handleAbort();
                break;
            case "ack":
                // nothing
                break;
        }
    }

    public void sendMessage(String recipient, String msg, int transId) {
        Node node = getNode();

        MultiValueMap<String, String> message= new LinkedMultiValueMap<>();
        message.add("message", msg);
        message.add("node", node.getNode());
        message.add("coordinator", node.getCoordinator());
        message.add("transId", String.valueOf(transId));

        HttpEntity<Map> request = getRequest(message);

        try {
            restTemplate.exchange(recipient + "/message", HttpMethod.POST, request, String.class);
        } catch (Exception e) {}
    }

    public void sendInquiry(String recipient, int transId) {
        Node node = getNode();

        MultiValueMap<String, String> message = new LinkedMultiValueMap<>();
        message.add("sender", node.getNode());
        message.add("transId", String.valueOf(transId));

        HttpEntity<Map> request = getRequest(message);

        try {
            restTemplate.exchange(recipient + "/message", HttpMethod.POST, request, String.class);
        } catch (Exception e) {}
    }

    private HttpEntity<Map> getRequest(MultiValueMap<String,String> message) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(message.toSingleValueMap(), headers);
    }
}
