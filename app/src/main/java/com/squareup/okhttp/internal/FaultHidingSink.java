/*    */ package com.squareup.okhttp.internal;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import okio.Buffer;
/*    */ import okio.ForwardingSink;
/*    */ 
/*    */ class FaultHidingSink extends ForwardingSink
/*    */ {
/*    */   private boolean hasErrors;
/*    */   
/*    */   public FaultHidingSink(okio.Sink delegate)
/*    */   {
/* 13 */     super(delegate);
/*    */   }
/*    */   
/*    */   public void write(Buffer source, long byteCount) throws IOException {
/* 17 */     if (this.hasErrors) {
/* 18 */       source.skip(byteCount);
/* 19 */       return;
/*    */     }
/*    */     try {
/* 22 */       super.write(source, byteCount);
/*    */     } catch (IOException e) {
/* 24 */       this.hasErrors = true;
/* 25 */       onException(e);
/*    */     }
/*    */   }
/*    */   
/*    */   public void flush() throws IOException {
/* 30 */     if (this.hasErrors) return;
/*    */     try {
/* 32 */       super.flush();
/*    */     } catch (IOException e) {
/* 34 */       this.hasErrors = true;
/* 35 */       onException(e);
/*    */     }
/*    */   }
/*    */   
/*    */   public void close() throws IOException {
/* 40 */     if (this.hasErrors) return;
/*    */     try {
/* 42 */       super.close();
/*    */     } catch (IOException e) {
/* 44 */       this.hasErrors = true;
/* 45 */       onException(e);
/*    */     }
/*    */   }
/*    */   
/*    */   protected void onException(IOException e) {}
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\FaultHidingSink.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */