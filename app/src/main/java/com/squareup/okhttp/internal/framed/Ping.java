/*    */ package com.squareup.okhttp.internal.framed;
/*    */ 
/*    */ import java.util.concurrent.CountDownLatch;
/*    */ import java.util.concurrent.TimeUnit;
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
/*    */ public final class Ping
/*    */ {
/* 25 */   private final CountDownLatch latch = new CountDownLatch(1);
/* 26 */   private long sent = -1L;
/* 27 */   private long received = -1L;
/*    */   
/*    */ 
/*    */ 
/*    */   void send()
/*    */   {
/* 33 */     if (this.sent != -1L) throw new IllegalStateException();
/* 34 */     this.sent = System.nanoTime();
/*    */   }
/*    */   
/*    */   void receive() {
/* 38 */     if ((this.received != -1L) || (this.sent == -1L)) throw new IllegalStateException();
/* 39 */     this.received = System.nanoTime();
/* 40 */     this.latch.countDown();
/*    */   }
/*    */   
/*    */   void cancel() {
/* 44 */     if ((this.received != -1L) || (this.sent == -1L)) throw new IllegalStateException();
/* 45 */     this.received = (this.sent - 1L);
/* 46 */     this.latch.countDown();
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public long roundTripTime()
/*    */     throws InterruptedException
/*    */   {
/* 55 */     this.latch.await();
/* 56 */     return this.received - this.sent;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public long roundTripTime(long timeout, TimeUnit unit)
/*    */     throws InterruptedException
/*    */   {
/* 65 */     if (this.latch.await(timeout, unit)) {
/* 66 */       return this.received - this.sent;
/*    */     }
/* 68 */     return -2L;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\Ping.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */