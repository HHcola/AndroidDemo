/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.Connection;
/*     */ import com.squareup.okhttp.Headers;
/*     */ import com.squareup.okhttp.Headers.Builder;
/*     */ import com.squareup.okhttp.Request;
/*     */ import com.squareup.okhttp.Response;
/*     */ import com.squareup.okhttp.Response.Builder;
/*     */ import com.squareup.okhttp.ResponseBody;
/*     */ import com.squareup.okhttp.Route;
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.io.RealConnection;
/*     */ import java.io.IOException;
/*     */ import java.net.ProtocolException;
/*     */ import java.net.Proxy;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.ForwardingTimeout;
/*     */ import okio.Okio;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Http1xStream
/*     */   implements HttpStream
/*     */ {
/*     */   private static final int STATE_IDLE = 0;
/*     */   private static final int STATE_OPEN_REQUEST_BODY = 1;
/*     */   private static final int STATE_WRITING_REQUEST_BODY = 2;
/*     */   private static final int STATE_READ_RESPONSE_HEADERS = 3;
/*     */   private static final int STATE_OPEN_RESPONSE_BODY = 4;
/*     */   private static final int STATE_READING_RESPONSE_BODY = 5;
/*     */   private static final int STATE_CLOSED = 6;
/*     */   private final StreamAllocation streamAllocation;
/*     */   private final BufferedSource source;
/*     */   private final BufferedSink sink;
/*     */   private HttpEngine httpEngine;
/*  75 */   private int state = 0;
/*     */   
/*     */   public Http1xStream(StreamAllocation streamAllocation, BufferedSource source, BufferedSink sink) {
/*  78 */     this.streamAllocation = streamAllocation;
/*  79 */     this.source = source;
/*  80 */     this.sink = sink;
/*     */   }
/*     */   
/*     */   public void setHttpEngine(HttpEngine httpEngine) {
/*  84 */     this.httpEngine = httpEngine;
/*     */   }
/*     */   
/*     */   public Sink createRequestBody(Request request, long contentLength) throws IOException {
/*  88 */     if ("chunked".equalsIgnoreCase(request.header("Transfer-Encoding")))
/*     */     {
/*  90 */       return newChunkedSink();
/*     */     }
/*     */     
/*  93 */     if (contentLength != -1L)
/*     */     {
/*  95 */       return newFixedLengthSink(contentLength);
/*     */     }
/*     */     
/*  98 */     throw new IllegalStateException("Cannot stream a request body without chunked encoding or a known content length!");
/*     */   }
/*     */   
/*     */   public void cancel()
/*     */   {
/* 103 */     RealConnection connection = this.streamAllocation.connection();
/* 104 */     if (connection != null) { connection.cancel();
/*     */     }
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
/*     */   public void writeRequestHeaders(Request request)
/*     */     throws IOException
/*     */   {
/* 120 */     this.httpEngine.writingRequestHeaders();
/* 121 */     String requestLine = RequestLine.get(request, this.httpEngine
/* 122 */       .getConnection().getRoute().getProxy().type());
/* 123 */     writeRequest(request.headers(), requestLine);
/*     */   }
/*     */   
/*     */   public Response.Builder readResponseHeaders() throws IOException {
/* 127 */     return readResponse();
/*     */   }
/*     */   
/*     */   public ResponseBody openResponseBody(Response response) throws IOException {
/* 131 */     Source source = getTransferStream(response);
/* 132 */     return new RealResponseBody(response.headers(), Okio.buffer(source));
/*     */   }
/*     */   
/*     */   private Source getTransferStream(Response response) throws IOException {
/* 136 */     if (!HttpEngine.hasBody(response)) {
/* 137 */       return newFixedLengthSource(0L);
/*     */     }
/*     */     
/* 140 */     if ("chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
/* 141 */       return newChunkedSource(this.httpEngine);
/*     */     }
/*     */     
/* 144 */     long contentLength = OkHeaders.contentLength(response);
/* 145 */     if (contentLength != -1L) {
/* 146 */       return newFixedLengthSource(contentLength);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 152 */     return newUnknownLengthSource();
/*     */   }
/*     */   
/*     */   public boolean isClosed()
/*     */   {
/* 157 */     return this.state == 6;
/*     */   }
/*     */   
/*     */   public void finishRequest() throws IOException {
/* 161 */     this.sink.flush();
/*     */   }
/*     */   
/*     */   public void writeRequest(Headers headers, String requestLine) throws IOException
/*     */   {
/* 166 */     if (this.state != 0) throw new IllegalStateException("state: " + this.state);
/* 167 */     this.sink.writeUtf8(requestLine).writeUtf8("\r\n");
/* 168 */     int i = 0; for (int size = headers.size(); i < size; i++)
/*     */     {
/*     */ 
/*     */ 
/* 172 */       this.sink.writeUtf8(headers.name(i)).writeUtf8(": ").writeUtf8(headers.value(i)).writeUtf8("\r\n");
/*     */     }
/* 174 */     this.sink.writeUtf8("\r\n");
/* 175 */     this.state = 1;
/*     */   }
/*     */   
/*     */   /* Error */
/*     */   public Response.Builder readResponse()
/*     */     throws IOException
/*     */   {
/*     */     // Byte code:
/*     */     //   0: aload_0
/*     */     //   1: getfield 4	com/squareup/okhttp/internal/http/Http1xStream:state	I
/*     */     //   4: iconst_1
/*     */     //   5: if_icmpeq +41 -> 46
/*     */     //   8: aload_0
/*     */     //   9: getfield 4	com/squareup/okhttp/internal/http/Http1xStream:state	I
/*     */     //   12: iconst_3
/*     */     //   13: if_icmpeq +33 -> 46
/*     */     //   16: new 18	java/lang/IllegalStateException
/*     */     //   19: dup
/*     */     //   20: new 44	java/lang/StringBuilder
/*     */     //   23: dup
/*     */     //   24: invokespecial 45	java/lang/StringBuilder:<init>	()V
/*     */     //   27: ldc 46
/*     */     //   29: invokevirtual 47	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*     */     //   32: aload_0
/*     */     //   33: getfield 4	com/squareup/okhttp/internal/http/Http1xStream:state	I
/*     */     //   36: invokevirtual 48	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
/*     */     //   39: invokevirtual 49	java/lang/StringBuilder:toString	()Ljava/lang/String;
/*     */     //   42: invokespecial 20	java/lang/IllegalStateException:<init>	(Ljava/lang/String;)V
/*     */     //   45: athrow
/*     */     //   46: aload_0
/*     */     //   47: getfield 3	com/squareup/okhttp/internal/http/Http1xStream:source	Lokio/BufferedSource;
/*     */     //   50: invokeinterface 56 1 0
/*     */     //   55: invokestatic 57	com/squareup/okhttp/internal/http/StatusLine:parse	(Ljava/lang/String;)Lcom/squareup/okhttp/internal/http/StatusLine;
/*     */     //   58: astore_1
/*     */     //   59: new 58	com/squareup/okhttp/Response$Builder
/*     */     //   62: dup
/*     */     //   63: invokespecial 59	com/squareup/okhttp/Response$Builder:<init>	()V
/*     */     //   66: aload_1
/*     */     //   67: getfield 60	com/squareup/okhttp/internal/http/StatusLine:protocol	Lcom/squareup/okhttp/Protocol;
/*     */     //   70: invokevirtual 61	com/squareup/okhttp/Response$Builder:protocol	(Lcom/squareup/okhttp/Protocol;)Lcom/squareup/okhttp/Response$Builder;
/*     */     //   73: aload_1
/*     */     //   74: getfield 62	com/squareup/okhttp/internal/http/StatusLine:code	I
/*     */     //   77: invokevirtual 63	com/squareup/okhttp/Response$Builder:code	(I)Lcom/squareup/okhttp/Response$Builder;
/*     */     //   80: aload_1
/*     */     //   81: getfield 64	com/squareup/okhttp/internal/http/StatusLine:message	Ljava/lang/String;
/*     */     //   84: invokevirtual 65	com/squareup/okhttp/Response$Builder:message	(Ljava/lang/String;)Lcom/squareup/okhttp/Response$Builder;
/*     */     //   87: aload_0
/*     */     //   88: invokevirtual 66	com/squareup/okhttp/internal/http/Http1xStream:readHeaders	()Lcom/squareup/okhttp/Headers;
/*     */     //   91: invokevirtual 67	com/squareup/okhttp/Response$Builder:headers	(Lcom/squareup/okhttp/Headers;)Lcom/squareup/okhttp/Response$Builder;
/*     */     //   94: astore_2
/*     */     //   95: aload_1
/*     */     //   96: getfield 62	com/squareup/okhttp/internal/http/StatusLine:code	I
/*     */     //   99: bipush 100
/*     */     //   101: if_icmpeq +10 -> 111
/*     */     //   104: aload_0
/*     */     //   105: iconst_4
/*     */     //   106: putfield 4	com/squareup/okhttp/internal/http/Http1xStream:state	I
/*     */     //   109: aload_2
/*     */     //   110: areturn
/*     */     //   111: goto -65 -> 46
/*     */     //   114: astore_1
/*     */     //   115: new 69	java/io/IOException
/*     */     //   118: dup
/*     */     //   119: new 44	java/lang/StringBuilder
/*     */     //   122: dup
/*     */     //   123: invokespecial 45	java/lang/StringBuilder:<init>	()V
/*     */     //   126: ldc 70
/*     */     //   128: invokevirtual 47	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*     */     //   131: aload_0
/*     */     //   132: getfield 2	com/squareup/okhttp/internal/http/Http1xStream:streamAllocation	Lcom/squareup/okhttp/internal/http/StreamAllocation;
/*     */     //   135: invokevirtual 71	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
/*     */     //   138: invokevirtual 49	java/lang/StringBuilder:toString	()Ljava/lang/String;
/*     */     //   141: invokespecial 72	java/io/IOException:<init>	(Ljava/lang/String;)V
/*     */     //   144: astore_2
/*     */     //   145: aload_2
/*     */     //   146: aload_1
/*     */     //   147: invokevirtual 73	java/io/IOException:initCause	(Ljava/lang/Throwable;)Ljava/lang/Throwable;
/*     */     //   150: pop
/*     */     //   151: aload_2
/*     */     //   152: athrow
/*     */     // Line number table:
/*     */     //   Java source line #180	-> byte code offset #0
/*     */     //   Java source line #181	-> byte code offset #16
/*     */     //   Java source line #186	-> byte code offset #46
/*     */     //   Java source line #188	-> byte code offset #59
/*     */     //   Java source line #189	-> byte code offset #70
/*     */     //   Java source line #190	-> byte code offset #77
/*     */     //   Java source line #191	-> byte code offset #84
/*     */     //   Java source line #192	-> byte code offset #88
/*     */     //   Java source line #194	-> byte code offset #95
/*     */     //   Java source line #195	-> byte code offset #104
/*     */     //   Java source line #196	-> byte code offset #109
/*     */     //   Java source line #198	-> byte code offset #111
/*     */     //   Java source line #199	-> byte code offset #114
/*     */     //   Java source line #201	-> byte code offset #115
/*     */     //   Java source line #202	-> byte code offset #145
/*     */     //   Java source line #203	-> byte code offset #151
/*     */     // Local variable table:
/*     */     //   start	length	slot	name	signature
/*     */     //   0	153	0	this	Http1xStream
/*     */     //   58	38	1	statusLine	StatusLine
/*     */     //   114	33	1	e	java.io.EOFException
/*     */     //   94	16	2	responseBuilder	Response.Builder
/*     */     //   144	8	2	exception	IOException
/*     */     // Exception table:
/*     */     //   from	to	target	type
/*     */     //   46	110	114	java/io/EOFException
/*     */     //   111	114	114	java/io/EOFException
/*     */   }
/*     */   
/*     */   public Headers readHeaders()
/*     */     throws IOException
/*     */   {
/* 209 */     Headers.Builder headers = new Headers.Builder();
/*     */     String line;
/* 211 */     while ((line = this.source.readUtf8LineStrict()).length() != 0) {
/* 212 */       Internal.instance.addLenient(headers, line);
/*     */     }
/* 214 */     return headers.build();
/*     */   }
/*     */   
/*     */   public Sink newChunkedSink() {
/* 218 */     if (this.state != 1) throw new IllegalStateException("state: " + this.state);
/* 219 */     this.state = 2;
/* 220 */     return new ChunkedSink(null);
/*     */   }
/*     */   
/*     */   public Sink newFixedLengthSink(long contentLength) {
/* 224 */     if (this.state != 1) throw new IllegalStateException("state: " + this.state);
/* 225 */     this.state = 2;
/* 226 */     return new FixedLengthSink(contentLength, null);
/*     */   }
/*     */   
/*     */   public void writeRequestBody(RetryableSink requestBody) throws IOException {
/* 230 */     if (this.state != 1) throw new IllegalStateException("state: " + this.state);
/* 231 */     this.state = 3;
/* 232 */     requestBody.writeToSocket(this.sink);
/*     */   }
/*     */   
/*     */   public Source newFixedLengthSource(long length) throws IOException {
/* 236 */     if (this.state != 4) throw new IllegalStateException("state: " + this.state);
/* 237 */     this.state = 5;
/* 238 */     return new FixedLengthSource(length);
/*     */   }
/*     */   
/*     */   public Source newChunkedSource(HttpEngine httpEngine) throws IOException {
/* 242 */     if (this.state != 4) throw new IllegalStateException("state: " + this.state);
/* 243 */     this.state = 5;
/* 244 */     return new ChunkedSource(httpEngine);
/*     */   }
/*     */   
/*     */   public Source newUnknownLengthSource() throws IOException {
/* 248 */     if (this.state != 4) throw new IllegalStateException("state: " + this.state);
/* 249 */     if (this.streamAllocation == null) throw new IllegalStateException("streamAllocation == null");
/* 250 */     this.state = 5;
/* 251 */     this.streamAllocation.noNewStreams();
/* 252 */     return new UnknownLengthSource(null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void detachTimeout(ForwardingTimeout timeout)
/*     */   {
/* 261 */     Timeout oldDelegate = timeout.delegate();
/* 262 */     timeout.setDelegate(Timeout.NONE);
/* 263 */     oldDelegate.clearDeadline();
/* 264 */     oldDelegate.clearTimeout();
/*     */   }
/*     */   
/*     */   private final class FixedLengthSink implements Sink
/*     */   {
/* 269 */     private final ForwardingTimeout timeout = new ForwardingTimeout(Http1xStream.this.sink.timeout());
/*     */     private boolean closed;
/*     */     private long bytesRemaining;
/*     */     
/*     */     private FixedLengthSink(long bytesRemaining) {
/* 274 */       this.bytesRemaining = bytesRemaining;
/*     */     }
/*     */     
/*     */     public Timeout timeout() {
/* 278 */       return this.timeout;
/*     */     }
/*     */     
/*     */     public void write(Buffer source, long byteCount) throws IOException {
/* 282 */       if (this.closed) throw new IllegalStateException("closed");
/* 283 */       Util.checkOffsetAndCount(source.size(), 0L, byteCount);
/* 284 */       if (byteCount > this.bytesRemaining) {
/* 285 */         throw new ProtocolException("expected " + this.bytesRemaining + " bytes but received " + byteCount);
/*     */       }
/*     */       
/* 288 */       Http1xStream.this.sink.write(source, byteCount);
/* 289 */       this.bytesRemaining -= byteCount;
/*     */     }
/*     */     
/*     */     public void flush() throws IOException {
/* 293 */       if (this.closed) return;
/* 294 */       Http1xStream.this.sink.flush();
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 298 */       if (this.closed) return;
/* 299 */       this.closed = true;
/* 300 */       if (this.bytesRemaining > 0L) throw new ProtocolException("unexpected end of stream");
/* 301 */       Http1xStream.this.detachTimeout(this.timeout);
/* 302 */       Http1xStream.this.state = 3;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private final class ChunkedSink
/*     */     implements Sink
/*     */   {
/* 312 */     private final ForwardingTimeout timeout = new ForwardingTimeout(Http1xStream.this.sink.timeout());
/*     */     
/*     */     private ChunkedSink() {}
/*     */     
/* 316 */     public Timeout timeout() { return this.timeout; }
/*     */     
/*     */     private boolean closed;
/*     */     public void write(Buffer source, long byteCount) throws IOException {
/* 320 */       if (this.closed) throw new IllegalStateException("closed");
/* 321 */       if (byteCount == 0L) { return;
/*     */       }
/* 323 */       Http1xStream.this.sink.writeHexadecimalUnsignedLong(byteCount);
/* 324 */       Http1xStream.this.sink.writeUtf8("\r\n");
/* 325 */       Http1xStream.this.sink.write(source, byteCount);
/* 326 */       Http1xStream.this.sink.writeUtf8("\r\n");
/*     */     }
/*     */     
/*     */     public synchronized void flush() throws IOException {
/* 330 */       if (this.closed) return;
/* 331 */       Http1xStream.this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void close() throws IOException {
/* 335 */       if (this.closed) return;
/* 336 */       this.closed = true;
/* 337 */       Http1xStream.this.sink.writeUtf8("0\r\n\r\n");
/* 338 */       Http1xStream.this.detachTimeout(this.timeout);
/* 339 */       Http1xStream.this.state = 3;
/*     */     }
/*     */   }
/*     */   
/*     */   private abstract class AbstractSource implements Source {
/* 344 */     protected final ForwardingTimeout timeout = new ForwardingTimeout(Http1xStream.this.source.timeout());
/*     */     
/*     */     private AbstractSource() {}
/*     */     
/* 348 */     public Timeout timeout() { return this.timeout; }
/*     */     
/*     */ 
/*     */     protected boolean closed;
/*     */     
/*     */     protected final void endOfInput()
/*     */       throws IOException
/*     */     {
/* 356 */       if (Http1xStream.this.state != 5) { throw new IllegalStateException("state: " + Http1xStream.this.state);
/*     */       }
/* 358 */       Http1xStream.this.detachTimeout(this.timeout);
/*     */       
/* 360 */       Http1xStream.this.state = 6;
/* 361 */       if (Http1xStream.this.streamAllocation != null) {
/* 362 */         Http1xStream.this.streamAllocation.streamFinished(Http1xStream.this);
/*     */       }
/*     */     }
/*     */     
/*     */     protected final void unexpectedEndOfInput() {
/* 367 */       if (Http1xStream.this.state == 6) { return;
/*     */       }
/* 369 */       Http1xStream.this.state = 6;
/* 370 */       if (Http1xStream.this.streamAllocation != null) {
/* 371 */         Http1xStream.this.streamAllocation.noNewStreams();
/* 372 */         Http1xStream.this.streamAllocation.streamFinished(Http1xStream.this);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private class FixedLengthSource extends Http1xStream.AbstractSource {
/*     */     private long bytesRemaining;
/*     */     
/*     */     public FixedLengthSource(long length) throws IOException {
/* 381 */       super(null);
/* 382 */       this.bytesRemaining = length;
/* 383 */       if (this.bytesRemaining == 0L) {
/* 384 */         endOfInput();
/*     */       }
/*     */     }
/*     */     
/*     */     public long read(Buffer sink, long byteCount) throws IOException {
/* 389 */       if (byteCount < 0L) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
/* 390 */       if (this.closed) throw new IllegalStateException("closed");
/* 391 */       if (this.bytesRemaining == 0L) { return -1L;
/*     */       }
/* 393 */       long read = Http1xStream.this.source.read(sink, Math.min(this.bytesRemaining, byteCount));
/* 394 */       if (read == -1L) {
/* 395 */         unexpectedEndOfInput();
/* 396 */         throw new ProtocolException("unexpected end of stream");
/*     */       }
/*     */       
/* 399 */       this.bytesRemaining -= read;
/* 400 */       if (this.bytesRemaining == 0L) {
/* 401 */         endOfInput();
/*     */       }
/* 403 */       return read;
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 407 */       if (this.closed) { return;
/*     */       }
/* 409 */       if ((this.bytesRemaining != 0L) && 
/* 410 */         (!Util.discard(this, 100, TimeUnit.MILLISECONDS))) {
/* 411 */         unexpectedEndOfInput();
/*     */       }
/*     */       
/* 414 */       this.closed = true;
/*     */     }
/*     */   }
/*     */   
/*     */   private class ChunkedSource extends Http1xStream.AbstractSource
/*     */   {
/*     */     private static final long NO_CHUNK_YET = -1L;
/* 421 */     private long bytesRemainingInChunk = -1L;
/* 422 */     private boolean hasMoreChunks = true;
/*     */     private final HttpEngine httpEngine;
/*     */     
/* 425 */     ChunkedSource(HttpEngine httpEngine) throws IOException { super(null);
/* 426 */       this.httpEngine = httpEngine;
/*     */     }
/*     */     
/*     */     public long read(Buffer sink, long byteCount) throws IOException {
/* 430 */       if (byteCount < 0L) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
/* 431 */       if (this.closed) throw new IllegalStateException("closed");
/* 432 */       if (!this.hasMoreChunks) { return -1L;
/*     */       }
/* 434 */       if ((this.bytesRemainingInChunk == 0L) || (this.bytesRemainingInChunk == -1L)) {
/* 435 */         readChunkSize();
/* 436 */         if (!this.hasMoreChunks) { return -1L;
/*     */         }
/*     */       }
/* 439 */       long read = Http1xStream.this.source.read(sink, Math.min(byteCount, this.bytesRemainingInChunk));
/* 440 */       if (read == -1L) {
/* 441 */         unexpectedEndOfInput();
/* 442 */         throw new ProtocolException("unexpected end of stream");
/*     */       }
/* 444 */       this.bytesRemainingInChunk -= read;
/* 445 */       return read;
/*     */     }
/*     */     
/*     */     private void readChunkSize() throws IOException
/*     */     {
/* 450 */       if (this.bytesRemainingInChunk != -1L) {
/* 451 */         Http1xStream.this.source.readUtf8LineStrict();
/*     */       }
/*     */       try {
/* 454 */         this.bytesRemainingInChunk = Http1xStream.this.source.readHexadecimalUnsignedLong();
/* 455 */         String extensions = Http1xStream.this.source.readUtf8LineStrict().trim();
/* 456 */         if ((this.bytesRemainingInChunk < 0L) || ((!extensions.isEmpty()) && (!extensions.startsWith(";")))) {
/* 457 */           throw new ProtocolException("expected chunk size and optional extensions but was \"" + this.bytesRemainingInChunk + extensions + "\"");
/*     */         }
/*     */       }
/*     */       catch (NumberFormatException e) {
/* 461 */         throw new ProtocolException(e.getMessage());
/*     */       }
/* 463 */       if (this.bytesRemainingInChunk == 0L) {
/* 464 */         this.hasMoreChunks = false;
/* 465 */         this.httpEngine.receiveHeaders(Http1xStream.this.readHeaders());
/* 466 */         endOfInput();
/*     */       }
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 471 */       if (this.closed) return;
/* 472 */       if ((this.hasMoreChunks) && (!Util.discard(this, 100, TimeUnit.MILLISECONDS))) {
/* 473 */         unexpectedEndOfInput();
/*     */       }
/* 475 */       this.closed = true;
/*     */     } }
/*     */   
/*     */   private class UnknownLengthSource extends Http1xStream.AbstractSource { private boolean inputExhausted;
/*     */     
/* 480 */     private UnknownLengthSource() { super(null); }
/*     */     
/*     */     public long read(Buffer sink, long byteCount)
/*     */       throws IOException
/*     */     {
/* 485 */       if (byteCount < 0L) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
/* 486 */       if (this.closed) throw new IllegalStateException("closed");
/* 487 */       if (this.inputExhausted) { return -1L;
/*     */       }
/* 489 */       long read = Http1xStream.this.source.read(sink, byteCount);
/* 490 */       if (read == -1L) {
/* 491 */         this.inputExhausted = true;
/* 492 */         endOfInput();
/* 493 */         return -1L;
/*     */       }
/* 495 */       return read;
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 499 */       if (this.closed) return;
/* 500 */       if (!this.inputExhausted) {
/* 501 */         unexpectedEndOfInput();
/*     */       }
/* 503 */       this.closed = true;
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\Http1xStream.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */