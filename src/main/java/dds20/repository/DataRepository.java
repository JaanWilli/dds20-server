package dds20.repository;

import dds20.entity.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository("dataRepository")
public interface DataRepository extends JpaRepository<Data, Long> {
    Data findTopByOrderByIdDesc();

    Data findByTransId(Integer transId);

    List<Data> findAllByIsStatus(Boolean status);

    Data findTopByIsStatusFalseOrderByIdDesc();

    Data findTopByIsStatusFalseAndSessionOrderByIdDesc(String session);

    List<Data> findAllBySession(String session);

    @Transactional
    void deleteAllBySession(String session);
}
