package cn.edu.zzti.funqchain.util;

import cn.edu.zzti.funqchain.exception.BizException;

import java.math.BigInteger;
import java.security.MessageDigest;

public final class CryptoUtils {

    public static String hash(String content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(content.getBytes("UTF-8"));
            return bytes2Hex(messageDigest.digest());
        } catch (Exception e) {
            throw new BizException("Hash 失败！", e);
        }
    }

    private static String bytes2Hex(byte[] bytes) {
        return new BigInteger(1, bytes).toString(16);
    }
}
