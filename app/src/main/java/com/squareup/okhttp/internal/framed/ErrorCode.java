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
/*    */ 
/*    */ 
/*    */ public enum ErrorCode
/*    */ {
/* 21 */   NO_ERROR(0, -1, 0), 
/*    */   
/* 23 */   PROTOCOL_ERROR(1, 1, 1), 
/*    */   
/*    */ 
/* 26 */   INVALID_STREAM(1, 2, -1), 
/*    */   
/*    */ 
/* 29 */   UNSUPPORTED_VERSION(1, 4, -1), 
/*    */   
/*    */ 
/* 32 */   STREAM_IN_USE(1, 8, -1), 
/*    */   
/*    */ 
/* 35 */   STREAM_ALREADY_CLOSED(1, 9, -1), 
/*    */   
/* 37 */   INTERNAL_ERROR(2, 6, 2), 
/*    */   
/* 39 */   FLOW_CONTROL_ERROR(3, 7, -1), 
/*    */   
/* 41 */   STREAM_CLOSED(5, -1, -1), 
/*    */   
/* 43 */   FRAME_TOO_LARGE(6, 11, -1), 
/*    */   
/* 45 */   REFUSED_STREAM(7, 3, -1), 
/*    */   
/* 47 */   CANCEL(8, 5, -1), 
/*    */   
/* 49 */   COMPRESSION_ERROR(9, -1, -1), 
/*    */   
/* 51 */   CONNECT_ERROR(10, -1, -1), 
/*    */   
/* 53 */   ENHANCE_YOUR_CALM(11, -1, -1), 
/*    */   
/* 55 */   INADEQUATE_SECURITY(12, -1, -1), 
/*    */   
/* 57 */   HTTP_1_1_REQUIRED(13, -1, -1), 
/*    */   
/* 59 */   INVALID_CREDENTIALS(-1, 10, -1);
/*    */   
/*    */   public final int httpCode;
/*    */   public final int spdyRstCode;
/*    */   public final int spdyGoAwayCode;
/*    */   
/*    */   private ErrorCode(int httpCode, int spdyRstCode, int spdyGoAwayCode) {
/* 66 */     this.httpCode = httpCode;
/* 67 */     this.spdyRstCode = spdyRstCode;
/* 68 */     this.spdyGoAwayCode = spdyGoAwayCode;
/*    */   }
/*    */   
/*    */   public static ErrorCode fromSpdy3Rst(int code) {
/* 72 */     for (ErrorCode errorCode : ) {
/* 73 */       if (errorCode.spdyRstCode == code) return errorCode;
/*    */     }
/* 75 */     return null;
/*    */   }
/*    */   
/*    */   public static ErrorCode fromHttp2(int code) {
/* 79 */     for (ErrorCode errorCode : ) {
/* 80 */       if (errorCode.httpCode == code) return errorCode;
/*    */     }
/* 82 */     return null;
/*    */   }
/*    */   
/*    */   public static ErrorCode fromSpdyGoAway(int code) {
/* 86 */     for (ErrorCode errorCode : ) {
/* 87 */       if (errorCode.spdyGoAwayCode == code) return errorCode;
/*    */     }
/* 89 */     return null;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\ErrorCode.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */