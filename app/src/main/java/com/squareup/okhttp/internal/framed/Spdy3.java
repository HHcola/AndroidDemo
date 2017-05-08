/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.io.IOException;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.ProtocolException;
/*     */ import java.nio.charset.Charset;
/*     */ import java.util.List;
/*     */ import java.util.zip.Deflater;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.ByteString;
/*     */ import okio.DeflaterSink;
/*     */ import okio.Okio;
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Spdy3
/*     */   implements Variant
/*     */ {
/*     */   static final int TYPE_DATA = 0;
/*     */   static final int TYPE_SYN_STREAM = 1;
/*     */   static final int TYPE_SYN_REPLY = 2;
/*     */   static final int TYPE_RST_STREAM = 3;
/*     */   static final int TYPE_SETTINGS = 4;
/*     */   static final int TYPE_PING = 6;
/*     */   static final int TYPE_GOAWAY = 7;
/*     */   static final int TYPE_HEADERS = 8;
/*     */   static final int TYPE_WINDOW_UPDATE = 9;
/*     */   static final int FLAG_FIN = 1;
/*     */   static final int FLAG_UNIDIRECTIONAL = 2;
/*     */   static final int VERSION = 3;
/*     */   static final byte[] DICTIONARY;
/*     */   
/*     */   public Protocol getProtocol()
/*     */   {
/*  39 */     return Protocol.SPDY_3;
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
/*     */   static
/*     */   {
/*     */     try
/*     */     {
/*  92 */       DICTIONARY = "\000\000\000\007options\000\000\000\004head\000\000\000\004post\000\000\000\003put\000\000\000\006delete\000\000\000\005trace\000\000\000\006accept\000\000\000\016accept-charset\000\000\000\017accept-encoding\000\000\000\017accept-language\000\000\000\raccept-ranges\000\000\000\003age\000\000\000\005allow\000\000\000\rauthorization\000\000\000\rcache-control\000\000\000\nconnection\000\000\000\fcontent-base\000\000\000\020content-encoding\000\000\000\020content-language\000\000\000\016content-length\000\000\000\020content-location\000\000\000\013content-md5\000\000\000\rcontent-range\000\000\000\fcontent-type\000\000\000\004date\000\000\000\004etag\000\000\000\006expect\000\000\000\007expires\000\000\000\004from\000\000\000\004host\000\000\000\bif-match\000\000\000\021if-modified-since\000\000\000\rif-none-match\000\000\000\bif-range\000\000\000\023if-unmodified-since\000\000\000\rlast-modified\000\000\000\blocation\000\000\000\fmax-forwards\000\000\000\006pragma\000\000\000\022proxy-authenticate\000\000\000\023proxy-authorization\000\000\000\005range\000\000\000\007referer\000\000\000\013retry-after\000\000\000\006server\000\000\000\002te\000\000\000\007trailer\000\000\000\021transfer-encoding\000\000\000\007upgrade\000\000\000\nuser-agent\000\000\000\004vary\000\000\000\003via\000\000\000\007warning\000\000\000\020www-authenticate\000\000\000\006method\000\000\000\003get\000\000\000\006status\000\000\000\006200 OK\000\000\000\007version\000\000\000\bHTTP/1.1\000\000\000\003url\000\000\000\006public\000\000\000\nset-cookie\000\000\000\nkeep-alive\000\000\000\006origin100101201202205206300302303304305306307402405406407408409410411412413414415416417502504505203 Non-Authoritative Information204 No Content301 Moved Permanently400 Bad Request401 Unauthorized403 Forbidden404 Not Found500 Internal Server Error501 Not Implemented503 Service UnavailableJan Feb Mar Apr May Jun Jul Aug Sept Oct Nov Dec 00:00:00 Mon, Tue, Wed, Thu, Fri, Sat, Sun, GMTchunked,text/html,image/png,image/jpg,image/gif,application/xml,application/xhtml+xml,text/plain,text/javascript,publicprivatemax-age=gzip,deflate,sdchcharset=utf-8charset=iso-8859-1,utf-,*,enq=0.".getBytes(Util.UTF_8.name());
/*     */     } catch (UnsupportedEncodingException e) {
/*  94 */       throw new AssertionError();
/*     */     }
/*     */   }
/*     */   
/*     */   public FrameReader newReader(BufferedSource source, boolean client) {
/*  99 */     return new Reader(source, client);
/*     */   }
/*     */   
/*     */   public FrameWriter newWriter(BufferedSink sink, boolean client) {
/* 103 */     return new Writer(sink, client);
/*     */   }
/*     */   
/*     */   static final class Reader implements FrameReader
/*     */   {
/*     */     private final BufferedSource source;
/*     */     private final boolean client;
/*     */     private final NameValueBlockReader headerBlockReader;
/*     */     
/*     */     Reader(BufferedSource source, boolean client) {
/* 113 */       this.source = source;
/* 114 */       this.headerBlockReader = new NameValueBlockReader(this.source);
/* 115 */       this.client = client;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */     public void readConnectionPreface() {}
/*     */     
/*     */ 
/*     */ 
/*     */     public boolean nextFrame(FrameReader.Handler handler)
/*     */       throws IOException
/*     */     {
/*     */       try
/*     */       {
/* 129 */         int w1 = this.source.readInt();
/* 130 */         w2 = this.source.readInt();
/*     */       } catch (IOException e) { int w2;
/* 132 */         return false; }
/*     */       int w2;
/*     */       int w1;
/* 135 */       boolean control = (w1 & 0x80000000) != 0;
/* 136 */       int flags = (w2 & 0xFF000000) >>> 24;
/* 137 */       int length = w2 & 0xFFFFFF;
/*     */       
/* 139 */       if (control) {
/* 140 */         int version = (w1 & 0x7FFF0000) >>> 16;
/* 141 */         int type = w1 & 0xFFFF;
/*     */         
/* 143 */         if (version != 3) {
/* 144 */           throw new ProtocolException("version != 3: " + version);
/*     */         }
/*     */         
/* 147 */         switch (type) {
/*     */         case 1: 
/* 149 */           readSynStream(handler, flags, length);
/* 150 */           return true;
/*     */         
/*     */         case 2: 
/* 153 */           readSynReply(handler, flags, length);
/* 154 */           return true;
/*     */         
/*     */         case 3: 
/* 157 */           readRstStream(handler, flags, length);
/* 158 */           return true;
/*     */         
/*     */         case 4: 
/* 161 */           readSettings(handler, flags, length);
/* 162 */           return true;
/*     */         
/*     */         case 6: 
/* 165 */           readPing(handler, flags, length);
/* 166 */           return true;
/*     */         
/*     */         case 7: 
/* 169 */           readGoAway(handler, flags, length);
/* 170 */           return true;
/*     */         
/*     */         case 8: 
/* 173 */           readHeaders(handler, flags, length);
/* 174 */           return true;
/*     */         
/*     */         case 9: 
/* 177 */           readWindowUpdate(handler, flags, length);
/* 178 */           return true;
/*     */         }
/*     */         
/* 181 */         this.source.skip(length);
/* 182 */         return true;
/*     */       }
/*     */       
/* 185 */       int streamId = w1 & 0x7FFFFFFF;
/* 186 */       boolean inFinished = (flags & 0x1) != 0;
/* 187 */       handler.data(inFinished, streamId, this.source, length);
/* 188 */       return true;
/*     */     }
/*     */     
/*     */     private void readSynStream(FrameReader.Handler handler, int flags, int length) throws IOException
/*     */     {
/* 193 */       int w1 = this.source.readInt();
/* 194 */       int w2 = this.source.readInt();
/* 195 */       int streamId = w1 & 0x7FFFFFFF;
/* 196 */       int associatedStreamId = w2 & 0x7FFFFFFF;
/* 197 */       this.source.readShort();
/* 198 */       List<Header> headerBlock = this.headerBlockReader.readNameValueBlock(length - 10);
/*     */       
/* 200 */       boolean inFinished = (flags & 0x1) != 0;
/* 201 */       boolean outFinished = (flags & 0x2) != 0;
/* 202 */       handler.headers(outFinished, inFinished, streamId, associatedStreamId, headerBlock, HeadersMode.SPDY_SYN_STREAM);
/*     */     }
/*     */     
/*     */     private void readSynReply(FrameReader.Handler handler, int flags, int length) throws IOException
/*     */     {
/* 207 */       int w1 = this.source.readInt();
/* 208 */       int streamId = w1 & 0x7FFFFFFF;
/* 209 */       List<Header> headerBlock = this.headerBlockReader.readNameValueBlock(length - 4);
/* 210 */       boolean inFinished = (flags & 0x1) != 0;
/* 211 */       handler.headers(false, inFinished, streamId, -1, headerBlock, HeadersMode.SPDY_REPLY);
/*     */     }
/*     */     
/*     */     private void readRstStream(FrameReader.Handler handler, int flags, int length) throws IOException {
/* 215 */       if (length != 8) throw ioException("TYPE_RST_STREAM length: %d != 8", new Object[] { Integer.valueOf(length) });
/* 216 */       int streamId = this.source.readInt() & 0x7FFFFFFF;
/* 217 */       int errorCodeInt = this.source.readInt();
/* 218 */       ErrorCode errorCode = ErrorCode.fromSpdy3Rst(errorCodeInt);
/* 219 */       if (errorCode == null) {
/* 220 */         throw ioException("TYPE_RST_STREAM unexpected error code: %d", new Object[] { Integer.valueOf(errorCodeInt) });
/*     */       }
/* 222 */       handler.rstStream(streamId, errorCode);
/*     */     }
/*     */     
/*     */     private void readHeaders(FrameReader.Handler handler, int flags, int length) throws IOException {
/* 226 */       int w1 = this.source.readInt();
/* 227 */       int streamId = w1 & 0x7FFFFFFF;
/* 228 */       List<Header> headerBlock = this.headerBlockReader.readNameValueBlock(length - 4);
/* 229 */       handler.headers(false, false, streamId, -1, headerBlock, HeadersMode.SPDY_HEADERS);
/*     */     }
/*     */     
/*     */     private void readWindowUpdate(FrameReader.Handler handler, int flags, int length) throws IOException {
/* 233 */       if (length != 8) throw ioException("TYPE_WINDOW_UPDATE length: %d != 8", new Object[] { Integer.valueOf(length) });
/* 234 */       int w1 = this.source.readInt();
/* 235 */       int w2 = this.source.readInt();
/* 236 */       int streamId = w1 & 0x7FFFFFFF;
/* 237 */       long increment = w2 & 0x7FFFFFFF;
/* 238 */       if (increment == 0L) throw ioException("windowSizeIncrement was 0", new Object[] { Long.valueOf(increment) });
/* 239 */       handler.windowUpdate(streamId, increment);
/*     */     }
/*     */     
/*     */     private void readPing(FrameReader.Handler handler, int flags, int length) throws IOException {
/* 243 */       if (length != 4) throw ioException("TYPE_PING length: %d != 4", new Object[] { Integer.valueOf(length) });
/* 244 */       int id = this.source.readInt();
/* 245 */       boolean ack = this.client == ((id & 0x1) == 1);
/* 246 */       handler.ping(ack, id, 0);
/*     */     }
/*     */     
/*     */     private void readGoAway(FrameReader.Handler handler, int flags, int length) throws IOException {
/* 250 */       if (length != 8) throw ioException("TYPE_GOAWAY length: %d != 8", new Object[] { Integer.valueOf(length) });
/* 251 */       int lastGoodStreamId = this.source.readInt() & 0x7FFFFFFF;
/* 252 */       int errorCodeInt = this.source.readInt();
/* 253 */       ErrorCode errorCode = ErrorCode.fromSpdyGoAway(errorCodeInt);
/* 254 */       if (errorCode == null) {
/* 255 */         throw ioException("TYPE_GOAWAY unexpected error code: %d", new Object[] { Integer.valueOf(errorCodeInt) });
/*     */       }
/* 257 */       handler.goAway(lastGoodStreamId, errorCode, ByteString.EMPTY);
/*     */     }
/*     */     
/*     */     private void readSettings(FrameReader.Handler handler, int flags, int length) throws IOException {
/* 261 */       int numberOfEntries = this.source.readInt();
/* 262 */       if (length != 4 + 8 * numberOfEntries) {
/* 263 */         throw ioException("TYPE_SETTINGS length: %d != 4 + 8 * %d", new Object[] { Integer.valueOf(length), Integer.valueOf(numberOfEntries) });
/*     */       }
/* 265 */       Settings settings = new Settings();
/* 266 */       for (int i = 0; i < numberOfEntries; i++) {
/* 267 */         int w1 = this.source.readInt();
/* 268 */         int value = this.source.readInt();
/* 269 */         int idFlags = (w1 & 0xFF000000) >>> 24;
/* 270 */         int id = w1 & 0xFFFFFF;
/* 271 */         settings.set(id, idFlags, value);
/*     */       }
/* 273 */       boolean clearPrevious = (flags & 0x1) != 0;
/* 274 */       handler.settings(clearPrevious, settings);
/*     */     }
/*     */     
/*     */     private static IOException ioException(String message, Object... args) throws IOException {
/* 278 */       throw new IOException(String.format(message, args));
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 282 */       this.headerBlockReader.close();
/*     */     }
/*     */   }
/*     */   
/*     */   static final class Writer implements FrameWriter
/*     */   {
/*     */     private final BufferedSink sink;
/*     */     private final Buffer headerBlockBuffer;
/*     */     private final BufferedSink headerBlockOut;
/*     */     private final boolean client;
/*     */     private boolean closed;
/*     */     
/*     */     Writer(BufferedSink sink, boolean client) {
/* 295 */       this.sink = sink;
/* 296 */       this.client = client;
/*     */       
/* 298 */       Deflater deflater = new Deflater();
/* 299 */       deflater.setDictionary(Spdy3.DICTIONARY);
/* 300 */       this.headerBlockBuffer = new Buffer();
/* 301 */       this.headerBlockOut = Okio.buffer(new DeflaterSink(this.headerBlockBuffer, deflater));
/*     */     }
/*     */     
/*     */ 
/*     */     public void ackSettings(Settings peerSettings) {}
/*     */     
/*     */ 
/*     */     public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders)
/*     */       throws IOException
/*     */     {}
/*     */     
/*     */ 
/*     */     public synchronized void connectionPreface() {}
/*     */     
/*     */ 
/*     */     public synchronized void flush()
/*     */       throws IOException
/*     */     {
/* 319 */       if (this.closed) throw new IOException("closed");
/* 320 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void synStream(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock)
/*     */       throws IOException
/*     */     {
/* 326 */       if (this.closed) throw new IOException("closed");
/* 327 */       writeNameValueBlockToBuffer(headerBlock);
/* 328 */       int length = (int)(10L + this.headerBlockBuffer.size());
/* 329 */       int type = 1;
/* 330 */       int flags = (outFinished ? 1 : 0) | (inFinished ? 2 : 0);
/*     */       
/* 332 */       int unused = 0;
/* 333 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 334 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 335 */       this.sink.writeInt(streamId & 0x7FFFFFFF);
/* 336 */       this.sink.writeInt(associatedStreamId & 0x7FFFFFFF);
/* 337 */       this.sink.writeShort((unused & 0x7) << 13 | (unused & 0x1F) << 8 | unused & 0xFF);
/* 338 */       this.sink.writeAll(this.headerBlockBuffer);
/* 339 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void synReply(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException
/*     */     {
/* 344 */       if (this.closed) throw new IOException("closed");
/* 345 */       writeNameValueBlockToBuffer(headerBlock);
/* 346 */       int type = 2;
/* 347 */       int flags = outFinished ? 1 : 0;
/* 348 */       int length = (int)(this.headerBlockBuffer.size() + 4L);
/*     */       
/* 350 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 351 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 352 */       this.sink.writeInt(streamId & 0x7FFFFFFF);
/* 353 */       this.sink.writeAll(this.headerBlockBuffer);
/* 354 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void headers(int streamId, List<Header> headerBlock) throws IOException
/*     */     {
/* 359 */       if (this.closed) throw new IOException("closed");
/* 360 */       writeNameValueBlockToBuffer(headerBlock);
/* 361 */       int flags = 0;
/* 362 */       int type = 8;
/* 363 */       int length = (int)(this.headerBlockBuffer.size() + 4L);
/*     */       
/* 365 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 366 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 367 */       this.sink.writeInt(streamId & 0x7FFFFFFF);
/* 368 */       this.sink.writeAll(this.headerBlockBuffer);
/*     */     }
/*     */     
/*     */     public synchronized void rstStream(int streamId, ErrorCode errorCode) throws IOException
/*     */     {
/* 373 */       if (this.closed) throw new IOException("closed");
/* 374 */       if (errorCode.spdyRstCode == -1) throw new IllegalArgumentException();
/* 375 */       int flags = 0;
/* 376 */       int type = 3;
/* 377 */       int length = 8;
/* 378 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 379 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 380 */       this.sink.writeInt(streamId & 0x7FFFFFFF);
/* 381 */       this.sink.writeInt(errorCode.spdyRstCode);
/* 382 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public int maxDataLength() {
/* 386 */       return 16383;
/*     */     }
/*     */     
/*     */     public synchronized void data(boolean outFinished, int streamId, Buffer source, int byteCount) throws IOException
/*     */     {
/* 391 */       int flags = outFinished ? 1 : 0;
/* 392 */       sendDataFrame(streamId, flags, source, byteCount);
/*     */     }
/*     */     
/*     */     void sendDataFrame(int streamId, int flags, Buffer buffer, int byteCount) throws IOException
/*     */     {
/* 397 */       if (this.closed) throw new IOException("closed");
/* 398 */       if (byteCount > 16777215L) {
/* 399 */         throw new IllegalArgumentException("FRAME_TOO_LARGE max size is 16Mib: " + byteCount);
/*     */       }
/* 401 */       this.sink.writeInt(streamId & 0x7FFFFFFF);
/* 402 */       this.sink.writeInt((flags & 0xFF) << 24 | byteCount & 0xFFFFFF);
/* 403 */       if (byteCount > 0) {
/* 404 */         this.sink.write(buffer, byteCount);
/*     */       }
/*     */     }
/*     */     
/*     */     private void writeNameValueBlockToBuffer(List<Header> headerBlock) throws IOException {
/* 409 */       this.headerBlockOut.writeInt(headerBlock.size());
/* 410 */       int i = 0; for (int size = headerBlock.size(); i < size; i++) {
/* 411 */         ByteString name = ((Header)headerBlock.get(i)).name;
/* 412 */         this.headerBlockOut.writeInt(name.size());
/* 413 */         this.headerBlockOut.write(name);
/* 414 */         ByteString value = ((Header)headerBlock.get(i)).value;
/* 415 */         this.headerBlockOut.writeInt(value.size());
/* 416 */         this.headerBlockOut.write(value);
/*     */       }
/* 418 */       this.headerBlockOut.flush();
/*     */     }
/*     */     
/*     */     public synchronized void settings(Settings settings) throws IOException {
/* 422 */       if (this.closed) throw new IOException("closed");
/* 423 */       int type = 4;
/* 424 */       int flags = 0;
/* 425 */       int size = settings.size();
/* 426 */       int length = 4 + size * 8;
/* 427 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 428 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 429 */       this.sink.writeInt(size);
/* 430 */       for (int i = 0; i <= 10; i++)
/* 431 */         if (settings.isSet(i)) {
/* 432 */           int settingsFlags = settings.flags(i);
/* 433 */           this.sink.writeInt((settingsFlags & 0xFF) << 24 | i & 0xFFFFFF);
/* 434 */           this.sink.writeInt(settings.get(i));
/*     */         }
/* 436 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void ping(boolean reply, int payload1, int payload2) throws IOException
/*     */     {
/* 441 */       if (this.closed) throw new IOException("closed");
/* 442 */       boolean payloadIsReply = this.client != ((payload1 & 0x1) == 1);
/* 443 */       if (reply != payloadIsReply) throw new IllegalArgumentException("payload != reply");
/* 444 */       int type = 6;
/* 445 */       int flags = 0;
/* 446 */       int length = 4;
/* 447 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 448 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 449 */       this.sink.writeInt(payload1);
/* 450 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode, byte[] ignored) throws IOException
/*     */     {
/* 455 */       if (this.closed) throw new IOException("closed");
/* 456 */       if (errorCode.spdyGoAwayCode == -1) {
/* 457 */         throw new IllegalArgumentException("errorCode.spdyGoAwayCode == -1");
/*     */       }
/* 459 */       int type = 7;
/* 460 */       int flags = 0;
/* 461 */       int length = 8;
/* 462 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 463 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 464 */       this.sink.writeInt(lastGoodStreamId);
/* 465 */       this.sink.writeInt(errorCode.spdyGoAwayCode);
/* 466 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void windowUpdate(int streamId, long increment) throws IOException
/*     */     {
/* 471 */       if (this.closed) throw new IOException("closed");
/* 472 */       if ((increment == 0L) || (increment > 2147483647L)) {
/* 473 */         throw new IllegalArgumentException("windowSizeIncrement must be between 1 and 0x7fffffff: " + increment);
/*     */       }
/*     */       
/* 476 */       int type = 9;
/* 477 */       int flags = 0;
/* 478 */       int length = 8;
/* 479 */       this.sink.writeInt(0x80030000 | type & 0xFFFF);
/* 480 */       this.sink.writeInt((flags & 0xFF) << 24 | length & 0xFFFFFF);
/* 481 */       this.sink.writeInt(streamId);
/* 482 */       this.sink.writeInt((int)increment);
/* 483 */       this.sink.flush();
/*     */     }
/*     */     
/*     */     public synchronized void close() throws IOException {
/* 487 */       this.closed = true;
/* 488 */       Util.closeAll(this.sink, this.headerBlockOut);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\Spdy3.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */