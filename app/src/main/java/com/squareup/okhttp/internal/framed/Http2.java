/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import java.io.IOException;
/*     */ import java.util.List;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.ByteString;
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
/*     */ public final class Http2
/*     */   implements Variant
/*     */ {
/*  43 */   private static final Logger logger = Logger.getLogger(FrameLogger.class.getName());
/*     */   
/*     */   public Protocol getProtocol() {
/*  46 */     return Protocol.HTTP_2;
/*     */   }
/*     */   
/*     */ 
/*  50 */   private static final ByteString CONNECTION_PREFACE = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
/*     */   
/*     */   static final int INITIAL_MAX_FRAME_SIZE = 16384;
/*     */   
/*     */   static final byte TYPE_DATA = 0;
/*     */   
/*     */   static final byte TYPE_HEADERS = 1;
/*     */   
/*     */   static final byte TYPE_PRIORITY = 2;
/*     */   
/*     */   static final byte TYPE_RST_STREAM = 3;
/*     */   
/*     */   static final byte TYPE_SETTINGS = 4;
/*     */   
/*     */   static final byte TYPE_PUSH_PROMISE = 5;
/*     */   static final byte TYPE_PING = 6;
/*     */   static final byte TYPE_GOAWAY = 7;
/*     */   static final byte TYPE_WINDOW_UPDATE = 8;
/*     */   static final byte TYPE_CONTINUATION = 9;
/*     */   static final byte FLAG_NONE = 0;
/*     */   static final byte FLAG_ACK = 1;
/*     */   static final byte FLAG_END_STREAM = 1;
/*     */   static final byte FLAG_END_HEADERS = 4;
/*     */   static final byte FLAG_END_PUSH_PROMISE = 4;
/*     */   static final byte FLAG_PADDED = 8;
/*     */   static final byte FLAG_PRIORITY = 32;
/*     */   static final byte FLAG_COMPRESSED = 32;
/*     */   
/*     */   public FrameReader newReader(BufferedSource source, boolean client)
/*     */   {
/*  80 */     return new Reader(source, 4096, client);
/*     */   }
/*     */   
/*     */   public FrameWriter newWriter(BufferedSink sink, boolean client) {
/*  84 */     return new Writer(sink, client);
/*     */   }
/*     */   
/*     */   static final class Reader implements FrameReader
/*     */   {
/*     */     private final BufferedSource source;
/*     */     private final Http2.ContinuationSource continuation;
/*     */     private final boolean client;
/*     */     final Hpack.Reader hpackReader;
/*     */     
/*     */     Reader(BufferedSource source, int headerTableSize, boolean client)
/*     */     {
/*  96 */       this.source = source;
/*  97 */       this.client = client;
/*  98 */       this.continuation = new Http2.ContinuationSource(this.source);
/*  99 */       this.hpackReader = new Hpack.Reader(headerTableSize, this.continuation);
/*     */     }
/*     */     
/*     */     public void readConnectionPreface() throws IOException {
/* 103 */       if (this.client) return;
/* 104 */       ByteString connectionPreface = this.source.readByteString(Http2.CONNECTION_PREFACE.size());
/* 105 */       if (Http2.logger.isLoggable(Level.FINE)) Http2.logger.fine(String.format("<< CONNECTION %s", new Object[] { connectionPreface.hex() }));
/* 106 */       if (!Http2.CONNECTION_PREFACE.equals(connectionPreface)) {
/* 107 */         throw Http2.ioException("Expected a connection header but was %s", new Object[] { connectionPreface.utf8() });
/*     */       }
/*     */     }
/*     */     
/*     */     public boolean nextFrame(FrameReader.Handler handler) throws IOException {
/*     */       try {
/* 113 */         this.source.require(9L);
/*     */       } catch (IOException e) {
/* 115 */         return false;
/*     */       }
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
/* 130 */       int length = Http2.readMedium(this.source);
/* 131 */       if ((length < 0) || (length > 16384)) {
/* 132 */         throw Http2.ioException("FRAME_SIZE_ERROR: %s", new Object[] { Integer.valueOf(length) });
/*     */       }
/* 134 */       byte type = (byte)(this.source.readByte() & 0xFF);
/* 135 */       byte flags = (byte)(this.source.readByte() & 0xFF);
/* 136 */       int streamId = this.source.readInt() & 0x7FFFFFFF;
/* 137 */       if (Http2.logger.isLoggable(Level.FINE)) { Http2.logger.fine(Http2.FrameLogger.formatHeader(true, streamId, length, type, flags));
/*     */       }
/* 139 */       switch (type) {
/*     */       case 0: 
/* 141 */         readData(handler, length, flags, streamId);
/* 142 */         break;
/*     */       
/*     */       case 1: 
/* 145 */         readHeaders(handler, length, flags, streamId);
/* 146 */         break;
/*     */       
/*     */       case 2: 
/* 149 */         readPriority(handler, length, flags, streamId);
/* 150 */         break;
/*     */       
/*     */       case 3: 
/* 153 */         readRstStream(handler, length, flags, streamId);
/* 154 */         break;
/*     */       
/*     */       case 4: 
/* 157 */         readSettings(handler, length, flags, streamId);
/* 158 */         break;
/*     */       
/*     */       case 5: 
/* 161 */         readPushPromise(handler, length, flags, streamId);
/* 162 */         break;
/*     */       
/*     */       case 6: 
/* 165 */         readPing(handler, length, flags, streamId);
/* 166 */         break;
/*     */       
/*     */       case 7: 
/* 169 */         readGoAway(handler, length, flags, streamId);
/* 170 */         break;
/*     */       
/*     */       case 8: 
/* 173 */         readWindowUpdate(handler, length, flags, streamId);
/* 174 */         break;
/*     */       
/*     */ 
/*     */       default: 
/* 178 */         this.source.skip(length);
/*     */       }
/* 180 */       return true;
/*     */     }
/*     */     
/*     */     private void readHeaders(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 185 */       if (streamId == 0) { throw Http2.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0", new Object[0]);
/*     */       }
/* 187 */       boolean endStream = (flags & 0x1) != 0;
/*     */       
/* 189 */       short padding = (flags & 0x8) != 0 ? (short)(this.source.readByte() & 0xFF) : 0;
/*     */       
/* 191 */       if ((flags & 0x20) != 0) {
/* 192 */         readPriority(handler, streamId);
/* 193 */         length -= 5;
/*     */       }
/*     */       
/* 196 */       length = Http2.lengthWithoutPadding(length, flags, padding);
/*     */       
/* 198 */       List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);
/*     */       
/* 200 */       handler.headers(false, endStream, streamId, -1, headerBlock, HeadersMode.HTTP_20_HEADERS);
/*     */     }
/*     */     
/*     */     private List<Header> readHeaderBlock(int length, short padding, byte flags, int streamId) throws IOException
/*     */     {
/* 205 */       this.continuation.length = (this.continuation.left = length);
/* 206 */       this.continuation.padding = padding;
/* 207 */       this.continuation.flags = flags;
/* 208 */       this.continuation.streamId = streamId;
/*     */       
/*     */ 
/*     */ 
/* 212 */       this.hpackReader.readHeaders();
/* 213 */       return this.hpackReader.getAndResetHeaderList();
/*     */     }
/*     */     
/*     */     private void readData(FrameReader.Handler handler, int length, byte flags, int streamId)
/*     */       throws IOException
/*     */     {
/* 219 */       boolean inFinished = (flags & 0x1) != 0;
/* 220 */       boolean gzipped = (flags & 0x20) != 0;
/* 221 */       if (gzipped) {
/* 222 */         throw Http2.ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA", new Object[0]);
/*     */       }
/*     */       
/* 225 */       short padding = (flags & 0x8) != 0 ? (short)(this.source.readByte() & 0xFF) : 0;
/* 226 */       length = Http2.lengthWithoutPadding(length, flags, padding);
/*     */       
/* 228 */       handler.data(inFinished, streamId, this.source, length);
/* 229 */       this.source.skip(padding);
/*     */     }
/*     */     
/*     */     private void readPriority(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 234 */       if (length != 5) throw Http2.ioException("TYPE_PRIORITY length: %d != 5", new Object[] { Integer.valueOf(length) });
/* 235 */       if (streamId == 0) throw Http2.ioException("TYPE_PRIORITY streamId == 0", new Object[0]);
/* 236 */       readPriority(handler, streamId);
/*     */     }
/*     */     
/*     */     private void readPriority(FrameReader.Handler handler, int streamId) throws IOException {
/* 240 */       int w1 = this.source.readInt();
/* 241 */       boolean exclusive = (w1 & 0x80000000) != 0;
/* 242 */       int streamDependency = w1 & 0x7FFFFFFF;
/* 243 */       int weight = (this.source.readByte() & 0xFF) + 1;
/* 244 */       handler.priority(streamId, streamDependency, weight, exclusive);
/*     */     }
/*     */     
/*     */     private void readRstStream(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 249 */       if (length != 4) throw Http2.ioException("TYPE_RST_STREAM length: %d != 4", new Object[] { Integer.valueOf(length) });
/* 250 */       if (streamId == 0) throw Http2.ioException("TYPE_RST_STREAM streamId == 0", new Object[0]);
/* 251 */       int errorCodeInt = this.source.readInt();
/* 252 */       ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
/* 253 */       if (errorCode == null) {
/* 254 */         throw Http2.ioException("TYPE_RST_STREAM unexpected error code: %d", new Object[] { Integer.valueOf(errorCodeInt) });
/*     */       }
/* 256 */       handler.rstStream(streamId, errorCode);
/*     */     }
/*     */     
/*     */     private void readSettings(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 261 */       if (streamId != 0) throw Http2.ioException("TYPE_SETTINGS streamId != 0", new Object[0]);
/* 262 */       if ((flags & 0x1) != 0) {
/* 263 */         if (length != 0) throw Http2.ioException("FRAME_SIZE_ERROR ack frame should be empty!", new Object[0]);
/* 264 */         handler.ackSettings();
/* 265 */         return;
/*     */       }
/*     */       
/* 268 */       if (length % 6 != 0) throw Http2.ioException("TYPE_SETTINGS length %% 6 != 0: %s", new Object[] { Integer.valueOf(length) });
/* 269 */       Settings settings = new Settings();
/* 270 */       for (int i = 0; i < length; i += 6) {
/* 271 */         short id = this.source.readShort();
/* 272 */         int value = this.source.readInt();
/*     */         
/* 274 */         switch (id) {
/*     */         case 1: 
/*     */           break;
/*     */         case 2: 
/* 278 */           if ((value != 0) && (value != 1)) {
/* 279 */             throw Http2.ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1", new Object[0]);
/*     */           }
/*     */           break;
/*     */         case 3: 
/* 283 */           id = 4;
/* 284 */           break;
/*     */         case 4: 
/* 286 */           id = 7;
/* 287 */           if (value < 0) {
/* 288 */             throw Http2.ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1", new Object[0]);
/*     */           }
/*     */           break;
/*     */         case 5: 
/* 292 */           if ((value < 16384) || (value > 16777215)) {
/* 293 */             throw Http2.ioException("PROTOCOL_ERROR SETTINGS_MAX_FRAME_SIZE: %s", new Object[] { Integer.valueOf(value) });
/*     */           }
/*     */           break;
/*     */         case 6: 
/*     */           break;
/*     */         default: 
/* 299 */           throw Http2.ioException("PROTOCOL_ERROR invalid settings id: %s", new Object[] { Short.valueOf(id) });
/*     */         }
/* 301 */         settings.set(id, 0, value);
/*     */       }
/* 303 */       handler.settings(false, settings);
/* 304 */       if (settings.getHeaderTableSize() >= 0) {
/* 305 */         this.hpackReader.headerTableSizeSetting(settings.getHeaderTableSize());
/*     */       }
/*     */     }
/*     */     
/*     */     private void readPushPromise(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 311 */       if (streamId == 0) {
/* 312 */         throw Http2.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0", new Object[0]);
/*     */       }
/* 314 */       short padding = (flags & 0x8) != 0 ? (short)(this.source.readByte() & 0xFF) : 0;
/* 315 */       int promisedStreamId = this.source.readInt() & 0x7FFFFFFF;
/* 316 */       length -= 4;
/* 317 */       length = Http2.lengthWithoutPadding(length, flags, padding);
/* 318 */       List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);
/* 319 */       handler.pushPromise(streamId, promisedStreamId, headerBlock);
/*     */     }
/*     */     
/*     */     private void readPing(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 324 */       if (length != 8) throw Http2.ioException("TYPE_PING length != 8: %s", new Object[] { Integer.valueOf(length) });
/* 325 */       if (streamId != 0) throw Http2.ioException("TYPE_PING streamId != 0", new Object[0]);
/* 326 */       int payload1 = this.source.readInt();
/* 327 */       int payload2 = this.source.readInt();
/* 328 */       boolean ack = (flags & 0x1) != 0;
/* 329 */       handler.ping(ack, payload1, payload2);
/*     */     }
/*     */     
/*     */     private void readGoAway(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 334 */       if (length < 8) throw Http2.ioException("TYPE_GOAWAY length < 8: %s", new Object[] { Integer.valueOf(length) });
/* 335 */       if (streamId != 0) throw Http2.ioException("TYPE_GOAWAY streamId != 0", new Object[0]);
/* 336 */       int lastStreamId = this.source.readInt();
/* 337 */       int errorCodeInt = this.source.readInt();
/* 338 */       int opaqueDataLength = length - 8;
/* 339 */       ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
/* 340 */       if (errorCode == null) {
/* 341 */         throw Http2.ioException("TYPE_GOAWAY unexpected error code: %d", new Object[] { Integer.valueOf(errorCodeInt) });
/*     */       }
/* 343 */       ByteString debugData = ByteString.EMPTY;
/* 344 */       if (opaqueDataLength > 0) {
/* 345 */         debugData = this.source.readByteString(opaqueDataLength);
/*     */       }
/* 347 */       handler.goAway(lastStreamId, errorCode, debugData);
/*     */     }
/*     */     
/*     */     private void readWindowUpdate(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException
/*     */     {
/* 352 */       if (length != 4) throw Http2.ioException("TYPE_WINDOW_UPDATE length !=4: %s", new Object[] { Integer.valueOf(length) });
/* 353 */       long increment = this.source.readInt() & 0x7FFFFFFF;
/* 354 */       if (increment == 0L) throw Http2.ioException("windowSizeIncrement was 0", new Object[] { Long.valueOf(increment) });
/* 355 */       handler.windowUpdate(streamId, increment);
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 359 */       this.source.close();
/*     */     }
/*     */   }
/*     */   
/*     */   static final class Writer implements FrameWriter {
/*     */     private final BufferedSink sink;
/*     */     private final boolean client;
/*     */     private final Buffer hpackBuffer;
/*     */     private final Hpack.Writer hpackWriter;
/*     */     private int maxFrameSize;
/*     */     private boolean closed;
/*     */     
/*     */     Writer(BufferedSink sink, boolean client) {
/* 372 */       this.sink = sink;
/* 373 */       this.client = client;
/* 374 */       this.hpackBuffer = new Buffer();
/* 375 */       this.hpackWriter = new Hpack.Writer(this.hpackBuffer);
/* 376 */       this.maxFrameSize = 16384;
/*     */     }
/*     */     
/*     */     public synchronized void flush() throws IOException {
/* 380 */       if (this.closed) throw new IOException("closed");
/* 381 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void ackSettings(Settings peerSettings) throws IOException {
/* 385 */       if (this.closed) throw new IOException("closed");
/* 386 */       this.maxFrameSize = peerSettings.getMaxFrameSize(this.maxFrameSize);
/* 387 */       int length = 0;
/* 388 */       byte type = 4;
/* 389 */       byte flags = 1;
/* 390 */       int streamId = 0;
/* 391 */       frameHeader(streamId, length, type, flags);
/* 392 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void connectionPreface() throws IOException {
/* 396 */       if (this.closed) throw new IOException("closed");
/* 397 */       if (!this.client) return;
/* 398 */       if (Http2.logger.isLoggable(Level.FINE)) {
/* 399 */         Http2.logger.fine(String.format(">> CONNECTION %s", new Object[] { Http2.CONNECTION_PREFACE.hex() }));
/*     */       }
/* 401 */       this.sink.write(Http2.CONNECTION_PREFACE.toByteArray());
/* 402 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void synStream(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock)
/*     */       throws IOException
/*     */     {
/* 408 */       if (inFinished) throw new UnsupportedOperationException();
/* 409 */       if (this.closed) throw new IOException("closed");
/* 410 */       headers(outFinished, streamId, headerBlock);
/*     */     }
/*     */     
/*     */     public synchronized void synReply(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException
/*     */     {
/* 415 */       if (this.closed) throw new IOException("closed");
/* 416 */       headers(outFinished, streamId, headerBlock);
/*     */     }
/*     */     
/*     */     public synchronized void headers(int streamId, List<Header> headerBlock) throws IOException
/*     */     {
/* 421 */       if (this.closed) throw new IOException("closed");
/* 422 */       headers(false, streamId, headerBlock);
/*     */     }
/*     */     
/*     */     public synchronized void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException
/*     */     {
/* 427 */       if (this.closed) throw new IOException("closed");
/* 428 */       this.hpackWriter.writeHeaders(requestHeaders);
/*     */       
/* 430 */       long byteCount = this.hpackBuffer.size();
/* 431 */       int length = (int)Math.min(this.maxFrameSize - 4, byteCount);
/* 432 */       byte type = 5;
/* 433 */       byte flags = byteCount == length ? 4 : 0;
/* 434 */       frameHeader(streamId, length + 4, type, flags);
/* 435 */       this.sink.writeInt(promisedStreamId & 0x7FFFFFFF);
/* 436 */       this.sink.write(this.hpackBuffer, length);
/*     */       
/* 438 */       if (byteCount > length) writeContinuationFrames(streamId, byteCount - length);
/*     */     }
/*     */     
/*     */     void headers(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException {
/* 442 */       if (this.closed) throw new IOException("closed");
/* 443 */       this.hpackWriter.writeHeaders(headerBlock);
/*     */       
/* 445 */       long byteCount = this.hpackBuffer.size();
/* 446 */       int length = (int)Math.min(this.maxFrameSize, byteCount);
/* 447 */       byte type = 1;
/* 448 */       byte flags = byteCount == length ? 4 : 0;
/* 449 */       if (outFinished) flags = (byte)(flags | 0x1);
/* 450 */       frameHeader(streamId, length, type, flags);
/* 451 */       this.sink.write(this.hpackBuffer, length);
/*     */       
/* 453 */       if (byteCount > length) writeContinuationFrames(streamId, byteCount - length);
/*     */     }
/*     */     
/*     */     private void writeContinuationFrames(int streamId, long byteCount) throws IOException {
/* 457 */       while (byteCount > 0L) {
/* 458 */         int length = (int)Math.min(this.maxFrameSize, byteCount);
/* 459 */         byteCount -= length;
/* 460 */         frameHeader(streamId, length, (byte)9, (byte)(byteCount == 0L ? 4 : 0));
/* 461 */         this.sink.write(this.hpackBuffer, length);
/*     */       }
/*     */     }
/*     */     
/*     */     public synchronized void rstStream(int streamId, ErrorCode errorCode) throws IOException
/*     */     {
/* 467 */       if (this.closed) throw new IOException("closed");
/* 468 */       if (errorCode.httpCode == -1) { throw new IllegalArgumentException();
/*     */       }
/* 470 */       int length = 4;
/* 471 */       byte type = 3;
/* 472 */       byte flags = 0;
/* 473 */       frameHeader(streamId, length, type, flags);
/* 474 */       this.sink.writeInt(errorCode.httpCode);
/* 475 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public int maxDataLength() {
/* 479 */       return this.maxFrameSize;
/*     */     }
/*     */     
/*     */     public synchronized void data(boolean outFinished, int streamId, Buffer source, int byteCount) throws IOException
/*     */     {
/* 484 */       if (this.closed) throw new IOException("closed");
/* 485 */       byte flags = 0;
/* 486 */       if (outFinished) flags = (byte)(flags | 0x1);
/* 487 */       dataFrame(streamId, flags, source, byteCount);
/*     */     }
/*     */     
/*     */     void dataFrame(int streamId, byte flags, Buffer buffer, int byteCount) throws IOException {
/* 491 */       byte type = 0;
/* 492 */       frameHeader(streamId, byteCount, type, flags);
/* 493 */       if (byteCount > 0) {
/* 494 */         this.sink.write(buffer, byteCount);
/*     */       }
/*     */     }
/*     */     
/*     */     public synchronized void settings(Settings settings) throws IOException {
/* 499 */       if (this.closed) throw new IOException("closed");
/* 500 */       int length = settings.size() * 6;
/* 501 */       byte type = 4;
/* 502 */       byte flags = 0;
/* 503 */       int streamId = 0;
/* 504 */       frameHeader(streamId, length, type, flags);
/* 505 */       for (int i = 0; i < 10; i++)
/* 506 */         if (settings.isSet(i)) {
/* 507 */           int id = i;
/* 508 */           if (id == 4) { id = 3;
/* 509 */           } else if (id == 7) id = 4;
/* 510 */           this.sink.writeShort(id);
/* 511 */           this.sink.writeInt(settings.get(i));
/*     */         }
/* 513 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void ping(boolean ack, int payload1, int payload2) throws IOException
/*     */     {
/* 518 */       if (this.closed) throw new IOException("closed");
/* 519 */       int length = 8;
/* 520 */       byte type = 6;
/* 521 */       byte flags = ack ? 1 : 0;
/* 522 */       int streamId = 0;
/* 523 */       frameHeader(streamId, length, type, flags);
/* 524 */       this.sink.writeInt(payload1);
/* 525 */       this.sink.writeInt(payload2);
/* 526 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode, byte[] debugData) throws IOException
/*     */     {
/* 531 */       if (this.closed) throw new IOException("closed");
/* 532 */       if (errorCode.httpCode == -1) throw Http2.illegalArgument("errorCode.httpCode == -1", new Object[0]);
/* 533 */       int length = 8 + debugData.length;
/* 534 */       byte type = 7;
/* 535 */       byte flags = 0;
/* 536 */       int streamId = 0;
/* 537 */       frameHeader(streamId, length, type, flags);
/* 538 */       this.sink.writeInt(lastGoodStreamId);
/* 539 */       this.sink.writeInt(errorCode.httpCode);
/* 540 */       if (debugData.length > 0) {
/* 541 */         this.sink.write(debugData);
/*     */       }
/* 543 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void windowUpdate(int streamId, long windowSizeIncrement) throws IOException
/*     */     {
/* 548 */       if (this.closed) throw new IOException("closed");
/* 549 */       if ((windowSizeIncrement == 0L) || (windowSizeIncrement > 2147483647L)) {
/* 550 */         throw Http2.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s", new Object[] {
/* 551 */           Long.valueOf(windowSizeIncrement) });
/*     */       }
/* 553 */       int length = 4;
/* 554 */       byte type = 8;
/* 555 */       byte flags = 0;
/* 556 */       frameHeader(streamId, length, type, flags);
/* 557 */       this.sink.writeInt((int)windowSizeIncrement);
/* 558 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void close() throws IOException {
/* 562 */       this.closed = true;
/* 563 */       this.sink.close();
/*     */     }
/*     */     
/*     */     void frameHeader(int streamId, int length, byte type, byte flags) throws IOException {
/* 567 */       if (Http2.logger.isLoggable(Level.FINE)) Http2.logger.fine(Http2.FrameLogger.formatHeader(false, streamId, length, type, flags));
/* 568 */       if (length > this.maxFrameSize) {
/* 569 */         throw Http2.illegalArgument("FRAME_SIZE_ERROR length > %d: %d", new Object[] { Integer.valueOf(this.maxFrameSize), Integer.valueOf(length) });
/*     */       }
/* 571 */       if ((streamId & 0x80000000) != 0) throw Http2.illegalArgument("reserved bit set: %s", new Object[] { Integer.valueOf(streamId) });
/* 572 */       Http2.writeMedium(this.sink, length);
/* 573 */       this.sink.writeByte(type & 0xFF);
/* 574 */       this.sink.writeByte(flags & 0xFF);
/* 575 */       this.sink.writeInt(streamId & 0x7FFFFFFF);
/*     */     }
/*     */   }
/*     */   
/*     */   private static IllegalArgumentException illegalArgument(String message, Object... args) {
/* 580 */     throw new IllegalArgumentException(String.format(message, args));
/*     */   }
/*     */   
/*     */   private static IOException ioException(String message, Object... args) throws IOException {
/* 584 */     throw new IOException(String.format(message, args));
/*     */   }
/*     */   
/*     */ 
/*     */   static final class ContinuationSource
/*     */     implements Source
/*     */   {
/*     */     private final BufferedSource source;
/*     */     
/*     */     int length;
/*     */     
/*     */     byte flags;
/*     */     
/*     */     int streamId;
/*     */     int left;
/*     */     short padding;
/*     */     
/*     */     public ContinuationSource(BufferedSource source)
/*     */     {
/* 603 */       this.source = source;
/*     */     }
/*     */     
/*     */     public long read(Buffer sink, long byteCount) throws IOException {
/* 607 */       while (this.left == 0) {
/* 608 */         this.source.skip(this.padding);
/* 609 */         this.padding = 0;
/* 610 */         if ((this.flags & 0x4) != 0) return -1L;
/* 611 */         readContinuationHeader();
/*     */       }
/*     */       
/*     */ 
/* 615 */       long read = this.source.read(sink, Math.min(byteCount, this.left));
/* 616 */       if (read == -1L) return -1L;
/* 617 */       this.left = ((int)(this.left - read));
/* 618 */       return read;
/*     */     }
/*     */     
/*     */     public Timeout timeout() {
/* 622 */       return this.source.timeout();
/*     */     }
/*     */     
/*     */     public void close() throws IOException
/*     */     {}
/*     */     
/*     */     private void readContinuationHeader() throws IOException {
/* 629 */       int previousStreamId = this.streamId;
/*     */       
/* 631 */       this.length = (this.left = Http2.readMedium(this.source));
/* 632 */       byte type = (byte)(this.source.readByte() & 0xFF);
/* 633 */       this.flags = ((byte)(this.source.readByte() & 0xFF));
/* 634 */       if (Http2.logger.isLoggable(Level.FINE)) Http2.logger.fine(Http2.FrameLogger.formatHeader(true, this.streamId, this.length, type, this.flags));
/* 635 */       this.streamId = (this.source.readInt() & 0x7FFFFFFF);
/* 636 */       if (type != 9) throw Http2.ioException("%s != TYPE_CONTINUATION", new Object[] { Byte.valueOf(type) });
/* 637 */       if (this.streamId != previousStreamId) throw Http2.ioException("TYPE_CONTINUATION streamId changed", new Object[0]);
/*     */     }
/*     */   }
/*     */   
/*     */   private static int lengthWithoutPadding(int length, byte flags, short padding) throws IOException
/*     */   {
/* 643 */     if ((flags & 0x8) != 0) length--;
/* 644 */     if (padding > length) {
/* 645 */       throw ioException("PROTOCOL_ERROR padding %s > remaining length %s", new Object[] { Short.valueOf(padding), Integer.valueOf(length) });
/*     */     }
/* 647 */     return (short)(length - padding);
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   static final class FrameLogger
/*     */   {
/*     */     static String formatHeader(boolean inbound, int streamId, int length, byte type, byte flags)
/*     */     {
/* 671 */       String formattedType = type < TYPES.length ? TYPES[type] : String.format("0x%02x", new Object[] { Byte.valueOf(type) });
/* 672 */       String formattedFlags = formatFlags(type, flags);
/* 673 */       return String.format("%s 0x%08x %5d %-13s %s", new Object[] { inbound ? "<<" : ">>", Integer.valueOf(streamId), Integer.valueOf(length), formattedType, formattedFlags });
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     static String formatFlags(byte type, byte flags)
/*     */     {
/* 683 */       if (flags == 0) return "";
/* 684 */       switch (type) {
/*     */       case 4: 
/*     */       case 6: 
/* 687 */         return flags == 1 ? "ACK" : BINARY[flags];
/*     */       case 2: 
/*     */       case 3: 
/*     */       case 7: 
/*     */       case 8: 
/* 692 */         return BINARY[flags];
/*     */       }
/* 694 */       String result = flags < FLAGS.length ? FLAGS[flags] : BINARY[flags];
/*     */       
/* 696 */       if ((type == 5) && ((flags & 0x4) != 0))
/* 697 */         return result.replace("HEADERS", "PUSH_PROMISE");
/* 698 */       if ((type == 0) && ((flags & 0x20) != 0)) {
/* 699 */         return result.replace("PRIORITY", "COMPRESSED");
/*     */       }
/* 701 */       return result;
/*     */     }
/*     */     
/*     */ 
/* 705 */     private static final String[] TYPES = { "DATA", "HEADERS", "PRIORITY", "RST_STREAM", "SETTINGS", "PUSH_PROMISE", "PING", "GOAWAY", "WINDOW_UPDATE", "CONTINUATION" };
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
/* 722 */     private static final String[] FLAGS = new String[64];
/* 723 */     private static final String[] BINARY = new String['Ā'];
/*     */     
/*     */     static {
/* 726 */       for (int i = 0; i < BINARY.length; i++) {
/* 727 */         BINARY[i] = String.format("%8s", new Object[] { Integer.toBinaryString(i) }).replace(' ', '0');
/*     */       }
/*     */       
/* 730 */       FLAGS[0] = "";
/* 731 */       FLAGS[1] = "END_STREAM";
/*     */       
/* 733 */       int[] prefixFlags = { 1 };
/*     */       
/* 735 */       FLAGS[8] = "PADDED";
/* 736 */       int prefixFlag; for (prefixFlag : prefixFlags) {
/* 737 */         FLAGS[(prefixFlag | 0x8)] = (FLAGS[prefixFlag] + "|PADDED");
/*     */       }
/*     */       
/* 740 */       FLAGS[4] = "END_HEADERS";
/* 741 */       FLAGS[32] = "PRIORITY";
/* 742 */       FLAGS[36] = "END_HEADERS|PRIORITY";
/* 743 */       int[] frameFlags = { 4, 32, 36 };
/*     */       
/*     */ 
/* 746 */       for (int frameFlag : frameFlags) {
/* 747 */         for (int prefixFlag : prefixFlags) {
/* 748 */           FLAGS[(prefixFlag | frameFlag)] = (FLAGS[prefixFlag] + '|' + FLAGS[frameFlag]);
/* 749 */           FLAGS[(prefixFlag | frameFlag | 0x8)] = (FLAGS[prefixFlag] + '|' + FLAGS[frameFlag] + "|PADDED");
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 754 */       for (int i = 0; i < FLAGS.length; i++) {
/* 755 */         if (FLAGS[i] == null) FLAGS[i] = BINARY[i];
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private static int readMedium(BufferedSource source)
/*     */     throws IOException
/*     */   {
/* 763 */     return (source.readByte() & 0xFF) << 16 | (source.readByte() & 0xFF) << 8 | source.readByte() & 0xFF;
/*     */   }
/*     */   
/*     */   private static void writeMedium(BufferedSink sink, int i) throws IOException {
/* 767 */     sink.writeByte(i >>> 16 & 0xFF);
/* 768 */     sink.writeByte(i >>> 8 & 0xFF);
/* 769 */     sink.writeByte(i & 0xFF);
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\Http2.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */