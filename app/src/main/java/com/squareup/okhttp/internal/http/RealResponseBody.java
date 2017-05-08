/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ import com.squareup.okhttp.Headers;
/*    */ import com.squareup.okhttp.MediaType;
/*    */ import com.squareup.okhttp.ResponseBody;
/*    */ import okio.BufferedSource;
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
/*    */ public final class RealResponseBody
/*    */   extends ResponseBody
/*    */ {
/*    */   private final Headers headers;
/*    */   private final BufferedSource source;
/*    */   
/*    */   public RealResponseBody(Headers headers, BufferedSource source)
/*    */   {
/* 28 */     this.headers = headers;
/* 29 */     this.source = source;
/*    */   }
/*    */   
/*    */   public MediaType contentType() {
/* 33 */     String contentType = this.headers.get("Content-Type");
/* 34 */     return contentType != null ? MediaType.parse(contentType) : null;
/*    */   }
/*    */   
/*    */   public long contentLength() {
/* 38 */     return OkHeaders.contentLength(this.headers);
/*    */   }
/*    */   
/*    */   public BufferedSource source() {
/* 42 */     return this.source;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\RealResponseBody.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */