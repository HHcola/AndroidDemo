/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.Address;
/*     */ import com.squareup.okhttp.CertificatePinner;
/*     */ import com.squareup.okhttp.Connection;
/*     */ import com.squareup.okhttp.Headers;
/*     */ import com.squareup.okhttp.Headers.Builder;
/*     */ import com.squareup.okhttp.HttpUrl;
/*     */ import com.squareup.okhttp.Interceptor;
/*     */ import com.squareup.okhttp.Interceptor.Chain;
/*     */ import com.squareup.okhttp.MediaType;
/*     */ import com.squareup.okhttp.OkHttpClient;
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import com.squareup.okhttp.Request;
/*     */ import com.squareup.okhttp.Request.Builder;
/*     */ import com.squareup.okhttp.RequestBody;
/*     */ import com.squareup.okhttp.Response;
/*     */ import com.squareup.okhttp.Response.Builder;
/*     */ import com.squareup.okhttp.ResponseBody;
/*     */ import com.squareup.okhttp.Route;
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.InternalCache;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.Version;
/*     */ import com.squareup.okhttp.internal.io.RealConnection;
/*     */ import java.io.IOException;
/*     */ import java.net.CookieHandler;
/*     */ import java.net.ProtocolException;
/*     */ import java.net.Proxy;
/*     */ import java.net.Proxy.Type;
/*     */ import java.util.Date;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import javax.net.ssl.HostnameVerifier;
/*     */ import javax.net.ssl.SSLSocketFactory;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.GzipSource;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class HttpEngine
/*     */ {
/*     */   public static final int MAX_FOLLOW_UPS = 20;
/*  94 */   private static final ResponseBody EMPTY_BODY = new ResponseBody() {
/*     */     public MediaType contentType() {
/*  96 */       return null;
/*     */     }
/*     */     
/*  99 */     public long contentLength() { return 0L; }
/*     */     
/*     */     public BufferedSource source() {
/* 102 */       return new Buffer();
/*     */     }
/*     */   };
/*     */   
/*     */   final OkHttpClient client;
/*     */   
/*     */   public final StreamAllocation streamAllocation;
/*     */   
/*     */   private final Response priorResponse;
/*     */   
/*     */   private HttpStream httpStream;
/* 113 */   long sentRequestMillis = -1L;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private boolean transparentGzip;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public final boolean bufferRequestBody;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private final Request userRequest;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private Request networkRequest;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private Response cacheResponse;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private Response userResponse;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private Sink requestBodyOut;
/*     */   
/*     */ 
/*     */ 
/*     */   private BufferedSink bufferedRequestBody;
/*     */   
/*     */ 
/*     */ 
/*     */   private final boolean callerWritesRequestBody;
/*     */   
/*     */ 
/*     */ 
/*     */   private final boolean forWebSocket;
/*     */   
/*     */ 
/*     */ 
/*     */   private CacheRequest storeRequest;
/*     */   
/*     */ 
/*     */ 
/*     */   private CacheStrategy cacheStrategy;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public HttpEngine(OkHttpClient client, Request request, boolean bufferRequestBody, boolean callerWritesRequestBody, boolean forWebSocket, StreamAllocation streamAllocation, RetryableSink requestBodyOut, Response priorResponse)
/*     */   {
/* 175 */     this.client = client;
/* 176 */     this.userRequest = request;
/* 177 */     this.bufferRequestBody = bufferRequestBody;
/* 178 */     this.callerWritesRequestBody = callerWritesRequestBody;
/* 179 */     this.forWebSocket = forWebSocket;
/* 180 */     this.streamAllocation = (streamAllocation != null ? streamAllocation : new StreamAllocation(client
/*     */     
/* 182 */       .getConnectionPool(), createAddress(client, request)));
/* 183 */     this.requestBodyOut = requestBodyOut;
/* 184 */     this.priorResponse = priorResponse;
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
/*     */   public void sendRequest()
/*     */     throws RequestException, RouteException, IOException
/*     */   {
/* 200 */     if (this.cacheStrategy != null) return;
/* 201 */     if (this.httpStream != null) { throw new IllegalStateException();
/*     */     }
/* 203 */     Request request = networkRequest(this.userRequest);
/*     */     
/* 205 */     InternalCache responseCache = Internal.instance.internalCache(this.client);
/*     */     
/* 207 */     Response cacheCandidate = responseCache != null ? responseCache.get(request) : null;
/*     */     
/*     */ 
/* 210 */     long now = System.currentTimeMillis();
/* 211 */     this.cacheStrategy = new CacheStrategy.Factory(now, request, cacheCandidate).get();
/* 212 */     this.networkRequest = this.cacheStrategy.networkRequest;
/* 213 */     this.cacheResponse = this.cacheStrategy.cacheResponse;
/*     */     
/* 215 */     if (responseCache != null) {
/* 216 */       responseCache.trackResponse(this.cacheStrategy);
/*     */     }
/*     */     
/* 219 */     if ((cacheCandidate != null) && (this.cacheResponse == null)) {
/* 220 */       Util.closeQuietly(cacheCandidate.body());
/*     */     }
/*     */     
/* 223 */     if (this.networkRequest != null) {
/* 224 */       this.httpStream = connect();
/* 225 */       this.httpStream.setHttpEngine(this);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 231 */       if ((this.callerWritesRequestBody) && (permitsRequestBody(this.networkRequest)) && (this.requestBodyOut == null)) {
/* 232 */         long contentLength = OkHeaders.contentLength(request);
/* 233 */         if (this.bufferRequestBody) {
/* 234 */           if (contentLength > 2147483647L) {
/* 235 */             throw new IllegalStateException("Use setFixedLengthStreamingMode() or setChunkedStreamingMode() for requests larger than 2 GiB.");
/*     */           }
/*     */           
/*     */ 
/* 239 */           if (contentLength != -1L)
/*     */           {
/* 241 */             this.httpStream.writeRequestHeaders(this.networkRequest);
/* 242 */             this.requestBodyOut = new RetryableSink((int)contentLength);
/*     */ 
/*     */           }
/*     */           else
/*     */           {
/* 247 */             this.requestBodyOut = new RetryableSink();
/*     */           }
/*     */         } else {
/* 250 */           this.httpStream.writeRequestHeaders(this.networkRequest);
/* 251 */           this.requestBodyOut = this.httpStream.createRequestBody(this.networkRequest, contentLength);
/*     */         }
/*     */       }
/*     */     }
/*     */     else {
/* 256 */       if (this.cacheResponse != null)
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 262 */         this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).build();
/*     */ 
/*     */ 
/*     */ 
/*     */       }
/*     */       else
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/* 272 */         this.userResponse = new Response.Builder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).protocol(Protocol.HTTP_1_1).code(504).message("Unsatisfiable Request (only-if-cached)").body(EMPTY_BODY).build();
/*     */       }
/*     */       
/* 275 */       this.userResponse = unzip(this.userResponse);
/*     */     }
/*     */   }
/*     */   
/*     */   private HttpStream connect() throws RouteException, RequestException, IOException {
/* 280 */     boolean doExtensiveHealthChecks = !this.networkRequest.method().equals("GET");
/* 281 */     return this.streamAllocation.newStream(this.client.getConnectTimeout(), this.client
/* 282 */       .getReadTimeout(), this.client.getWriteTimeout(), this.client
/* 283 */       .getRetryOnConnectionFailure(), doExtensiveHealthChecks);
/*     */   }
/*     */   
/*     */   private static Response stripBody(Response response)
/*     */   {
/* 288 */     return (response != null) && (response.body() != null) ? response.newBuilder().body(null).build() : response;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void writingRequestHeaders()
/*     */   {
/* 298 */     if (this.sentRequestMillis != -1L) throw new IllegalStateException();
/* 299 */     this.sentRequestMillis = System.currentTimeMillis();
/*     */   }
/*     */   
/*     */   boolean permitsRequestBody(Request request) {
/* 303 */     return HttpMethod.permitsRequestBody(request.method());
/*     */   }
/*     */   
/*     */   public Sink getRequestBody()
/*     */   {
/* 308 */     if (this.cacheStrategy == null) throw new IllegalStateException();
/* 309 */     return this.requestBodyOut;
/*     */   }
/*     */   
/*     */   public BufferedSink getBufferedRequestBody() {
/* 313 */     BufferedSink result = this.bufferedRequestBody;
/* 314 */     if (result != null) return result;
/* 315 */     Sink requestBody = getRequestBody();
/*     */     
/* 317 */     return requestBody != null ? (this.bufferedRequestBody = Okio.buffer(requestBody)) : null;
/*     */   }
/*     */   
/*     */   public boolean hasResponse()
/*     */   {
/* 322 */     return this.userResponse != null;
/*     */   }
/*     */   
/*     */   public Request getRequest() {
/* 326 */     return this.userRequest;
/*     */   }
/*     */   
/*     */ 
/*     */   public Response getResponse()
/*     */   {
/* 332 */     if (this.userResponse == null) throw new IllegalStateException();
/* 333 */     return this.userResponse;
/*     */   }
/*     */   
/*     */   public Connection getConnection() {
/* 337 */     return this.streamAllocation.connection();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public HttpEngine recover(RouteException e)
/*     */   {
/* 346 */     if (!this.streamAllocation.recover(e)) {
/* 347 */       return null;
/*     */     }
/*     */     
/* 350 */     if (!this.client.getRetryOnConnectionFailure()) {
/* 351 */       return null;
/*     */     }
/*     */     
/* 354 */     StreamAllocation streamAllocation = close();
/*     */     
/*     */ 
/* 357 */     return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, this.callerWritesRequestBody, this.forWebSocket, streamAllocation, (RetryableSink)this.requestBodyOut, this.priorResponse);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public HttpEngine recover(IOException e, Sink requestBodyOut)
/*     */   {
/* 368 */     if (!this.streamAllocation.recover(e, requestBodyOut)) {
/* 369 */       return null;
/*     */     }
/*     */     
/* 372 */     if (!this.client.getRetryOnConnectionFailure()) {
/* 373 */       return null;
/*     */     }
/*     */     
/* 376 */     StreamAllocation streamAllocation = close();
/*     */     
/*     */ 
/* 379 */     return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, this.callerWritesRequestBody, this.forWebSocket, streamAllocation, (RetryableSink)requestBodyOut, this.priorResponse);
/*     */   }
/*     */   
/*     */   public HttpEngine recover(IOException e)
/*     */   {
/* 384 */     return recover(e, this.requestBodyOut);
/*     */   }
/*     */   
/*     */   private void maybeCache() throws IOException {
/* 388 */     InternalCache responseCache = Internal.instance.internalCache(this.client);
/* 389 */     if (responseCache == null) { return;
/*     */     }
/*     */     
/* 392 */     if (!CacheStrategy.isCacheable(this.userResponse, this.networkRequest)) {
/* 393 */       if (HttpMethod.invalidatesCache(this.networkRequest.method())) {
/*     */         try {
/* 395 */           responseCache.remove(this.networkRequest);
/*     */         }
/*     */         catch (IOException localIOException) {}
/*     */       }
/*     */       
/* 400 */       return;
/*     */     }
/*     */     
/*     */ 
/* 404 */     this.storeRequest = responseCache.put(stripBody(this.userResponse));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void releaseStreamAllocation()
/*     */     throws IOException
/*     */   {
/* 413 */     this.streamAllocation.release();
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
/*     */   public void cancel()
/*     */   {
/* 426 */     this.streamAllocation.cancel();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public StreamAllocation close()
/*     */   {
/* 434 */     if (this.bufferedRequestBody != null)
/*     */     {
/* 436 */       Util.closeQuietly(this.bufferedRequestBody);
/* 437 */     } else if (this.requestBodyOut != null) {
/* 438 */       Util.closeQuietly(this.requestBodyOut);
/*     */     }
/*     */     
/* 441 */     if (this.userResponse != null) {
/* 442 */       Util.closeQuietly(this.userResponse.body());
/*     */     }
/*     */     else {
/* 445 */       this.streamAllocation.connectionFailed();
/*     */     }
/*     */     
/* 448 */     return this.streamAllocation;
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
/*     */   private Response unzip(Response response)
/*     */     throws IOException
/*     */   {
/* 465 */     if ((!this.transparentGzip) || (!"gzip".equalsIgnoreCase(this.userResponse.header("Content-Encoding")))) {
/* 466 */       return response;
/*     */     }
/*     */     
/* 469 */     if (response.body() == null) {
/* 470 */       return response;
/*     */     }
/*     */     
/* 473 */     GzipSource responseBody = new GzipSource(response.body().source());
/*     */     
/*     */ 
/*     */ 
/* 477 */     Headers strippedHeaders = response.headers().newBuilder().removeAll("Content-Encoding").removeAll("Content-Length").build();
/*     */     
/*     */ 
/*     */ 
/* 481 */     return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody))).build();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean hasBody(Response response)
/*     */   {
/* 490 */     if (response.request().method().equals("HEAD")) {
/* 491 */       return false;
/*     */     }
/*     */     
/* 494 */     int responseCode = response.code();
/* 495 */     if (((responseCode < 100) || (responseCode >= 200)) && (responseCode != 204) && (responseCode != 304))
/*     */     {
/*     */ 
/* 498 */       return true;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 504 */     if ((OkHeaders.contentLength(response) != -1L) || 
/* 505 */       ("chunked".equalsIgnoreCase(response.header("Transfer-Encoding")))) {
/* 506 */       return true;
/*     */     }
/*     */     
/* 509 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Request networkRequest(Request request)
/*     */     throws IOException
/*     */   {
/* 519 */     Request.Builder result = request.newBuilder();
/*     */     
/* 521 */     if (request.header("Host") == null) {
/* 522 */       result.header("Host", Util.hostHeader(request.httpUrl()));
/*     */     }
/*     */     
/* 525 */     if (request.header("Connection") == null) {
/* 526 */       result.header("Connection", "Keep-Alive");
/*     */     }
/*     */     
/* 529 */     if (request.header("Accept-Encoding") == null) {
/* 530 */       this.transparentGzip = true;
/* 531 */       result.header("Accept-Encoding", "gzip");
/*     */     }
/*     */     
/* 534 */     CookieHandler cookieHandler = this.client.getCookieHandler();
/* 535 */     if (cookieHandler != null)
/*     */     {
/*     */ 
/*     */ 
/* 539 */       Map<String, List<String>> headers = OkHeaders.toMultimap(result.build().headers(), null);
/*     */       
/* 541 */       Map<String, List<String>> cookies = cookieHandler.get(request.uri(), headers);
/*     */       
/*     */ 
/* 544 */       OkHeaders.addCookies(result, cookies);
/*     */     }
/*     */     
/* 547 */     if (request.header("User-Agent") == null) {
/* 548 */       result.header("User-Agent", Version.userAgent());
/*     */     }
/*     */     
/* 551 */     return result.build();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void readResponse()
/*     */     throws IOException
/*     */   {
/* 559 */     if (this.userResponse != null) {
/* 560 */       return;
/*     */     }
/* 562 */     if ((this.networkRequest == null) && (this.cacheResponse == null)) {
/* 563 */       throw new IllegalStateException("call sendRequest() first!");
/*     */     }
/* 565 */     if (this.networkRequest == null) {
/*     */       return;
/*     */     }
/*     */     
/*     */     Response networkResponse;
/*     */     Response networkResponse;
/* 571 */     if (this.forWebSocket) {
/* 572 */       this.httpStream.writeRequestHeaders(this.networkRequest);
/* 573 */       networkResponse = readNetworkResponse();
/*     */     } else { Response networkResponse;
/* 575 */       if (!this.callerWritesRequestBody) {
/* 576 */         networkResponse = new NetworkInterceptorChain(0, this.networkRequest).proceed(this.networkRequest);
/*     */       }
/*     */       else
/*     */       {
/* 580 */         if ((this.bufferedRequestBody != null) && (this.bufferedRequestBody.buffer().size() > 0L)) {
/* 581 */           this.bufferedRequestBody.emit();
/*     */         }
/*     */         
/*     */ 
/* 585 */         if (this.sentRequestMillis == -1L) {
/* 586 */           if ((OkHeaders.contentLength(this.networkRequest) == -1L) && ((this.requestBodyOut instanceof RetryableSink)))
/*     */           {
/* 588 */             long contentLength = ((RetryableSink)this.requestBodyOut).contentLength();
/*     */             
/*     */ 
/* 591 */             this.networkRequest = this.networkRequest.newBuilder().header("Content-Length", Long.toString(contentLength)).build();
/*     */           }
/* 593 */           this.httpStream.writeRequestHeaders(this.networkRequest);
/*     */         }
/*     */         
/*     */ 
/* 597 */         if (this.requestBodyOut != null) {
/* 598 */           if (this.bufferedRequestBody != null)
/*     */           {
/* 600 */             this.bufferedRequestBody.close();
/*     */           } else {
/* 602 */             this.requestBodyOut.close();
/*     */           }
/* 604 */           if ((this.requestBodyOut instanceof RetryableSink)) {
/* 605 */             this.httpStream.writeRequestBody((RetryableSink)this.requestBodyOut);
/*     */           }
/*     */         }
/*     */         
/* 609 */         networkResponse = readNetworkResponse();
/*     */       }
/*     */     }
/* 612 */     receiveHeaders(networkResponse.headers());
/*     */     
/*     */ 
/* 615 */     if (this.cacheResponse != null) {
/* 616 */       if (validate(this.cacheResponse, networkResponse))
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 623 */         this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).headers(combine(this.cacheResponse.headers(), networkResponse.headers())).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(networkResponse)).build();
/* 624 */         networkResponse.body().close();
/* 625 */         releaseStreamAllocation();
/*     */         
/*     */ 
/*     */ 
/* 629 */         InternalCache responseCache = Internal.instance.internalCache(this.client);
/* 630 */         responseCache.trackConditionalCacheHit();
/* 631 */         responseCache.update(this.cacheResponse, stripBody(this.userResponse));
/* 632 */         this.userResponse = unzip(this.userResponse);
/* 633 */         return;
/*     */       }
/* 635 */       Util.closeQuietly(this.cacheResponse.body());
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 644 */     this.userResponse = networkResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(networkResponse)).build();
/*     */     
/* 646 */     if (hasBody(this.userResponse)) {
/* 647 */       maybeCache();
/* 648 */       this.userResponse = unzip(cacheWritingResponse(this.storeRequest, this.userResponse));
/*     */     }
/*     */   }
/*     */   
/*     */   class NetworkInterceptorChain implements Interceptor.Chain {
/*     */     private final int index;
/*     */     private final Request request;
/*     */     private int calls;
/*     */     
/*     */     NetworkInterceptorChain(int index, Request request) {
/* 658 */       this.index = index;
/* 659 */       this.request = request;
/*     */     }
/*     */     
/*     */     public Connection connection() {
/* 663 */       return HttpEngine.this.streamAllocation.connection();
/*     */     }
/*     */     
/*     */     public Request request() {
/* 667 */       return this.request;
/*     */     }
/*     */     
/*     */     public Response proceed(Request request) throws IOException {
/* 671 */       this.calls += 1;
/*     */       
/* 673 */       if (this.index > 0) {
/* 674 */         Interceptor caller = (Interceptor)HttpEngine.this.client.networkInterceptors().get(this.index - 1);
/* 675 */         Address address = connection().getRoute().getAddress();
/*     */         
/*     */ 
/* 678 */         if ((!request.httpUrl().host().equals(address.getUriHost())) || 
/* 679 */           (request.httpUrl().port() != address.getUriPort())) {
/* 680 */           throw new IllegalStateException("network interceptor " + caller + " must retain the same host and port");
/*     */         }
/*     */         
/*     */ 
/*     */ 
/* 685 */         if (this.calls > 1) {
/* 686 */           throw new IllegalStateException("network interceptor " + caller + " must call proceed() exactly once");
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 691 */       if (this.index < HttpEngine.this.client.networkInterceptors().size())
/*     */       {
/* 693 */         NetworkInterceptorChain chain = new NetworkInterceptorChain(HttpEngine.this, this.index + 1, request);
/* 694 */         Interceptor interceptor = (Interceptor)HttpEngine.this.client.networkInterceptors().get(this.index);
/* 695 */         Response interceptedResponse = interceptor.intercept(chain);
/*     */         
/*     */ 
/* 698 */         if (chain.calls != 1) {
/* 699 */           throw new IllegalStateException("network interceptor " + interceptor + " must call proceed() exactly once");
/*     */         }
/*     */         
/* 702 */         if (interceptedResponse == null) {
/* 703 */           throw new NullPointerException("network interceptor " + interceptor + " returned null");
/*     */         }
/*     */         
/*     */ 
/* 707 */         return interceptedResponse;
/*     */       }
/*     */       
/* 710 */       HttpEngine.this.httpStream.writeRequestHeaders(request);
/*     */       
/*     */ 
/* 713 */       HttpEngine.this.networkRequest = request;
/*     */       
/* 715 */       if ((HttpEngine.this.permitsRequestBody(request)) && (request.body() != null)) {
/* 716 */         Sink requestBodyOut = HttpEngine.this.httpStream.createRequestBody(request, request.body().contentLength());
/* 717 */         BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
/* 718 */         request.body().writeTo(bufferedRequestBody);
/* 719 */         bufferedRequestBody.close();
/*     */       }
/*     */       
/* 722 */       Response response = HttpEngine.this.readNetworkResponse();
/*     */       
/* 724 */       int code = response.code();
/* 725 */       if (((code == 204) || (code == 205)) && (response.body().contentLength() > 0L))
/*     */       {
/* 727 */         throw new ProtocolException("HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
/*     */       }
/*     */       
/* 730 */       return response;
/*     */     }
/*     */   }
/*     */   
/*     */   private Response readNetworkResponse() throws IOException {
/* 735 */     this.httpStream.finishRequest();
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 742 */     Response networkResponse = this.httpStream.readResponseHeaders().request(this.networkRequest).handshake(this.streamAllocation.connection().getHandshake()).header(OkHeaders.SENT_MILLIS, Long.toString(this.sentRequestMillis)).header(OkHeaders.RECEIVED_MILLIS, Long.toString(System.currentTimeMillis())).build();
/*     */     
/* 744 */     if (!this.forWebSocket)
/*     */     {
/*     */ 
/* 747 */       networkResponse = networkResponse.newBuilder().body(this.httpStream.openResponseBody(networkResponse)).build();
/*     */     }
/*     */     
/* 750 */     if (("close".equalsIgnoreCase(networkResponse.request().header("Connection"))) || 
/* 751 */       ("close".equalsIgnoreCase(networkResponse.header("Connection")))) {
/* 752 */       this.streamAllocation.noNewStreams();
/*     */     }
/*     */     
/* 755 */     return networkResponse;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Response cacheWritingResponse(final CacheRequest cacheRequest, Response response)
/*     */     throws IOException
/*     */   {
/* 766 */     if (cacheRequest == null) return response;
/* 767 */     Sink cacheBodyUnbuffered = cacheRequest.body();
/* 768 */     if (cacheBodyUnbuffered == null) { return response;
/*     */     }
/* 770 */     final BufferedSource source = response.body().source();
/* 771 */     final BufferedSink cacheBody = Okio.buffer(cacheBodyUnbuffered);
/*     */     
/* 773 */     Source cacheWritingSource = new Source()
/*     */     {
/*     */       boolean cacheRequestClosed;
/*     */       
/*     */       public long read(Buffer sink, long byteCount) throws IOException {
/*     */         try {
/* 779 */           bytesRead = source.read(sink, byteCount);
/*     */         } catch (IOException e) { long bytesRead;
/* 781 */           if (!this.cacheRequestClosed) {
/* 782 */             this.cacheRequestClosed = true;
/* 783 */             cacheRequest.abort();
/*     */           }
/* 785 */           throw e;
/*     */         }
/*     */         long bytesRead;
/* 788 */         if (bytesRead == -1L) {
/* 789 */           if (!this.cacheRequestClosed) {
/* 790 */             this.cacheRequestClosed = true;
/* 791 */             cacheBody.close();
/*     */           }
/* 793 */           return -1L;
/*     */         }
/*     */         
/* 796 */         sink.copyTo(cacheBody.buffer(), sink.size() - bytesRead, bytesRead);
/* 797 */         cacheBody.emitCompleteSegments();
/* 798 */         return bytesRead;
/*     */       }
/*     */       
/*     */       public Timeout timeout() {
/* 802 */         return source.timeout();
/*     */       }
/*     */       
/*     */       public void close() throws IOException {
/* 806 */         if ((!this.cacheRequestClosed) && 
/* 807 */           (!Util.discard(this, 100, TimeUnit.MILLISECONDS))) {
/* 808 */           this.cacheRequestClosed = true;
/* 809 */           cacheRequest.abort();
/*     */         }
/* 811 */         source.close();
/*     */ 
/*     */       }
/*     */       
/*     */ 
/* 816 */     };
/* 817 */     return response.newBuilder().body(new RealResponseBody(response.headers(), Okio.buffer(cacheWritingSource))).build();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private static boolean validate(Response cached, Response network)
/*     */   {
/* 825 */     if (network.code() == 304) {
/* 826 */       return true;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 832 */     Date lastModified = cached.headers().getDate("Last-Modified");
/* 833 */     if (lastModified != null) {
/* 834 */       Date networkLastModified = network.headers().getDate("Last-Modified");
/* 835 */       if ((networkLastModified != null) && 
/* 836 */         (networkLastModified.getTime() < lastModified.getTime())) {
/* 837 */         return true;
/*     */       }
/*     */     }
/*     */     
/* 841 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private static Headers combine(Headers cachedHeaders, Headers networkHeaders)
/*     */     throws IOException
/*     */   {
/* 849 */     Headers.Builder result = new Headers.Builder();
/*     */     
/* 851 */     int i = 0; for (int size = cachedHeaders.size(); i < size; i++) {
/* 852 */       String fieldName = cachedHeaders.name(i);
/* 853 */       String value = cachedHeaders.value(i);
/* 854 */       if ((!"Warning".equalsIgnoreCase(fieldName)) || (!value.startsWith("1")))
/*     */       {
/*     */ 
/* 857 */         if ((!OkHeaders.isEndToEnd(fieldName)) || (networkHeaders.get(fieldName) == null)) {
/* 858 */           result.add(fieldName, value);
/*     */         }
/*     */       }
/*     */     }
/* 862 */     int i = 0; for (int size = networkHeaders.size(); i < size; i++) {
/* 863 */       String fieldName = networkHeaders.name(i);
/* 864 */       if (!"Content-Length".equalsIgnoreCase(fieldName))
/*     */       {
/*     */ 
/* 867 */         if (OkHeaders.isEndToEnd(fieldName)) {
/* 868 */           result.add(fieldName, networkHeaders.value(i));
/*     */         }
/*     */       }
/*     */     }
/* 872 */     return result.build();
/*     */   }
/*     */   
/*     */   public void receiveHeaders(Headers headers) throws IOException {
/* 876 */     CookieHandler cookieHandler = this.client.getCookieHandler();
/* 877 */     if (cookieHandler != null) {
/* 878 */       cookieHandler.put(this.userRequest.uri(), OkHeaders.toMultimap(headers, null));
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Request followUpRequest()
/*     */     throws IOException
/*     */   {
/* 888 */     if (this.userResponse == null) throw new IllegalStateException();
/* 889 */     Connection connection = this.streamAllocation.connection();
/*     */     
/* 891 */     Route route = connection != null ? connection.getRoute() : null;
/*     */     
/*     */ 
/*     */ 
/* 895 */     Proxy selectedProxy = route != null ? route.getProxy() : this.client.getProxy();
/* 896 */     int responseCode = this.userResponse.code();
/*     */     
/* 898 */     String method = this.userRequest.method();
/* 899 */     switch (responseCode) {
/*     */     case 407: 
/* 901 */       if (selectedProxy.type() != Proxy.Type.HTTP) {
/* 902 */         throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
/*     */       }
/*     */     
/*     */     case 401: 
/* 906 */       return OkHeaders.processAuthHeader(this.client.getAuthenticator(), this.userResponse, selectedProxy);
/*     */     
/*     */ 
/*     */ 
/*     */     case 307: 
/*     */     case 308: 
/* 912 */       if ((!method.equals("GET")) && (!method.equals("HEAD"))) {
/* 913 */         return null;
/*     */       }
/*     */     
/*     */ 
/*     */     case 300: 
/*     */     case 301: 
/*     */     case 302: 
/*     */     case 303: 
/* 921 */       if (!this.client.getFollowRedirects()) { return null;
/*     */       }
/* 923 */       String location = this.userResponse.header("Location");
/* 924 */       if (location == null) return null;
/* 925 */       HttpUrl url = this.userRequest.httpUrl().resolve(location);
/*     */       
/*     */ 
/* 928 */       if (url == null) { return null;
/*     */       }
/*     */       
/* 931 */       boolean sameScheme = url.scheme().equals(this.userRequest.httpUrl().scheme());
/* 932 */       if ((!sameScheme) && (!this.client.getFollowSslRedirects())) { return null;
/*     */       }
/*     */       
/* 935 */       Request.Builder requestBuilder = this.userRequest.newBuilder();
/* 936 */       if (HttpMethod.permitsRequestBody(method)) {
/* 937 */         if (HttpMethod.redirectsToGet(method)) {
/* 938 */           requestBuilder.method("GET", null);
/*     */         } else {
/* 940 */           requestBuilder.method(method, null);
/*     */         }
/* 942 */         requestBuilder.removeHeader("Transfer-Encoding");
/* 943 */         requestBuilder.removeHeader("Content-Length");
/* 944 */         requestBuilder.removeHeader("Content-Type");
/*     */       }
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 950 */       if (!sameConnection(url)) {
/* 951 */         requestBuilder.removeHeader("Authorization");
/*     */       }
/*     */       
/* 954 */       return requestBuilder.url(url).build();
/*     */     }
/*     */     
/* 957 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean sameConnection(HttpUrl followUp)
/*     */   {
/* 966 */     HttpUrl url = this.userRequest.httpUrl();
/*     */     
/*     */ 
/* 969 */     return (url.host().equals(followUp.host())) && (url.port() == followUp.port()) && (url.scheme().equals(followUp.scheme()));
/*     */   }
/*     */   
/*     */   private static Address createAddress(OkHttpClient client, Request request) {
/* 973 */     SSLSocketFactory sslSocketFactory = null;
/* 974 */     HostnameVerifier hostnameVerifier = null;
/* 975 */     CertificatePinner certificatePinner = null;
/* 976 */     if (request.isHttps()) {
/* 977 */       sslSocketFactory = client.getSslSocketFactory();
/* 978 */       hostnameVerifier = client.getHostnameVerifier();
/* 979 */       certificatePinner = client.getCertificatePinner();
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 985 */     return new Address(request.httpUrl().host(), request.httpUrl().port(), client.getDns(), client.getSocketFactory(), sslSocketFactory, hostnameVerifier, certificatePinner, client.getAuthenticator(), client.getProxy(), client.getProtocols(), client.getConnectionSpecs(), client.getProxySelector());
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\HttpEngine.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */