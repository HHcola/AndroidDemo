/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.http.HttpEngine;
/*     */ import java.util.ArrayDeque;
/*     */ import java.util.Deque;
/*     */ import java.util.Iterator;
/*     */ import java.util.concurrent.ExecutorService;
/*     */ import java.util.concurrent.SynchronousQueue;
/*     */ import java.util.concurrent.ThreadPoolExecutor;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Dispatcher
/*     */ {
/*  37 */   private int maxRequests = 64;
/*  38 */   private int maxRequestsPerHost = 5;
/*     */   
/*     */ 
/*     */   private ExecutorService executorService;
/*     */   
/*     */ 
/*  44 */   private final Deque<Call.AsyncCall> readyCalls = new ArrayDeque();
/*     */   
/*     */ 
/*  47 */   private final Deque<Call.AsyncCall> runningCalls = new ArrayDeque();
/*     */   
/*     */ 
/*  50 */   private final Deque<Call> executedCalls = new ArrayDeque();
/*     */   
/*     */   public Dispatcher(ExecutorService executorService) {
/*  53 */     this.executorService = executorService;
/*     */   }
/*     */   
/*     */   public Dispatcher() {}
/*     */   
/*     */   public synchronized ExecutorService getExecutorService()
/*     */   {
/*  60 */     if (this.executorService == null)
/*     */     {
/*  62 */       this.executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp Dispatcher", false));
/*     */     }
/*  64 */     return this.executorService;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized void setMaxRequests(int maxRequests)
/*     */   {
/*  75 */     if (maxRequests < 1) {
/*  76 */       throw new IllegalArgumentException("max < 1: " + maxRequests);
/*     */     }
/*  78 */     this.maxRequests = maxRequests;
/*  79 */     promoteCalls();
/*     */   }
/*     */   
/*     */   public synchronized int getMaxRequests() {
/*  83 */     return this.maxRequests;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized void setMaxRequestsPerHost(int maxRequestsPerHost)
/*     */   {
/*  96 */     if (maxRequestsPerHost < 1) {
/*  97 */       throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
/*     */     }
/*  99 */     this.maxRequestsPerHost = maxRequestsPerHost;
/* 100 */     promoteCalls();
/*     */   }
/*     */   
/*     */   public synchronized int getMaxRequestsPerHost() {
/* 104 */     return this.maxRequestsPerHost;
/*     */   }
/*     */   
/*     */   synchronized void enqueue(Call.AsyncCall call) {
/* 108 */     if ((this.runningCalls.size() < this.maxRequests) && (runningCallsForHost(call) < this.maxRequestsPerHost)) {
/* 109 */       this.runningCalls.add(call);
/* 110 */       getExecutorService().execute(call);
/*     */     } else {
/* 112 */       this.readyCalls.add(call);
/*     */     }
/*     */   }
/*     */   
/*     */   public synchronized void cancel(Object tag)
/*     */   {
/* 118 */     for (Call.AsyncCall call : this.readyCalls) {
/* 119 */       if (Util.equal(tag, call.tag())) {
/* 120 */         call.cancel();
/*     */       }
/*     */     }
/*     */     
/* 124 */     for (Call.AsyncCall call : this.runningCalls) {
/* 125 */       if (Util.equal(tag, call.tag())) {
/* 126 */         call.get().canceled = true;
/* 127 */         HttpEngine engine = call.get().engine;
/* 128 */         if (engine != null) { engine.cancel();
/*     */         }
/*     */       }
/*     */     }
/* 132 */     for (Call call : this.executedCalls) {
/* 133 */       if (Util.equal(tag, call.tag())) {
/* 134 */         call.cancel();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   synchronized void finished(Call.AsyncCall call)
/*     */   {
/* 141 */     if (!this.runningCalls.remove(call)) throw new AssertionError("AsyncCall wasn't running!");
/* 142 */     promoteCalls();
/*     */   }
/*     */   
/*     */   private void promoteCalls() {
/* 146 */     if (this.runningCalls.size() >= this.maxRequests) return;
/* 147 */     if (this.readyCalls.isEmpty()) { return;
/*     */     }
/* 149 */     for (Iterator<Call.AsyncCall> i = this.readyCalls.iterator(); i.hasNext();) {
/* 150 */       Call.AsyncCall call = (Call.AsyncCall)i.next();
/*     */       
/* 152 */       if (runningCallsForHost(call) < this.maxRequestsPerHost) {
/* 153 */         i.remove();
/* 154 */         this.runningCalls.add(call);
/* 155 */         getExecutorService().execute(call);
/*     */       }
/*     */       
/* 158 */       if (this.runningCalls.size() >= this.maxRequests) return;
/*     */     }
/*     */   }
/*     */   
/*     */   private int runningCallsForHost(Call.AsyncCall call)
/*     */   {
/* 164 */     int result = 0;
/* 165 */     for (Call.AsyncCall c : this.runningCalls) {
/* 166 */       if (c.host().equals(call.host())) result++;
/*     */     }
/* 168 */     return result;
/*     */   }
/*     */   
/*     */   synchronized void executed(Call call)
/*     */   {
/* 173 */     this.executedCalls.add(call);
/*     */   }
/*     */   
/*     */   synchronized void finished(Call call)
/*     */   {
/* 178 */     if (!this.executedCalls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
/*     */   }
/*     */   
/*     */   public synchronized int getRunningCallCount() {
/* 182 */     return this.runningCalls.size();
/*     */   }
/*     */   
/*     */   public synchronized int getQueuedCallCount() {
/* 186 */     return this.readyCalls.size();
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Dispatcher.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */