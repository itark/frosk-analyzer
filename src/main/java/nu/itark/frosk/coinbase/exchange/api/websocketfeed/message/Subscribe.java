package nu.itark.frosk.coinbase.exchange.api.websocketfeed.message;

public class Subscribe {
    public static final String HEARTBEAT = "heartbeat";
    public static final String LEVEL2 = "level2";
    public static final String FULL = "full";
	String type;
//    String[] product_ids;
   // String[] product_ids = new String[]{"BTC-EUR"}; // make this configurable.
    //String[] channels = new String[]{"level2", "heartbeat"}; // make this configurable.
    //String[] channels = new String[]{HEARTBEAT}; // make this configurable.

   // https://docs.pro.coinbase.com/#channels
    Channels[] channels;

    // Used for signing the subscribe message to the Websocket feed
    String signature;
    String passphrase;
    String timestamp;
    String apiKey;

    public Subscribe() {
        this.type = "subscribe";
    }

//    public Subscribe(String[] product_ids) {
//        this.type = "subscribe";
//        this.product_ids = product_ids;
//    }

    public Subscribe(Channels[] channels) {
        this.type = "subscribe";
        this.channels = channels;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

//    public String[] getProduct_ids() {
//        return product_ids;
//    }
//
//    public void setProduct_ids(String[] product_ids) {
//        this.product_ids = product_ids;
//    }

    public Subscribe setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public Subscribe  setPassphrase(String passphrase) {
        this.passphrase = passphrase;
        return this;
    }

    public Subscribe setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Subscribe setKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }


    /**
     * @return the signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * @return the passphrase
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * @return the timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @return the apiKey
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * @param apiKey the apiKey to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}
