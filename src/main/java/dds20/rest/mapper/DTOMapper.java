package dds20.rest.mapper;

import dds20.entity.Data;
import dds20.entity.Node;
import dds20.rest.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g., UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for creating information (POST).
 */
@Mapper
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "node", target = "node")
    @Mapping(source = "transId", target = "transId")
    @Mapping(source = "coordinator", target = "coordinator")
    @Mapping(source = "subordinates", target = "subordinates")
    @Mapping(source = "isStatus", target = "isStatus")
    DataGetDTO convertEntityToDataGetDTO(Data data);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "active", target = "active")
    @Mapping(source = "dieAfter", target = "dieAfter")
    NodeGetDTO convertEntityToNodeGetDTO(Node node);

    @Mapping(source = "isCoordinator", target = "isCoordinator")
    @Mapping(source = "isSubordinate", target = "isSubordinate")
    @Mapping(source = "coordinator", target = "coordinator")
    @Mapping(source = "subordinates", target = "subordinates")
    Node convertSetupPostDTOtoEntity(SetupPostDTO setupPostDTO);

    @Mapping(source = "message", target = "message")
    @Mapping(source = "node", target = "node")
    @Mapping(source = "transId", target = "transId")
    @Mapping(source = "coordinator", target = "coordinator")
    @Mapping(source = "subordinates", target = "subordinates")
    @Mapping(source = "isStatus", target = "isStatus")
    Data convertMessagePostDTOtoEntity(MessagePostDTO messagePostDTO);
}
