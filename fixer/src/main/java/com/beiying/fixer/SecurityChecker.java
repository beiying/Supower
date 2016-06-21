package com.beiying.fixer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.security.auth.x500.X500Principal;

/**
 * Created by beiying on 2015/10/19.
 */
public class SecurityChecker {
    private static final String TAG = "SecurityChecker";

    private static final String SP_NAME = "_andfix";
    private static final String SP_MD5 = "_md5";
    private static final String CLASSES_DEX = "classes.dex";

    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
    private final Context mContext;

    private PublicKey mPublicKey;
    private boolean mDebuggable;

    public SecurityChecker(Context context) {
        mContext = context;
        init(mContext);
    }

    private void init(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream stream = new ByteArrayInputStream(packageInfo.signatures[0].toByteArray());
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(stream);
            mDebuggable = certificate.getSubjectX500Principal().equals(DEBUG_DN);
            mPublicKey = certificate.getPublicKey();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyApk(File path) {
        if (mDebuggable) {
            Log.d(TAG, "mDebuggable = true");
            return true;
        }

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(path);
            JarEntry entry = jarFile.getJarEntry(CLASSES_DEX);
            if (null == entry) {
                return false;
            }
            loadDigests(jarFile, entry);
            Certificate[] certificates = entry.getCertificates();
            if (certificates == null) {
                return false;
            }

            return check(path, certificates);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean check(File path, Certificate[] certificates) {
        if (certificates.length > 0) {
            for (int i = certificates.length - 1;i >= 0;i--) {
                try {
                    certificates[i].verify(mPublicKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void loadDigests(JarFile jarFile, JarEntry entry) throws IOException{
        InputStream is = null;
        try {
            is = jarFile.getInputStream(entry);
            byte[] bytes = new byte[8192];
            while (is.read(bytes) > 0) {

            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public boolean verifyOpt(File optFile) {
        String fingerprint = getFileMD5(optFile);
        String saved = getFingerprint(optFile.getName());
        if (fingerprint != null && TextUtils.equals(fingerprint, saved)) {
            return true;
        }
        return false;
    }

    public void saveOptSig(File optFile) {
        String fingerprint = getFileMD5(optFile);
        saveFingerprint(optFile.getName(), fingerprint);
    }

    private void saveFingerprint(String fileName, String md5) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(fileName + SP_MD5, md5);
        editor.commit();
    }

    private String getFingerprint(String fileName) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(fileName + SP_MD5, null);
    }

    private String getFileMD5(File optFile) {
        if (!optFile.isFile()) {
            return null;
        }

        MessageDigest digest = null;
        FileInputStream fin = null;
        byte[] buffer =new byte[8192];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            fin = new FileInputStream(optFile);
            while ((len = fin.read()) > 0) {
                digest.update(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {

            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BigInteger bigInt = new BigInteger(digest.digest());
        return bigInt.toString();
    }
}
