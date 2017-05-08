/*    */ package com.squareup.okhttp.internal.framed;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import java.util.List;
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
/*    */ public abstract interface PushObserver
/*    */ {
/* 76 */   public static final PushObserver CANCEL = new PushObserver()
/*    */   {
/*    */     public boolean onRequest(int streamId, List<Header> requestHeaders) {
/* 79 */       return true;
/*    */     }
/*    */     
/*    */     public boolean onHeaders(int streamId, List<Header> responseHeaders, boolean last) {
/* 83 */       return true;
/*    */     }
/*    */     
/*    */     public boolean onData(int streamId, BufferedSource source, int byteCount, boolean last) throws IOException
/*    */     {
/* 88 */       source.skip(byteCount);
/* 89 */       return true;
/*    */     }
/*    */     
/*    */     public void onReset(int streamId, ErrorCode errorCode) {}
/*    */   };
/*    */   
/*    */   public abstract boolean onRequest(int paramInt, List<Header> paramList);
/*    */   
/*    */   public abstract boolean onHeaders(int paramInt, List<Header> paramList, boolean paramBoolean);
/*    */   
/*    */   public abstract boolean onData(int paramInt1, BufferedSource paramBufferedSource, int paramInt2, boolean paramBoolean)
/*    */     throws IOException;
/*    */   
/*    */   public abstract void onReset(int paramInt, ErrorCode paramErrorCode);
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\PushObserver.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */