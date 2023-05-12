package nu.itark.frosk.crypto.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
/**
 * <pre>
 *     {
 *       "id": "BTC-USD",
 *       "base_currency": "BTC",
 *       "quote_currency": "USD",
 *       "base_min_size": "0.001",
 *       "base_max_size": "280",
 *       "base_increment": "0.00000001",
 *       "quote_increment": "0.01",
 *       "display_name": "BTC/USD",
 *       "status": "online",
 *       "margin_enabled": false,
 *       "status_message": "",
 *       "min_market_funds": "5",
 *       "max_market_funds": "1000000",
 *       "post_only": false,
 *       "limit_only": false,
 *       "cancel_only": false,
 *       "type": "spot"
 *     }
 *
 *     {
 *   "products": {
 *     "product_id": "BTC-USD",
 *     "price": "140.21",
 *     "price_percentage_change_24h": "9.43%",
 *     "volume_24h": "1908432",
 *     "volume_percentage_change_24h": "9.43%",
 *     "base_increment": "0.00000001",
 *     "quote_increment": "0.00000001",
 *     "quote_min_size": "0.00000001",
 *     "quote_max_size": "1000",
 *     "base_min_size": "0.00000001",
 *     "base_max_size": "1000",
 *     "base_name": "Bitcoin",
 *     "quote_name": "US Dollar",
 *     "watched": true,
 *     "is_disabled": false,
 *     "new": true,
 *     "status": "string",
 *     "cancel_only": true,
 *     "limit_only": true,
 *     "post_only": true,
 *     "trading_disabled": false,
 *     "auction_mode": true,
 *     "product_type": "SPOT",
 *     "quote_currency_id": "USD",
 *     "base_currency_id": "BTC",
 *     "mid_market_price": "140.22",
 *     "base_display_symbol": "BTC",
 *     "quote_display_symbol": "USD"
 *   },
 *   "num_products": 100
 * }
 *
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Product {
    private String product_id;
    private String price;
    private String price_percentage_change_24h;
    private String volume_24h;
    private String volume_percentage_change_24h;
    private String base_increment;
    private String quote_increment;
    private String quote_min_size;
    private String quote_max_size;
    private String base_min_size;
    private String base_max_size;
    private String base_name;
    private String quote_name;
    private String watched;
    private String is_disabled;
    private String NEW;
    private String status;
    private String cancel_only;
    private String limit_only;
    private String post_only;
    private String trading_disabled;
    private String auction_mode;
    private String product_type;
    private String quote_currency_id;
    private String base_currency_id;
    private String mid_market_price;
    private String base_display_symbol;
    private String quote_display_symbol;
}
