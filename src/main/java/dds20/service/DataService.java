package dds20.service;

import dds20.entity.Data;
import dds20.repository.DataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data Service
 * This class is the "worker" and responsible for all functionality related to the data
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class DataService {

    private final Logger log = LoggerFactory.getLogger(DataService.class);

    private final DataRepository dataRepository;

    @Autowired
    public DataService(@Qualifier("dataRepository") DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public Data getData() {
        return this.dataRepository.findTopByOrderByIdDesc();
    }

    public Data saveData(Data newData) {
        // saves the given entity but data is only persisted in the database once flush() is called
        newData = dataRepository.save(newData);
        dataRepository.flush();

        log.debug("Created Information for Data: {}", newData);
        return newData;
    }

    public Data getDataFromTransId(Integer transId) {
        return this.dataRepository.findByTransId(transId);
    }
}
