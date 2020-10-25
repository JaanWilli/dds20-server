package dds20.service;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.repository.DataRepository;
import dds20.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    private final DataRepository dataRepository;
    private final NodeRepository nodeRepository;

    private static final String PREPARE = "PREPARE";
    private static final String COMMIT = "COMMIT";
    private static final String ABORT = "ABORT";
    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String ACK = "ACK";
    private static final String END = "END";

    private final Map<String, String> votes = new HashMap<>();
    private final List<String> acks = new ArrayList<>();

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

    public void clearData() {
        dataRepository.deleteAll();
        dataRepository.flush();
        votes.clear();
        acks.clear();
    }

    public void startTransaction() {
        for (String s : getSubordinates()) {
            writeSendLog(PREPARE, s);
            sendMessage(s, PREPARE, 1);
        }
    }

    public void handleMessage(Data data) {
        writeReceiveLog(data.getMessage(), data.getNode());

        switch (data.getMessage().toUpperCase()) {
            case PREPARE:
                handlePrepare();
                break;
            case YES:
            case NO:
                handleVote(data);
                break;
            case COMMIT:
                handleCommit();
                break;
            case ABORT:
                handleAbort();
                break;
            case ACK:
                handleAck(data);
                break;
        }
    }

    private void handlePrepare() {
        Node node = getNode();
        String msg;

        if (node.getVote()) {
            writeRecord(PREPARE);
            msg = YES;
        }
        else {
            writeRecord(ABORT);
            msg = NO;
        }
        writeSendLog(msg, node.getCoordinator());
        sendMessage(node.getCoordinator(), msg, 1);
    }

    private void handleVote(Data data) {
        Node node = getNode();
        votes.put(data.getNode(), data.getMessage());

        // if all votes arrived
        if (votes.keySet().size() == node.getSubordinates().size()) {
            // if at least one of the votes is NO
            if (!votes.containsValue(NO)) {
                writeLog("Received YES VOTE from all subordinates");
                writeRecord(COMMIT);
                for (String s : node.getSubordinates()) {
                    writeSendLog(COMMIT, s);
                    sendMessage(s, COMMIT, 1);
                }
            }
            else {
                writeLog("Received NO VOTE from at least one subordinate");
                writeRecord(ABORT);
                int c = 0;
                for (Map.Entry<String, String> n : votes.entrySet()) {
                    if (n.getValue().equalsIgnoreCase(YES)) {
                        writeSendLog(ABORT, n.getKey());
                        sendMessage(n.getKey(), ABORT, 1);
                        c++;
                    }
                }
                // if no acks are necessary to write END
                if (c == 0) {
                    writeRecord(END);
                }
            }
        }

    }

    private void handleCommit() {
        Node node = getNode();

        writeRecord(COMMIT);
        writeSendLog(ACK, node.getCoordinator());
        sendMessage(node.getCoordinator(), ACK, 1);
    }

    private void handleAbort() {
        Node node = getNode();

        writeRecord(ABORT);
        writeSendLog(ACK, node.getCoordinator());
        sendMessage(node.getCoordinator(), ACK, 1);
    }

    private void handleAck(Data data) {
        acks.add(data.getNode());
        int numberOfYesVotes = Collections.frequency(new ArrayList<>(votes.values()), YES);
        if (acks.size() == numberOfYesVotes) {
            writeRecord(END);
        }
    }

    public void sendMessage(String recipient, String msg, int transId) {
        Node node = getNode();

        MultiValueMap<String, String> message = new LinkedMultiValueMap<>();
        message.add("message", msg);
        message.add("node", node.getNode());
        message.add("coordinator", node.getCoordinator());
        message.add("transId", String.valueOf(transId));

        HttpEntity<Map> request = getRequest(message);

        try {
            restTemplate.exchange(recipient + "/message", HttpMethod.POST, request, String.class);
        }
        catch (Exception ignored) {
        }
    }

    public void sendInquiry(String recipient, int transId) {
        Node node = getNode();

        MultiValueMap<String, String> message = new LinkedMultiValueMap<>();
        message.add("sender", node.getNode());
        message.add("transId", String.valueOf(transId));

        HttpEntity<Map> request = getRequest(message);

        try {
            restTemplate.exchange(recipient + "/message", HttpMethod.POST, request, String.class);
        }
        catch (Exception ignored) {
        }
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

    private void writeSendLog(String msg, String recipient) {
        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Sending \"%s\" to %s", msg, recipient));
        saveData(log);
    }

    private void writeReceiveLog(String msg, String sender) {
        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Receiving \"%s\" from %s", msg, sender));
        saveData(log);
    }

    private void writeLog(String msg) {
        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(msg);
        saveData(log);
    }

    private void writeRecord(String msg) {
        Data data = new Data();
        data.setIsStatus(false);
        data.setMessage(msg);
        saveData(data);
    }

    private HttpEntity<Map> getRequest(MultiValueMap<String, String> message) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(message.toSingleValueMap(), headers);
    }
}
