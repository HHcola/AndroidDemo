/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.NamedRunnable;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.io.Closeable;
/*     */ import java.io.IOException;
/*     */ import java.io.InterruptedIOException;
/*     */ import java.net.InetSocketAddress;
/*     */ import java.net.Socket;
/*     */ import java.util.Collection;
/*     */ import java.util.HashMap;
/*     */ import java.util.LinkedHashSet;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.ExecutorService;
/*     */ import java.util.concurrent.LinkedBlockingQueue;
/*     */ import java.util.concurrent.SynchronousQueue;
/*     */ import java.util.concurrent.ThreadPoolExecutor;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.ByteString;
/*     */ import okio.Okio;
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
/*     */ public final class FramedConnection
/*     */   implements Closeable
/*     */ {
/*  69 */   private static final ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), 
/*     */   
/*  71 */     Util.threadFactory("OkHttp FramedConnection", true));
/*     */   
/*     */ 
/*     */ 
/*     */   final Protocol protocol;
/*     */   
/*     */ 
/*     */   final boolean client;
/*     */   
/*     */ 
/*     */   private final Listener listener;
/*     */   
/*     */ 
/*  84 */   private final Map<Integer, FramedStream> streams = new HashMap();
/*     */   private final String hostName;
/*     */   private int lastGoodStreamId;
/*     */   private int nextStreamId;
/*     */   private boolean shutdown;
/*  89 */   private long idleStartTimeNs = System.nanoTime();
/*     */   
/*     */ 
/*     */ 
/*     */   private final ExecutorService pushExecutor;
/*     */   
/*     */ 
/*     */   private Map<Integer, Ping> pings;
/*     */   
/*     */ 
/*     */   private final PushObserver pushObserver;
/*     */   
/*     */ 
/*     */   private int nextPingId;
/*     */   
/*     */ 
/* 105 */   long unacknowledgedBytesRead = 0L;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   long bytesLeftInWriteWindow;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/* 115 */   Settings okHttpSettings = new Settings();
/*     */   
/*     */ 
/*     */   private static final int OKHTTP_CLIENT_WINDOW_SIZE = 16777216;
/*     */   
/*     */ 
/* 121 */   final Settings peerSettings = new Settings();
/*     */   
/* 123 */   private boolean receivedInitialPeerSettings = false;
/*     */   final Variant variant;
/*     */   final Socket socket;
/*     */   final FrameWriter frameWriter;
/*     */   final Reader readerRunnable;
/*     */   
/*     */   private FramedConnection(Builder builder)
/*     */     throws IOException
/*     */   {
/* 132 */     this.protocol = builder.protocol;
/* 133 */     this.pushObserver = builder.pushObserver;
/* 134 */     this.client = builder.client;
/* 135 */     this.listener = builder.listener;
/*     */     
/* 137 */     this.nextStreamId = (builder.client ? 1 : 2);
/* 138 */     if ((builder.client) && (this.protocol == Protocol.HTTP_2)) {
/* 139 */       this.nextStreamId += 2;
/*     */     }
/*     */     
/* 142 */     this.nextPingId = (builder.client ? 1 : 2);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 148 */     if (builder.client) {
/* 149 */       this.okHttpSettings.set(7, 0, 16777216);
/*     */     }
/*     */     
/* 152 */     this.hostName = builder.hostName;
/*     */     
/* 154 */     if (this.protocol == Protocol.HTTP_2) {
/* 155 */       this.variant = new Http2();
/*     */       
/*     */ 
/*     */ 
/* 159 */       this.pushExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory(String.format("OkHttp %s Push Observer", new Object[] { this.hostName }), true));
/*     */       
/* 161 */       this.peerSettings.set(7, 0, 65535);
/* 162 */       this.peerSettings.set(5, 0, 16384);
/* 163 */     } else if (this.protocol == Protocol.SPDY_3) {
/* 164 */       this.variant = new Spdy3();
/* 165 */       this.pushExecutor = null;
/*     */     } else {
/* 167 */       throw new AssertionError(this.protocol);
/*     */     }
/* 169 */     this.bytesLeftInWriteWindow = this.peerSettings.getInitialWindowSize(65536);
/* 170 */     this.socket = builder.socket;
/* 171 */     this.frameWriter = this.variant.newWriter(builder.sink, this.client);
/*     */     
/* 173 */     this.readerRunnable = new Reader(this.variant.newReader(builder.source, this.client), null);
/* 174 */     new Thread(this.readerRunnable).start();
/*     */   }
/*     */   
/*     */   public Protocol getProtocol()
/*     */   {
/* 179 */     return this.protocol;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized int openStreamCount()
/*     */   {
/* 187 */     return this.streams.size();
/*     */   }
/*     */   
/*     */   synchronized FramedStream getStream(int id) {
/* 191 */     return (FramedStream)this.streams.get(Integer.valueOf(id));
/*     */   }
/*     */   
/*     */   synchronized FramedStream removeStream(int streamId) {
/* 195 */     FramedStream stream = (FramedStream)this.streams.remove(Integer.valueOf(streamId));
/* 196 */     if ((stream != null) && (this.streams.isEmpty())) {
/* 197 */       setIdle(true);
/*     */     }
/* 199 */     notifyAll();
/* 200 */     return stream;
/*     */   }
/*     */   
/*     */   private synchronized void setIdle(boolean value) {
/* 204 */     this.idleStartTimeNs = (value ? System.nanoTime() : Long.MAX_VALUE);
/*     */   }
/*     */   
/*     */   public synchronized boolean isIdle()
/*     */   {
/* 209 */     return this.idleStartTimeNs != Long.MAX_VALUE;
/*     */   }
/*     */   
/*     */   public synchronized int maxConcurrentStreams() {
/* 213 */     return this.peerSettings.getMaxConcurrentStreams(Integer.MAX_VALUE);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized long getIdleStartTimeNs()
/*     */   {
/* 221 */     return this.idleStartTimeNs;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public FramedStream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out)
/*     */     throws IOException
/*     */   {
/* 234 */     if (this.client) throw new IllegalStateException("Client cannot push requests.");
/* 235 */     if (this.protocol != Protocol.HTTP_2) throw new IllegalStateException("protocol != HTTP_2");
/* 236 */     return newStream(associatedStreamId, requestHeaders, out, false);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public FramedStream newStream(List<Header> requestHeaders, boolean out, boolean in)
/*     */     throws IOException
/*     */   {
/* 249 */     return newStream(0, requestHeaders, out, in);
/*     */   }
/*     */   
/*     */   private FramedStream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out, boolean in) throws IOException
/*     */   {
/* 254 */     boolean outFinished = !out;
/* 255 */     boolean inFinished = !in;
/*     */     
/*     */     FramedStream stream;
/*     */     
/* 259 */     synchronized (this.frameWriter) { int streamId;
/* 260 */       synchronized (this) {
/* 261 */         if (this.shutdown) {
/* 262 */           throw new IOException("shutdown");
/*     */         }
/* 264 */         streamId = this.nextStreamId;
/* 265 */         this.nextStreamId += 2;
/* 266 */         stream = new FramedStream(streamId, this, outFinished, inFinished, requestHeaders);
/* 267 */         if (stream.isOpen()) {
/* 268 */           this.streams.put(Integer.valueOf(streamId), stream);
/* 269 */           setIdle(false);
/*     */         }
/*     */       }
/* 272 */       if (associatedStreamId == 0) {
/* 273 */         this.frameWriter.synStream(outFinished, inFinished, streamId, associatedStreamId, requestHeaders);
/*     */       } else {
/* 275 */         if (this.client) {
/* 276 */           throw new IllegalArgumentException("client streams shouldn't have associated stream IDs");
/*     */         }
/* 278 */         this.frameWriter.pushPromise(associatedStreamId, streamId, requestHeaders);
/*     */       }
/*     */     }
/*     */     
/* 282 */     if (!out) {
/* 283 */       this.frameWriter.flush();
/*     */     }
/*     */     
/* 286 */     return stream;
/*     */   }
/*     */   
/*     */   void writeSynReply(int streamId, boolean outFinished, List<Header> alternating) throws IOException
/*     */   {
/* 291 */     this.frameWriter.synReply(outFinished, streamId, alternating);
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
/*     */ 
/*     */ 
/*     */   public void writeData(int streamId, boolean outFinished, Buffer buffer, long byteCount)
/*     */     throws IOException
/*     */   {
/* 308 */     if (byteCount == 0L) {
/* 309 */       this.frameWriter.data(outFinished, streamId, buffer, 0);
/* 310 */       return;
/*     */     }
/*     */     
/* 313 */     while (byteCount > 0L) {
/*     */       int toWrite;
/* 315 */       synchronized (this) {
/*     */         try {
/* 317 */           while (this.bytesLeftInWriteWindow <= 0L)
/*     */           {
/*     */ 
/* 320 */             if (!this.streams.containsKey(Integer.valueOf(streamId))) {
/* 321 */               throw new IOException("stream closed");
/*     */             }
/* 323 */             wait();
/*     */           }
/*     */         } catch (InterruptedException e) {
/* 326 */           throw new InterruptedIOException();
/*     */         }
/*     */         
/* 329 */         toWrite = (int)Math.min(byteCount, this.bytesLeftInWriteWindow);
/* 330 */         toWrite = Math.min(toWrite, this.frameWriter.maxDataLength());
/* 331 */         this.bytesLeftInWriteWindow -= toWrite;
/*     */       }
/*     */       
/* 334 */       byteCount -= toWrite;
/* 335 */       this.frameWriter.data((outFinished) && (byteCount == 0L), streamId, buffer, toWrite);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   void addBytesToWriteWindow(long delta)
/*     */   {
/* 344 */     this.bytesLeftInWriteWindow += delta;
/* 345 */     if (delta > 0L) notifyAll();
/*     */   }
/*     */   
/*     */   void writeSynResetLater(final int streamId, final ErrorCode errorCode) {
/* 349 */     executor.submit(new NamedRunnable("OkHttp %s stream %d", new Object[] { this.hostName, Integer.valueOf(streamId) }) {
/*     */       public void execute() {
/*     */         try {
/* 352 */           FramedConnection.this.writeSynReset(streamId, errorCode);
/*     */         }
/*     */         catch (IOException localIOException) {}
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   void writeSynReset(int streamId, ErrorCode statusCode) throws IOException {
/* 360 */     this.frameWriter.rstStream(streamId, statusCode);
/*     */   }
/*     */   
/*     */   void writeWindowUpdateLater(final int streamId, final long unacknowledgedBytesRead) {
/* 364 */     executor.execute(new NamedRunnable("OkHttp Window Update %s stream %d", new Object[] { this.hostName, Integer.valueOf(streamId) }) {
/*     */       public void execute() {
/*     */         try {
/* 367 */           FramedConnection.this.frameWriter.windowUpdate(streamId, unacknowledgedBytesRead);
/*     */         }
/*     */         catch (IOException localIOException) {}
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public Ping ping()
/*     */     throws IOException
/*     */   {
/* 379 */     Ping ping = new Ping();
/*     */     int pingId;
/* 381 */     synchronized (this) {
/* 382 */       if (this.shutdown) {
/* 383 */         throw new IOException("shutdown");
/*     */       }
/* 385 */       pingId = this.nextPingId;
/* 386 */       this.nextPingId += 2;
/* 387 */       if (this.pings == null) this.pings = new HashMap();
/* 388 */       this.pings.put(Integer.valueOf(pingId), ping);
/*     */     }
/* 390 */     writePing(false, pingId, 1330343787, ping);
/* 391 */     return ping;
/*     */   }
/*     */   
/*     */   private void writePingLater(final boolean reply, final int payload1, final int payload2, final Ping ping)
/*     */   {
/* 396 */     executor.execute(new NamedRunnable("OkHttp %s ping %08x%08x", new Object[] { this.hostName, 
/* 397 */       Integer.valueOf(payload1), Integer.valueOf(payload2) })
/*     */       {
/*     */         public void execute() {
/*     */           try {
/* 400 */             FramedConnection.this.writePing(reply, payload1, payload2, ping);
/*     */           }
/*     */           catch (IOException localIOException) {}
/*     */         }
/*     */       });
/*     */   }
/*     */   
/*     */   private void writePing(boolean reply, int payload1, int payload2, Ping ping) throws IOException {
/* 408 */     synchronized (this.frameWriter)
/*     */     {
/* 410 */       if (ping != null) ping.send();
/* 411 */       this.frameWriter.ping(reply, payload1, payload2);
/*     */     }
/*     */   }
/*     */   
/*     */   private synchronized Ping removePing(int id) {
/* 416 */     return this.pings != null ? (Ping)this.pings.remove(Integer.valueOf(id)) : null;
/*     */   }
/*     */   
/*     */   public void flush() throws IOException {
/* 420 */     this.frameWriter.flush();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void shutdown(ErrorCode statusCode)
/*     */     throws IOException
/*     */   {
/* 430 */     synchronized (this.frameWriter) {
/*     */       int lastGoodStreamId;
/* 432 */       synchronized (this) {
/* 433 */         if (this.shutdown) {
/* 434 */           return;
/*     */         }
/* 436 */         this.shutdown = true;
/* 437 */         lastGoodStreamId = this.lastGoodStreamId;
/*     */       }
/*     */       
/* 440 */       this.frameWriter.goAway(lastGoodStreamId, statusCode, Util.EMPTY_BYTE_ARRAY);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void close()
/*     */     throws IOException
/*     */   {
/* 450 */     close(ErrorCode.NO_ERROR, ErrorCode.CANCEL);
/*     */   }
/*     */   
/*     */   private void close(ErrorCode connectionCode, ErrorCode streamCode) throws IOException {
/* 454 */     assert (!Thread.holdsLock(this));
/* 455 */     IOException thrown = null;
/*     */     try {
/* 457 */       shutdown(connectionCode);
/*     */     } catch (IOException e) {
/* 459 */       thrown = e;
/*     */     }
/*     */     
/* 462 */     FramedStream[] streamsToClose = null;
/* 463 */     Ping[] pingsToCancel = null;
/* 464 */     synchronized (this) {
/* 465 */       if (!this.streams.isEmpty()) {
/* 466 */         streamsToClose = (FramedStream[])this.streams.values().toArray(new FramedStream[this.streams.size()]);
/* 467 */         this.streams.clear();
/* 468 */         setIdle(false);
/*     */       }
/* 470 */       if (this.pings != null) {
/* 471 */         pingsToCancel = (Ping[])this.pings.values().toArray(new Ping[this.pings.size()]);
/* 472 */         this.pings = null;
/*     */       }
/*     */     }
/*     */     
/* 476 */     if (streamsToClose != null) {
/* 477 */       for (FramedStream stream : streamsToClose) {
/*     */         try {
/* 479 */           stream.close(streamCode);
/*     */         } catch (IOException e) {
/* 481 */           if (thrown != null) { thrown = e;
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/* 486 */     if (pingsToCancel != null) {
/* 487 */       for (Ping ping : pingsToCancel) {
/* 488 */         ping.cancel();
/*     */       }
/*     */     }
/*     */     
/*     */     try
/*     */     {
/* 494 */       this.frameWriter.close();
/*     */     } catch (IOException e) {
/* 496 */       if (thrown == null) { thrown = e;
/*     */       }
/*     */     }
/*     */     try
/*     */     {
/* 501 */       this.socket.close();
/*     */     } catch (IOException e) {
/* 503 */       thrown = e;
/*     */     }
/*     */     
/* 506 */     if (thrown != null) { throw thrown;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public void sendConnectionPreface()
/*     */     throws IOException
/*     */   {
/* 514 */     this.frameWriter.connectionPreface();
/* 515 */     this.frameWriter.settings(this.okHttpSettings);
/* 516 */     int windowSize = this.okHttpSettings.getInitialWindowSize(65536);
/* 517 */     if (windowSize != 65536) {
/* 518 */       this.frameWriter.windowUpdate(0, windowSize - 65536);
/*     */     }
/*     */   }
/*     */   
/*     */   public void setSettings(Settings settings) throws IOException
/*     */   {
/* 524 */     synchronized (this.frameWriter) {
/* 525 */       synchronized (this) {
/* 526 */         if (this.shutdown) {
/* 527 */           throw new IOException("shutdown");
/*     */         }
/* 529 */         this.okHttpSettings.merge(settings);
/* 530 */         this.frameWriter.settings(settings);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public static class Builder {
/*     */     private Socket socket;
/*     */     private String hostName;
/*     */     private BufferedSource source;
/*     */     private BufferedSink sink;
/* 540 */     private FramedConnection.Listener listener = FramedConnection.Listener.REFUSE_INCOMING_STREAMS;
/* 541 */     private Protocol protocol = Protocol.SPDY_3;
/* 542 */     private PushObserver pushObserver = PushObserver.CANCEL;
/*     */     
/*     */     private boolean client;
/*     */     
/*     */ 
/*     */     public Builder(boolean client)
/*     */       throws IOException
/*     */     {
/* 550 */       this.client = client;
/*     */     }
/*     */     
/*     */     public Builder socket(Socket socket) throws IOException {
/* 554 */       return socket(socket, ((InetSocketAddress)socket.getRemoteSocketAddress()).getHostName(), 
/* 555 */         Okio.buffer(Okio.source(socket)), Okio.buffer(Okio.sink(socket)));
/*     */     }
/*     */     
/*     */     public Builder socket(Socket socket, String hostName, BufferedSource source, BufferedSink sink)
/*     */     {
/* 560 */       this.socket = socket;
/* 561 */       this.hostName = hostName;
/* 562 */       this.source = source;
/* 563 */       this.sink = sink;
/* 564 */       return this;
/*     */     }
/*     */     
/*     */     public Builder listener(FramedConnection.Listener listener) {
/* 568 */       this.listener = listener;
/* 569 */       return this;
/*     */     }
/*     */     
/*     */     public Builder protocol(Protocol protocol) {
/* 573 */       this.protocol = protocol;
/* 574 */       return this;
/*     */     }
/*     */     
/*     */     public Builder pushObserver(PushObserver pushObserver) {
/* 578 */       this.pushObserver = pushObserver;
/* 579 */       return this;
/*     */     }
/*     */     
/*     */     public FramedConnection build() throws IOException {
/* 583 */       return new FramedConnection(this, null);
/*     */     }
/*     */   }
/*     */   
/*     */   class Reader
/*     */     extends NamedRunnable
/*     */     implements FrameReader.Handler
/*     */   {
/*     */     final FrameReader frameReader;
/*     */     
/*     */     private Reader(FrameReader frameReader)
/*     */     {
/* 595 */       super(new Object[] { FramedConnection.this.hostName });
/* 596 */       this.frameReader = frameReader;
/*     */     }
/*     */     
/*     */     protected void execute() {
/* 600 */       ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
/* 601 */       ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;
/*     */       try {
/* 603 */         if (!FramedConnection.this.client) {
/* 604 */           this.frameReader.readConnectionPreface();
/*     */         }
/* 606 */         while (this.frameReader.nextFrame(this)) {}
/*     */         
/* 608 */         connectionErrorCode = ErrorCode.NO_ERROR;
/* 609 */         streamErrorCode = ErrorCode.CANCEL;
/*     */       } catch (IOException e) {
/* 611 */         connectionErrorCode = ErrorCode.PROTOCOL_ERROR;
/* 612 */         streamErrorCode = ErrorCode.PROTOCOL_ERROR;
/*     */       } finally {
/*     */         try {
/* 615 */           FramedConnection.this.close(connectionErrorCode, streamErrorCode);
/*     */         }
/*     */         catch (IOException localIOException3) {}
/* 618 */         Util.closeQuietly(this.frameReader);
/*     */       }
/*     */     }
/*     */     
/*     */     public void data(boolean inFinished, int streamId, BufferedSource source, int length) throws IOException
/*     */     {
/* 624 */       if (FramedConnection.this.pushedStream(streamId)) {
/* 625 */         FramedConnection.this.pushDataLater(streamId, source, length, inFinished);
/* 626 */         return;
/*     */       }
/* 628 */       FramedStream dataStream = FramedConnection.this.getStream(streamId);
/* 629 */       if (dataStream == null) {
/* 630 */         FramedConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
/* 631 */         source.skip(length);
/* 632 */         return;
/*     */       }
/* 634 */       dataStream.receiveData(source, length);
/* 635 */       if (inFinished) {
/* 636 */         dataStream.receiveFin();
/*     */       }
/*     */     }
/*     */     
/*     */     public void headers(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock, HeadersMode headersMode)
/*     */     {
/* 642 */       if (FramedConnection.this.pushedStream(streamId)) {
/* 643 */         FramedConnection.this.pushHeadersLater(streamId, headerBlock, inFinished); return;
/*     */       }
/*     */       
/*     */       FramedStream stream;
/* 647 */       synchronized (FramedConnection.this)
/*     */       {
/* 649 */         if (FramedConnection.this.shutdown) { return;
/*     */         }
/* 651 */         stream = FramedConnection.this.getStream(streamId);
/*     */         
/* 653 */         if (stream == null)
/*     */         {
/* 655 */           if (headersMode.failIfStreamAbsent()) {
/* 656 */             FramedConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
/* 657 */             return;
/*     */           }
/*     */           
/*     */ 
/* 661 */           if (streamId <= FramedConnection.this.lastGoodStreamId) { return;
/*     */           }
/*     */           
/* 664 */           if (streamId % 2 == FramedConnection.this.nextStreamId % 2) { return;
/*     */           }
/*     */           
/*     */ 
/* 668 */           final FramedStream newStream = new FramedStream(streamId, FramedConnection.this, outFinished, inFinished, headerBlock);
/*     */           
/* 670 */           FramedConnection.this.lastGoodStreamId = streamId;
/* 671 */           FramedConnection.this.streams.put(Integer.valueOf(streamId), newStream);
/* 672 */           FramedConnection.executor.execute(new NamedRunnable("OkHttp %s stream %d", new Object[] { FramedConnection.this.hostName, Integer.valueOf(streamId) }) {
/*     */             public void execute() {
/*     */               try {
/* 675 */                 FramedConnection.this.listener.onStream(newStream);
/*     */               } catch (IOException e) {
/* 677 */                 Internal.logger.log(Level.INFO, "FramedConnection.Listener failure for " + FramedConnection.this.hostName, e);
/*     */                 try {
/* 679 */                   newStream.close(ErrorCode.PROTOCOL_ERROR);
/*     */                 }
/*     */                 catch (IOException localIOException1) {}
/*     */               }
/*     */             }
/* 684 */           });
/* 685 */           return;
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 690 */       if (headersMode.failIfStreamPresent()) {
/* 691 */         stream.closeLater(ErrorCode.PROTOCOL_ERROR);
/* 692 */         FramedConnection.this.removeStream(streamId);
/* 693 */         return;
/*     */       }
/*     */       
/*     */ 
/* 697 */       stream.receiveHeaders(headerBlock, headersMode);
/* 698 */       if (inFinished) stream.receiveFin();
/*     */     }
/*     */     
/*     */     public void rstStream(int streamId, ErrorCode errorCode) {
/* 702 */       if (FramedConnection.this.pushedStream(streamId)) {
/* 703 */         FramedConnection.this.pushResetLater(streamId, errorCode);
/* 704 */         return;
/*     */       }
/* 706 */       FramedStream rstStream = FramedConnection.this.removeStream(streamId);
/* 707 */       if (rstStream != null) {
/* 708 */         rstStream.receiveRstStream(errorCode);
/*     */       }
/*     */     }
/*     */     
/*     */     public void settings(boolean clearPrevious, Settings newSettings) {
/* 713 */       long delta = 0L;
/* 714 */       FramedStream[] streamsToNotify = null;
/* 715 */       int priorWriteWindowSize; int peerInitialWindowSize; synchronized (FramedConnection.this) {
/* 716 */         priorWriteWindowSize = FramedConnection.this.peerSettings.getInitialWindowSize(65536);
/* 717 */         if (clearPrevious) FramedConnection.this.peerSettings.clear();
/* 718 */         FramedConnection.this.peerSettings.merge(newSettings);
/* 719 */         if (FramedConnection.this.getProtocol() == Protocol.HTTP_2) {
/* 720 */           ackSettingsLater(newSettings);
/*     */         }
/* 722 */         peerInitialWindowSize = FramedConnection.this.peerSettings.getInitialWindowSize(65536);
/* 723 */         if ((peerInitialWindowSize != -1) && (peerInitialWindowSize != priorWriteWindowSize)) {
/* 724 */           delta = peerInitialWindowSize - priorWriteWindowSize;
/* 725 */           if (!FramedConnection.this.receivedInitialPeerSettings) {
/* 726 */             FramedConnection.this.addBytesToWriteWindow(delta);
/* 727 */             FramedConnection.this.receivedInitialPeerSettings = true;
/*     */           }
/* 729 */           if (!FramedConnection.this.streams.isEmpty()) {
/* 730 */             streamsToNotify = (FramedStream[])FramedConnection.this.streams.values().toArray(new FramedStream[FramedConnection.this.streams.size()]);
/*     */           }
/*     */         }
/* 733 */         FramedConnection.executor.execute(new NamedRunnable("OkHttp %s settings", new Object[] { FramedConnection.this.hostName }) {
/*     */           public void execute() {
/* 735 */             FramedConnection.this.listener.onSettings(FramedConnection.this);
/*     */           }
/*     */         });
/*     */       }
/* 739 */       if ((streamsToNotify != null) && (delta != 0L)) {
/* 740 */         for (FramedStream stream : streamsToNotify) {
/* 741 */           synchronized (stream) {
/* 742 */             stream.addBytesToWriteWindow(delta);
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     private void ackSettingsLater(final Settings peerSettings) {
/* 749 */       FramedConnection.executor.execute(new NamedRunnable("OkHttp %s ACK Settings", new Object[] { FramedConnection.this.hostName }) {
/*     */         public void execute() {
/*     */           try {
/* 752 */             FramedConnection.this.frameWriter.ackSettings(peerSettings);
/*     */           }
/*     */           catch (IOException localIOException) {}
/*     */         }
/*     */       });
/*     */     }
/*     */     
/*     */ 
/*     */     public void ackSettings() {}
/*     */     
/*     */     public void ping(boolean reply, int payload1, int payload2)
/*     */     {
/* 764 */       if (reply) {
/* 765 */         Ping ping = FramedConnection.this.removePing(payload1);
/* 766 */         if (ping != null) {
/* 767 */           ping.receive();
/*     */         }
/*     */       }
/*     */       else {
/* 771 */         FramedConnection.this.writePingLater(true, payload1, payload2, null);
/*     */       }
/*     */     }
/*     */     
/*     */     public void goAway(int lastGoodStreamId, ErrorCode errorCode, ByteString debugData) {
/* 776 */       if (debugData.size() > 0) {}
/*     */       
/*     */ 
/*     */       FramedStream[] streamsCopy;
/*     */       
/* 781 */       synchronized (FramedConnection.this) {
/* 782 */         streamsCopy = (FramedStream[])FramedConnection.this.streams.values().toArray(new FramedStream[FramedConnection.this.streams.size()]);
/* 783 */         FramedConnection.this.shutdown = true;
/*     */       }
/*     */       
/*     */ 
/* 787 */       for (FramedStream framedStream : streamsCopy) {
/* 788 */         if ((framedStream.getId() > lastGoodStreamId) && (framedStream.isLocallyInitiated())) {
/* 789 */           framedStream.receiveRstStream(ErrorCode.REFUSED_STREAM);
/* 790 */           FramedConnection.this.removeStream(framedStream.getId());
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     public void windowUpdate(int streamId, long windowSizeIncrement) {
/* 796 */       if (streamId == 0) {
/* 797 */         synchronized (FramedConnection.this) {
/* 798 */           FramedConnection.this.bytesLeftInWriteWindow += windowSizeIncrement;
/* 799 */           FramedConnection.this.notifyAll();
/*     */         }
/*     */       } else {
/* 802 */         FramedStream stream = FramedConnection.this.getStream(streamId);
/* 803 */         if (stream != null) {
/* 804 */           synchronized (stream) {
/* 805 */             stream.addBytesToWriteWindow(windowSizeIncrement);
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */     public void priority(int streamId, int streamDependency, int weight, boolean exclusive) {}
/*     */     
/*     */ 
/*     */     public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders)
/*     */     {
/* 818 */       FramedConnection.this.pushRequestLater(promisedStreamId, requestHeaders);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */     public void alternateService(int streamId, String origin, ByteString protocol, String host, int port, long maxAge) {}
/*     */   }
/*     */   
/*     */ 
/*     */   private boolean pushedStream(int streamId)
/*     */   {
/* 829 */     return (this.protocol == Protocol.HTTP_2) && (streamId != 0) && ((streamId & 0x1) == 0);
/*     */   }
/*     */   
/*     */ 
/* 833 */   private final Set<Integer> currentPushRequests = new LinkedHashSet();
/*     */   
/*     */   private void pushRequestLater(final int streamId, final List<Header> requestHeaders) {
/* 836 */     synchronized (this) {
/* 837 */       if (this.currentPushRequests.contains(Integer.valueOf(streamId))) {
/* 838 */         writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
/* 839 */         return;
/*     */       }
/* 841 */       this.currentPushRequests.add(Integer.valueOf(streamId));
/*     */     }
/* 843 */     this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Request[%s]", new Object[] { this.hostName, Integer.valueOf(streamId) }) {
/*     */       public void execute() {
/* 845 */         boolean cancel = FramedConnection.this.pushObserver.onRequest(streamId, requestHeaders);
/*     */         try {
/* 847 */           if (cancel) {
/* 848 */             FramedConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
/* 849 */             synchronized (FramedConnection.this) {
/* 850 */               FramedConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
/*     */             }
/*     */           }
/*     */         }
/*     */         catch (IOException localIOException) {}
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void pushHeadersLater(final int streamId, final List<Header> requestHeaders, final boolean inFinished)
/*     */   {
/* 861 */     this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Headers[%s]", new Object[] { this.hostName, Integer.valueOf(streamId) }) {
/*     */       public void execute() {
/* 863 */         boolean cancel = FramedConnection.this.pushObserver.onHeaders(streamId, requestHeaders, inFinished);
/*     */         try {
/* 865 */           if (cancel) FramedConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
/* 866 */           if ((cancel) || (inFinished)) {
/* 867 */             synchronized (FramedConnection.this) {
/* 868 */               FramedConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
/*     */             }
/*     */           }
/*     */         }
/*     */         catch (IOException localIOException) {}
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void pushDataLater(final int streamId, BufferedSource source, final int byteCount, final boolean inFinished)
/*     */     throws IOException
/*     */   {
/* 883 */     final Buffer buffer = new Buffer();
/* 884 */     source.require(byteCount);
/* 885 */     source.read(buffer, byteCount);
/* 886 */     if (buffer.size() != byteCount) throw new IOException(buffer.size() + " != " + byteCount);
/* 887 */     this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Data[%s]", new Object[] { this.hostName, Integer.valueOf(streamId) }) {
/*     */       public void execute() {
/*     */         try {
/* 890 */           boolean cancel = FramedConnection.this.pushObserver.onData(streamId, buffer, byteCount, inFinished);
/* 891 */           if (cancel) FramedConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
/* 892 */           if ((cancel) || (inFinished)) {
/* 893 */             synchronized (FramedConnection.this) {
/* 894 */               FramedConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
/*     */             }
/*     */           }
/*     */         }
/*     */         catch (IOException localIOException) {}
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void pushResetLater(final int streamId, final ErrorCode errorCode) {
/* 904 */     this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Reset[%s]", new Object[] { this.hostName, Integer.valueOf(streamId) }) {
/*     */       public void execute() {
/* 906 */         FramedConnection.this.pushObserver.onReset(streamId, errorCode);
/* 907 */         synchronized (FramedConnection.this) {
/* 908 */           FramedConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
/*     */         }
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public static abstract class Listener
/*     */   {
/* 916 */     public static final Listener REFUSE_INCOMING_STREAMS = new Listener() {
/*     */       public void onStream(FramedStream stream) throws IOException {
/* 918 */         stream.close(ErrorCode.REFUSED_STREAM);
/*     */       }
/*     */     };
/*     */     
/*     */     public abstract void onStream(FramedStream paramFramedStream)
/*     */       throws IOException;
/*     */     
/*     */     public void onSettings(FramedConnection connection) {}
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\FramedConnection.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */