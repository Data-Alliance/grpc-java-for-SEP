package io.grpc.examples.helloworld;

import foundation.icon.icx.data.Address;

public class CommonData {

    public static final String PRIVATE_KEY_STRING =
            "2d42994b2f7735bbc93a3e64381864d06747e574aa94655c516f9ad0a74eed79";

    public static final String PASSWORD = "Pa55w0rd";

    public static final String URL = "http://127.0.0.1:9000/api/v3"; //node url
    public static final String URI4testnet = "https://bicon.net.solidwallet.io/api/v3"; //testnet node uri

    // Default address to deploy score.
    public static final Address SCORE_INSTALL_ADDRESS = new Address("cx0000000000000000000000000000000000000000");

    // Default address to call Governance SCORE API.
    public static final Address GOVERNANCE_ADDRESS = new Address("cx0000000000000000000000000000000000000001");

    public static final String TOKEN_ADDRESS = "cxcc7ef86cdae93a89b6c08206a7962bcb9abb7bf4";

    public static final String ADDRESS_1 = "hxc5bdfc07a86869e345c9eec73283654df6a0559b";

}
