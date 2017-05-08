/*    */ package com.squareup.okhttp.internal.tls;
/*    */ 
/*    */ import java.lang.reflect.InvocationTargetException;
/*    */ import java.lang.reflect.Method;
/*    */ import java.security.cert.TrustAnchor;
/*    */ import java.security.cert.X509Certificate;
/*    */ import javax.net.ssl.X509TrustManager;
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
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class AndroidTrustRootIndex
/*    */   implements TrustRootIndex
/*    */ {
/*    */   private final X509TrustManager trustManager;
/*    */   private final Method findByIssuerAndSignatureMethod;
/*    */   
/*    */   public AndroidTrustRootIndex(X509TrustManager trustManager, Method findByIssuerAndSignatureMethod)
/*    */   {
/* 35 */     this.findByIssuerAndSignatureMethod = findByIssuerAndSignatureMethod;
/* 36 */     this.trustManager = trustManager;
/*    */   }
/*    */   
/*    */   public X509Certificate findByIssuerAndSignature(X509Certificate cert) {
/*    */     try {
/* 41 */       TrustAnchor trustAnchor = (TrustAnchor)this.findByIssuerAndSignatureMethod.invoke(this.trustManager, new Object[] { cert });
/*    */       
/*    */ 
/* 44 */       return trustAnchor != null ? trustAnchor.getTrustedCert() : null;
/*    */     }
/*    */     catch (IllegalAccessException e) {
/* 47 */       throw new AssertionError();
/*    */     } catch (InvocationTargetException e) {}
/* 49 */     return null;
/*    */   }
/*    */   
/*    */ 
/*    */   public static TrustRootIndex get(X509TrustManager trustManager)
/*    */   {
/*    */     try
/*    */     {
/* 57 */       Method method = trustManager.getClass().getDeclaredMethod("findTrustAnchorByIssuerAndSignature", new Class[] { X509Certificate.class });
/*    */       
/* 59 */       method.setAccessible(true);
/* 60 */       return new AndroidTrustRootIndex(trustManager, method);
/*    */     } catch (NoSuchMethodException e) {}
/* 62 */     return null;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\tls\AndroidTrustRootIndex.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */