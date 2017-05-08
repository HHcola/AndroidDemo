/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.util.Arrays;
/*     */ import java.util.List;
/*     */ import javax.net.ssl.SSLSocket;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class ConnectionSpec
/*     */ {
/*  43 */   private static final CipherSuite[] APPROVED_CIPHER_SUITES = { CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA };
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  64 */   public static final ConnectionSpec MODERN_TLS = new Builder(true)
/*  65 */     .cipherSuites(APPROVED_CIPHER_SUITES)
/*  66 */     .tlsVersions(new TlsVersion[] { TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0 })
/*  67 */     .supportsTlsExtensions(true)
/*  68 */     .build();
/*     */   
/*     */ 
/*  71 */   public static final ConnectionSpec COMPATIBLE_TLS = new Builder(MODERN_TLS)
/*  72 */     .tlsVersions(new TlsVersion[] { TlsVersion.TLS_1_0 })
/*  73 */     .supportsTlsExtensions(true)
/*  74 */     .build();
/*     */   
/*     */ 
/*  77 */   public static final ConnectionSpec CLEARTEXT = new Builder(false).build();
/*     */   private final boolean tls;
/*     */   private final boolean supportsTlsExtensions;
/*     */   private final String[] cipherSuites;
/*     */   private final String[] tlsVersions;
/*     */   
/*     */   private ConnectionSpec(Builder builder)
/*     */   {
/*  85 */     this.tls = builder.tls;
/*  86 */     this.cipherSuites = builder.cipherSuites;
/*  87 */     this.tlsVersions = builder.tlsVersions;
/*  88 */     this.supportsTlsExtensions = builder.supportsTlsExtensions;
/*     */   }
/*     */   
/*     */   public boolean isTls() {
/*  92 */     return this.tls;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<CipherSuite> cipherSuites()
/*     */   {
/* 100 */     if (this.cipherSuites == null) { return null;
/*     */     }
/* 102 */     CipherSuite[] result = new CipherSuite[this.cipherSuites.length];
/* 103 */     for (int i = 0; i < this.cipherSuites.length; i++) {
/* 104 */       result[i] = CipherSuite.forJavaName(this.cipherSuites[i]);
/*     */     }
/* 106 */     return Util.immutableList(result);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<TlsVersion> tlsVersions()
/*     */   {
/* 114 */     if (this.tlsVersions == null) { return null;
/*     */     }
/* 116 */     TlsVersion[] result = new TlsVersion[this.tlsVersions.length];
/* 117 */     for (int i = 0; i < this.tlsVersions.length; i++) {
/* 118 */       result[i] = TlsVersion.forJavaName(this.tlsVersions[i]);
/*     */     }
/* 120 */     return Util.immutableList(result);
/*     */   }
/*     */   
/*     */   public boolean supportsTlsExtensions() {
/* 124 */     return this.supportsTlsExtensions;
/*     */   }
/*     */   
/*     */   void apply(SSLSocket sslSocket, boolean isFallback)
/*     */   {
/* 129 */     ConnectionSpec specToApply = supportedSpec(sslSocket, isFallback);
/*     */     
/* 131 */     if (specToApply.tlsVersions != null) {
/* 132 */       sslSocket.setEnabledProtocols(specToApply.tlsVersions);
/*     */     }
/* 134 */     if (specToApply.cipherSuites != null) {
/* 135 */       sslSocket.setEnabledCipherSuites(specToApply.cipherSuites);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private ConnectionSpec supportedSpec(SSLSocket sslSocket, boolean isFallback)
/*     */   {
/* 146 */     String[] cipherSuitesIntersection = this.cipherSuites != null ? (String[])Util.intersect(String.class, this.cipherSuites, sslSocket.getEnabledCipherSuites()) : sslSocket.getEnabledCipherSuites();
/*     */     
/*     */ 
/* 149 */     String[] tlsVersionsIntersection = this.tlsVersions != null ? (String[])Util.intersect(String.class, this.tlsVersions, sslSocket.getEnabledProtocols()) : sslSocket.getEnabledProtocols();
/*     */     
/*     */ 
/*     */ 
/* 153 */     if ((isFallback) && (Util.contains(sslSocket.getSupportedCipherSuites(), "TLS_FALLBACK_SCSV"))) {
/* 154 */       cipherSuitesIntersection = Util.concat(cipherSuitesIntersection, "TLS_FALLBACK_SCSV");
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 160 */     return new Builder(this).cipherSuites(cipherSuitesIntersection).tlsVersions(tlsVersionsIntersection).build();
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
/*     */ 
/*     */   public boolean isCompatible(SSLSocket socket)
/*     */   {
/* 175 */     if (!this.tls) {
/* 176 */       return false;
/*     */     }
/*     */     
/* 179 */     if ((this.tlsVersions != null) && 
/* 180 */       (!nonEmptyIntersection(this.tlsVersions, socket.getEnabledProtocols()))) {
/* 181 */       return false;
/*     */     }
/*     */     
/* 184 */     if ((this.cipherSuites != null) && 
/* 185 */       (!nonEmptyIntersection(this.cipherSuites, socket.getEnabledCipherSuites()))) {
/* 186 */       return false;
/*     */     }
/*     */     
/* 189 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private static boolean nonEmptyIntersection(String[] a, String[] b)
/*     */   {
/* 198 */     if ((a == null) || (b == null) || (a.length == 0) || (b.length == 0)) {
/* 199 */       return false;
/*     */     }
/* 201 */     for (String toFind : a) {
/* 202 */       if (Util.contains(b, toFind)) {
/* 203 */         return true;
/*     */       }
/*     */     }
/* 206 */     return false;
/*     */   }
/*     */   
/*     */   public boolean equals(Object other) {
/* 210 */     if (!(other instanceof ConnectionSpec)) return false;
/* 211 */     if (other == this) { return true;
/*     */     }
/* 213 */     ConnectionSpec that = (ConnectionSpec)other;
/* 214 */     if (this.tls != that.tls) { return false;
/*     */     }
/* 216 */     if (this.tls) {
/* 217 */       if (!Arrays.equals(this.cipherSuites, that.cipherSuites)) return false;
/* 218 */       if (!Arrays.equals(this.tlsVersions, that.tlsVersions)) return false;
/* 219 */       if (this.supportsTlsExtensions != that.supportsTlsExtensions) { return false;
/*     */       }
/*     */     }
/* 222 */     return true;
/*     */   }
/*     */   
/*     */   public int hashCode() {
/* 226 */     int result = 17;
/* 227 */     if (this.tls) {
/* 228 */       result = 31 * result + Arrays.hashCode(this.cipherSuites);
/* 229 */       result = 31 * result + Arrays.hashCode(this.tlsVersions);
/* 230 */       result = 31 * result + (this.supportsTlsExtensions ? 0 : 1);
/*     */     }
/* 232 */     return result;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 236 */     if (!this.tls) {
/* 237 */       return "ConnectionSpec()";
/*     */     }
/*     */     
/* 240 */     String cipherSuitesString = this.cipherSuites != null ? cipherSuites().toString() : "[all enabled]";
/* 241 */     String tlsVersionsString = this.tlsVersions != null ? tlsVersions().toString() : "[all enabled]";
/* 242 */     return "ConnectionSpec(cipherSuites=" + cipherSuitesString + ", tlsVersions=" + tlsVersionsString + ", supportsTlsExtensions=" + this.supportsTlsExtensions + ")";
/*     */   }
/*     */   
/*     */ 
/*     */   public static final class Builder
/*     */   {
/*     */     private boolean tls;
/*     */     
/*     */     private String[] cipherSuites;
/*     */     private String[] tlsVersions;
/*     */     private boolean supportsTlsExtensions;
/*     */     
/*     */     Builder(boolean tls)
/*     */     {
/* 256 */       this.tls = tls;
/*     */     }
/*     */     
/*     */     public Builder(ConnectionSpec connectionSpec) {
/* 260 */       this.tls = connectionSpec.tls;
/* 261 */       this.cipherSuites = connectionSpec.cipherSuites;
/* 262 */       this.tlsVersions = connectionSpec.tlsVersions;
/* 263 */       this.supportsTlsExtensions = connectionSpec.supportsTlsExtensions;
/*     */     }
/*     */     
/*     */     public Builder allEnabledCipherSuites() {
/* 267 */       if (!this.tls) throw new IllegalStateException("no cipher suites for cleartext connections");
/* 268 */       this.cipherSuites = null;
/* 269 */       return this;
/*     */     }
/*     */     
/*     */     public Builder cipherSuites(CipherSuite... cipherSuites) {
/* 273 */       if (!this.tls) { throw new IllegalStateException("no cipher suites for cleartext connections");
/*     */       }
/* 275 */       String[] strings = new String[cipherSuites.length];
/* 276 */       for (int i = 0; i < cipherSuites.length; i++) {
/* 277 */         strings[i] = cipherSuites[i].javaName;
/*     */       }
/* 279 */       return cipherSuites(strings);
/*     */     }
/*     */     
/*     */     public Builder cipherSuites(String... cipherSuites) {
/* 283 */       if (!this.tls) { throw new IllegalStateException("no cipher suites for cleartext connections");
/*     */       }
/* 285 */       if (cipherSuites.length == 0) {
/* 286 */         throw new IllegalArgumentException("At least one cipher suite is required");
/*     */       }
/*     */       
/* 289 */       this.cipherSuites = ((String[])cipherSuites.clone());
/* 290 */       return this;
/*     */     }
/*     */     
/*     */     public Builder allEnabledTlsVersions() {
/* 294 */       if (!this.tls) throw new IllegalStateException("no TLS versions for cleartext connections");
/* 295 */       this.tlsVersions = null;
/* 296 */       return this;
/*     */     }
/*     */     
/*     */     public Builder tlsVersions(TlsVersion... tlsVersions) {
/* 300 */       if (!this.tls) { throw new IllegalStateException("no TLS versions for cleartext connections");
/*     */       }
/* 302 */       String[] strings = new String[tlsVersions.length];
/* 303 */       for (int i = 0; i < tlsVersions.length; i++) {
/* 304 */         strings[i] = tlsVersions[i].javaName;
/*     */       }
/*     */       
/* 307 */       return tlsVersions(strings);
/*     */     }
/*     */     
/*     */     public Builder tlsVersions(String... tlsVersions) {
/* 311 */       if (!this.tls) { throw new IllegalStateException("no TLS versions for cleartext connections");
/*     */       }
/* 313 */       if (tlsVersions.length == 0) {
/* 314 */         throw new IllegalArgumentException("At least one TLS version is required");
/*     */       }
/*     */       
/* 317 */       this.tlsVersions = ((String[])tlsVersions.clone());
/* 318 */       return this;
/*     */     }
/*     */     
/*     */     public Builder supportsTlsExtensions(boolean supportsTlsExtensions) {
/* 322 */       if (!this.tls) throw new IllegalStateException("no TLS extensions for cleartext connections");
/* 323 */       this.supportsTlsExtensions = supportsTlsExtensions;
/* 324 */       return this;
/*     */     }
/*     */     
/*     */     public ConnectionSpec build() {
/* 328 */       return new ConnectionSpec(this, null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\ConnectionSpec.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */