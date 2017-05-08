/*    */ package com.squareup.okhttp;
/*    */ 
/*    */ import java.io.IOException;
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
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public enum Protocol
/*    */ {
/* 36 */   HTTP_1_0("http/1.0"), 
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/* 45 */   HTTP_1_1("http/1.1"), 
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
/* 57 */   SPDY_3("spdy/3.1"), 
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
/* 69 */   HTTP_2("h2");
/*    */   
/*    */   private final String protocol;
/*    */   
/*    */   private Protocol(String protocol) {
/* 74 */     this.protocol = protocol;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public static Protocol get(String protocol)
/*    */     throws IOException
/*    */   {
/* 83 */     if (protocol.equals(HTTP_1_0.protocol)) return HTTP_1_0;
/* 84 */     if (protocol.equals(HTTP_1_1.protocol)) return HTTP_1_1;
/* 85 */     if (protocol.equals(HTTP_2.protocol)) return HTTP_2;
/* 86 */     if (protocol.equals(SPDY_3.protocol)) return SPDY_3;
/* 87 */     throw new IOException("Unexpected protocol: " + protocol);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public String toString()
/*    */   {
/* 95 */     return this.protocol;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Protocol.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */