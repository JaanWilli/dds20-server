package dds20.service;

import dds20.entity.Node;
import dds20.repository.NodeRepository;
import dds20.rest.dto.SettingsPostDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Node Service
 * This class is the "worker" and responsible for all functionality related to the node
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class NodeService {

    private final NodeRepository nodeRepository;

    @Autowired
    public NodeService(@Qualifier("nodeRepository") NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public Node getNode(String session) {
        return this.nodeRepository.findBySession(session);
    }

    public boolean isActive(String session) {
        Node node = getNode(session);
        return (node != null) ? node.getActive() : false;
    }

    public void updateSettings(String session, SettingsPostDTO newSettings) {
        Node node = getNode(session);
        node.setActive(newSettings.getActive());
        node.setDieAfter(newSettings.getDieAfter());
        node.setVote(newSettings.getVote());
        saveNode(node);
    }

    public void saveNode(Node newNode) {
        nodeRepository.saveAndFlush(newNode);
    }

    public void clearNode(String session) {
        this.nodeRepository.deleteBySession(session);
        this.nodeRepository.flush();
    }
}
