/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.security.Principal;
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.X509Certificate;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import javax.net.ssl.SSLPeerUnverifiedException;
/*     */ import javax.net.ssl.SSLSession;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Handshake
/*     */ {
/*     */   private final String cipherSuite;
/*     */   private final List<Certificate> peerCertificates;
/*     */   private final List<Certificate> localCertificates;
/*     */   
/*     */   private Handshake(String cipherSuite, List<Certificate> peerCertificates, List<Certificate> localCertificates)
/*     */   {
/*  42 */     this.cipherSuite = cipherSuite;
/*  43 */     this.peerCertificates = peerCertificates;
/*  44 */     this.localCertificates = localCertificates;
/*     */   }
/*     */   
/*     */   public static Handshake get(SSLSession session) {
/*  48 */     String cipherSuite = session.getCipherSuite();
/*  49 */     if (cipherSuite == null) throw new IllegalStateException("cipherSuite == null");
/*     */     Certificate[] peerCertificates;
/*     */     try
/*     */     {
/*  53 */       peerCertificates = session.getPeerCertificates();
/*     */     } catch (SSLPeerUnverifiedException ignored) { Certificate[] peerCertificates;
/*  55 */       peerCertificates = null;
/*     */     }
/*     */     
/*     */ 
/*  59 */     List<Certificate> peerCertificatesList = peerCertificates != null ? Util.immutableList(peerCertificates) : Collections.emptyList();
/*     */     
/*  61 */     Certificate[] localCertificates = session.getLocalCertificates();
/*     */     
/*     */ 
/*  64 */     List<Certificate> localCertificatesList = localCertificates != null ? Util.immutableList(localCertificates) : Collections.emptyList();
/*     */     
/*  66 */     return new Handshake(cipherSuite, peerCertificatesList, localCertificatesList);
/*     */   }
/*     */   
/*     */   public static Handshake get(String cipherSuite, List<Certificate> peerCertificates, List<Certificate> localCertificates)
/*     */   {
/*  71 */     if (cipherSuite == null) { throw new IllegalArgumentException("cipherSuite == null");
/*     */     }
/*  73 */     return new Handshake(cipherSuite, Util.immutableList(peerCertificates), Util.immutableList(localCertificates));
/*     */   }
/*     */   
/*     */   public String cipherSuite()
/*     */   {
/*  78 */     return this.cipherSuite;
/*     */   }
/*     */   
/*     */   public List<Certificate> peerCertificates()
/*     */   {
/*  83 */     return this.peerCertificates;
/*     */   }
/*     */   
/*     */ 
/*     */   public Principal peerPrincipal()
/*     */   {
/*  89 */     return !this.peerCertificates.isEmpty() ? ((X509Certificate)this.peerCertificates.get(0)).getSubjectX500Principal() : null;
/*     */   }
/*     */   
/*     */ 
/*     */   public List<Certificate> localCertificates()
/*     */   {
/*  95 */     return this.localCertificates;
/*     */   }
/*     */   
/*     */ 
/*     */   public Principal localPrincipal()
/*     */   {
/* 101 */     return !this.localCertificates.isEmpty() ? ((X509Certificate)this.localCertificates.get(0)).getSubjectX500Principal() : null;
/*     */   }
/*     */   
/*     */   public boolean equals(Object other)
/*     */   {
/* 106 */     if (!(other instanceof Handshake)) return false;
/* 107 */     Handshake that = (Handshake)other;
/*     */     
/*     */ 
/* 110 */     return (this.cipherSuite.equals(that.cipherSuite)) && (this.peerCertificates.equals(that.peerCertificates)) && (this.localCertificates.equals(that.localCertificates));
/*     */   }
/*     */   
/*     */   public int hashCode() {
/* 114 */     int result = 17;
/* 115 */     result = 31 * result + this.cipherSuite.hashCode();
/* 116 */     result = 31 * result + this.peerCertificates.hashCode();
/* 117 */     result = 31 * result + this.localCertificates.hashCode();
/* 118 */     return result;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Handshake.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */