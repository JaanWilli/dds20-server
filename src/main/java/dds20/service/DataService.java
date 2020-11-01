package dds20.service;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.repository.DataRepository;
import dds20.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
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

    private final NodeService nodeService;

    private final DataRepository dataRepository;
    private final NodeRepository nodeRepository;

    private static final String PREPARE = "PREPARE";
    private static final String COMMIT = "COMMIT";
    private static final String ABORT = "ABORT";
    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String ACK = "ACK";
    private static final String END = "END";

    private final List<Data> bufferMessages = new ArrayList<>();
    private final Map<String, String> votes = new HashMap<>();
    private final List<String> acksNeeded = new ArrayList<>();
    private final List<String> acksReceived = new ArrayList<>();
    private Timer timer;


    @Autowired
    private final RestTemplate restTemplate;

    @Autowired
    public DataService(NodeService nodeService,
                       @Qualifier("dataRepository") DataRepository dataRepository,
                       @Qualifier("nodeRepository") NodeRepository nodeRepository,
                       RestTemplate restTemplate) {
        this.nodeService = nodeService;
        this.dataRepository = dataRepository;
        this.nodeRepository = nodeRepository;
        this.restTemplate = restTemplate;
        this.timer = new Timer();
    }

    @Scheduled(fixedRate = 1000)
    public void allVotes() {
        Node node = getNode();
        if (node != null && node.getActive() && node.getIsCoordinator()) {
            // if all votes arrived
            if (votes.keySet().size() == node.getSubordinates().size()) {
                // if at least one of the votes is NO
                if (!votes.containsValue(NO)) {
                    writeLog("Received YES VOTE from all subordinates");
                    writeRecord(COMMIT);

                    if (node.getDieAfter().equals("commit/abort")) {
                        die();
                        return;
                    }

                    for (String s : node.getSubordinates()) {
                        writeSendLog(COMMIT, s);
                        sendMessage(s, COMMIT, 1);
                        acksNeeded.add(s);
                    }
                    if (node.getDieAfter().equals("result")) {
                        die();
                    }
                }
                else {
                    writeLog("Received NO VOTE from at least one subordinate");
                    writeRecord(ABORT);

                    if (node.getDieAfter().equals("commit/abort")) {
                        die();
                        return;
                    }

                    int c = 0;
                    for (Map.Entry<String, String> n : votes.entrySet()) {
                        if (n.getValue().equalsIgnoreCase(YES)) {
                            writeSendLog(ABORT, n.getKey());
                            sendMessage(n.getKey(), ABORT, 1);
                            acksNeeded.add(n.getKey());
                            c++;
                        }
                    }
                    if (node.getDieAfter().equals("result")) {
                        die();
                    }
                    // if no acks are necessary to write END
                    if (c == 0) {
                        writeRecord(END);
                    }
                }
                votes.clear();
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void allAcks() {
        Node node = getNode();
        if (node != null) {
            if (node.getActive() && node.getIsCoordinator()) {
                Data lastData = getLastDataEntry();
                if (lastData != null) {
                    String lastMsg = lastData.getMessage();
                    if (lastMsg != null && (lastMsg.equalsIgnoreCase(COMMIT) || lastMsg.equalsIgnoreCase(ABORT))) {
                        if (acksReceived.size() == acksNeeded.size()) {
                            if (acksNeeded.size() > 0) {
                                writeLog("Received ACK from all subordinates");
                            }
                            else {
                                writeLog("Received NO from all subordinates.");
                            }
                            writeRecord(END);
                            acksReceived.clear();
                            acksNeeded.clear();
                        }
                    }
                }
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void handleMessage() {
        Node node = getNode();
        if (node != null && node.getActive() && bufferMessages.size() > 0) {
            Data data = bufferMessages.remove(0);
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
    }

    public void clearData() {
        dataRepository.deleteAll();
        dataRepository.flush();
        bufferMessages.clear();
        votes.clear();
        acksNeeded.clear();
        acksReceived.clear();
        timer.cancel();
    }

    public void startTransaction() {
        Data startMessage = new Data();
        startMessage.setIsStatus(true);
        startMessage.setMessage("Received start command from client");
        saveData(startMessage);

        Node node = getNode();
        node.setActive(true);
        nodeService.saveNode(node);

        for (String s : getSubordinates()) {
            writeSendLog(PREPARE, s);
            sendMessage(s, PREPARE, 1);
        }

        if (node.getDieAfter().equals("prepare")) {
            die();
        }
    }

    public void receiveMessage(Data data) {
        bufferMessages.add(data);
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

        if (node.getDieAfter().equals("prepare")) {
            die();
            return;
        }

        writeSendLog(msg, node.getCoordinator());
        sendMessage(node.getCoordinator(), msg, 1);

        if (node.getDieAfter().equals("vote")) {
            die();
        }
    }

    private void handleVote(Data data) {
        votes.put(data.getNode(), data.getMessage());
    }

    private void handleCommit() {
        Node node = getNode();
        writeRecord(COMMIT);

        if (node.getDieAfter().equals("commit/abort")) {
            die();
            return;
        }

        writeSendLog(ACK, node.getCoordinator());
        sendMessage(node.getCoordinator(), ACK, 1);
    }

    private void handleAbort() {
        Node node = getNode();

        writeRecord(ABORT);

        if (node.getDieAfter().equals("commit/abort")) {
            die();
        }

        writeSendLog(ACK, node.getCoordinator());
        sendMessage(node.getCoordinator(), ACK, 1);
    }

    private void handleAck(Data data) {
        acksReceived.add(data.getNode());
    }

    public void startRecovery() {
        Node node = getNode();
        node.setActive(true);
        nodeService.saveNode(node);

        Data lastData = getLastDataEntry();
        if (lastData == null) {
            writeRecord(ABORT);
            return;
        }
        String lastMsg = lastData.getMessage();
        if (lastMsg.equalsIgnoreCase(PREPARE)) {
            writeSendLog("INQURY", node.getCoordinator());
            sendInquiry(node.getCoordinator(), 1);
        }
        else if (lastMsg.equalsIgnoreCase(COMMIT) || lastMsg.equalsIgnoreCase(ABORT)) {
            List<String> noAcks = getSubordinatesNoAck();
            for (String sub : noAcks) {
                writeSendLog(lastMsg, sub);
                sendMessage(sub, lastMsg, 1);
            }
        }
    }

    public void handleInquiry(String sender, int transId) {
        writeReceiveLog("INQUIRY", sender);
        Data lastData = getLastDataEntry();
        if (lastData == null) {
            votes.put(sender, null);
            return;
        }
        String lastMsg = lastData.getMessage();
        if (lastMsg.equalsIgnoreCase(COMMIT) || lastMsg.equalsIgnoreCase(ABORT)) {
            writeSendLog(lastMsg, sender);
            sendMessage(sender, lastMsg, transId);
        }
        else {
            writeSendLog(ABORT, sender);
            sendMessage(sender, ABORT, transId);
        }
    }

    public void die() {
        Node node = getNode();
        node.setActive(false);
        nodeService.saveNode(node);
        writeLog("Node died");
        startReviveTimer();
    }

    public void startReviveTimer() {
        int milliseconds = 5000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                writeLog("Starting recovery process");
                startRecovery();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
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
            restTemplate.exchange(recipient + "/inquiry", HttpMethod.POST, request, String.class);
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

    public synchronized void saveData(Data newData) {
        dataRepository.saveAndFlush(newData);
    }

    private Node getNode() {
        return this.nodeRepository.findTopByOrderByIdDesc();
    }

    private List<String> getSubordinates() {
        return nodeRepository.findTopByOrderByIdDesc().getSubordinates();
    }

    private List<String> getSubordinatesNoAck() {
        List<String> ret = new ArrayList<>();
        Node node = getNode();
        for (String sub : node.getSubordinates()) {
            if (!acksReceived.contains(sub)) {
                ret.add(sub);
            }
        }
        return ret;
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
