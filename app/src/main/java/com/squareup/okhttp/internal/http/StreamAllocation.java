/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.Address;
/*     */ import com.squareup.okhttp.ConnectionPool;
/*     */ import com.squareup.okhttp.Route;
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.RouteDatabase;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.io.RealConnection;
/*     */ import java.io.IOException;
/*     */ import java.io.InterruptedIOException;
/*     */ import java.lang.ref.Reference;
/*     */ import java.lang.ref.WeakReference;
/*     */ import java.net.ProtocolException;
/*     */ import java.net.SocketTimeoutException;
/*     */ import java.security.cert.CertificateException;
/*     */ import java.util.List;
/*     */ import javax.net.ssl.SSLHandshakeException;
/*     */ import javax.net.ssl.SSLPeerUnverifiedException;
/*     */ import okio.Sink;
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
/*     */ public final class StreamAllocation
/*     */ {
/*     */   public final Address address;
/*     */   private final ConnectionPool connectionPool;
/*     */   private RouteSelector routeSelector;
/*     */   private RealConnection connection;
/*     */   private boolean released;
/*     */   private boolean canceled;
/*     */   private HttpStream stream;
/*     */   
/*     */   public StreamAllocation(ConnectionPool connectionPool, Address address)
/*     */   {
/*  87 */     this.connectionPool = connectionPool;
/*  88 */     this.address = address;
/*     */   }
/*     */   
/*     */   /* Error */
/*     */   public HttpStream newStream(int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks)
/*     */     throws RouteException, IOException
/*     */   {
/*     */     // Byte code:
/*     */     //   0: aload_0
/*     */     //   1: iload_1
/*     */     //   2: iload_2
/*     */     //   3: iload_3
/*     */     //   4: iload 4
/*     */     //   6: iload 5
/*     */     //   8: invokespecial 4	com/squareup/okhttp/internal/http/StreamAllocation:findHealthyConnection	(IIIZZ)Lcom/squareup/okhttp/internal/io/RealConnection;
/*     */     //   11: astore 6
/*     */     //   13: aload 6
/*     */     //   15: getfield 5	com/squareup/okhttp/internal/io/RealConnection:framedConnection	Lcom/squareup/okhttp/internal/framed/FramedConnection;
/*     */     //   18: ifnull +21 -> 39
/*     */     //   21: new 6	com/squareup/okhttp/internal/http/Http2xStream
/*     */     //   24: dup
/*     */     //   25: aload_0
/*     */     //   26: aload 6
/*     */     //   28: getfield 5	com/squareup/okhttp/internal/io/RealConnection:framedConnection	Lcom/squareup/okhttp/internal/framed/FramedConnection;
/*     */     //   31: invokespecial 7	com/squareup/okhttp/internal/http/Http2xStream:<init>	(Lcom/squareup/okhttp/internal/http/StreamAllocation;Lcom/squareup/okhttp/internal/framed/FramedConnection;)V
/*     */     //   34: astore 7
/*     */     //   36: goto +70 -> 106
/*     */     //   39: aload 6
/*     */     //   41: invokevirtual 8	com/squareup/okhttp/internal/io/RealConnection:getSocket	()Ljava/net/Socket;
/*     */     //   44: iload_2
/*     */     //   45: invokevirtual 9	java/net/Socket:setSoTimeout	(I)V
/*     */     //   48: aload 6
/*     */     //   50: getfield 10	com/squareup/okhttp/internal/io/RealConnection:source	Lokio/BufferedSource;
/*     */     //   53: invokeinterface 11 1 0
/*     */     //   58: iload_2
/*     */     //   59: i2l
/*     */     //   60: getstatic 12	java/util/concurrent/TimeUnit:MILLISECONDS	Ljava/util/concurrent/TimeUnit;
/*     */     //   63: invokevirtual 13	okio/Timeout:timeout	(JLjava/util/concurrent/TimeUnit;)Lokio/Timeout;
/*     */     //   66: pop
/*     */     //   67: aload 6
/*     */     //   69: getfield 14	com/squareup/okhttp/internal/io/RealConnection:sink	Lokio/BufferedSink;
/*     */     //   72: invokeinterface 15 1 0
/*     */     //   77: iload_3
/*     */     //   78: i2l
/*     */     //   79: getstatic 12	java/util/concurrent/TimeUnit:MILLISECONDS	Ljava/util/concurrent/TimeUnit;
/*     */     //   82: invokevirtual 13	okio/Timeout:timeout	(JLjava/util/concurrent/TimeUnit;)Lokio/Timeout;
/*     */     //   85: pop
/*     */     //   86: new 16	com/squareup/okhttp/internal/http/Http1xStream
/*     */     //   89: dup
/*     */     //   90: aload_0
/*     */     //   91: aload 6
/*     */     //   93: getfield 10	com/squareup/okhttp/internal/io/RealConnection:source	Lokio/BufferedSource;
/*     */     //   96: aload 6
/*     */     //   98: getfield 14	com/squareup/okhttp/internal/io/RealConnection:sink	Lokio/BufferedSink;
/*     */     //   101: invokespecial 17	com/squareup/okhttp/internal/http/Http1xStream:<init>	(Lcom/squareup/okhttp/internal/http/StreamAllocation;Lokio/BufferedSource;Lokio/BufferedSink;)V
/*     */     //   104: astore 7
/*     */     //   106: aload_0
/*     */     //   107: getfield 2	com/squareup/okhttp/internal/http/StreamAllocation:connectionPool	Lcom/squareup/okhttp/ConnectionPool;
/*     */     //   110: dup
/*     */     //   111: astore 8
/*     */     //   113: monitorenter
/*     */     //   114: aload 6
/*     */     //   116: dup
/*     */     //   117: getfield 18	com/squareup/okhttp/internal/io/RealConnection:streamCount	I
/*     */     //   120: iconst_1
/*     */     //   121: iadd
/*     */     //   122: putfield 18	com/squareup/okhttp/internal/io/RealConnection:streamCount	I
/*     */     //   125: aload_0
/*     */     //   126: aload 7
/*     */     //   128: putfield 19	com/squareup/okhttp/internal/http/StreamAllocation:stream	Lcom/squareup/okhttp/internal/http/HttpStream;
/*     */     //   131: aload 7
/*     */     //   133: aload 8
/*     */     //   135: monitorexit
/*     */     //   136: areturn
/*     */     //   137: astore 9
/*     */     //   139: aload 8
/*     */     //   141: monitorexit
/*     */     //   142: aload 9
/*     */     //   144: athrow
/*     */     //   145: astore 6
/*     */     //   147: new 21	com/squareup/okhttp/internal/http/RouteException
/*     */     //   150: dup
/*     */     //   151: aload 6
/*     */     //   153: invokespecial 22	com/squareup/okhttp/internal/http/RouteException:<init>	(Ljava/io/IOException;)V
/*     */     //   156: athrow
/*     */     // Line number table:
/*     */     //   Java source line #95	-> byte code offset #0
/*     */     //   Java source line #99	-> byte code offset #13
/*     */     //   Java source line #100	-> byte code offset #21
/*     */     //   Java source line #102	-> byte code offset #39
/*     */     //   Java source line #103	-> byte code offset #48
/*     */     //   Java source line #104	-> byte code offset #67
/*     */     //   Java source line #105	-> byte code offset #86
/*     */     //   Java source line #108	-> byte code offset #106
/*     */     //   Java source line #109	-> byte code offset #114
/*     */     //   Java source line #110	-> byte code offset #125
/*     */     //   Java source line #111	-> byte code offset #131
/*     */     //   Java source line #112	-> byte code offset #137
/*     */     //   Java source line #113	-> byte code offset #145
/*     */     //   Java source line #114	-> byte code offset #147
/*     */     // Local variable table:
/*     */     //   start	length	slot	name	signature
/*     */     //   0	157	0	this	StreamAllocation
/*     */     //   0	157	1	connectTimeout	int
/*     */     //   0	157	2	readTimeout	int
/*     */     //   0	157	3	writeTimeout	int
/*     */     //   0	157	4	connectionRetryEnabled	boolean
/*     */     //   0	157	5	doExtensiveHealthChecks	boolean
/*     */     //   11	104	6	resultConnection	RealConnection
/*     */     //   145	7	6	e	IOException
/*     */     //   34	3	7	resultStream	HttpStream
/*     */     //   104	28	7	resultStream	HttpStream
/*     */     //   137	6	9	localObject1	Object
/*     */     // Exception table:
/*     */     //   from	to	target	type
/*     */     //   114	136	137	finally
/*     */     //   137	142	137	finally
/*     */     //   0	136	145	java/io/IOException
/*     */     //   137	145	145	java/io/IOException
/*     */   }
/*     */   
/*     */   private RealConnection findHealthyConnection(int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks)
/*     */     throws IOException, RouteException
/*     */   {
/*     */     for (;;)
/*     */     {
/* 126 */       RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout, connectionRetryEnabled);
/*     */       
/*     */ 
/*     */ 
/* 130 */       synchronized (this.connectionPool) {
/* 131 */         if (candidate.streamCount == 0) {
/* 132 */           return candidate;
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 137 */       if (candidate.isHealthy(doExtensiveHealthChecks)) {
/* 138 */         return candidate;
/*     */       }
/*     */       
/* 141 */       connectionFailed();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled)
/*     */     throws IOException, RouteException
/*     */   {
/* 151 */     synchronized (this.connectionPool) {
/* 152 */       if (this.released) throw new IllegalStateException("released");
/* 153 */       if (this.stream != null) throw new IllegalStateException("stream != null");
/* 154 */       if (this.canceled) { throw new IOException("Canceled");
/*     */       }
/* 156 */       RealConnection allocatedConnection = this.connection;
/* 157 */       if ((allocatedConnection != null) && (!allocatedConnection.noNewStreams)) {
/* 158 */         return allocatedConnection;
/*     */       }
/*     */       
/*     */ 
/* 162 */       RealConnection pooledConnection = Internal.instance.get(this.connectionPool, this.address, this);
/* 163 */       if (pooledConnection != null) {
/* 164 */         this.connection = pooledConnection;
/* 165 */         return pooledConnection;
/*     */       }
/*     */       
/*     */ 
/* 169 */       if (this.routeSelector == null) {
/* 170 */         this.routeSelector = new RouteSelector(this.address, routeDatabase());
/*     */       }
/*     */     }
/*     */     
/* 174 */     Route route = this.routeSelector.next();
/* 175 */     RealConnection newConnection = new RealConnection(route);
/* 176 */     acquire(newConnection);
/*     */     
/* 178 */     synchronized (this.connectionPool) {
/* 179 */       Internal.instance.put(this.connectionPool, newConnection);
/* 180 */       this.connection = newConnection;
/* 181 */       if (this.canceled) { throw new IOException("Canceled");
/*     */       }
/*     */     }
/* 184 */     newConnection.connect(connectTimeout, readTimeout, writeTimeout, this.address.getConnectionSpecs(), connectionRetryEnabled);
/*     */     
/* 186 */     routeDatabase().connected(newConnection.getRoute());
/*     */     
/* 188 */     return newConnection;
/*     */   }
/*     */   
/*     */   public void streamFinished(HttpStream stream) {
/* 192 */     synchronized (this.connectionPool) {
/* 193 */       if ((stream == null) || (stream != this.stream)) {
/* 194 */         throw new IllegalStateException("expected " + this.stream + " but was " + stream);
/*     */       }
/*     */     }
/* 197 */     deallocate(false, false, true);
/*     */   }
/*     */   
/*     */   /* Error */
/*     */   public HttpStream stream()
/*     */   {
/*     */     // Byte code:
/*     */     //   0: aload_0
/*     */     //   1: getfield 2	com/squareup/okhttp/internal/http/StreamAllocation:connectionPool	Lcom/squareup/okhttp/ConnectionPool;
/*     */     //   4: dup
/*     */     //   5: astore_1
/*     */     //   6: monitorenter
/*     */     //   7: aload_0
/*     */     //   8: getfield 19	com/squareup/okhttp/internal/http/StreamAllocation:stream	Lcom/squareup/okhttp/internal/http/HttpStream;
/*     */     //   11: aload_1
/*     */     //   12: monitorexit
/*     */     //   13: areturn
/*     */     //   14: astore_2
/*     */     //   15: aload_1
/*     */     //   16: monitorexit
/*     */     //   17: aload_2
/*     */     //   18: athrow
/*     */     // Line number table:
/*     */     //   Java source line #201	-> byte code offset #0
/*     */     //   Java source line #202	-> byte code offset #7
/*     */     //   Java source line #203	-> byte code offset #14
/*     */     // Local variable table:
/*     */     //   start	length	slot	name	signature
/*     */     //   0	19	0	this	StreamAllocation
/*     */     //   5	11	1	Ljava/lang/Object;	Object
/*     */     //   14	4	2	localObject1	Object
/*     */     // Exception table:
/*     */     //   from	to	target	type
/*     */     //   7	13	14	finally
/*     */     //   14	17	14	finally
/*     */   }
/*     */   
/*     */   private RouteDatabase routeDatabase()
/*     */   {
/* 207 */     return Internal.instance.routeDatabase(this.connectionPool);
/*     */   }
/*     */   
/*     */   public synchronized RealConnection connection() {
/* 211 */     return this.connection;
/*     */   }
/*     */   
/*     */   public void release() {
/* 215 */     deallocate(false, true, false);
/*     */   }
/*     */   
/*     */   public void noNewStreams()
/*     */   {
/* 220 */     deallocate(true, false, false);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void deallocate(boolean noNewStreams, boolean released, boolean streamFinished)
/*     */   {
/* 228 */     RealConnection connectionToClose = null;
/* 229 */     synchronized (this.connectionPool) {
/* 230 */       if (streamFinished) {
/* 231 */         this.stream = null;
/*     */       }
/* 233 */       if (released) {
/* 234 */         this.released = true;
/*     */       }
/* 236 */       if (this.connection != null) {
/* 237 */         if (noNewStreams) {
/* 238 */           this.connection.noNewStreams = true;
/*     */         }
/* 240 */         if ((this.stream == null) && ((this.released) || (this.connection.noNewStreams))) {
/* 241 */           release(this.connection);
/* 242 */           if (this.connection.streamCount > 0) {
/* 243 */             this.routeSelector = null;
/*     */           }
/* 245 */           if (this.connection.allocations.isEmpty()) {
/* 246 */             this.connection.idleAtNanos = System.nanoTime();
/* 247 */             if (Internal.instance.connectionBecameIdle(this.connectionPool, this.connection)) {
/* 248 */               connectionToClose = this.connection;
/*     */             }
/*     */           }
/* 251 */           this.connection = null;
/*     */         }
/*     */       }
/*     */     }
/* 255 */     if (connectionToClose != null) {
/* 256 */       Util.closeQuietly(connectionToClose.getSocket());
/*     */     }
/*     */   }
/*     */   
/*     */   public void cancel() {
/*     */     HttpStream streamToCancel;
/*     */     RealConnection connectionToCancel;
/* 263 */     synchronized (this.connectionPool) {
/* 264 */       this.canceled = true;
/* 265 */       streamToCancel = this.stream;
/* 266 */       connectionToCancel = this.connection;
/*     */     }
/* 268 */     if (streamToCancel != null) {
/* 269 */       streamToCancel.cancel();
/* 270 */     } else if (connectionToCancel != null) {
/* 271 */       connectionToCancel.cancel();
/*     */     }
/*     */   }
/*     */   
/*     */   private void connectionFailed(IOException e) {
/* 276 */     synchronized (this.connectionPool) {
/* 277 */       if (this.routeSelector != null) {
/* 278 */         if (this.connection.streamCount == 0)
/*     */         {
/* 280 */           Route failedRoute = this.connection.getRoute();
/* 281 */           this.routeSelector.connectFailed(failedRoute, e);
/*     */         }
/*     */         else {
/* 284 */           this.routeSelector = null;
/*     */         }
/*     */       }
/*     */     }
/* 288 */     connectionFailed();
/*     */   }
/*     */   
/*     */   public void connectionFailed()
/*     */   {
/* 293 */     deallocate(true, false, true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void acquire(RealConnection connection)
/*     */   {
/* 301 */     connection.allocations.add(new WeakReference(this));
/*     */   }
/*     */   
/*     */   private void release(RealConnection connection)
/*     */   {
/* 306 */     int i = 0; for (int size = connection.allocations.size(); i < size; i++) {
/* 307 */       Reference<StreamAllocation> reference = (Reference)connection.allocations.get(i);
/* 308 */       if (reference.get() == this) {
/* 309 */         connection.allocations.remove(i);
/* 310 */         return;
/*     */       }
/*     */     }
/* 313 */     throw new IllegalStateException();
/*     */   }
/*     */   
/*     */   public boolean recover(RouteException e) {
/* 317 */     if (this.connection != null) {
/* 318 */       connectionFailed(e.getLastConnectException());
/*     */     }
/*     */     
/* 321 */     if (((this.routeSelector != null) && (!this.routeSelector.hasNext())) || 
/* 322 */       (!isRecoverable(e))) {
/* 323 */       return false;
/*     */     }
/*     */     
/* 326 */     return true;
/*     */   }
/*     */   
/*     */   public boolean recover(IOException e, Sink requestBodyOut) {
/* 330 */     if (this.connection != null) {
/* 331 */       int streamCount = this.connection.streamCount;
/* 332 */       connectionFailed(e);
/*     */       
/* 334 */       if (streamCount == 1)
/*     */       {
/*     */ 
/* 337 */         return false;
/*     */       }
/*     */     }
/*     */     
/* 341 */     boolean canRetryRequestBody = (requestBodyOut == null) || ((requestBodyOut instanceof RetryableSink));
/* 342 */     if (((this.routeSelector != null) && (!this.routeSelector.hasNext())) || 
/* 343 */       (!isRecoverable(e)) || (!canRetryRequestBody))
/*     */     {
/* 345 */       return false;
/*     */     }
/*     */     
/* 348 */     return true;
/*     */   }
/*     */   
/*     */   private boolean isRecoverable(IOException e)
/*     */   {
/* 353 */     if ((e instanceof ProtocolException)) {
/* 354 */       return false;
/*     */     }
/*     */     
/*     */ 
/* 358 */     if ((e instanceof InterruptedIOException)) {
/* 359 */       return false;
/*     */     }
/*     */     
/* 362 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private boolean isRecoverable(RouteException e)
/*     */   {
/* 370 */     IOException ioe = e.getLastConnectException();
/*     */     
/*     */ 
/* 373 */     if ((ioe instanceof ProtocolException)) {
/* 374 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 379 */     if ((ioe instanceof InterruptedIOException)) {
/* 380 */       return ioe instanceof SocketTimeoutException;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 385 */     if ((ioe instanceof SSLHandshakeException))
/*     */     {
/*     */ 
/* 388 */       if ((ioe.getCause() instanceof CertificateException)) {
/* 389 */         return false;
/*     */       }
/*     */     }
/* 392 */     if ((ioe instanceof SSLPeerUnverifiedException))
/*     */     {
/* 394 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 400 */     return true;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 404 */     return this.address.toString();
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\StreamAllocation.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */