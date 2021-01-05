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

    private static final int respawnTimer = 3000;
    private static final int ackTimer = 8000;
    private static final int voteTimer = 8000;
    private static final int responseTimer = 8000;

    private final Map<String, List<Data>> bufferMessages = new HashMap<>();
    private final Map<String, Map<String, String>> votes = new HashMap<>();
    private final Map<String, List<String>> acksNeeded = new HashMap<>();
    private final Map<String, List<String>> acksReceived = new HashMap<>();
    private final Map<String, Timer> timer = new HashMap<>();


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
    }

    /**
     * Regularly checks if all votes arrived
     * If yes and all votes are YES, send out COMMITs
     * If yes and one vote is NO, send out ABORTs
     */
    @Scheduled(fixedRate = 1000)
    public void allVotes() {
        for (Map.Entry<String, Map<String, String>> voteMap : votes.entrySet()) {
            Node node = getNode(voteMap.getKey());
            Map<String, String> votes = voteMap.getValue();
            if (node != null && node.getActive() && node.getIsCoordinator()) {
                // if all votes arrived
                if (votes.keySet().size() == node.getSubordinates().size()) {
                    timer.get(node.getSession()).cancel();
                    initAcks(node.getSession());
                    // if at least one of the votes is NO
                    if (!votes.containsValue(NO)) {
                        votes.clear();
                        writeLog(node.getSession(), "Received YES VOTE from all subordinates");
                        writeRecord(node.getSession(), COMMIT);

                        if (node.getDieAfter().equals("commit/abort")) {
                            die(node.getSession());
                            return;
                        }

                        for (String s : node.getSubordinates()) {
                            writeSendLog(node.getSession(), COMMIT, s);
                            sendMessage(node.getSession(), s, COMMIT, 1);
                            acksNeeded.get(node.getSession()).add(s);
                        }
                        if (node.getDieAfter().equals("result")) {
                            die(node.getSession());
                            return;
                        }
                        startTimer(node.getSession(), ackTimer, "Not all acknowledgements received");
                    }
                    else {
                        writeLog(node.getSession(), "Received NO VOTE from at least one subordinate");
                        writeRecord(node.getSession(), ABORT);

                        if (node.getDieAfter().equals("commit/abort")) {
                            die(node.getSession());
                            voteMap.getValue().clear();
                            return;
                        }

                        int c = 0;
                        for (Map.Entry<String, String> vote : votes.entrySet()) {
                            if (vote.getValue() == null || vote.getValue().equalsIgnoreCase(YES)) {
                                writeSendLog(node.getSession(), ABORT, vote.getKey());
                                sendMessage(node.getSession(), vote.getKey(), ABORT, 1);
                                acksNeeded.get(node.getSession()).add(vote.getKey());
                                c++;
                            }
                        }
                        voteMap.getValue().clear();
                        if (node.getDieAfter().equals("result")) {
                            die(node.getSession());
                            return;
                        }
                        // if no acks are necessary to write END
                        if (c == 0) {
                            writeRecord(node.getSession(), END);
                        }
                    }
                }
            }
        }
    }

    /**
     * Regularly checks if all acknowledgements arrived
     * If yes, writes END
     */
    @Scheduled(fixedRate = 1000)
    public void allAcks() {
        for (Map.Entry<String, List<String>> e : acksNeeded.entrySet()) {
            Node node = getNode(e.getKey());
            if (node != null && acksReceived.containsKey(node.getSession())) {
                if (node.getActive() && node.getIsCoordinator()) {
                    Data lastData = getLastDataEntry(node.getSession());
                    if (lastData != null) {
                        String lastMsg = lastData.getMessage();
                        if (lastMsg != null && (lastMsg.equalsIgnoreCase(COMMIT) || lastMsg.equalsIgnoreCase(ABORT))) {
                            if (acksReceived.get(node.getSession()).size() > 0
                                    && acksReceived.get(node.getSession()).size() == e.getValue().size()) {
                                timer.get(node.getSession()).cancel();
                                writeLog(node.getSession(), "Received ACK from all subordinates");
                                writeRecord(node.getSession(), END);
                                acksReceived.get(node.getSession()).clear();
                                acksNeeded.get(node.getSession()).clear();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Regularly processes arrived messages from a buffer
     * Calls the respective message handler
     */
    @Scheduled(fixedRate = 500)
    public void handleMessage() {
        for (Map.Entry<String, List<Data>> messages : bufferMessages.entrySet()) {
            if (messages.getValue().size() > 0) {
                Node node = getNode(messages.getKey());
                if (node != null && node.getActive()) {
                    Data data = messages.getValue().remove(0);
                    writeReceiveLog(node.getSession(), data.getMessage(), data.getNode());

                    switch (data.getMessage().toUpperCase()) {
                        case PREPARE:
                            handlePrepare(node);
                            break;
                        case YES:
                        case NO:
                            handleVote(node, data);
                            break;
                        case COMMIT:
                            handleCommit(node);
                            break;
                        case ABORT:
                            handleAbort(node);
                            break;
                        case ACK:
                            handleAck(node, data);
                            break;
                    }
                }
            }
        }
    }

    public void clearData(String session) {
        dataRepository.deleteAllBySession(session);
        dataRepository.flush();
        if (bufferMessages.containsKey(session)) {
            bufferMessages.clear();
        }
        if (votes.containsKey(session)) {
            votes.clear();
        }
        if (acksNeeded.containsKey(session)) {
            acksNeeded.get(session).clear();
        }
        if (acksReceived.containsKey(session)) {
            acksReceived.clear();
        }
        if (timer.containsKey(session)) {
            timer.get(session).cancel();
        }
    }

    /**
     * Activates the node and starts the transaction by sending out PREPAREs
     */
    public void startTransaction(String session) {
        Data startMessage = new Data();
        startMessage.setIsStatus(true);
        startMessage.setMessage("Received start command from client");
        startMessage.setSession(session);
        saveData(startMessage);

        Node node = getNode(session);
        node.setActive(true);
        nodeService.saveNode(node);

        for (String s : getSubordinates()) {
            writeSendLog(node.getSession(), PREPARE, s);
            sendMessage(session, s, PREPARE, 1);
        }

        if (node.getDieAfter().equals("prepare")) {
            die(node.getSession());
            return;
        }
        startTimer(session, voteTimer, "Not all votes received");
    }

    public synchronized void receiveMessage(String session, Data data) {
        initMessageBuffer(session);
        bufferMessages.get(session).add(data);
    }

    private void handlePrepare(Node node) {
        String msg;

        if (node.getVote()) {
            writeRecord(node.getSession(), PREPARE);
            msg = YES;
        }
        else {
            writeRecord(node.getSession(), ABORT);
            msg = NO;
        }

        if (node.getDieAfter().equals("prepare")) {
            die(node.getSession());
            return;
        }

        writeSendLog(node.getSession(), msg, node.getCoordinator());
        sendMessage(node.getSession(), node.getCoordinator(), msg, 1);

        if (msg.equals(YES)) {
            startTimer(node.getSession(), responseTimer, "No response after vote");
        }

        if (node.getDieAfter().equals("vote")) {
            die(node.getSession());
        }
    }

    private void handleVote(Node node, Data data) {
        initVotes(node.getSession());
        votes.get(node.getSession()).put(data.getNode(), data.getMessage());
    }

    private void handleCommit(Node node) {
        if (timer.containsKey(node.getSession())) {
            timer.get(node.getSession()).cancel();
        }
        writeRecord(node.getSession(), COMMIT);

        if (node.getDieAfter().equals("commit/abort")) {
            die(node.getSession());
            return;
        }

        writeSendLog(node.getSession(), ACK, node.getCoordinator());
        sendMessage(node.getSession(), node.getCoordinator(), ACK, 1);
    }

    private void handleAbort(Node node) {
        if (timer.containsKey(node.getSession())) {
            timer.get(node.getSession()).cancel();
        }
        writeRecord(node.getSession(), ABORT);

        if (node.getDieAfter().equals("commit/abort")) {
            die(node.getSession());
        }

        writeSendLog(node.getSession(), ACK, node.getCoordinator());
        sendMessage(node.getSession(), node.getCoordinator(), ACK, 1);
    }

    private void handleAck(Node node, Data data) {
        initAcks(node.getSession());
        acksReceived.get(node.getSession()).add(data.getNode());
    }

    /**
     * Recovery process that is called from timers
     */
    public void startRecovery(String session) {
        Node node = getNode(session);
        node.setActive(true);
        nodeService.saveNode(node);

        Data lastData = getLastDataEntry(session);
        if (lastData == null) {
            writeRecord(session, ABORT);
            startEndTimer(session,10000);
            return;
        }
        String lastMsg = lastData.getMessage();
        if (lastMsg.equalsIgnoreCase(PREPARE)) {
            writeSendLog(session, "INQURY", node.getCoordinator());
            sendInquiry(session, node.getCoordinator(), 1);
            startTimer(session, responseTimer, "No response after inquiry");
        }
        else if ((lastMsg.equalsIgnoreCase(COMMIT) || lastMsg.equalsIgnoreCase(ABORT)) &&
                node.getIsCoordinator()) {
            initAcks(session);
            for (String sub : node.getSubordinates()) {
                if (!acksReceived.get(session).contains(sub)) {
                    writeSendLog(session, lastMsg, sub);
                    sendMessage(session, sub, lastMsg, 1);
                    if (!acksNeeded.get(session).contains(sub)) {
                        acksNeeded.get(session).add(sub);
                    }
                }
            }
            startTimer(session, ackTimer, "Not all acknowledgements received");
        }
    }

    /**
     * Handle inquiries by resending the last state
     */
    public void handleInquiry(String session, String sender, int transId) {
        writeReceiveLog(session, "INQUIRY", sender);
        Data lastData = getLastDataEntry(session);
        if (lastData == null) {
            initVotes(session);
            votes.get(session).put(sender, null);
            return;
        }
        String lastMsg = lastData.getMessage();
        if (lastMsg.equalsIgnoreCase(COMMIT) || lastMsg.equalsIgnoreCase(ABORT)) {
            writeSendLog(session, lastMsg, sender);
            sendMessage(session, sender, lastMsg, transId);
            initAcks(session);
            if (!acksNeeded.get(session).contains(sender)) {
                acksNeeded.get(session).add(sender);
            }
        }
        else {
            writeSendLog(session, ABORT, sender);
            sendMessage(session, sender, ABORT, transId);
        }
    }

    public void die(String session) {
        Node node = getNode(session);
        node.setActive(false);
        node.setDieAfter("never");
        nodeService.saveNode(node);
        writeLog(session, "Node died");
        startTimer(session, respawnTimer);
    }

    public void startTimer(String session, int ms) {
        startTimer(session, ms, null);
    }

    public void startTimer(String session, int ms, String msg) {
        if (timer.containsKey(session)) {
            timer.get(session).cancel();
        }
        this.timer.put(session, new Timer());
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (msg != null) {
                    writeLog(session, msg);
                }
                writeLog(session,"Start recovery");
                startRecovery(session);
            }
        };
        this.timer.get(session).schedule(timerTask, ms);
    }

    /**
     * Special case where the coordinator aborted after recovering and received no inquries
     * Then write END
     */
    public void startEndTimer(String session, int ms) {
        if (timer.containsKey(session)) {
            timer.get(session).cancel();
        }
        this.timer.put(session, new Timer());
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                writeRecord(session, END);
            }
        };
        this.timer.get(session).schedule(timerTask, ms);
    }

    public void sendMessage(String session, String recipient, String msg, int transId) {
        Node node = getNode(session);

        MultiValueMap<String, String> message = new LinkedMultiValueMap<>();
        message.add("message", msg);
        message.add("node", node.getNode());
        message.add("coordinator", node.getCoordinator());
        message.add("transId", String.valueOf(transId));

        HttpEntity<Map> request = getRequest(message);

        try {
            restTemplate.exchange(recipient + "/message?session={session}", HttpMethod.POST, request, String.class, session);
            System.out.println("The node sent a message to " + recipient);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendInquiry(String session, String recipient, int transId) {
        Node node = getNode(session);

        MultiValueMap<String, String> message = new LinkedMultiValueMap<>();
        message.add("sender", node.getNode());
        message.add("transId", String.valueOf(transId));

        HttpEntity<Map> request = getRequest(message);

        try {
            restTemplate.exchange(recipient + "/inquiry?session={session}", HttpMethod.POST, request, String.class, session);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Data> getAllData(String session) {
        return this.dataRepository.findAllBySession(session);
    }

    public Data getLastDataEntry(String session) {
        return this.dataRepository.findTopByIsStatusFalseAndSessionOrderByIdDesc(session);
    }

    public synchronized void saveData(Data newData) {
        dataRepository.saveAndFlush(newData);
    }

    private Node getNode(String session) {
        return this.nodeRepository.findBySession(session);
    }

    private List<String> getSubordinates() {
        return nodeRepository.findTopByOrderByIdDesc().getSubordinates();
    }

    private void initMessageBuffer(String session) {
        if (!bufferMessages.containsKey(session)) {
            bufferMessages.put(session, new ArrayList<>());
        }
    }

    private void initVotes(String session) {
        if (!votes.containsKey(session)) {
            votes.put(session, new HashMap<>());
        }
    }

    private void initAcks(String session) {
        if (!acksNeeded.containsKey(session)) {
            acksNeeded.put(session, new ArrayList<>());
        }
        if (!acksReceived.containsKey(session)) {
            acksReceived.put(session, new ArrayList<>());
        }
    }

    private void writeSendLog(String session, String msg, String recipient) {
        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Sending \"%s\" to %s", msg, recipient));
        log.setSession(session);
        saveData(log);
    }

    private void writeReceiveLog(String session, String msg, String sender) {
        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(String.format("Receiving \"%s\" from %s", msg, sender));
        log.setSession(session);
        saveData(log);
    }

    private void writeLog(String session, String msg) {
        Data log = new Data();
        log.setIsStatus(true);
        log.setMessage(msg);
        log.setSession(session);
        saveData(log);
    }

    private void writeRecord(String session, String msg) {
        Data data = new Data();
        data.setIsStatus(false);
        data.setMessage(msg);
        data.setSession(session);
        saveData(data);
    }

    private HttpEntity<Map> getRequest(MultiValueMap<String, String> message) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(message.toSingleValueMap(), headers);
    }
}
