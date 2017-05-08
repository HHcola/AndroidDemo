/*     */ package com.squareup.okhttp.internal.tls;
/*     */ 
/*     */ import java.security.GeneralSecurityException;
/*     */ import java.security.Principal;
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.X509Certificate;
/*     */ import java.util.ArrayDeque;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Deque;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import javax.net.ssl.SSLPeerUnverifiedException;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class CertificateChainCleaner
/*     */ {
/*     */   private static final int MAX_SIGNERS = 9;
/*     */   private final TrustRootIndex trustRootIndex;
/*     */   
/*     */   public CertificateChainCleaner(TrustRootIndex trustRootIndex)
/*     */   {
/*  49 */     this.trustRootIndex = trustRootIndex;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Certificate> clean(List<Certificate> chain)
/*     */     throws SSLPeerUnverifiedException
/*     */   {
/*  60 */     Deque<Certificate> queue = new ArrayDeque(chain);
/*  61 */     List<Certificate> result = new ArrayList();
/*  62 */     result.add(queue.removeFirst());
/*  63 */     boolean foundTrustedCertificate = false;
/*     */     
/*     */     label226:
/*  66 */     for (int c = 0; c < 9; c++) {
/*  67 */       X509Certificate toVerify = (X509Certificate)result.get(result.size() - 1);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*  72 */       X509Certificate trustedCert = this.trustRootIndex.findByIssuerAndSignature(toVerify);
/*  73 */       if (trustedCert != null) {
/*  74 */         if ((result.size() > 1) || (!toVerify.equals(trustedCert))) {
/*  75 */           result.add(trustedCert);
/*     */         }
/*  77 */         if (verifySignature(trustedCert, trustedCert)) {
/*  78 */           return result;
/*     */         }
/*  80 */         foundTrustedCertificate = true;
/*     */ 
/*     */       }
/*     */       else
/*     */       {
/*     */ 
/*  86 */         for (Iterator<Certificate> i = queue.iterator(); i.hasNext();) {
/*  87 */           X509Certificate signingCert = (X509Certificate)i.next();
/*  88 */           if (verifySignature(toVerify, signingCert)) {
/*  89 */             i.remove();
/*  90 */             result.add(signingCert);
/*     */             
/*     */             break label226;
/*     */           }
/*     */         }
/*     */         
/*  96 */         if (foundTrustedCertificate) {
/*  97 */           return result;
/*     */         }
/*     */         
/*     */ 
/* 101 */         throw new SSLPeerUnverifiedException("Failed to find a trusted cert that signed " + toVerify);
/*     */       }
/*     */     }
/* 104 */     throw new SSLPeerUnverifiedException("Certificate chain too long: " + result);
/*     */   }
/*     */   
/*     */   private boolean verifySignature(X509Certificate toVerify, X509Certificate signingCert)
/*     */   {
/* 109 */     if (!toVerify.getIssuerDN().equals(signingCert.getSubjectDN())) return false;
/*     */     try {
/* 111 */       toVerify.verify(signingCert.getPublicKey());
/* 112 */       return true;
/*     */     } catch (GeneralSecurityException verifyFailed) {}
/* 114 */     return false;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\tls\CertificateChainCleaner.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */