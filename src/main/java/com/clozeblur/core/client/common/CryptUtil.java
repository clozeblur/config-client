package com.clozeblur.core.client.common;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

/**
 * @author clozeblur
 * <html>数据库解密</html>
 */
public class CryptUtil {
    /**
     * 统一处理参数
     * @param source 配置项集合
     */
    @SuppressWarnings("unchecked")
    public static void decryptProperty(Map source){
        if(source != null) {
            Set propertyNames = source.keySet();
            for (Object key : propertyNames) {
                if (CryptUtil.accept(key)) {
                    Object original = source.get(key);
                    String value = original == null ? "" : String.valueOf(original);
                    String newValue = CryptUtil.decrypt(value);
                    if (!value.equals(newValue)) {
                        source.put(key, newValue);
                    }
                }

            }
        }
    }

    /**
     * 检测key值是否符合解密约定规则
     * @param key key
     * @return 成功/失败
     */
    public static boolean accept(Object key){
        String checkKey = String.valueOf(key).toLowerCase();
        return checkKey.endsWith("password") || checkKey.endsWith("pwd");
    }

    /**
     * 解密
     * @param secret 加密串
     * @return 解密后字符
     */
    public static String decrypt(String secret) {
        if(secret == null || "".equals(secret)) return secret;
        try {
            byte[] kbytes = "TjE5MGQ5".getBytes();
            SecretKeySpec key = new SecretKeySpec(kbytes, "Blowfish");

            BigInteger n = new BigInteger(secret, 16);
            byte[] encoding = n.toByteArray();

            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decode = cipher.doFinal(encoding);
            return new String(decode);
        } catch (Exception e) {
            return secret;
        }
    }
}