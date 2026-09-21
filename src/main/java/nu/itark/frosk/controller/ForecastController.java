package nu.itark.frosk.controller;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.forecast.ProphetForecastResponse;
import nu.itark.frosk.service.DailyPnlForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Registered on all three profiles (equity, crypto, kraken-futures) — same
 * as every other controller, no {@code @Profile} restriction. Behavior is
 * gated by {@code forecast.prophet.enabled} at the service layer, not by
 * bean presence; currently only {@code application-equity.properties} turns
 * it on.
 */
@RestController
@Slf4j
public class ForecastController {

    @Autowired
    private DailyPnlForecastService dailyPnlForecastService;

    @Value("${forecast.prophet.horizon.days:14}")
    private int defaultHorizonDays;

    /**
     * @return the Prophet forecast for {@code strategy}, or an empty/insufficient
     *         placeholder (never a 4xx/5xx) when disabled or the Python service
     *         is unreachable — {@code sufficientData=false} and an empty
     *         {@code forecast} list are the caller-visible signal, not an HTTP error.
     * @Example http://localhost:8080/forecast/daily-pnl?strategy=ShortTermMomentumLongTermStrengthStrategy
     */
    @GetMapping("/forecast/daily-pnl")
    public ProphetForecastResponse getDailyPnlForecast(@RequestParam("strategy") String strategy,
                                                         @RequestParam(value = "horizon", required = false) Integer horizon) {
        int horizonDays = horizon != null ? horizon : defaultHorizonDays;
        log.info("/forecast/daily-pnl strategy={} horizon={}", strategy, horizonDays);
        return dailyPnlForecastService.getForecast(strategy, horizonDays)
                .orElseGet(() -> new ProphetForecastResponse(List.of(), horizonDays, 0, false));
    }
}
