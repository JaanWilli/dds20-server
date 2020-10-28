package dds20.service;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.repository.NodeRepository;
import dds20.rest.dto.SettingsPostDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Node Service
 * This class is the "worker" and responsible for all functionality related to the node
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class NodeService {

    private final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final NodeRepository nodeRepository;

    @Autowired
    public NodeService(@Qualifier("nodeRepository") NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public Node getNode() {
        return this.nodeRepository.findTopByOrderByIdDesc();
    }

    public boolean isActive() {
        Node node = getNode();
        return (node != null) ? node.getActive() : false;
    }

    public void updateSettings(SettingsPostDTO newSettings) {
        Node node = getNode();
        node.setActive(newSettings.getActive());
        node.setDieAfter(newSettings.getDieAfter());
        node.setVote(newSettings.getVote());
        saveNode(node);
    }

    public void saveNode(Node newNode) {
        nodeRepository.saveAndFlush(newNode);
    }

    public void clearNode() {
        nodeRepository.deleteAll();
        nodeRepository.flush();
    }
}
