package ch.uzh.ifi.seal.soprafs20.repository;

import ch.uzh.ifi.seal.soprafs20.entity.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("dataRepository")
public interface DataRepository extends JpaRepository<Data, Long> {
    Data findByName(String name);

    Data findByUsername(String username);
}
