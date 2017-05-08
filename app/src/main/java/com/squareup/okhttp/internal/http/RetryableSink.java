/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ import com.squareup.okhttp.internal.Util;
/*    */ import java.io.IOException;
/*    */ import java.net.ProtocolException;
/*    */ import okio.Buffer;
/*    */ import okio.Sink;
/*    */ import okio.Timeout;
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
/*    */ public final class RetryableSink
/*    */   implements Sink
/*    */ {
/*    */   private boolean closed;
/*    */   private final int limit;
/* 35 */   private final Buffer content = new Buffer();
/*    */   
/*    */   public RetryableSink(int limit) {
/* 38 */     this.limit = limit;
/*    */   }
/*    */   
/*    */   public RetryableSink() {
/* 42 */     this(-1);
/*    */   }
/*    */   
/*    */   public void close() throws IOException {
/* 46 */     if (this.closed) return;
/* 47 */     this.closed = true;
/* 48 */     if (this.content.size() < this.limit)
/*    */     {
/* 50 */       throw new ProtocolException("content-length promised " + this.limit + " bytes, but received " + this.content.size());
/*    */     }
/*    */   }
/*    */   
/*    */   public void write(Buffer source, long byteCount) throws IOException {
/* 55 */     if (this.closed) throw new IllegalStateException("closed");
/* 56 */     Util.checkOffsetAndCount(source.size(), 0L, byteCount);
/* 57 */     if ((this.limit != -1) && (this.content.size() > this.limit - byteCount)) {
/* 58 */       throw new ProtocolException("exceeded content-length limit of " + this.limit + " bytes");
/*    */     }
/* 60 */     this.content.write(source, byteCount);
/*    */   }
/*    */   
/*    */   public void flush() throws IOException
/*    */   {}
/*    */   
/*    */   public Timeout timeout() {
/* 67 */     return Timeout.NONE;
/*    */   }
/*    */   
/*    */   public long contentLength() throws IOException {
/* 71 */     return this.content.size();
/*    */   }
/*    */   
/*    */   public void writeToSocket(Sink socketOut) throws IOException
/*    */   {
/* 76 */     Buffer buffer = new Buffer();
/* 77 */     this.content.copyTo(buffer, 0L, this.content.size());
/* 78 */     socketOut.write(buffer, buffer.size());
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\RetryableSink.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */