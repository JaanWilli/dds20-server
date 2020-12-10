package dds20.repository;

import dds20.entity.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("dataRepository")
public interface DataRepository extends JpaRepository<Data, Long> {
    Data findTopByOrderByIdDesc();

    Data findByTransId(Integer transId);

    List<Data> findAllByIsStatus(Boolean status);

    Data findTopByIsStatusFalseOrderByIdDesc();
}
