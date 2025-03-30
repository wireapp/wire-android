package com.wearezeta.auto.common.legalhold;

import com.wearezeta.auto.common.credentials.Credentials;

public class LegalHoldServiceSettings {

    public final static String SERVICE_BASE_URL = "https://legal-hold.integrations.zinfra.io";
    public final static String SERVICE_AUTH_TOKEN = Credentials.get("LH_SERVICE_AUTH_TOKEN");

    // If the public key is outdated, we can run the following command in terminal to receive the current key
    // openssl s_client -showcerts -servername legal-hold.integrations.zinfra.io -connect legal-hold.integrations.zinfra.io:443 2>/dev/null | openssl x509 -inform pem -pubkey -noout
    public final static String SERVICE_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyuZBqj0dQ+LTW9yPF1a/\n" +
            "HHXrVfUl7P74w3CKeBKjQuZ6zQNiGBkNq3VxSpWtidE5F8cN2NLsJjNfeCHJ/pdW\n" +
            "/SCm8AB+CwA9ROfd4KJKqpQYCdzMCUeCiHtcMuguFca1Ke9V/Uou4iI/oLPIq4xd\n" +
            "ODW9RkuSKxW3pDzsVTuSiWum84fGg2VKmMzMA1NGdhIUoCbVAd77edklQRSwjvaH\n" +
            "FCSvROVi3D0MhoG9y8KX32uI3YGQoF90bys61i/nntNNS8NoAoZGdWoBAlv3GlqT\n" +
            "0136eWNk0WatfcT880/TBP9iGC+FMds3koRu8P5NdbXg8zAQP4+/Hkd1jKs8BpTB\n" +
            "dQIDAQAB\n" +
            "-----END PUBLIC KEY-----";



}
