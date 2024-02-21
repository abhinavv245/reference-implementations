package com.ondc.onboarding;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.rfc8032.Ed25519;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.bouncycastle.jcajce.spec.XDHParameterSpec.X25519;

public class Utils {
    public static CryptoKeyPair generateSigningKeyPair() {
        SecureRandom RANDOM = new SecureRandom();
        byte[] privateKey = new byte[Ed25519.SECRET_KEY_SIZE];
        byte[] publicKey = new byte[Ed25519.PUBLIC_KEY_SIZE];
        RANDOM.nextBytes(privateKey);
        Ed25519.generatePublicKey(privateKey, 0, publicKey, 0);
        return new CryptoKeyPair(publicKey,privateKey) ;
    }

    public static CryptoKeyPair generateEncDecKey() throws InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator kpg= KeyPairGenerator.getInstance(X25519, BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(256); // 32 Byte or 256 Bits
        KeyPair kp = kpg.generateKeyPair();
        return new CryptoKeyPair(kp.getPublic().getEncoded(),kp.getPrivate().getEncoded());
    }

    public static String fromBase64(byte[] src){
        return Base64.getEncoder().encodeToString(src);
    }

    public static String sign(byte[] privateKey,byte[] message) {
        // initialise signature variable
        byte[] signature = new byte[Ed25519.SIGNATURE_SIZE];

        // sign the received message with given private key
        Ed25519.sign(privateKey, 0, message, 0, message.length, signature, 0);
        return  fromBase64(signature);
    }

    public static byte[] encryptDecrypt(int mode, byte[] challenge_string,byte[] privateKey, byte[] publicKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyAgreement keyAgreement=KeyAgreement.getInstance(X25519, BouncyCastleProvider.PROVIDER_NAME);
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey);
        PublicKey publickey = KeyFactory.getInstance(X25519, BouncyCastleProvider.PROVIDER_NAME)
                .generatePublic(x509EncodedKeySpec);
        PrivateKey privatekey = KeyFactory.getInstance(X25519, BouncyCastleProvider.PROVIDER_NAME)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
        keyAgreement.init(privatekey);
        keyAgreement.doPhase(publickey, true);
        byte[] secret = keyAgreement.generateSecret();
        SecretKey originalKey = new SecretKeySpec(secret , 0, secret.length, "AES");
        Cipher cipher = Cipher.getInstance("AES", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(mode, originalKey);
        return cipher.doFinal(challenge_string);
    }

}
