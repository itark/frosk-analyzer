package nu.itark.frosk.bot.bot.test.util.junit.configuration;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Property (from application.properties).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Configuration.class)
@ExtendWith(ConfigurationExtension.class)
public @interface Property {

    /** Key. */
    String key();

    /** Value. */
    String value() default "";

}
