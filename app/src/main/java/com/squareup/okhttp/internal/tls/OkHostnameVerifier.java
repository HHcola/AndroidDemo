/*     */ package com.squareup.okhttp.internal.tls;
/*     */ 
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.CertificateParsingException;
/*     */ import java.security.cert.X509Certificate;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.Locale;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import javax.net.ssl.HostnameVerifier;
/*     */ import javax.net.ssl.SSLException;
/*     */ import javax.net.ssl.SSLSession;
/*     */ import javax.security.auth.x500.X500Principal;
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
/*     */ public final class OkHostnameVerifier
/*     */   implements HostnameVerifier
/*     */ {
/*  39 */   public static final OkHostnameVerifier INSTANCE = new OkHostnameVerifier();
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
/*  52 */   private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");
/*     */   
/*     */ 
/*     */   private static final int ALT_DNS_NAME = 2;
/*     */   
/*     */   private static final int ALT_IPA_NAME = 7;
/*     */   
/*     */ 
/*     */   public boolean verify(String host, SSLSession session)
/*     */   {
/*     */     try
/*     */     {
/*  64 */       Certificate[] certificates = session.getPeerCertificates();
/*  65 */       return verify(host, (X509Certificate)certificates[0]);
/*     */     } catch (SSLException e) {}
/*  67 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public boolean verify(String host, X509Certificate certificate)
/*     */   {
/*  74 */     return verifyAsIpAddress(host) ? verifyIpAddress(host, certificate) : verifyHostName(host, certificate);
/*     */   }
/*     */   
/*     */   static boolean verifyAsIpAddress(String host) {
/*  78 */     return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private boolean verifyIpAddress(String ipAddress, X509Certificate certificate)
/*     */   {
/*  85 */     List<String> altNames = getSubjectAltNames(certificate, 7);
/*  86 */     int i = 0; for (int size = altNames.size(); i < size; i++) {
/*  87 */       if (ipAddress.equalsIgnoreCase((String)altNames.get(i))) {
/*  88 */         return true;
/*     */       }
/*     */     }
/*  91 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private boolean verifyHostName(String hostName, X509Certificate certificate)
/*     */   {
/*  98 */     hostName = hostName.toLowerCase(Locale.US);
/*  99 */     boolean hasDns = false;
/* 100 */     List<String> altNames = getSubjectAltNames(certificate, 2);
/* 101 */     int i = 0; for (int size = altNames.size(); i < size; i++) {
/* 102 */       hasDns = true;
/* 103 */       if (verifyHostName(hostName, (String)altNames.get(i))) {
/* 104 */         return true;
/*     */       }
/*     */     }
/*     */     
/* 108 */     if (!hasDns) {
/* 109 */       X500Principal principal = certificate.getSubjectX500Principal();
/*     */       
/* 111 */       String cn = new DistinguishedNameParser(principal).findMostSpecific("cn");
/* 112 */       if (cn != null) {
/* 113 */         return verifyHostName(hostName, cn);
/*     */       }
/*     */     }
/*     */     
/* 117 */     return false;
/*     */   }
/*     */   
/*     */   public static List<String> allSubjectAltNames(X509Certificate certificate) {
/* 121 */     List<String> altIpaNames = getSubjectAltNames(certificate, 7);
/* 122 */     List<String> altDnsNames = getSubjectAltNames(certificate, 2);
/* 123 */     List<String> result = new ArrayList(altIpaNames.size() + altDnsNames.size());
/* 124 */     result.addAll(altIpaNames);
/* 125 */     result.addAll(altDnsNames);
/* 126 */     return result;
/*     */   }
/*     */   
/*     */   private static List<String> getSubjectAltNames(X509Certificate certificate, int type) {
/* 130 */     List<String> result = new ArrayList();
/*     */     try {
/* 132 */       Collection<?> subjectAltNames = certificate.getSubjectAlternativeNames();
/* 133 */       if (subjectAltNames == null) {
/* 134 */         return Collections.emptyList();
/*     */       }
/* 136 */       for (Object subjectAltName : subjectAltNames) {
/* 137 */         List<?> entry = (List)subjectAltName;
/* 138 */         if ((entry != null) && (entry.size() >= 2))
/*     */         {
/*     */ 
/* 141 */           Integer altNameType = (Integer)entry.get(0);
/* 142 */           if (altNameType != null)
/*     */           {
/*     */ 
/* 145 */             if (altNameType.intValue() == type) {
/* 146 */               String altName = (String)entry.get(1);
/* 147 */               if (altName != null)
/* 148 */                 result.add(altName);
/*     */             } }
/*     */         }
/*     */       }
/* 152 */       return result;
/*     */     } catch (CertificateParsingException e) {}
/* 154 */     return Collections.emptyList();
/*     */   }
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
/*     */   private boolean verifyHostName(String hostName, String pattern)
/*     */   {
/* 168 */     if ((hostName == null) || (hostName.length() == 0) || (hostName.startsWith(".")) || 
/* 169 */       (hostName.endsWith("..")))
/*     */     {
/* 171 */       return false;
/*     */     }
/* 173 */     if ((pattern == null) || (pattern.length() == 0) || (pattern.startsWith(".")) || 
/* 174 */       (pattern.endsWith("..")))
/*     */     {
/* 176 */       return false;
/*     */     }
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
/* 188 */     if (!hostName.endsWith(".")) {
/* 189 */       hostName = hostName + '.';
/*     */     }
/* 191 */     if (!pattern.endsWith(".")) {
/* 192 */       pattern = pattern + '.';
/*     */     }
/*     */     
/*     */ 
/* 196 */     pattern = pattern.toLowerCase(Locale.US);
/*     */     
/*     */ 
/* 199 */     if (!pattern.contains("*"))
/*     */     {
/* 201 */       return hostName.equals(pattern);
/*     */     }
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
/* 215 */     if ((!pattern.startsWith("*.")) || (pattern.indexOf('*', 1) != -1))
/*     */     {
/*     */ 
/* 218 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 224 */     if (hostName.length() < pattern.length())
/*     */     {
/* 226 */       return false;
/*     */     }
/*     */     
/* 229 */     if ("*.".equals(pattern))
/*     */     {
/* 231 */       return false;
/*     */     }
/*     */     
/*     */ 
/* 235 */     String suffix = pattern.substring(1);
/* 236 */     if (!hostName.endsWith(suffix))
/*     */     {
/* 238 */       return false;
/*     */     }
/*     */     
/*     */ 
/* 242 */     int suffixStartIndexInHostName = hostName.length() - suffix.length();
/* 243 */     if ((suffixStartIndexInHostName > 0) && 
/* 244 */       (hostName.lastIndexOf('.', suffixStartIndexInHostName - 1) != -1))
/*     */     {
/* 246 */       return false;
/*     */     }
/*     */     
/*     */ 
/* 250 */     return true;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\tls\OkHostnameVerifier.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */