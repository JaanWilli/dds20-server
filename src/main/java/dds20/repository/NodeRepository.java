package dds20.repository;

import dds20.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("nodeRepository")
public interface NodeRepository extends JpaRepository<Node, Long> {
    Node findTopByOrderByIdDesc();
}
