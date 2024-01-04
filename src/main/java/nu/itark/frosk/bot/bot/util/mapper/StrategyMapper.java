package nu.itark.frosk.bot.bot.util.mapper;

import nu.itark.frosk.bot.bot.domain.Strategy;
import nu.itark.frosk.bot.bot.dto.strategy.StrategyDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Strategy mapper.
 */
//@Mapper
public interface StrategyMapper {

    // =================================================================================================================
    // DTO to Domain.

/*
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
*/
    Strategy mapToStrategy(StrategyDTO source);

    // =================================================================================================================
    // Domain to DTO.

    StrategyDTO mapToStrategyDTO(Strategy source);

}
