package test;

import crypto.AESUtil;

public class GenerateMasterKey {
    public static void main(String[] args) throws Exception {
        javax.crypto.SecretKey key = AESUtil.generateKey();
        String base64 = AESUtil.encodeKey(key);
        System.out.println("🔑 Copiez cette clé dans DatabaseEncryption.java :");
        System.out.println(base64);
    }
}