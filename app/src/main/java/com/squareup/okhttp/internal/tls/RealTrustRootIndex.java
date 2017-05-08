/*    */ package com.squareup.okhttp.internal.tls;
/*    */ 
/*    */ import java.security.PublicKey;
/*    */ import java.security.cert.X509Certificate;
/*    */ import java.util.ArrayList;
/*    */ import java.util.LinkedHashMap;
/*    */ import java.util.List;
/*    */ import java.util.Map;
/*    */ import javax.security.auth.x500.X500Principal;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class RealTrustRootIndex
/*    */   implements TrustRootIndex
/*    */ {
/*    */   private final Map<X500Principal, List<X509Certificate>> subjectToCaCerts;
/*    */   
/*    */   public RealTrustRootIndex(X509Certificate... caCerts)
/*    */   {
/* 30 */     this.subjectToCaCerts = new LinkedHashMap();
/* 31 */     for (X509Certificate caCert : caCerts) {
/* 32 */       X500Principal subject = caCert.getSubjectX500Principal();
/* 33 */       List<X509Certificate> subjectCaCerts = (List)this.subjectToCaCerts.get(subject);
/* 34 */       if (subjectCaCerts == null) {
/* 35 */         subjectCaCerts = new ArrayList(1);
/* 36 */         this.subjectToCaCerts.put(subject, subjectCaCerts);
/*    */       }
/* 38 */       subjectCaCerts.add(caCert);
/*    */     }
/*    */   }
/*    */   
/*    */   public X509Certificate findByIssuerAndSignature(X509Certificate cert) {
/* 43 */     X500Principal issuer = cert.getIssuerX500Principal();
/* 44 */     List<X509Certificate> subjectCaCerts = (List)this.subjectToCaCerts.get(issuer);
/* 45 */     if (subjectCaCerts == null) { return null;
/*    */     }
/* 47 */     for (X509Certificate caCert : subjectCaCerts) {
/* 48 */       PublicKey publicKey = caCert.getPublicKey();
/*    */       try {
/* 50 */         cert.verify(publicKey);
/* 51 */         return caCert;
/*    */       }
/*    */       catch (Exception localException) {}
/*    */     }
/*    */     
/* 56 */     return null;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\tls\RealTrustRootIndex.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */