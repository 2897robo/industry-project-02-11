package com.team11.backend.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Component
public class AES256Cipher {

    private final String secretKey;
    private final String IV;

    public AES256Cipher(@Value("${aes.key}") String aesKey, @Value("${aes.iv}") String aesIv) {
        secretKey = aesKey;
        IV = aesIv;
    }

    // 편의 메서드 추가
    public String encrypt(String str) {
        try {
            return AES_Encode(str);
        } catch (Exception e) {
            log.error("암호화 실패: {}", e.getMessage());
            throw new RuntimeException("암호화 실패", e);
        }
    }

    public String decrypt(String str) {
        try {
            return AES_Decode(str);
        } catch (Exception e) {
            log.error("복호화 실패: {}", e.getMessage());
            throw new RuntimeException("복호화 실패", e);
        }
    }

    public String AES_Encode(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();

        SecretKey secureKey = new SecretKeySpec(keyData, "AES");

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes()));

        byte[] encrypted = c.doFinal(str.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String AES_Encode(byte[] str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();

        SecretKey secureKey = new SecretKeySpec(keyData, "AES");

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes()));

        byte[] encrypted = c.doFinal(str);

        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String AES_Decode(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();
        SecretKey secureKey = new SecretKeySpec(keyData, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));

        byte[] byteStr = Base64.getDecoder().decode(str.getBytes());

        return new String(c.doFinal(byteStr), StandardCharsets.UTF_8);
    }

    public String AES_Decode(byte[] str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();
        SecretKey secureKey = new SecretKeySpec(keyData, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));

        byte[] byteStr = Base64.getDecoder().decode(str);

        return new String(c.doFinal(byteStr), StandardCharsets.UTF_8);
    }

    public byte[] AES_Decode_Byte(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();
        SecretKey secureKey = new SecretKeySpec(keyData, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));

        byte[] byteStr = Base64.getDecoder().decode(str);

        return c.doFinal(byteStr);
    }

    public String AES_Encode_Manual(String str, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String iv = key.substring(0, 16);
        byte[] keyData = key.getBytes();

        log.debug("####### key = {}", key);
        log.debug("####### key.length() = {}", key.length());
        log.debug("####### iv.getBytes().length = {}", iv.getBytes().length);

        SecretKey secureKey = new SecretKeySpec(keyData, "AES");

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(iv.getBytes()));

        byte[] encrypted = c.doFinal(str.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(encrypted));
    }

    public String genAesKey() {
        Random rand = new Random();
        String[] d = {"1","2","3","4","5","6","7","8","9","0","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
        byte[] r = new byte[32];

        StringBuilder sb = new StringBuilder();

        int len = r.length;
        int idx = 0;

        for(int i=0; i < len; i++ ) {
            idx = Math.abs(rand.nextInt(61));
            sb.append( d[idx] );
        }

        return sb.toString();
    }
}
