package com.example.xmdownloadertest;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class MD5Tool {

    public static String md5(byte[] bytes) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(bytes);
            String md5 = byteArrayToHexString(m.digest());
            return md5;
        } catch (Exception e) {
            return null;
        }
    }

    public static String md5(byte[] bytes, int offset, int length) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(bytes,offset,length);
            String md5 = byteArrayToHexString(m.digest());
            return md5;
        } catch (Exception e) {
            return null;
        }
    }

    public static String md5(File file) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[1024 * 4]; // 4k buffer
            int l;
            while ((l = fis.read(buf, 0, buf.length)) != -1) {
                m.update(buf, 0, l);
            }
            fis.close();
            String md5 = byteArrayToHexString(m.digest());
            return md5;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder resultSb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        int n = b;
        if (n < 0) {
            n = 0x100 + n;
        }
        int d1 = n >> 4;
        int d2 = n & 0xF;
        return hexDigits[d1] + hexDigits[d2];
    }

}
