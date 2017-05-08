/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.InternalCache;
/*     */ import com.squareup.okhttp.internal.RouteDatabase;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.http.AuthenticatorAdapter;
/*     */ import com.squareup.okhttp.internal.http.HttpEngine;
/*     */ import com.squareup.okhttp.internal.http.StreamAllocation;
/*     */ import com.squareup.okhttp.internal.io.RealConnection;
/*     */ import com.squareup.okhttp.internal.tls.OkHostnameVerifier;
/*     */ import java.net.CookieHandler;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.Proxy;
/*     */ import java.net.ProxySelector;
/*     */ import java.net.UnknownHostException;
/*     */ import java.security.GeneralSecurityException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import javax.net.SocketFactory;
/*     */ import javax.net.ssl.HostnameVerifier;
/*     */ import javax.net.ssl.SSLContext;
/*     */ import javax.net.ssl.SSLSocket;
/*     */ import javax.net.ssl.SSLSocketFactory;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class OkHttpClient
/*     */   implements Cloneable
/*     */ {
/*  54 */   private static final List<Protocol> DEFAULT_PROTOCOLS = Util.immutableList(new Protocol[] { Protocol.HTTP_2, Protocol.SPDY_3, Protocol.HTTP_1_1 });
/*     */   
/*     */ 
/*  57 */   private static final List<ConnectionSpec> DEFAULT_CONNECTION_SPECS = Util.immutableList(new ConnectionSpec[] { ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT });
/*     */   private static SSLSocketFactory defaultSslSocketFactory;
/*     */   
/*     */   static {
/*  61 */     Internal.instance = new Internal() {
/*     */       public void addLenient(Headers.Builder builder, String line) {
/*  63 */         builder.addLenient(line);
/*     */       }
/*     */       
/*     */       public void addLenient(Headers.Builder builder, String name, String value) {
/*  67 */         builder.addLenient(name, value);
/*     */       }
/*     */       
/*     */       public void setCache(OkHttpClient client, InternalCache internalCache) {
/*  71 */         client.setInternalCache(internalCache);
/*     */       }
/*     */       
/*     */       public InternalCache internalCache(OkHttpClient client) {
/*  75 */         return client.internalCache();
/*     */       }
/*     */       
/*     */       public boolean connectionBecameIdle(ConnectionPool pool, RealConnection connection)
/*     */       {
/*  80 */         return pool.connectionBecameIdle(connection);
/*     */       }
/*     */       
/*     */       public RealConnection get(ConnectionPool pool, Address address, StreamAllocation streamAllocation)
/*     */       {
/*  85 */         return pool.get(address, streamAllocation);
/*     */       }
/*     */       
/*     */       public void put(ConnectionPool pool, RealConnection connection) {
/*  89 */         pool.put(connection);
/*     */       }
/*     */       
/*     */       public RouteDatabase routeDatabase(ConnectionPool connectionPool) {
/*  93 */         return connectionPool.routeDatabase;
/*     */       }
/*     */       
/*     */       public void callEnqueue(Call call, Callback responseCallback, boolean forWebSocket)
/*     */       {
/*  98 */         call.enqueue(responseCallback, forWebSocket);
/*     */       }
/*     */       
/*     */       public StreamAllocation callEngineGetStreamAllocation(Call call) {
/* 102 */         return call.engine.streamAllocation;
/*     */       }
/*     */       
/*     */       public void apply(ConnectionSpec tlsConfiguration, SSLSocket sslSocket, boolean isFallback)
/*     */       {
/* 107 */         tlsConfiguration.apply(sslSocket, isFallback);
/*     */       }
/*     */       
/*     */       public HttpUrl getHttpUrlChecked(String url) throws MalformedURLException, UnknownHostException
/*     */       {
/* 112 */         return HttpUrl.getChecked(url);
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */ 
/*     */   private final RouteDatabase routeDatabase;
/*     */   
/*     */   private Dispatcher dispatcher;
/*     */   
/*     */   private Proxy proxy;
/*     */   private List<Protocol> protocols;
/*     */   private List<ConnectionSpec> connectionSpecs;
/* 125 */   private final List<Interceptor> interceptors = new ArrayList();
/* 126 */   private final List<Interceptor> networkInterceptors = new ArrayList();
/*     */   
/*     */   private ProxySelector proxySelector;
/*     */   
/*     */   private CookieHandler cookieHandler;
/*     */   
/*     */   private InternalCache internalCache;
/*     */   private Cache cache;
/*     */   private SocketFactory socketFactory;
/*     */   private SSLSocketFactory sslSocketFactory;
/*     */   private HostnameVerifier hostnameVerifier;
/*     */   private CertificatePinner certificatePinner;
/*     */   private Authenticator authenticator;
/*     */   private ConnectionPool connectionPool;
/*     */   private Dns dns;
/* 141 */   private boolean followSslRedirects = true;
/* 142 */   private boolean followRedirects = true;
/* 143 */   private boolean retryOnConnectionFailure = true;
/* 144 */   private int connectTimeout = 10000;
/* 145 */   private int readTimeout = 10000;
/* 146 */   private int writeTimeout = 10000;
/*     */   
/*     */   public OkHttpClient() {
/* 149 */     this.routeDatabase = new RouteDatabase();
/* 150 */     this.dispatcher = new Dispatcher();
/*     */   }
/*     */   
/*     */   private OkHttpClient(OkHttpClient okHttpClient) {
/* 154 */     this.routeDatabase = okHttpClient.routeDatabase;
/* 155 */     this.dispatcher = okHttpClient.dispatcher;
/* 156 */     this.proxy = okHttpClient.proxy;
/* 157 */     this.protocols = okHttpClient.protocols;
/* 158 */     this.connectionSpecs = okHttpClient.connectionSpecs;
/* 159 */     this.interceptors.addAll(okHttpClient.interceptors);
/* 160 */     this.networkInterceptors.addAll(okHttpClient.networkInterceptors);
/* 161 */     this.proxySelector = okHttpClient.proxySelector;
/* 162 */     this.cookieHandler = okHttpClient.cookieHandler;
/* 163 */     this.cache = okHttpClient.cache;
/* 164 */     this.internalCache = (this.cache != null ? this.cache.internalCache : okHttpClient.internalCache);
/* 165 */     this.socketFactory = okHttpClient.socketFactory;
/* 166 */     this.sslSocketFactory = okHttpClient.sslSocketFactory;
/* 167 */     this.hostnameVerifier = okHttpClient.hostnameVerifier;
/* 168 */     this.certificatePinner = okHttpClient.certificatePinner;
/* 169 */     this.authenticator = okHttpClient.authenticator;
/* 170 */     this.connectionPool = okHttpClient.connectionPool;
/* 171 */     this.dns = okHttpClient.dns;
/* 172 */     this.followSslRedirects = okHttpClient.followSslRedirects;
/* 173 */     this.followRedirects = okHttpClient.followRedirects;
/* 174 */     this.retryOnConnectionFailure = okHttpClient.retryOnConnectionFailure;
/* 175 */     this.connectTimeout = okHttpClient.connectTimeout;
/* 176 */     this.readTimeout = okHttpClient.readTimeout;
/* 177 */     this.writeTimeout = okHttpClient.writeTimeout;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setConnectTimeout(long timeout, TimeUnit unit)
/*     */   {
/* 187 */     if (timeout < 0L) throw new IllegalArgumentException("timeout < 0");
/* 188 */     if (unit == null) throw new IllegalArgumentException("unit == null");
/* 189 */     long millis = unit.toMillis(timeout);
/* 190 */     if (millis > 2147483647L) throw new IllegalArgumentException("Timeout too large.");
/* 191 */     if ((millis == 0L) && (timeout > 0L)) throw new IllegalArgumentException("Timeout too small.");
/* 192 */     this.connectTimeout = ((int)millis);
/*     */   }
/*     */   
/*     */   public int getConnectTimeout()
/*     */   {
/* 197 */     return this.connectTimeout;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setReadTimeout(long timeout, TimeUnit unit)
/*     */   {
/* 207 */     if (timeout < 0L) throw new IllegalArgumentException("timeout < 0");
/* 208 */     if (unit == null) throw new IllegalArgumentException("unit == null");
/* 209 */     long millis = unit.toMillis(timeout);
/* 210 */     if (millis > 2147483647L) throw new IllegalArgumentException("Timeout too large.");
/* 211 */     if ((millis == 0L) && (timeout > 0L)) throw new IllegalArgumentException("Timeout too small.");
/* 212 */     this.readTimeout = ((int)millis);
/*     */   }
/*     */   
/*     */   public int getReadTimeout()
/*     */   {
/* 217 */     return this.readTimeout;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setWriteTimeout(long timeout, TimeUnit unit)
/*     */   {
/* 225 */     if (timeout < 0L) throw new IllegalArgumentException("timeout < 0");
/* 226 */     if (unit == null) throw new IllegalArgumentException("unit == null");
/* 227 */     long millis = unit.toMillis(timeout);
/* 228 */     if (millis > 2147483647L) throw new IllegalArgumentException("Timeout too large.");
/* 229 */     if ((millis == 0L) && (timeout > 0L)) throw new IllegalArgumentException("Timeout too small.");
/* 230 */     this.writeTimeout = ((int)millis);
/*     */   }
/*     */   
/*     */   public int getWriteTimeout()
/*     */   {
/* 235 */     return this.writeTimeout;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setProxy(Proxy proxy)
/*     */   {
/* 245 */     this.proxy = proxy;
/* 246 */     return this;
/*     */   }
/*     */   
/*     */   public Proxy getProxy() {
/* 250 */     return this.proxy;
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
/*     */   public OkHttpClient setProxySelector(ProxySelector proxySelector)
/*     */   {
/* 263 */     this.proxySelector = proxySelector;
/* 264 */     return this;
/*     */   }
/*     */   
/*     */   public ProxySelector getProxySelector() {
/* 268 */     return this.proxySelector;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setCookieHandler(CookieHandler cookieHandler)
/*     */   {
/* 279 */     this.cookieHandler = cookieHandler;
/* 280 */     return this;
/*     */   }
/*     */   
/*     */   public CookieHandler getCookieHandler() {
/* 284 */     return this.cookieHandler;
/*     */   }
/*     */   
/*     */   void setInternalCache(InternalCache internalCache)
/*     */   {
/* 289 */     this.internalCache = internalCache;
/* 290 */     this.cache = null;
/*     */   }
/*     */   
/*     */   InternalCache internalCache() {
/* 294 */     return this.internalCache;
/*     */   }
/*     */   
/*     */   public OkHttpClient setCache(Cache cache) {
/* 298 */     this.cache = cache;
/* 299 */     this.internalCache = null;
/* 300 */     return this;
/*     */   }
/*     */   
/*     */   public Cache getCache() {
/* 304 */     return this.cache;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setDns(Dns dns)
/*     */   {
/* 313 */     this.dns = dns;
/* 314 */     return this;
/*     */   }
/*     */   
/*     */   public Dns getDns() {
/* 318 */     return this.dns;
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
/*     */   public OkHttpClient setSocketFactory(SocketFactory socketFactory)
/*     */   {
/* 331 */     this.socketFactory = socketFactory;
/* 332 */     return this;
/*     */   }
/*     */   
/*     */   public SocketFactory getSocketFactory() {
/* 336 */     return this.socketFactory;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setSslSocketFactory(SSLSocketFactory sslSocketFactory)
/*     */   {
/* 345 */     this.sslSocketFactory = sslSocketFactory;
/* 346 */     return this;
/*     */   }
/*     */   
/*     */   public SSLSocketFactory getSslSocketFactory() {
/* 350 */     return this.sslSocketFactory;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setHostnameVerifier(HostnameVerifier hostnameVerifier)
/*     */   {
/* 360 */     this.hostnameVerifier = hostnameVerifier;
/* 361 */     return this;
/*     */   }
/*     */   
/*     */   public HostnameVerifier getHostnameVerifier() {
/* 365 */     return this.hostnameVerifier;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setCertificatePinner(CertificatePinner certificatePinner)
/*     */   {
/* 375 */     this.certificatePinner = certificatePinner;
/* 376 */     return this;
/*     */   }
/*     */   
/*     */   public CertificatePinner getCertificatePinner() {
/* 380 */     return this.certificatePinner;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setAuthenticator(Authenticator authenticator)
/*     */   {
/* 391 */     this.authenticator = authenticator;
/* 392 */     return this;
/*     */   }
/*     */   
/*     */   public Authenticator getAuthenticator() {
/* 396 */     return this.authenticator;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setConnectionPool(ConnectionPool connectionPool)
/*     */   {
/* 406 */     this.connectionPool = connectionPool;
/* 407 */     return this;
/*     */   }
/*     */   
/*     */   public ConnectionPool getConnectionPool() {
/* 411 */     return this.connectionPool;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setFollowSslRedirects(boolean followProtocolRedirects)
/*     */   {
/* 422 */     this.followSslRedirects = followProtocolRedirects;
/* 423 */     return this;
/*     */   }
/*     */   
/*     */   public boolean getFollowSslRedirects() {
/* 427 */     return this.followSslRedirects;
/*     */   }
/*     */   
/*     */   public void setFollowRedirects(boolean followRedirects)
/*     */   {
/* 432 */     this.followRedirects = followRedirects;
/*     */   }
/*     */   
/*     */   public boolean getFollowRedirects() {
/* 436 */     return this.followRedirects;
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
/*     */   public void setRetryOnConnectionFailure(boolean retryOnConnectionFailure)
/*     */   {
/* 458 */     this.retryOnConnectionFailure = retryOnConnectionFailure;
/*     */   }
/*     */   
/*     */   public boolean getRetryOnConnectionFailure() {
/* 462 */     return this.retryOnConnectionFailure;
/*     */   }
/*     */   
/*     */   RouteDatabase routeDatabase() {
/* 466 */     return this.routeDatabase;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient setDispatcher(Dispatcher dispatcher)
/*     */   {
/* 474 */     if (dispatcher == null) throw new IllegalArgumentException("dispatcher == null");
/* 475 */     this.dispatcher = dispatcher;
/* 476 */     return this;
/*     */   }
/*     */   
/*     */   public Dispatcher getDispatcher() {
/* 480 */     return this.dispatcher;
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
/*     */   public OkHttpClient setProtocols(List<Protocol> protocols)
/*     */   {
/* 514 */     protocols = Util.immutableList(protocols);
/* 515 */     if (!protocols.contains(Protocol.HTTP_1_1)) {
/* 516 */       throw new IllegalArgumentException("protocols doesn't contain http/1.1: " + protocols);
/*     */     }
/* 518 */     if (protocols.contains(Protocol.HTTP_1_0)) {
/* 519 */       throw new IllegalArgumentException("protocols must not contain http/1.0: " + protocols);
/*     */     }
/* 521 */     if (protocols.contains(null)) {
/* 522 */       throw new IllegalArgumentException("protocols must not contain null");
/*     */     }
/* 524 */     this.protocols = Util.immutableList(protocols);
/* 525 */     return this;
/*     */   }
/*     */   
/*     */   public List<Protocol> getProtocols() {
/* 529 */     return this.protocols;
/*     */   }
/*     */   
/*     */   public OkHttpClient setConnectionSpecs(List<ConnectionSpec> connectionSpecs) {
/* 533 */     this.connectionSpecs = Util.immutableList(connectionSpecs);
/* 534 */     return this;
/*     */   }
/*     */   
/*     */   public List<ConnectionSpec> getConnectionSpecs() {
/* 538 */     return this.connectionSpecs;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Interceptor> interceptors()
/*     */   {
/* 547 */     return this.interceptors;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Interceptor> networkInterceptors()
/*     */   {
/* 556 */     return this.networkInterceptors;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public Call newCall(Request request)
/*     */   {
/* 563 */     return new Call(this, request);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public OkHttpClient cancel(Object tag)
/*     */   {
/* 571 */     getDispatcher().cancel(tag);
/* 572 */     return this;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   OkHttpClient copyWithDefaults()
/*     */   {
/* 580 */     OkHttpClient result = new OkHttpClient(this);
/* 581 */     if (result.proxySelector == null) {
/* 582 */       result.proxySelector = ProxySelector.getDefault();
/*     */     }
/* 584 */     if (result.cookieHandler == null) {
/* 585 */       result.cookieHandler = CookieHandler.getDefault();
/*     */     }
/* 587 */     if (result.socketFactory == null) {
/* 588 */       result.socketFactory = SocketFactory.getDefault();
/*     */     }
/* 590 */     if (result.sslSocketFactory == null) {
/* 591 */       result.sslSocketFactory = getDefaultSSLSocketFactory();
/*     */     }
/* 593 */     if (result.hostnameVerifier == null) {
/* 594 */       result.hostnameVerifier = OkHostnameVerifier.INSTANCE;
/*     */     }
/* 596 */     if (result.certificatePinner == null) {
/* 597 */       result.certificatePinner = CertificatePinner.DEFAULT;
/*     */     }
/* 599 */     if (result.authenticator == null) {
/* 600 */       result.authenticator = AuthenticatorAdapter.INSTANCE;
/*     */     }
/* 602 */     if (result.connectionPool == null) {
/* 603 */       result.connectionPool = ConnectionPool.getDefault();
/*     */     }
/* 605 */     if (result.protocols == null) {
/* 606 */       result.protocols = DEFAULT_PROTOCOLS;
/*     */     }
/* 608 */     if (result.connectionSpecs == null) {
/* 609 */       result.connectionSpecs = DEFAULT_CONNECTION_SPECS;
/*     */     }
/* 611 */     if (result.dns == null) {
/* 612 */       result.dns = Dns.SYSTEM;
/*     */     }
/* 614 */     return result;
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
/*     */   private synchronized SSLSocketFactory getDefaultSSLSocketFactory()
/*     */   {
/* 629 */     if (defaultSslSocketFactory == null) {
/*     */       try {
/* 631 */         SSLContext sslContext = SSLContext.getInstance("TLS");
/* 632 */         sslContext.init(null, null, null);
/* 633 */         defaultSslSocketFactory = sslContext.getSocketFactory();
/*     */       } catch (GeneralSecurityException e) {
/* 635 */         throw new AssertionError();
/*     */       }
/*     */     }
/* 638 */     return defaultSslSocketFactory;
/*     */   }
/*     */   
/*     */   public OkHttpClient clone()
/*     */   {
/* 643 */     return new OkHttpClient(this);
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\OkHttpClient.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */