/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.security.Principal;
/*     */ import java.security.PublicKey;
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.X509Certificate;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.LinkedHashSet;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import javax.net.ssl.SSLPeerUnverifiedException;
/*     */ import okio.ByteString;
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
/*     */ public final class CertificatePinner
/*     */ {
/* 133 */   public static final CertificatePinner DEFAULT = new Builder().build();
/*     */   private final Map<String, Set<ByteString>> hostnameToPins;
/*     */   
/*     */   private CertificatePinner(Builder builder)
/*     */   {
/* 138 */     this.hostnameToPins = Util.immutableMap(builder.hostnameToPins);
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
/*     */   public void check(String hostname, List<Certificate> peerCertificates)
/*     */     throws SSLPeerUnverifiedException
/*     */   {
/* 153 */     Set<ByteString> pins = findMatchingPins(hostname);
/*     */     
/* 155 */     if (pins == null) { return;
/*     */     }
/* 157 */     int i = 0; for (int size = peerCertificates.size(); i < size; i++) {
/* 158 */       X509Certificate x509Certificate = (X509Certificate)peerCertificates.get(i);
/* 159 */       if (pins.contains(sha1(x509Certificate))) { return;
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 165 */     StringBuilder message = new StringBuilder().append("Certificate pinning failure!").append("\n  Peer certificate chain:");
/* 166 */     int i = 0; for (int size = peerCertificates.size(); i < size; i++) {
/* 167 */       X509Certificate x509Certificate = (X509Certificate)peerCertificates.get(i);
/* 168 */       message.append("\n    ").append(pin(x509Certificate))
/* 169 */         .append(": ").append(x509Certificate.getSubjectDN().getName());
/*     */     }
/* 171 */     message.append("\n  Pinned certificates for ").append(hostname).append(":");
/* 172 */     for (ByteString pin : pins) {
/* 173 */       message.append("\n    sha1/").append(pin.base64());
/*     */     }
/* 175 */     throw new SSLPeerUnverifiedException(message.toString());
/*     */   }
/*     */   
/*     */   /**
/*     */    * @deprecated
/*     */    */
/* 181 */   public void check(String hostname, Certificate... peerCertificates) throws SSLPeerUnverifiedException { check(hostname, Arrays.asList(peerCertificates)); }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   Set<ByteString> findMatchingPins(String hostname)
/*     */   {
/* 189 */     Set<ByteString> directPins = (Set)this.hostnameToPins.get(hostname);
/* 190 */     Set<ByteString> wildcardPins = null;
/*     */     
/* 192 */     int indexOfFirstDot = hostname.indexOf('.');
/* 193 */     int indexOfLastDot = hostname.lastIndexOf('.');
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 198 */     if (indexOfFirstDot != indexOfLastDot)
/*     */     {
/* 200 */       wildcardPins = (Set)this.hostnameToPins.get("*." + hostname.substring(indexOfFirstDot + 1));
/*     */     }
/*     */     
/* 203 */     if ((directPins == null) && (wildcardPins == null)) { return null;
/*     */     }
/* 205 */     if ((directPins != null) && (wildcardPins != null)) {
/* 206 */       Set<ByteString> pins = new LinkedHashSet();
/* 207 */       pins.addAll(directPins);
/* 208 */       pins.addAll(wildcardPins);
/* 209 */       return pins;
/*     */     }
/*     */     
/* 212 */     if (directPins != null) { return directPins;
/*     */     }
/* 214 */     return wildcardPins;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static String pin(Certificate certificate)
/*     */   {
/* 223 */     if (!(certificate instanceof X509Certificate)) {
/* 224 */       throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
/*     */     }
/* 226 */     return "sha1/" + sha1((X509Certificate)certificate).base64();
/*     */   }
/*     */   
/*     */   private static ByteString sha1(X509Certificate x509Certificate) {
/* 230 */     return Util.sha1(ByteString.of(x509Certificate.getPublicKey().getEncoded()));
/*     */   }
/*     */   
/*     */   public static final class Builder
/*     */   {
/* 235 */     private final Map<String, Set<ByteString>> hostnameToPins = new LinkedHashMap();
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder add(String hostname, String... pins)
/*     */     {
/* 246 */       if (hostname == null) { throw new IllegalArgumentException("hostname == null");
/*     */       }
/* 248 */       Set<ByteString> hostPins = new LinkedHashSet();
/* 249 */       Set<ByteString> previousPins = (Set)this.hostnameToPins.put(hostname, Collections.unmodifiableSet(hostPins));
/* 250 */       if (previousPins != null) {
/* 251 */         hostPins.addAll(previousPins);
/*     */       }
/*     */       
/* 254 */       for (String pin : pins) {
/* 255 */         if (!pin.startsWith("sha1/")) {
/* 256 */           throw new IllegalArgumentException("pins must start with 'sha1/': " + pin);
/*     */         }
/* 258 */         ByteString decodedPin = ByteString.decodeBase64(pin.substring("sha1/".length()));
/* 259 */         if (decodedPin == null) {
/* 260 */           throw new IllegalArgumentException("pins must be base64: " + pin);
/*     */         }
/* 262 */         hostPins.add(decodedPin);
/*     */       }
/*     */       
/* 265 */       return this;
/*     */     }
/*     */     
/*     */     public CertificatePinner build() {
/* 269 */       return new CertificatePinner(this, null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\CertificatePinner.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */