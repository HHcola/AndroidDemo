/*    */ package com.squareup.okhttp.internal.framed;
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
/*    */ public enum HeadersMode
/*    */ {
/* 19 */   SPDY_SYN_STREAM, 
/* 20 */   SPDY_REPLY, 
/* 21 */   SPDY_HEADERS, 
/* 22 */   HTTP_20_HEADERS;
/*    */   
/*    */   private HeadersMode() {}
/*    */   
/* 26 */   public boolean failIfStreamAbsent() { return (this == SPDY_REPLY) || (this == SPDY_HEADERS); }
/*    */   
/*    */ 
/*    */   public boolean failIfStreamPresent()
/*    */   {
/* 31 */     return this == SPDY_SYN_STREAM;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public boolean failIfHeadersAbsent()
/*    */   {
/* 39 */     return this == SPDY_HEADERS;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public boolean failIfHeadersPresent()
/*    */   {
/* 47 */     return this == SPDY_REPLY;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\HeadersMode.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */