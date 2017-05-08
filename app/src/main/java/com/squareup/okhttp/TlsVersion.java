/*    */ package com.squareup.okhttp;
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
/*    */ 
/*    */ 
/*    */ public enum TlsVersion
/*    */ {
/* 25 */   TLS_1_2("TLSv1.2"), 
/* 26 */   TLS_1_1("TLSv1.1"), 
/* 27 */   TLS_1_0("TLSv1"), 
/* 28 */   SSL_3_0("SSLv3");
/*    */   
/*    */   final String javaName;
/*    */   
/*    */   private TlsVersion(String javaName)
/*    */   {
/* 34 */     this.javaName = javaName;
/*    */   }
/*    */   
/*    */   public static TlsVersion forJavaName(String javaName) {
/* 38 */     switch (javaName) {
/* 39 */     case "TLSv1.2":  return TLS_1_2;
/* 40 */     case "TLSv1.1":  return TLS_1_1;
/* 41 */     case "TLSv1":  return TLS_1_0;
/* 42 */     case "SSLv3":  return SSL_3_0;
/*    */     }
/* 44 */     throw new IllegalArgumentException("Unexpected TLS version: " + javaName);
/*    */   }
/*    */   
/*    */   public String javaName() {
/* 48 */     return this.javaName;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\TlsVersion.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */