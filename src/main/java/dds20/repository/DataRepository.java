package dds20.repository;

import dds20.entity.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("dataRepository")
public interface DataRepository extends JpaRepository<Data, Long> {
    //Data findByName(String name);

    //Data findByUsername(String username);
}
