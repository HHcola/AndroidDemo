/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.DiskLruCache;
/*     */ import com.squareup.okhttp.internal.DiskLruCache.Editor;
/*     */ import com.squareup.okhttp.internal.DiskLruCache.Snapshot;
/*     */ import com.squareup.okhttp.internal.InternalCache;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.http.CacheRequest;
/*     */ import com.squareup.okhttp.internal.http.CacheStrategy;
/*     */ import com.squareup.okhttp.internal.http.HttpMethod;
/*     */ import com.squareup.okhttp.internal.http.OkHeaders;
/*     */ import com.squareup.okhttp.internal.http.StatusLine;
/*     */ import com.squareup.okhttp.internal.io.FileSystem;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.CertificateEncodingException;
/*     */ import java.security.cert.CertificateException;
/*     */ import java.security.cert.CertificateFactory;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.NoSuchElementException;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.ByteString;
/*     */ import okio.ForwardingSink;
/*     */ import okio.ForwardingSource;
/*     */ import okio.Okio;
/*     */ import okio.Sink;
/*     */ import okio.Source;
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
/*     */ public final class Cache
/*     */ {
/*     */   private static final int VERSION = 201105;
/*     */   private static final int ENTRY_METADATA = 0;
/*     */   private static final int ENTRY_BODY = 1;
/*     */   private static final int ENTRY_COUNT = 2;
/* 137 */   final InternalCache internalCache = new InternalCache() {
/*     */     public Response get(Request request) throws IOException {
/* 139 */       return Cache.this.get(request);
/*     */     }
/*     */     
/* 142 */     public CacheRequest put(Response response) throws IOException { return Cache.this.put(response); }
/*     */     
/*     */     public void remove(Request request) throws IOException {
/* 145 */       Cache.this.remove(request);
/*     */     }
/*     */     
/* 148 */     public void update(Response cached, Response network) throws IOException { Cache.this.update(cached, network); }
/*     */     
/*     */     public void trackConditionalCacheHit() {
/* 151 */       Cache.this.trackConditionalCacheHit();
/*     */     }
/*     */     
/* 154 */     public void trackResponse(CacheStrategy cacheStrategy) { Cache.this.trackResponse(cacheStrategy); }
/*     */   };
/*     */   
/*     */   private final DiskLruCache cache;
/*     */   
/*     */   private int writeSuccessCount;
/*     */   
/*     */   private int writeAbortCount;
/*     */   private int networkCount;
/*     */   private int hitCount;
/*     */   private int requestCount;
/*     */   
/*     */   public Cache(File directory, long maxSize)
/*     */   {
/* 168 */     this(directory, maxSize, FileSystem.SYSTEM);
/*     */   }
/*     */   
/*     */   Cache(File directory, long maxSize, FileSystem fileSystem) {
/* 172 */     this.cache = DiskLruCache.create(fileSystem, directory, 201105, 2, maxSize);
/*     */   }
/*     */   
/*     */   private static String urlToKey(Request request) {
/* 176 */     return Util.md5Hex(request.urlString());
/*     */   }
/*     */   
/*     */   Response get(Request request) {
/* 180 */     String key = urlToKey(request);
/*     */     
/*     */     try
/*     */     {
/* 184 */       DiskLruCache.Snapshot snapshot = this.cache.get(key);
/* 185 */       if (snapshot == null) {
/* 186 */         return null;
/*     */       }
/*     */     }
/*     */     catch (IOException e) {
/* 190 */       return null;
/*     */     }
/*     */     DiskLruCache.Snapshot snapshot;
/*     */     try {
/* 194 */       entry = new Entry(snapshot.getSource(0));
/*     */     } catch (IOException e) { Entry entry;
/* 196 */       Util.closeQuietly(snapshot);
/* 197 */       return null;
/*     */     }
/*     */     Entry entry;
/* 200 */     Response response = entry.response(request, snapshot);
/*     */     
/* 202 */     if (!entry.matches(request, response)) {
/* 203 */       Util.closeQuietly(response.body());
/* 204 */       return null;
/*     */     }
/*     */     
/* 207 */     return response;
/*     */   }
/*     */   
/*     */   private CacheRequest put(Response response) throws IOException {
/* 211 */     String requestMethod = response.request().method();
/*     */     
/* 213 */     if (HttpMethod.invalidatesCache(response.request().method())) {
/*     */       try {
/* 215 */         remove(response.request());
/*     */       }
/*     */       catch (IOException localIOException1) {}
/*     */       
/* 219 */       return null;
/*     */     }
/* 221 */     if (!requestMethod.equals("GET"))
/*     */     {
/*     */ 
/*     */ 
/* 225 */       return null;
/*     */     }
/*     */     
/* 228 */     if (OkHeaders.hasVaryAll(response)) {
/* 229 */       return null;
/*     */     }
/*     */     
/* 232 */     Entry entry = new Entry(response);
/* 233 */     DiskLruCache.Editor editor = null;
/*     */     try {
/* 235 */       editor = this.cache.edit(urlToKey(response.request()));
/* 236 */       if (editor == null) {
/* 237 */         return null;
/*     */       }
/* 239 */       entry.writeTo(editor);
/* 240 */       return new CacheRequestImpl(editor);
/*     */     } catch (IOException e) {
/* 242 */       abortQuietly(editor); }
/* 243 */     return null;
/*     */   }
/*     */   
/*     */   private void remove(Request request) throws IOException
/*     */   {
/* 248 */     this.cache.remove(urlToKey(request));
/*     */   }
/*     */   
/*     */   private void update(Response cached, Response network) {
/* 252 */     Entry entry = new Entry(network);
/* 253 */     DiskLruCache.Snapshot snapshot = ((CacheResponseBody)cached.body()).snapshot;
/* 254 */     DiskLruCache.Editor editor = null;
/*     */     try {
/* 256 */       editor = snapshot.edit();
/* 257 */       if (editor != null) {
/* 258 */         entry.writeTo(editor);
/* 259 */         editor.commit();
/*     */       }
/*     */     } catch (IOException e) {
/* 262 */       abortQuietly(editor);
/*     */     }
/*     */   }
/*     */   
/*     */   private void abortQuietly(DiskLruCache.Editor editor)
/*     */   {
/*     */     try {
/* 269 */       if (editor != null) {
/* 270 */         editor.abort();
/*     */       }
/*     */     }
/*     */     catch (IOException localIOException) {}
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
/*     */   public void initialize()
/*     */     throws IOException
/*     */   {
/* 290 */     this.cache.initialize();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void delete()
/*     */     throws IOException
/*     */   {
/* 299 */     this.cache.delete();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void evictAll()
/*     */     throws IOException
/*     */   {
/* 307 */     this.cache.evictAll();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Iterator<String> urls()
/*     */     throws IOException
/*     */   {
/* 320 */     new Iterator() {
/* 321 */       final Iterator<DiskLruCache.Snapshot> delegate = Cache.this.cache.snapshots();
/*     */       String nextUrl;
/*     */       boolean canRemove;
/*     */       
/*     */       public boolean hasNext()
/*     */       {
/* 327 */         if (this.nextUrl != null) { return true;
/*     */         }
/* 329 */         this.canRemove = false;
/* 330 */         while (this.delegate.hasNext()) {
/* 331 */           DiskLruCache.Snapshot snapshot = (DiskLruCache.Snapshot)this.delegate.next();
/*     */           try {
/* 333 */             BufferedSource metadata = Okio.buffer(snapshot.getSource(0));
/* 334 */             this.nextUrl = metadata.readUtf8LineStrict();
/* 335 */             return true;
/*     */ 
/*     */           }
/*     */           catch (IOException localIOException) {}finally
/*     */           {
/* 340 */             snapshot.close();
/*     */           }
/*     */         }
/*     */         
/* 344 */         return false;
/*     */       }
/*     */       
/*     */       public String next() {
/* 348 */         if (!hasNext()) throw new NoSuchElementException();
/* 349 */         String result = this.nextUrl;
/* 350 */         this.nextUrl = null;
/* 351 */         this.canRemove = true;
/* 352 */         return result;
/*     */       }
/*     */       
/*     */       public void remove() {
/* 356 */         if (!this.canRemove) throw new IllegalStateException("remove() before next()");
/* 357 */         this.delegate.remove();
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   public synchronized int getWriteAbortCount() {
/* 363 */     return this.writeAbortCount;
/*     */   }
/*     */   
/*     */   public synchronized int getWriteSuccessCount() {
/* 367 */     return this.writeSuccessCount;
/*     */   }
/*     */   
/*     */   public long getSize() throws IOException {
/* 371 */     return this.cache.size();
/*     */   }
/*     */   
/*     */   public long getMaxSize() {
/* 375 */     return this.cache.getMaxSize();
/*     */   }
/*     */   
/*     */   public void flush() throws IOException {
/* 379 */     this.cache.flush();
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/* 383 */     this.cache.close();
/*     */   }
/*     */   
/*     */   public File getDirectory() {
/* 387 */     return this.cache.getDirectory();
/*     */   }
/*     */   
/*     */   public boolean isClosed() {
/* 391 */     return this.cache.isClosed();
/*     */   }
/*     */   
/*     */   private synchronized void trackResponse(CacheStrategy cacheStrategy) {
/* 395 */     this.requestCount += 1;
/*     */     
/* 397 */     if (cacheStrategy.networkRequest != null)
/*     */     {
/* 399 */       this.networkCount += 1;
/*     */     }
/* 401 */     else if (cacheStrategy.cacheResponse != null)
/*     */     {
/* 403 */       this.hitCount += 1;
/*     */     }
/*     */   }
/*     */   
/*     */   private synchronized void trackConditionalCacheHit() {
/* 408 */     this.hitCount += 1;
/*     */   }
/*     */   
/*     */   public synchronized int getNetworkCount() {
/* 412 */     return this.networkCount;
/*     */   }
/*     */   
/*     */   public synchronized int getHitCount() {
/* 416 */     return this.hitCount;
/*     */   }
/*     */   
/*     */   public synchronized int getRequestCount() {
/* 420 */     return this.requestCount;
/*     */   }
/*     */   
/*     */   private final class CacheRequestImpl implements CacheRequest {
/*     */     private final DiskLruCache.Editor editor;
/*     */     private Sink cacheOut;
/*     */     private boolean done;
/*     */     private Sink body;
/*     */     
/*     */     public CacheRequestImpl(final DiskLruCache.Editor editor) throws IOException {
/* 430 */       this.editor = editor;
/* 431 */       this.cacheOut = editor.newSink(1);
/* 432 */       this.body = new ForwardingSink(this.cacheOut) {
/*     */         public void close() throws IOException {
/* 434 */           synchronized (Cache.this) {
/* 435 */             if (Cache.CacheRequestImpl.this.done) {
/* 436 */               return;
/*     */             }
/* 438 */             Cache.CacheRequestImpl.this.done = true;
/* 439 */             Cache.access$808(Cache.this);
/*     */           }
/* 441 */           super.close();
/* 442 */           editor.commit();
/*     */         }
/*     */       };
/*     */     }
/*     */     
/*     */     public void abort() {
/* 448 */       synchronized (Cache.this) {
/* 449 */         if (this.done) {
/* 450 */           return;
/*     */         }
/* 452 */         this.done = true;
/* 453 */         Cache.access$908(Cache.this);
/*     */       }
/* 455 */       Util.closeQuietly(this.cacheOut);
/*     */       try {
/* 457 */         this.editor.abort();
/*     */       }
/*     */       catch (IOException localIOException) {}
/*     */     }
/*     */     
/*     */     public Sink body() {
/* 463 */       return this.body;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private static final class Entry
/*     */   {
/*     */     private final String url;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final Headers varyHeaders;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final String requestMethod;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final Protocol protocol;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final int code;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final String message;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final Headers responseHeaders;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private final Handshake handshake;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Entry(Source in)
/*     */       throws IOException
/*     */     {
/*     */       try
/*     */       {
/* 528 */         BufferedSource source = Okio.buffer(in);
/* 529 */         this.url = source.readUtf8LineStrict();
/* 530 */         this.requestMethod = source.readUtf8LineStrict();
/* 531 */         Headers.Builder varyHeadersBuilder = new Headers.Builder();
/* 532 */         int varyRequestHeaderLineCount = Cache.readInt(source);
/* 533 */         for (int i = 0; i < varyRequestHeaderLineCount; i++) {
/* 534 */           varyHeadersBuilder.addLenient(source.readUtf8LineStrict());
/*     */         }
/* 536 */         this.varyHeaders = varyHeadersBuilder.build();
/*     */         
/* 538 */         StatusLine statusLine = StatusLine.parse(source.readUtf8LineStrict());
/* 539 */         this.protocol = statusLine.protocol;
/* 540 */         this.code = statusLine.code;
/* 541 */         this.message = statusLine.message;
/* 542 */         Headers.Builder responseHeadersBuilder = new Headers.Builder();
/* 543 */         int responseHeaderLineCount = Cache.readInt(source);
/* 544 */         for (int i = 0; i < responseHeaderLineCount; i++) {
/* 545 */           responseHeadersBuilder.addLenient(source.readUtf8LineStrict());
/*     */         }
/* 547 */         this.responseHeaders = responseHeadersBuilder.build();
/*     */         
/* 549 */         if (isHttps()) {
/* 550 */           String blank = source.readUtf8LineStrict();
/* 551 */           if (blank.length() > 0) {
/* 552 */             throw new IOException("expected \"\" but was \"" + blank + "\"");
/*     */           }
/* 554 */           String cipherSuite = source.readUtf8LineStrict();
/* 555 */           List<Certificate> peerCertificates = readCertificateList(source);
/* 556 */           List<Certificate> localCertificates = readCertificateList(source);
/* 557 */           this.handshake = Handshake.get(cipherSuite, peerCertificates, localCertificates);
/*     */         } else {
/* 559 */           this.handshake = null;
/*     */         }
/*     */       } finally {
/* 562 */         in.close();
/*     */       }
/*     */     }
/*     */     
/*     */     public Entry(Response response) {
/* 567 */       this.url = response.request().urlString();
/* 568 */       this.varyHeaders = OkHeaders.varyHeaders(response);
/* 569 */       this.requestMethod = response.request().method();
/* 570 */       this.protocol = response.protocol();
/* 571 */       this.code = response.code();
/* 572 */       this.message = response.message();
/* 573 */       this.responseHeaders = response.headers();
/* 574 */       this.handshake = response.handshake();
/*     */     }
/*     */     
/*     */     public void writeTo(DiskLruCache.Editor editor) throws IOException {
/* 578 */       BufferedSink sink = Okio.buffer(editor.newSink(0));
/*     */       
/* 580 */       sink.writeUtf8(this.url);
/* 581 */       sink.writeByte(10);
/* 582 */       sink.writeUtf8(this.requestMethod);
/* 583 */       sink.writeByte(10);
/* 584 */       sink.writeDecimalLong(this.varyHeaders.size());
/* 585 */       sink.writeByte(10);
/* 586 */       int i = 0; for (int size = this.varyHeaders.size(); i < size; i++) {
/* 587 */         sink.writeUtf8(this.varyHeaders.name(i));
/* 588 */         sink.writeUtf8(": ");
/* 589 */         sink.writeUtf8(this.varyHeaders.value(i));
/* 590 */         sink.writeByte(10);
/*     */       }
/*     */       
/* 593 */       sink.writeUtf8(new StatusLine(this.protocol, this.code, this.message).toString());
/* 594 */       sink.writeByte(10);
/* 595 */       sink.writeDecimalLong(this.responseHeaders.size());
/* 596 */       sink.writeByte(10);
/* 597 */       int i = 0; for (int size = this.responseHeaders.size(); i < size; i++) {
/* 598 */         sink.writeUtf8(this.responseHeaders.name(i));
/* 599 */         sink.writeUtf8(": ");
/* 600 */         sink.writeUtf8(this.responseHeaders.value(i));
/* 601 */         sink.writeByte(10);
/*     */       }
/*     */       
/* 604 */       if (isHttps()) {
/* 605 */         sink.writeByte(10);
/* 606 */         sink.writeUtf8(this.handshake.cipherSuite());
/* 607 */         sink.writeByte(10);
/* 608 */         writeCertList(sink, this.handshake.peerCertificates());
/* 609 */         writeCertList(sink, this.handshake.localCertificates());
/*     */       }
/* 611 */       sink.close();
/*     */     }
/*     */     
/*     */     private boolean isHttps() {
/* 615 */       return this.url.startsWith("https://");
/*     */     }
/*     */     
/*     */     private List<Certificate> readCertificateList(BufferedSource source) throws IOException {
/* 619 */       int length = Cache.readInt(source);
/* 620 */       if (length == -1) return Collections.emptyList();
/*     */       try
/*     */       {
/* 623 */         CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
/* 624 */         List<Certificate> result = new ArrayList(length);
/* 625 */         for (int i = 0; i < length; i++) {
/* 626 */           String line = source.readUtf8LineStrict();
/* 627 */           Buffer bytes = new Buffer();
/* 628 */           bytes.write(ByteString.decodeBase64(line));
/* 629 */           result.add(certificateFactory.generateCertificate(bytes.inputStream()));
/*     */         }
/* 631 */         return result;
/*     */       } catch (CertificateException e) {
/* 633 */         throw new IOException(e.getMessage());
/*     */       }
/*     */     }
/*     */     
/*     */     private void writeCertList(BufferedSink sink, List<Certificate> certificates) throws IOException
/*     */     {
/*     */       try {
/* 640 */         sink.writeDecimalLong(certificates.size());
/* 641 */         sink.writeByte(10);
/* 642 */         int i = 0; for (int size = certificates.size(); i < size; i++) {
/* 643 */           byte[] bytes = ((Certificate)certificates.get(i)).getEncoded();
/* 644 */           String line = ByteString.of(bytes).base64();
/* 645 */           sink.writeUtf8(line);
/* 646 */           sink.writeByte(10);
/*     */         }
/*     */       } catch (CertificateEncodingException e) {
/* 649 */         throw new IOException(e.getMessage());
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */     public boolean matches(Request request, Response response)
/*     */     {
/* 656 */       return (this.url.equals(request.urlString())) && (this.requestMethod.equals(request.method())) && (OkHeaders.varyMatches(response, this.varyHeaders, request));
/*     */     }
/*     */     
/*     */     public Response response(Request request, DiskLruCache.Snapshot snapshot) {
/* 660 */       String contentType = this.responseHeaders.get("Content-Type");
/* 661 */       String contentLength = this.responseHeaders.get("Content-Length");
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 666 */       Request cacheRequest = new Request.Builder().url(this.url).method(this.requestMethod, null).headers(this.varyHeaders).build();
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 675 */       return new Response.Builder().request(cacheRequest).protocol(this.protocol).code(this.code).message(this.message).headers(this.responseHeaders).body(new Cache.CacheResponseBody(snapshot, contentType, contentLength)).handshake(this.handshake).build();
/*     */     }
/*     */   }
/*     */   
/*     */   private static int readInt(BufferedSource source) throws IOException {
/*     */     try {
/* 681 */       long result = source.readDecimalLong();
/* 682 */       String line = source.readUtf8LineStrict();
/* 683 */       if ((result < 0L) || (result > 2147483647L) || (!line.isEmpty())) {
/* 684 */         throw new IOException("expected an int but was \"" + result + line + "\"");
/*     */       }
/* 686 */       return (int)result;
/*     */     } catch (NumberFormatException e) {
/* 688 */       throw new IOException(e.getMessage());
/*     */     }
/*     */   }
/*     */   
/*     */   private static class CacheResponseBody extends ResponseBody
/*     */   {
/*     */     private final DiskLruCache.Snapshot snapshot;
/*     */     private final BufferedSource bodySource;
/*     */     private final String contentType;
/*     */     private final String contentLength;
/*     */     
/*     */     public CacheResponseBody(final DiskLruCache.Snapshot snapshot, String contentType, String contentLength) {
/* 700 */       this.snapshot = snapshot;
/* 701 */       this.contentType = contentType;
/* 702 */       this.contentLength = contentLength;
/*     */       
/* 704 */       Source source = snapshot.getSource(1);
/* 705 */       this.bodySource = Okio.buffer(new ForwardingSource(source) {
/*     */         public void close() throws IOException {
/* 707 */           snapshot.close();
/* 708 */           super.close();
/*     */         }
/*     */       });
/*     */     }
/*     */     
/*     */     public MediaType contentType() {
/* 714 */       return this.contentType != null ? MediaType.parse(this.contentType) : null;
/*     */     }
/*     */     
/*     */     public long contentLength() {
/*     */       try {
/* 719 */         return this.contentLength != null ? Long.parseLong(this.contentLength) : -1L;
/*     */       } catch (NumberFormatException e) {}
/* 721 */       return -1L;
/*     */     }
/*     */     
/*     */     public BufferedSource source()
/*     */     {
/* 726 */       return this.bodySource;
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Cache.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */