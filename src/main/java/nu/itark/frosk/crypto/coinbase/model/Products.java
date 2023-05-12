package nu.itark.frosk.crypto.coinbase.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class Products {
    List<Product> products;
    int num_products;
}
