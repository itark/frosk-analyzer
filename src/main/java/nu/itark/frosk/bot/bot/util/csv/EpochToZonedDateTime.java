package nu.itark.frosk.bot.bot.util.csv;

import com.opencsv.bean.AbstractBeanField;
import lombok.NonNull;
import nu.itark.frosk.bot.bot.domain.ImportedTicker;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Allows to transform an epoch time to a Zoned date time.
 */
public class EpochToZonedDateTime extends AbstractBeanField<ImportedTicker> {

    /** To milliseconds. */
    public static final int MILLISECONDS = 1_000;

    @Override
    protected final Object convert(@NonNull final String value) {
        return ZonedDateTime.ofInstant(new Date(Long.parseLong(value.trim()) * MILLISECONDS).toInstant(), ZoneId.systemDefault());
    }

}
