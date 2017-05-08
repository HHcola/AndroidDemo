/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public enum CipherSuite
/*     */ {
/*  36 */   TLS_RSA_WITH_NULL_MD5("SSL_RSA_WITH_NULL_MD5", 1, 5246, 6, 10), 
/*  37 */   TLS_RSA_WITH_NULL_SHA("SSL_RSA_WITH_NULL_SHA", 2, 5246, 6, 10), 
/*  38 */   TLS_RSA_EXPORT_WITH_RC4_40_MD5("SSL_RSA_EXPORT_WITH_RC4_40_MD5", 3, 4346, 6, 10), 
/*  39 */   TLS_RSA_WITH_RC4_128_MD5("SSL_RSA_WITH_RC4_128_MD5", 4, 5246, 6, 10), 
/*  40 */   TLS_RSA_WITH_RC4_128_SHA("SSL_RSA_WITH_RC4_128_SHA", 5, 5246, 6, 10), 
/*     */   
/*     */ 
/*  43 */   TLS_RSA_EXPORT_WITH_DES40_CBC_SHA("SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", 8, 4346, 6, 10), 
/*  44 */   TLS_RSA_WITH_DES_CBC_SHA("SSL_RSA_WITH_DES_CBC_SHA", 9, 5469, 6, 10), 
/*  45 */   TLS_RSA_WITH_3DES_EDE_CBC_SHA("SSL_RSA_WITH_3DES_EDE_CBC_SHA", 10, 5246, 6, 10), 
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  52 */   TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", 17, 4346, 6, 10), 
/*  53 */   TLS_DHE_DSS_WITH_DES_CBC_SHA("SSL_DHE_DSS_WITH_DES_CBC_SHA", 18, 5469, 6, 10), 
/*  54 */   TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", 19, 5246, 6, 10), 
/*  55 */   TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA("SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", 20, 4346, 6, 10), 
/*  56 */   TLS_DHE_RSA_WITH_DES_CBC_SHA("SSL_DHE_RSA_WITH_DES_CBC_SHA", 21, 5469, 6, 10), 
/*  57 */   TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", 22, 5246, 6, 10), 
/*  58 */   TLS_DH_anon_EXPORT_WITH_RC4_40_MD5("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5", 23, 4346, 6, 10), 
/*  59 */   TLS_DH_anon_WITH_RC4_128_MD5("SSL_DH_anon_WITH_RC4_128_MD5", 24, 5246, 6, 10), 
/*  60 */   TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA", 25, 4346, 6, 10), 
/*  61 */   TLS_DH_anon_WITH_DES_CBC_SHA("SSL_DH_anon_WITH_DES_CBC_SHA", 26, 5469, 6, 10), 
/*  62 */   TLS_DH_anon_WITH_3DES_EDE_CBC_SHA("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA", 27, 5246, 6, 10), 
/*  63 */   TLS_KRB5_WITH_DES_CBC_SHA("TLS_KRB5_WITH_DES_CBC_SHA", 30, 2712, 6, Integer.MAX_VALUE), 
/*  64 */   TLS_KRB5_WITH_3DES_EDE_CBC_SHA("TLS_KRB5_WITH_3DES_EDE_CBC_SHA", 31, 2712, 6, Integer.MAX_VALUE), 
/*  65 */   TLS_KRB5_WITH_RC4_128_SHA("TLS_KRB5_WITH_RC4_128_SHA", 32, 2712, 6, Integer.MAX_VALUE), 
/*     */   
/*  67 */   TLS_KRB5_WITH_DES_CBC_MD5("TLS_KRB5_WITH_DES_CBC_MD5", 34, 2712, 6, Integer.MAX_VALUE), 
/*  68 */   TLS_KRB5_WITH_3DES_EDE_CBC_MD5("TLS_KRB5_WITH_3DES_EDE_CBC_MD5", 35, 2712, 6, Integer.MAX_VALUE), 
/*  69 */   TLS_KRB5_WITH_RC4_128_MD5("TLS_KRB5_WITH_RC4_128_MD5", 36, 2712, 6, Integer.MAX_VALUE), 
/*     */   
/*  71 */   TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA("TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA", 38, 2712, 6, Integer.MAX_VALUE), 
/*     */   
/*  73 */   TLS_KRB5_EXPORT_WITH_RC4_40_SHA("TLS_KRB5_EXPORT_WITH_RC4_40_SHA", 40, 2712, 6, Integer.MAX_VALUE), 
/*  74 */   TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5("TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5", 41, 2712, 6, Integer.MAX_VALUE), 
/*     */   
/*  76 */   TLS_KRB5_EXPORT_WITH_RC4_40_MD5("TLS_KRB5_EXPORT_WITH_RC4_40_MD5", 43, 2712, 6, Integer.MAX_VALUE), 
/*     */   
/*     */ 
/*     */ 
/*  80 */   TLS_RSA_WITH_AES_128_CBC_SHA("TLS_RSA_WITH_AES_128_CBC_SHA", 47, 5246, 6, 10), 
/*     */   
/*     */ 
/*  83 */   TLS_DHE_DSS_WITH_AES_128_CBC_SHA("TLS_DHE_DSS_WITH_AES_128_CBC_SHA", 50, 5246, 6, 10), 
/*  84 */   TLS_DHE_RSA_WITH_AES_128_CBC_SHA("TLS_DHE_RSA_WITH_AES_128_CBC_SHA", 51, 5246, 6, 10), 
/*  85 */   TLS_DH_anon_WITH_AES_128_CBC_SHA("TLS_DH_anon_WITH_AES_128_CBC_SHA", 52, 5246, 6, 10), 
/*  86 */   TLS_RSA_WITH_AES_256_CBC_SHA("TLS_RSA_WITH_AES_256_CBC_SHA", 53, 5246, 6, 10), 
/*     */   
/*     */ 
/*  89 */   TLS_DHE_DSS_WITH_AES_256_CBC_SHA("TLS_DHE_DSS_WITH_AES_256_CBC_SHA", 56, 5246, 6, 10), 
/*  90 */   TLS_DHE_RSA_WITH_AES_256_CBC_SHA("TLS_DHE_RSA_WITH_AES_256_CBC_SHA", 57, 5246, 6, 10), 
/*  91 */   TLS_DH_anon_WITH_AES_256_CBC_SHA("TLS_DH_anon_WITH_AES_256_CBC_SHA", 58, 5246, 6, 10), 
/*  92 */   TLS_RSA_WITH_NULL_SHA256("TLS_RSA_WITH_NULL_SHA256", 59, 5246, 7, 21), 
/*  93 */   TLS_RSA_WITH_AES_128_CBC_SHA256("TLS_RSA_WITH_AES_128_CBC_SHA256", 60, 5246, 7, 21), 
/*  94 */   TLS_RSA_WITH_AES_256_CBC_SHA256("TLS_RSA_WITH_AES_256_CBC_SHA256", 61, 5246, 7, 21), 
/*     */   
/*     */ 
/*  97 */   TLS_DHE_DSS_WITH_AES_128_CBC_SHA256("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", 64, 5246, 7, 21), 
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 104 */   TLS_DHE_RSA_WITH_AES_128_CBC_SHA256("TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", 103, 5246, 7, 21), 
/*     */   
/*     */ 
/* 107 */   TLS_DHE_DSS_WITH_AES_256_CBC_SHA256("TLS_DHE_DSS_WITH_AES_256_CBC_SHA256", 106, 5246, 7, 21), 
/* 108 */   TLS_DHE_RSA_WITH_AES_256_CBC_SHA256("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", 107, 5246, 7, 21), 
/* 109 */   TLS_DH_anon_WITH_AES_128_CBC_SHA256("TLS_DH_anon_WITH_AES_128_CBC_SHA256", 108, 5246, 7, 21), 
/* 110 */   TLS_DH_anon_WITH_AES_256_CBC_SHA256("TLS_DH_anon_WITH_AES_256_CBC_SHA256", 109, 5246, 7, 21), 
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 135 */   TLS_RSA_WITH_AES_128_GCM_SHA256("TLS_RSA_WITH_AES_128_GCM_SHA256", 156, 5288, 8, 21), 
/* 136 */   TLS_RSA_WITH_AES_256_GCM_SHA384("TLS_RSA_WITH_AES_256_GCM_SHA384", 157, 5288, 8, 21), 
/* 137 */   TLS_DHE_RSA_WITH_AES_128_GCM_SHA256("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", 158, 5288, 8, 21), 
/* 138 */   TLS_DHE_RSA_WITH_AES_256_GCM_SHA384("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", 159, 5288, 8, 21), 
/*     */   
/*     */ 
/* 141 */   TLS_DHE_DSS_WITH_AES_128_GCM_SHA256("TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", 162, 5288, 8, 21), 
/* 142 */   TLS_DHE_DSS_WITH_AES_256_GCM_SHA384("TLS_DHE_DSS_WITH_AES_256_GCM_SHA384", 163, 5288, 8, 21), 
/*     */   
/*     */ 
/* 145 */   TLS_DH_anon_WITH_AES_128_GCM_SHA256("TLS_DH_anon_WITH_AES_128_GCM_SHA256", 166, 5288, 8, 21), 
/* 146 */   TLS_DH_anon_WITH_AES_256_GCM_SHA384("TLS_DH_anon_WITH_AES_256_GCM_SHA384", 167, 5288, 8, 21), 
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 177 */   TLS_EMPTY_RENEGOTIATION_INFO_SCSV("TLS_EMPTY_RENEGOTIATION_INFO_SCSV", 255, 5746, 6, 14), 
/* 178 */   TLS_ECDH_ECDSA_WITH_NULL_SHA("TLS_ECDH_ECDSA_WITH_NULL_SHA", 49153, 4492, 7, 14), 
/* 179 */   TLS_ECDH_ECDSA_WITH_RC4_128_SHA("TLS_ECDH_ECDSA_WITH_RC4_128_SHA", 49154, 4492, 7, 14), 
/* 180 */   TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA("TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", 49155, 4492, 7, 14), 
/* 181 */   TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", 49156, 4492, 7, 14), 
/* 182 */   TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA", 49157, 4492, 7, 14), 
/* 183 */   TLS_ECDHE_ECDSA_WITH_NULL_SHA("TLS_ECDHE_ECDSA_WITH_NULL_SHA", 49158, 4492, 7, 14), 
/* 184 */   TLS_ECDHE_ECDSA_WITH_RC4_128_SHA("TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", 49159, 4492, 7, 14), 
/* 185 */   TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA("TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", 49160, 4492, 7, 14), 
/* 186 */   TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", 49161, 4492, 7, 14), 
/* 187 */   TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", 49162, 4492, 7, 14), 
/* 188 */   TLS_ECDH_RSA_WITH_NULL_SHA("TLS_ECDH_RSA_WITH_NULL_SHA", 49163, 4492, 7, 14), 
/* 189 */   TLS_ECDH_RSA_WITH_RC4_128_SHA("TLS_ECDH_RSA_WITH_RC4_128_SHA", 49164, 4492, 7, 14), 
/* 190 */   TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA("TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", 49165, 4492, 7, 14), 
/* 191 */   TLS_ECDH_RSA_WITH_AES_128_CBC_SHA("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", 49166, 4492, 7, 14), 
/* 192 */   TLS_ECDH_RSA_WITH_AES_256_CBC_SHA("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA", 49167, 4492, 7, 14), 
/* 193 */   TLS_ECDHE_RSA_WITH_NULL_SHA("TLS_ECDHE_RSA_WITH_NULL_SHA", 49168, 4492, 7, 14), 
/* 194 */   TLS_ECDHE_RSA_WITH_RC4_128_SHA("TLS_ECDHE_RSA_WITH_RC4_128_SHA", 49169, 4492, 7, 14), 
/* 195 */   TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA("TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", 49170, 4492, 7, 14), 
/* 196 */   TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", 49171, 4492, 7, 14), 
/* 197 */   TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", 49172, 4492, 7, 14), 
/* 198 */   TLS_ECDH_anon_WITH_NULL_SHA("TLS_ECDH_anon_WITH_NULL_SHA", 49173, 4492, 7, 14), 
/* 199 */   TLS_ECDH_anon_WITH_RC4_128_SHA("TLS_ECDH_anon_WITH_RC4_128_SHA", 49174, 4492, 7, 14), 
/* 200 */   TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA("TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA", 49175, 4492, 7, 14), 
/* 201 */   TLS_ECDH_anon_WITH_AES_128_CBC_SHA("TLS_ECDH_anon_WITH_AES_128_CBC_SHA", 49176, 4492, 7, 14), 
/* 202 */   TLS_ECDH_anon_WITH_AES_256_CBC_SHA("TLS_ECDH_anon_WITH_AES_256_CBC_SHA", 49177, 4492, 7, 14), 
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 212 */   TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", 49187, 5289, 7, 21), 
/* 213 */   TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", 49188, 5289, 7, 21), 
/* 214 */   TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", 49189, 5289, 7, 21), 
/* 215 */   TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384", 49190, 5289, 7, 21), 
/* 216 */   TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", 49191, 5289, 7, 21), 
/* 217 */   TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", 49192, 5289, 7, 21), 
/* 218 */   TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", 49193, 5289, 7, 21), 
/* 219 */   TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384", 49194, 5289, 7, 21), 
/* 220 */   TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", 49195, 5289, 8, 21), 
/* 221 */   TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", 49196, 5289, 8, 21), 
/* 222 */   TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256("TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", 49197, 5289, 8, 21), 
/* 223 */   TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384("TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384", 49198, 5289, 8, 21), 
/* 224 */   TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", 49199, 5289, 8, 21), 
/* 225 */   TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", 49200, 5289, 8, 21), 
/* 226 */   TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256("TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256", 49201, 5289, 8, 21), 
/* 227 */   TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384("TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384", 49202, 5289, 8, 21);
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   final String javaName;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private CipherSuite(String javaName, int value, int rfc, int sinceJavaVersion, int sinceAndroidVersion)
/*     */   {
/* 367 */     this.javaName = javaName;
/*     */   }
/*     */   
/*     */ 
/*     */   public static CipherSuite forJavaName(String javaName)
/*     */   {
/* 373 */     return javaName.startsWith("SSL_") ? valueOf("TLS_" + javaName.substring(4)) : valueOf(javaName);
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\CipherSuite.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */