/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import java.io.EOFException;
/*     */ import java.io.IOException;
/*     */ import java.io.InterruptedIOException;
/*     */ import java.net.SocketTimeoutException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import okio.AsyncTimeout;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSource;
/*     */ import okio.Sink;
/*     */ import okio.Source;
/*     */ import okio.Timeout;
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
/*     */ public final class FramedStream
/*     */ {
/*  45 */   long unacknowledgedBytesRead = 0L;
/*     */   
/*     */ 
/*     */   long bytesLeftInWriteWindow;
/*     */   
/*     */ 
/*     */   private final int id;
/*     */   
/*     */ 
/*     */   private final FramedConnection connection;
/*     */   
/*     */ 
/*     */   private final List<Header> requestHeaders;
/*     */   
/*     */ 
/*     */   private List<Header> responseHeaders;
/*     */   
/*     */   private final FramedDataSource source;
/*     */   
/*     */   final FramedDataSink sink;
/*     */   
/*  66 */   private final StreamTimeout readTimeout = new StreamTimeout();
/*  67 */   private final StreamTimeout writeTimeout = new StreamTimeout();
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  74 */   private ErrorCode errorCode = null;
/*     */   
/*     */   FramedStream(int id, FramedConnection connection, boolean outFinished, boolean inFinished, List<Header> requestHeaders)
/*     */   {
/*  78 */     if (connection == null) throw new NullPointerException("connection == null");
/*  79 */     if (requestHeaders == null) throw new NullPointerException("requestHeaders == null");
/*  80 */     this.id = id;
/*  81 */     this.connection = connection;
/*     */     
/*  83 */     this.bytesLeftInWriteWindow = connection.peerSettings.getInitialWindowSize(65536);
/*     */     
/*  85 */     this.source = new FramedDataSource(connection.okHttpSettings.getInitialWindowSize(65536), null);
/*  86 */     this.sink = new FramedDataSink();
/*  87 */     this.source.finished = inFinished;
/*  88 */     this.sink.finished = outFinished;
/*  89 */     this.requestHeaders = requestHeaders;
/*     */   }
/*     */   
/*     */   public int getId() {
/*  93 */     return this.id;
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
/*     */ 
/*     */   public synchronized boolean isOpen()
/*     */   {
/* 107 */     if (this.errorCode != null) {
/* 108 */       return false;
/*     */     }
/* 110 */     if (((this.source.finished) || (this.source.closed)) && 
/* 111 */       ((this.sink.finished) || (this.sink.closed)) && (this.responseHeaders != null))
/*     */     {
/* 113 */       return false;
/*     */     }
/* 115 */     return true;
/*     */   }
/*     */   
/*     */   public boolean isLocallyInitiated()
/*     */   {
/* 120 */     boolean streamIsClient = (this.id & 0x1) == 1;
/* 121 */     return this.connection.client == streamIsClient;
/*     */   }
/*     */   
/*     */   public FramedConnection getConnection() {
/* 125 */     return this.connection;
/*     */   }
/*     */   
/*     */   public List<Header> getRequestHeaders() {
/* 129 */     return this.requestHeaders;
/*     */   }
/*     */   
/*     */   /* Error */
/*     */   public synchronized List<Header> getResponseHeaders()
/*     */     throws IOException
/*     */   {
/*     */     // Byte code:
/*     */     //   0: aload_0
/*     */     //   1: getfield 7	com/squareup/okhttp/internal/framed/FramedStream:readTimeout	Lcom/squareup/okhttp/internal/framed/FramedStream$StreamTimeout;
/*     */     //   4: invokevirtual 38	com/squareup/okhttp/internal/framed/FramedStream$StreamTimeout:enter	()V
/*     */     //   7: aload_0
/*     */     //   8: getfield 36	com/squareup/okhttp/internal/framed/FramedStream:responseHeaders	Ljava/util/List;
/*     */     //   11: ifnonnull +17 -> 28
/*     */     //   14: aload_0
/*     */     //   15: getfield 6	com/squareup/okhttp/internal/framed/FramedStream:errorCode	Lcom/squareup/okhttp/internal/framed/ErrorCode;
/*     */     //   18: ifnonnull +10 -> 28
/*     */     //   21: aload_0
/*     */     //   22: invokespecial 5	com/squareup/okhttp/internal/framed/FramedStream:waitForIo	()V
/*     */     //   25: goto -18 -> 7
/*     */     //   28: aload_0
/*     */     //   29: getfield 7	com/squareup/okhttp/internal/framed/FramedStream:readTimeout	Lcom/squareup/okhttp/internal/framed/FramedStream$StreamTimeout;
/*     */     //   32: invokevirtual 39	com/squareup/okhttp/internal/framed/FramedStream$StreamTimeout:exitAndThrowIfTimedOut	()V
/*     */     //   35: goto +13 -> 48
/*     */     //   38: astore_1
/*     */     //   39: aload_0
/*     */     //   40: getfield 7	com/squareup/okhttp/internal/framed/FramedStream:readTimeout	Lcom/squareup/okhttp/internal/framed/FramedStream$StreamTimeout;
/*     */     //   43: invokevirtual 39	com/squareup/okhttp/internal/framed/FramedStream$StreamTimeout:exitAndThrowIfTimedOut	()V
/*     */     //   46: aload_1
/*     */     //   47: athrow
/*     */     //   48: aload_0
/*     */     //   49: getfield 36	com/squareup/okhttp/internal/framed/FramedStream:responseHeaders	Ljava/util/List;
/*     */     //   52: ifnull +8 -> 60
/*     */     //   55: aload_0
/*     */     //   56: getfield 36	com/squareup/okhttp/internal/framed/FramedStream:responseHeaders	Ljava/util/List;
/*     */     //   59: areturn
/*     */     //   60: new 40	java/io/IOException
/*     */     //   63: dup
/*     */     //   64: new 41	java/lang/StringBuilder
/*     */     //   67: dup
/*     */     //   68: invokespecial 42	java/lang/StringBuilder:<init>	()V
/*     */     //   71: ldc 43
/*     */     //   73: invokevirtual 44	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*     */     //   76: aload_0
/*     */     //   77: getfield 6	com/squareup/okhttp/internal/framed/FramedStream:errorCode	Lcom/squareup/okhttp/internal/framed/ErrorCode;
/*     */     //   80: invokevirtual 45	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
/*     */     //   83: invokevirtual 46	java/lang/StringBuilder:toString	()Ljava/lang/String;
/*     */     //   86: invokespecial 47	java/io/IOException:<init>	(Ljava/lang/String;)V
/*     */     //   89: athrow
/*     */     // Line number table:
/*     */     //   Java source line #137	-> byte code offset #0
/*     */     //   Java source line #139	-> byte code offset #7
/*     */     //   Java source line #140	-> byte code offset #21
/*     */     //   Java source line #143	-> byte code offset #28
/*     */     //   Java source line #144	-> byte code offset #35
/*     */     //   Java source line #143	-> byte code offset #38
/*     */     //   Java source line #145	-> byte code offset #48
/*     */     //   Java source line #146	-> byte code offset #60
/*     */     // Local variable table:
/*     */     //   start	length	slot	name	signature
/*     */     //   0	90	0	this	FramedStream
/*     */     //   38	9	1	localObject	Object
/*     */     // Exception table:
/*     */     //   from	to	target	type
/*     */     //   7	28	38	finally
/*     */   }
/*     */   
/*     */   public synchronized ErrorCode getErrorCode()
/*     */   {
/* 154 */     return this.errorCode;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void reply(List<Header> responseHeaders, boolean out)
/*     */     throws IOException
/*     */   {
/* 164 */     assert (!Thread.holdsLock(this));
/* 165 */     boolean outFinished = false;
/* 166 */     synchronized (this) {
/* 167 */       if (responseHeaders == null) {
/* 168 */         throw new NullPointerException("responseHeaders == null");
/*     */       }
/* 170 */       if (this.responseHeaders != null) {
/* 171 */         throw new IllegalStateException("reply already sent");
/*     */       }
/* 173 */       this.responseHeaders = responseHeaders;
/* 174 */       if (!out) {
/* 175 */         this.sink.finished = true;
/* 176 */         outFinished = true;
/*     */       }
/*     */     }
/* 179 */     this.connection.writeSynReply(this.id, outFinished, responseHeaders);
/*     */     
/* 181 */     if (outFinished) {
/* 182 */       this.connection.flush();
/*     */     }
/*     */   }
/*     */   
/*     */   public Timeout readTimeout() {
/* 187 */     return this.readTimeout;
/*     */   }
/*     */   
/*     */   public Timeout writeTimeout() {
/* 191 */     return this.writeTimeout;
/*     */   }
/*     */   
/*     */   public Source getSource()
/*     */   {
/* 196 */     return this.source;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Sink getSink()
/*     */   {
/* 206 */     synchronized (this) {
/* 207 */       if ((this.responseHeaders == null) && (!isLocallyInitiated())) {
/* 208 */         throw new IllegalStateException("reply before requesting the sink");
/*     */       }
/*     */     }
/* 211 */     return this.sink;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void close(ErrorCode rstStatusCode)
/*     */     throws IOException
/*     */   {
/* 219 */     if (!closeInternal(rstStatusCode)) {
/* 220 */       return;
/*     */     }
/* 222 */     this.connection.writeSynReset(this.id, rstStatusCode);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void closeLater(ErrorCode errorCode)
/*     */   {
/* 230 */     if (!closeInternal(errorCode)) {
/* 231 */       return;
/*     */     }
/* 233 */     this.connection.writeSynResetLater(this.id, errorCode);
/*     */   }
/*     */   
/*     */   private boolean closeInternal(ErrorCode errorCode)
/*     */   {
/* 238 */     assert (!Thread.holdsLock(this));
/* 239 */     synchronized (this) {
/* 240 */       if (this.errorCode != null) {
/* 241 */         return false;
/*     */       }
/* 243 */       if ((this.source.finished) && (this.sink.finished)) {
/* 244 */         return false;
/*     */       }
/* 246 */       this.errorCode = errorCode;
/* 247 */       notifyAll();
/*     */     }
/* 249 */     this.connection.removeStream(this.id);
/* 250 */     return true;
/*     */   }
/*     */   
/*     */   void receiveHeaders(List<Header> headers, HeadersMode headersMode) {
/* 254 */     assert (!Thread.holdsLock(this));
/* 255 */     ErrorCode errorCode = null;
/* 256 */     boolean open = true;
/* 257 */     synchronized (this) {
/* 258 */       if (this.responseHeaders == null) {
/* 259 */         if (headersMode.failIfHeadersAbsent()) {
/* 260 */           errorCode = ErrorCode.PROTOCOL_ERROR;
/*     */         } else {
/* 262 */           this.responseHeaders = headers;
/* 263 */           open = isOpen();
/* 264 */           notifyAll();
/*     */         }
/*     */       }
/* 267 */       else if (headersMode.failIfHeadersPresent()) {
/* 268 */         errorCode = ErrorCode.STREAM_IN_USE;
/*     */       } else {
/* 270 */         List<Header> newHeaders = new ArrayList();
/* 271 */         newHeaders.addAll(this.responseHeaders);
/* 272 */         newHeaders.addAll(headers);
/* 273 */         this.responseHeaders = newHeaders;
/*     */       }
/*     */     }
/*     */     
/* 277 */     if (errorCode != null) {
/* 278 */       closeLater(errorCode);
/* 279 */     } else if (!open) {
/* 280 */       this.connection.removeStream(this.id);
/*     */     }
/*     */   }
/*     */   
/*     */   void receiveData(BufferedSource in, int length) throws IOException {
/* 285 */     assert (!Thread.holdsLock(this));
/* 286 */     this.source.receive(in, length);
/*     */   }
/*     */   
/*     */   void receiveFin() {
/* 290 */     assert (!Thread.holdsLock(this));
/*     */     boolean open;
/* 292 */     synchronized (this) {
/* 293 */       this.source.finished = true;
/* 294 */       open = isOpen();
/* 295 */       notifyAll();
/*     */     }
/* 297 */     if (!open) {
/* 298 */       this.connection.removeStream(this.id);
/*     */     }
/*     */   }
/*     */   
/*     */   synchronized void receiveRstStream(ErrorCode errorCode) {
/* 303 */     if (this.errorCode == null) {
/* 304 */       this.errorCode = errorCode;
/* 305 */       notifyAll();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private final class FramedDataSource
/*     */     implements Source
/*     */   {
/* 316 */     private final Buffer receiveBuffer = new Buffer();
/*     */     
/*     */ 
/* 319 */     private final Buffer readBuffer = new Buffer();
/*     */     
/*     */ 
/*     */     private final long maxByteCount;
/*     */     
/*     */ 
/*     */     private boolean closed;
/*     */     
/*     */ 
/*     */     private boolean finished;
/*     */     
/*     */ 
/*     */ 
/*     */     private FramedDataSource(long maxByteCount)
/*     */     {
/* 334 */       this.maxByteCount = maxByteCount;
/*     */     }
/*     */     
/*     */     public long read(Buffer sink, long byteCount) throws IOException
/*     */     {
/* 339 */       if (byteCount < 0L) { throw new IllegalArgumentException("byteCount < 0: " + byteCount);
/*     */       }
/*     */       long read;
/* 342 */       synchronized (FramedStream.this) {
/* 343 */         waitUntilReadable();
/* 344 */         checkNotClosed();
/* 345 */         if (this.readBuffer.size() == 0L) { return -1L;
/*     */         }
/*     */         
/* 348 */         read = this.readBuffer.read(sink, Math.min(byteCount, this.readBuffer.size()));
/*     */         
/*     */ 
/* 351 */         FramedStream.this.unacknowledgedBytesRead += read;
/*     */         
/* 353 */         if (FramedStream.this.unacknowledgedBytesRead >= FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2) {
/* 354 */           FramedStream.this.connection.writeWindowUpdateLater(FramedStream.this.id, FramedStream.this.unacknowledgedBytesRead);
/* 355 */           FramedStream.this.unacknowledgedBytesRead = 0L;
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 360 */       synchronized (FramedStream.this.connection) {
/* 361 */         FramedStream.this.connection.unacknowledgedBytesRead += read;
/*     */         
/* 363 */         if (FramedStream.this.connection.unacknowledgedBytesRead >= FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2) {
/* 364 */           FramedStream.this.connection.writeWindowUpdateLater(0, FramedStream.this.connection.unacknowledgedBytesRead);
/* 365 */           FramedStream.this.connection.unacknowledgedBytesRead = 0L;
/*     */         }
/*     */       }
/*     */       
/* 369 */       return read;
/*     */     }
/*     */     
/*     */     /* Error */
/*     */     private void waitUntilReadable()
/*     */       throws IOException
/*     */     {
/*     */       // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: getfield 5	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:this$0	Lcom/squareup/okhttp/internal/framed/FramedStream;
/*     */       //   4: invokestatic 35	com/squareup/okhttp/internal/framed/FramedStream:access$700	(Lcom/squareup/okhttp/internal/framed/FramedStream;)Lcom/squareup/okhttp/internal/framed/FramedStream$StreamTimeout;
/*     */       //   7: invokevirtual 36	com/squareup/okhttp/internal/framed/FramedStream$StreamTimeout:enter	()V
/*     */       //   10: aload_0
/*     */       //   11: getfield 10	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:readBuffer	Lokio/Buffer;
/*     */       //   14: invokevirtual 22	okio/Buffer:size	()J
/*     */       //   17: lconst_0
/*     */       //   18: lcmp
/*     */       //   19: ifne +37 -> 56
/*     */       //   22: aload_0
/*     */       //   23: getfield 3	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:finished	Z
/*     */       //   26: ifne +30 -> 56
/*     */       //   29: aload_0
/*     */       //   30: getfield 2	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:closed	Z
/*     */       //   33: ifne +23 -> 56
/*     */       //   36: aload_0
/*     */       //   37: getfield 5	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:this$0	Lcom/squareup/okhttp/internal/framed/FramedStream;
/*     */       //   40: invokestatic 37	com/squareup/okhttp/internal/framed/FramedStream:access$800	(Lcom/squareup/okhttp/internal/framed/FramedStream;)Lcom/squareup/okhttp/internal/framed/ErrorCode;
/*     */       //   43: ifnonnull +13 -> 56
/*     */       //   46: aload_0
/*     */       //   47: getfield 5	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:this$0	Lcom/squareup/okhttp/internal/framed/FramedStream;
/*     */       //   50: invokestatic 38	com/squareup/okhttp/internal/framed/FramedStream:access$900	(Lcom/squareup/okhttp/internal/framed/FramedStream;)V
/*     */       //   53: goto -43 -> 10
/*     */       //   56: aload_0
/*     */       //   57: getfield 5	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:this$0	Lcom/squareup/okhttp/internal/framed/FramedStream;
/*     */       //   60: invokestatic 35	com/squareup/okhttp/internal/framed/FramedStream:access$700	(Lcom/squareup/okhttp/internal/framed/FramedStream;)Lcom/squareup/okhttp/internal/framed/FramedStream$StreamTimeout;
/*     */       //   63: invokevirtual 39	com/squareup/okhttp/internal/framed/FramedStream$StreamTimeout:exitAndThrowIfTimedOut	()V
/*     */       //   66: goto +16 -> 82
/*     */       //   69: astore_1
/*     */       //   70: aload_0
/*     */       //   71: getfield 5	com/squareup/okhttp/internal/framed/FramedStream$FramedDataSource:this$0	Lcom/squareup/okhttp/internal/framed/FramedStream;
/*     */       //   74: invokestatic 35	com/squareup/okhttp/internal/framed/FramedStream:access$700	(Lcom/squareup/okhttp/internal/framed/FramedStream;)Lcom/squareup/okhttp/internal/framed/FramedStream$StreamTimeout;
/*     */       //   77: invokevirtual 39	com/squareup/okhttp/internal/framed/FramedStream$StreamTimeout:exitAndThrowIfTimedOut	()V
/*     */       //   80: aload_1
/*     */       //   81: athrow
/*     */       //   82: return
/*     */       // Line number table:
/*     */       //   Java source line #374	-> byte code offset #0
/*     */       //   Java source line #376	-> byte code offset #10
/*     */       //   Java source line #377	-> byte code offset #46
/*     */       //   Java source line #380	-> byte code offset #56
/*     */       //   Java source line #381	-> byte code offset #66
/*     */       //   Java source line #380	-> byte code offset #69
/*     */       //   Java source line #382	-> byte code offset #82
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	signature
/*     */       //   0	83	0	this	FramedDataSource
/*     */       //   69	12	1	localObject	Object
/*     */       // Exception table:
/*     */       //   from	to	target	type
/*     */       //   10	56	69	finally
/*     */     }
/*     */     
/*     */     void receive(BufferedSource in, long byteCount)
/*     */       throws IOException
/*     */     {
/* 385 */       assert (!Thread.holdsLock(FramedStream.this));
/*     */       
/* 387 */       while (byteCount > 0L) {
/*     */         boolean finished;
/*     */         boolean flowControlError;
/* 390 */         synchronized (FramedStream.this) {
/* 391 */           finished = this.finished;
/* 392 */           flowControlError = byteCount + this.readBuffer.size() > this.maxByteCount;
/*     */         }
/*     */         
/*     */ 
/* 396 */         if (flowControlError) {
/* 397 */           in.skip(byteCount);
/* 398 */           FramedStream.this.closeLater(ErrorCode.FLOW_CONTROL_ERROR);
/* 399 */           return;
/*     */         }
/*     */         
/*     */ 
/* 403 */         if (finished) {
/* 404 */           in.skip(byteCount);
/* 405 */           return;
/*     */         }
/*     */         
/*     */ 
/* 409 */         long read = in.read(this.receiveBuffer, byteCount);
/* 410 */         if (read == -1L) throw new EOFException();
/* 411 */         byteCount -= read;
/*     */         
/*     */ 
/* 414 */         synchronized (FramedStream.this) {
/* 415 */           boolean wasEmpty = this.readBuffer.size() == 0L;
/* 416 */           this.readBuffer.writeAll(this.receiveBuffer);
/* 417 */           if (wasEmpty) {
/* 418 */             FramedStream.this.notifyAll();
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     public Timeout timeout() {
/* 425 */       return FramedStream.this.readTimeout;
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 429 */       synchronized (FramedStream.this) {
/* 430 */         this.closed = true;
/* 431 */         this.readBuffer.clear();
/* 432 */         FramedStream.this.notifyAll();
/*     */       }
/* 434 */       FramedStream.this.cancelStreamIfNecessary();
/*     */     }
/*     */     
/*     */     private void checkNotClosed() throws IOException {
/* 438 */       if (this.closed) {
/* 439 */         throw new IOException("stream closed");
/*     */       }
/* 441 */       if (FramedStream.this.errorCode != null) {
/* 442 */         throw new IOException("stream was reset: " + FramedStream.this.errorCode);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void cancelStreamIfNecessary() throws IOException {
/* 448 */     assert (!Thread.holdsLock(this));
/*     */     boolean cancel;
/*     */     boolean open;
/* 451 */     synchronized (this) {
/* 452 */       cancel = (!this.source.finished) && (this.source.closed) && ((this.sink.finished) || (this.sink.closed));
/* 453 */       open = isOpen();
/*     */     }
/* 455 */     if (cancel)
/*     */     {
/*     */ 
/*     */ 
/*     */ 
/* 460 */       close(ErrorCode.CANCEL);
/* 461 */     } else if (!open) {
/* 462 */       this.connection.removeStream(this.id);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   final class FramedDataSink
/*     */     implements Sink
/*     */   {
/*     */     private static final long EMIT_BUFFER_SIZE = 16384L;
/*     */     
/*     */ 
/*     */ 
/* 477 */     private final Buffer sendBuffer = new Buffer();
/*     */     
/*     */     private boolean closed;
/*     */     
/*     */     private boolean finished;
/*     */     
/*     */     FramedDataSink() {}
/*     */     
/*     */     public void write(Buffer source, long byteCount)
/*     */       throws IOException
/*     */     {
/* 488 */       assert (!Thread.holdsLock(FramedStream.this));
/* 489 */       this.sendBuffer.write(source, byteCount);
/* 490 */       while (this.sendBuffer.size() >= 16384L) {
/* 491 */         emitDataFrame(false);
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */     private void emitDataFrame(boolean outFinished)
/*     */       throws IOException
/*     */     {
/*     */       long toWrite;
/*     */       
/* 501 */       synchronized (FramedStream.this) {
/* 502 */         FramedStream.this.writeTimeout.enter();
/*     */         try {
/* 504 */           while ((FramedStream.this.bytesLeftInWriteWindow <= 0L) && (!this.finished) && (!this.closed) && (FramedStream.this.errorCode == null)) {
/* 505 */             FramedStream.this.waitForIo();
/*     */           }
/*     */         } finally {
/* 508 */           FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
/*     */         }
/*     */         
/* 511 */         FramedStream.this.checkOutNotClosed();
/* 512 */         toWrite = Math.min(FramedStream.this.bytesLeftInWriteWindow, this.sendBuffer.size());
/* 513 */         FramedStream.this.bytesLeftInWriteWindow -= toWrite;
/*     */       }
/*     */       
/* 516 */       FramedStream.this.writeTimeout.enter();
/*     */       try {
/* 518 */         FramedStream.this.connection.writeData(FramedStream.this.id, (outFinished) && (toWrite == this.sendBuffer.size()), this.sendBuffer, toWrite);
/*     */       } finally {
/* 520 */         FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
/*     */       }
/*     */     }
/*     */     
/*     */     public void flush() throws IOException {
/* 525 */       assert (!Thread.holdsLock(FramedStream.this));
/* 526 */       synchronized (FramedStream.this) {
/* 527 */         FramedStream.this.checkOutNotClosed();
/*     */       }
/* 529 */       while (this.sendBuffer.size() > 0L) {
/* 530 */         emitDataFrame(false);
/* 531 */         FramedStream.this.connection.flush();
/*     */       }
/*     */     }
/*     */     
/*     */     public Timeout timeout() {
/* 536 */       return FramedStream.this.writeTimeout;
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 540 */       assert (!Thread.holdsLock(FramedStream.this));
/* 541 */       synchronized (FramedStream.this) {
/* 542 */         if (this.closed) return;
/*     */       }
/* 544 */       if (!FramedStream.this.sink.finished)
/*     */       {
/* 546 */         if (this.sendBuffer.size() > 0L) {
/* 547 */           while (this.sendBuffer.size() > 0L) {
/* 548 */             emitDataFrame(true);
/*     */           }
/*     */         }
/*     */         
/* 552 */         FramedStream.this.connection.writeData(FramedStream.this.id, true, null, 0L);
/*     */       }
/*     */       
/* 555 */       synchronized (FramedStream.this) {
/* 556 */         this.closed = true;
/*     */       }
/* 558 */       FramedStream.this.connection.flush();
/* 559 */       FramedStream.this.cancelStreamIfNecessary();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   void addBytesToWriteWindow(long delta)
/*     */   {
/* 568 */     this.bytesLeftInWriteWindow += delta;
/* 569 */     if (delta > 0L) notifyAll();
/*     */   }
/*     */   
/*     */   private void checkOutNotClosed() throws IOException {
/* 573 */     if (this.sink.closed)
/* 574 */       throw new IOException("stream closed");
/* 575 */     if (this.sink.finished)
/* 576 */       throw new IOException("stream finished");
/* 577 */     if (this.errorCode != null) {
/* 578 */       throw new IOException("stream was reset: " + this.errorCode);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void waitForIo()
/*     */     throws InterruptedIOException
/*     */   {
/*     */     try
/*     */     {
/* 588 */       wait();
/*     */     } catch (InterruptedException e) {
/* 590 */       throw new InterruptedIOException();
/*     */     }
/*     */   }
/*     */   
/*     */   class StreamTimeout
/*     */     extends AsyncTimeout
/*     */   {
/*     */     StreamTimeout() {}
/*     */     
/*     */     protected void timedOut()
/*     */     {
/* 601 */       FramedStream.this.closeLater(ErrorCode.CANCEL);
/*     */     }
/*     */     
/*     */     protected IOException newTimeoutException(IOException cause) {
/* 605 */       SocketTimeoutException socketTimeoutException = new SocketTimeoutException("timeout");
/* 606 */       if (cause != null) {
/* 607 */         socketTimeoutException.initCause(cause);
/*     */       }
/* 609 */       return socketTimeoutException;
/*     */     }
/*     */     
/*     */     public void exitAndThrowIfTimedOut() throws IOException {
/* 613 */       if (exit()) throw newTimeoutException(null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\FramedStream.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */