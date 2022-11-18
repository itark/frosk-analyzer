package nu.itark.frosk.analysis;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class IconManager {

    static String ICON_PROVIDER_URL = "https://cryptoicons.org/api/icon/";
    static String SIZE = "/20";

    public static String getIconUrl(String securityName) {
        final String coin = StringUtils.substringBefore(securityName, "-");
        return ICON_PROVIDER_URL + coin.toLowerCase(Locale.ROOT) + SIZE;
    }

}
