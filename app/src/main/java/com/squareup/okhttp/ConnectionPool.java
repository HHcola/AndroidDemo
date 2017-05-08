/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.RouteDatabase;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.http.StreamAllocation;
/*     */ import com.squareup.okhttp.internal.io.RealConnection;
/*     */ import java.lang.ref.Reference;
/*     */ import java.util.ArrayDeque;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Deque;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.concurrent.Executor;
/*     */ import java.util.concurrent.LinkedBlockingQueue;
/*     */ import java.util.concurrent.ThreadPoolExecutor;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import java.util.logging.Logger;
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
/*     */ public final class ConnectionPool
/*     */ {
/*     */   private static final long DEFAULT_KEEP_ALIVE_DURATION_MS = 300000L;
/*     */   private static final ConnectionPool systemDefault;
/*     */   
/*     */   static
/*     */   {
/*  64 */     String keepAlive = System.getProperty("http.keepAlive");
/*  65 */     String keepAliveDuration = System.getProperty("http.keepAliveDuration");
/*  66 */     String maxIdleConnections = System.getProperty("http.maxConnections");
/*     */     
/*  68 */     long keepAliveDurationMs = keepAliveDuration != null ? Long.parseLong(keepAliveDuration) : 300000L;
/*     */     
/*  70 */     if ((keepAlive != null) && (!Boolean.parseBoolean(keepAlive))) {
/*  71 */       systemDefault = new ConnectionPool(0, keepAliveDurationMs);
/*  72 */     } else if (maxIdleConnections != null) {
/*  73 */       systemDefault = new ConnectionPool(Integer.parseInt(maxIdleConnections), keepAliveDurationMs);
/*     */     } else {
/*  75 */       systemDefault = new ConnectionPool(5, keepAliveDurationMs);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  84 */   private final Executor executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), 
/*     */   
/*  86 */     Util.threadFactory("OkHttp ConnectionPool", true));
/*     */   
/*     */   private final int maxIdleConnections;
/*     */   
/*     */   private final long keepAliveDurationNs;
/*  91 */   private Runnable cleanupRunnable = new Runnable() {
/*     */     public void run() {
/*     */       for (;;) {
/*  94 */         long waitNanos = ConnectionPool.this.cleanup(System.nanoTime());
/*  95 */         if (waitNanos == -1L) return;
/*  96 */         if (waitNanos > 0L) {
/*  97 */           long waitMillis = waitNanos / 1000000L;
/*  98 */           waitNanos -= waitMillis * 1000000L;
/*  99 */           synchronized (ConnectionPool.this) {
/*     */             try {
/* 101 */               ConnectionPool.this.wait(waitMillis, (int)waitNanos);
/*     */             }
/*     */             catch (InterruptedException localInterruptedException) {}
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */   };
/*     */   
/* 110 */   private final Deque<RealConnection> connections = new ArrayDeque();
/* 111 */   final RouteDatabase routeDatabase = new RouteDatabase();
/*     */   
/*     */   public ConnectionPool(int maxIdleConnections, long keepAliveDurationMs) {
/* 114 */     this(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS);
/*     */   }
/*     */   
/*     */   public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
/* 118 */     this.maxIdleConnections = maxIdleConnections;
/* 119 */     this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);
/*     */     
/*     */ 
/* 122 */     if (keepAliveDuration <= 0L) {
/* 123 */       throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
/*     */     }
/*     */   }
/*     */   
/*     */   public static ConnectionPool getDefault() {
/* 128 */     return systemDefault;
/*     */   }
/*     */   
/*     */   public synchronized int getIdleConnectionCount()
/*     */   {
/* 133 */     int total = 0;
/* 134 */     for (RealConnection connection : this.connections) {
/* 135 */       if (connection.allocations.isEmpty()) total++;
/*     */     }
/* 137 */     return total;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized int getConnectionCount()
/*     */   {
/* 147 */     return this.connections.size();
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   public synchronized int getSpdyConnectionCount()
/*     */   {
/* 153 */     return getMultiplexedConnectionCount();
/*     */   }
/*     */   
/*     */   public synchronized int getMultiplexedConnectionCount()
/*     */   {
/* 158 */     int total = 0;
/* 159 */     for (RealConnection connection : this.connections) {
/* 160 */       if (connection.isMultiplexed()) total++;
/*     */     }
/* 162 */     return total;
/*     */   }
/*     */   
/*     */   public synchronized int getHttpConnectionCount()
/*     */   {
/* 167 */     return this.connections.size() - getMultiplexedConnectionCount();
/*     */   }
/*     */   
/*     */   RealConnection get(Address address, StreamAllocation streamAllocation)
/*     */   {
/* 172 */     assert (Thread.holdsLock(this));
/* 173 */     for (RealConnection connection : this.connections)
/*     */     {
/*     */ 
/* 176 */       if ((connection.allocations.size() < connection.allocationLimit()) && 
/* 177 */         (address.equals(connection.getRoute().address)) && (!connection.noNewStreams))
/*     */       {
/* 179 */         streamAllocation.acquire(connection);
/* 180 */         return connection;
/*     */       }
/*     */     }
/* 183 */     return null;
/*     */   }
/*     */   
/*     */   void put(RealConnection connection) {
/* 187 */     assert (Thread.holdsLock(this));
/* 188 */     if (this.connections.isEmpty()) {
/* 189 */       this.executor.execute(this.cleanupRunnable);
/*     */     }
/* 191 */     this.connections.add(connection);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   boolean connectionBecameIdle(RealConnection connection)
/*     */   {
/* 199 */     assert (Thread.holdsLock(this));
/* 200 */     if ((connection.noNewStreams) || (this.maxIdleConnections == 0)) {
/* 201 */       this.connections.remove(connection);
/* 202 */       return true;
/*     */     }
/* 204 */     notifyAll();
/* 205 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */   public void evictAll()
/*     */   {
/* 211 */     List<RealConnection> evictedConnections = new ArrayList();
/* 212 */     Iterator<RealConnection> i; synchronized (this) {
/* 213 */       for (i = this.connections.iterator(); i.hasNext();) {
/* 214 */         RealConnection connection = (RealConnection)i.next();
/* 215 */         if (connection.allocations.isEmpty()) {
/* 216 */           connection.noNewStreams = true;
/* 217 */           evictedConnections.add(connection);
/* 218 */           i.remove();
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 223 */     for (??? = evictedConnections.iterator(); ((Iterator)???).hasNext();) { RealConnection connection = (RealConnection)((Iterator)???).next();
/* 224 */       Util.closeQuietly(connection.getSocket());
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   long cleanup(long now)
/*     */   {
/* 236 */     int inUseConnectionCount = 0;
/* 237 */     int idleConnectionCount = 0;
/* 238 */     RealConnection longestIdleConnection = null;
/* 239 */     long longestIdleDurationNs = Long.MIN_VALUE;
/*     */     
/*     */ 
/* 242 */     synchronized (this) {
/* 243 */       for (Iterator<RealConnection> i = this.connections.iterator(); i.hasNext();) {
/* 244 */         RealConnection connection = (RealConnection)i.next();
/*     */         
/*     */ 
/* 247 */         if (pruneAndGetAllocationCount(connection, now) > 0) {
/* 248 */           inUseConnectionCount++;
/*     */         }
/*     */         else
/*     */         {
/* 252 */           idleConnectionCount++;
/*     */           
/*     */ 
/* 255 */           long idleDurationNs = now - connection.idleAtNanos;
/* 256 */           if (idleDurationNs > longestIdleDurationNs) {
/* 257 */             longestIdleDurationNs = idleDurationNs;
/* 258 */             longestIdleConnection = connection;
/*     */           }
/*     */         }
/*     */       }
/* 262 */       if ((longestIdleDurationNs >= this.keepAliveDurationNs) || (idleConnectionCount > this.maxIdleConnections))
/*     */       {
/*     */ 
/*     */ 
/* 266 */         this.connections.remove(longestIdleConnection);
/*     */       } else {
/* 268 */         if (idleConnectionCount > 0)
/*     */         {
/* 270 */           return this.keepAliveDurationNs - longestIdleDurationNs;
/*     */         }
/* 272 */         if (inUseConnectionCount > 0)
/*     */         {
/* 274 */           return this.keepAliveDurationNs;
/*     */         }
/*     */         
/*     */ 
/* 278 */         return -1L;
/*     */       }
/*     */     }
/*     */     
/* 282 */     Util.closeQuietly(longestIdleConnection.getSocket());
/*     */     
/*     */ 
/* 285 */     return 0L;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private int pruneAndGetAllocationCount(RealConnection connection, long now)
/*     */   {
/* 295 */     List<Reference<StreamAllocation>> references = connection.allocations;
/* 296 */     for (int i = 0; i < references.size();) {
/* 297 */       Reference<StreamAllocation> reference = (Reference)references.get(i);
/*     */       
/* 299 */       if (reference.get() != null) {
/* 300 */         i++;
/*     */ 
/*     */       }
/*     */       else
/*     */       {
/* 305 */         Internal.logger.warning("A connection to " + connection.getRoute().getAddress().url() + " was leaked. Did you forget to close a response body?");
/*     */         
/* 307 */         references.remove(i);
/* 308 */         connection.noNewStreams = true;
/*     */         
/*     */ 
/* 311 */         if (references.isEmpty()) {
/* 312 */           connection.idleAtNanos = (now - this.keepAliveDurationNs);
/* 313 */           return 0;
/*     */         }
/*     */       }
/*     */     }
/* 317 */     return references.size();
/*     */   }
/*     */   
/*     */   void setCleanupRunnableForTest(Runnable cleanupRunnable) {
/* 321 */     this.cleanupRunnable = cleanupRunnable;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\ConnectionPool.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */