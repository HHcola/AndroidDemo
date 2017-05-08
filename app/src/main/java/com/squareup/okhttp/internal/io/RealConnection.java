/*     */ package com.squareup.okhttp.internal.io;
/*     */ 
/*     */ import com.squareup.okhttp.Address;
/*     */ import com.squareup.okhttp.CertificatePinner;
/*     */ import com.squareup.okhttp.Connection;
/*     */ import com.squareup.okhttp.ConnectionSpec;
/*     */ import com.squareup.okhttp.Handshake;
/*     */ import com.squareup.okhttp.HttpUrl;
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import com.squareup.okhttp.Request;
/*     */ import com.squareup.okhttp.Request.Builder;
/*     */ import com.squareup.okhttp.Response;
/*     */ import com.squareup.okhttp.Response.Builder;
/*     */ import com.squareup.okhttp.Route;
/*     */ import com.squareup.okhttp.internal.ConnectionSpecSelector;
/*     */ import com.squareup.okhttp.internal.Platform;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.Version;
/*     */ import com.squareup.okhttp.internal.framed.FramedConnection;
/*     */ import com.squareup.okhttp.internal.framed.FramedConnection.Builder;
/*     */ import com.squareup.okhttp.internal.http.Http1xStream;
/*     */ import com.squareup.okhttp.internal.http.OkHeaders;
/*     */ import com.squareup.okhttp.internal.http.RouteException;
/*     */ import com.squareup.okhttp.internal.http.StreamAllocation;
/*     */ import com.squareup.okhttp.internal.tls.CertificateChainCleaner;
/*     */ import com.squareup.okhttp.internal.tls.OkHostnameVerifier;
/*     */ import com.squareup.okhttp.internal.tls.TrustRootIndex;
/*     */ import java.io.IOException;
/*     */ import java.lang.ref.Reference;
/*     */ import java.net.ConnectException;
/*     */ import java.net.Proxy;
/*     */ import java.net.Proxy.Type;
/*     */ import java.net.Socket;
/*     */ import java.net.SocketTimeoutException;
/*     */ import java.net.UnknownServiceException;
/*     */ import java.security.Principal;
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.X509Certificate;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import javax.net.SocketFactory;
/*     */ import javax.net.ssl.HostnameVerifier;
/*     */ import javax.net.ssl.SSLPeerUnverifiedException;
/*     */ import javax.net.ssl.SSLSocket;
/*     */ import javax.net.ssl.SSLSocketFactory;
/*     */ import javax.net.ssl.X509TrustManager;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSink;
/*     */ import okio.BufferedSource;
/*     */ import okio.Okio;
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
/*     */ public final class RealConnection
/*     */   implements Connection
/*     */ {
/*     */   private final Route route;
/*     */   private Socket rawSocket;
/*     */   public Socket socket;
/*     */   private Handshake handshake;
/*     */   private Protocol protocol;
/*     */   public volatile FramedConnection framedConnection;
/*     */   public int streamCount;
/*     */   public BufferedSource source;
/*     */   public BufferedSink sink;
/*  84 */   public final List<Reference<StreamAllocation>> allocations = new ArrayList();
/*     */   public boolean noNewStreams;
/*  86 */   public long idleAtNanos = Long.MAX_VALUE;
/*     */   private static SSLSocketFactory lastSslSocketFactory;
/*     */   
/*  89 */   public RealConnection(Route route) { this.route = route; }
/*     */   
/*     */   public void connect(int connectTimeout, int readTimeout, int writeTimeout, List<ConnectionSpec> connectionSpecs, boolean connectionRetryEnabled)
/*     */     throws RouteException
/*     */   {
/*  94 */     if (this.protocol != null) { throw new IllegalStateException("already connected");
/*     */     }
/*  96 */     RouteException routeException = null;
/*  97 */     ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
/*  98 */     Proxy proxy = this.route.getProxy();
/*  99 */     Address address = this.route.getAddress();
/*     */     
/* 101 */     if ((this.route.getAddress().getSslSocketFactory() == null) && 
/* 102 */       (!connectionSpecs.contains(ConnectionSpec.CLEARTEXT))) {
/* 103 */       throw new RouteException(new UnknownServiceException("CLEARTEXT communication not supported: " + connectionSpecs));
/*     */     }
/*     */     
/*     */ 
/* 107 */     while (this.protocol == null) {
/*     */       try
/*     */       {
/* 110 */         this.rawSocket = ((proxy.type() == Proxy.Type.DIRECT) || (proxy.type() == Proxy.Type.HTTP) ? address.getSocketFactory().createSocket() : new Socket(proxy));
/*     */         
/* 112 */         connectSocket(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
/*     */       } catch (IOException e) {
/* 114 */         Util.closeQuietly(this.socket);
/* 115 */         Util.closeQuietly(this.rawSocket);
/* 116 */         this.socket = null;
/* 117 */         this.rawSocket = null;
/* 118 */         this.source = null;
/* 119 */         this.sink = null;
/* 120 */         this.handshake = null;
/* 121 */         this.protocol = null;
/*     */         
/* 123 */         if (routeException == null) {
/* 124 */           routeException = new RouteException(e);
/*     */         } else {
/* 126 */           routeException.addConnectException(e);
/*     */         }
/*     */         
/* 129 */         if ((!connectionRetryEnabled) || (!connectionSpecSelector.connectionFailed(e))) {
/* 130 */           throw routeException;
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void connectSocket(int connectTimeout, int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector)
/*     */     throws IOException
/*     */   {
/* 139 */     this.rawSocket.setSoTimeout(readTimeout);
/*     */     try {
/* 141 */       Platform.get().connectSocket(this.rawSocket, this.route.getSocketAddress(), connectTimeout);
/*     */     } catch (ConnectException e) {
/* 143 */       throw new ConnectException("Failed to connect to " + this.route.getSocketAddress());
/*     */     }
/* 145 */     this.source = Okio.buffer(Okio.source(this.rawSocket));
/* 146 */     this.sink = Okio.buffer(Okio.sink(this.rawSocket));
/*     */     
/* 148 */     if (this.route.getAddress().getSslSocketFactory() != null) {
/* 149 */       connectTls(readTimeout, writeTimeout, connectionSpecSelector);
/*     */     } else {
/* 151 */       this.protocol = Protocol.HTTP_1_1;
/* 152 */       this.socket = this.rawSocket;
/*     */     }
/*     */     
/* 155 */     if ((this.protocol == Protocol.SPDY_3) || (this.protocol == Protocol.HTTP_2)) {
/* 156 */       this.socket.setSoTimeout(0);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 161 */       FramedConnection framedConnection = new FramedConnection.Builder(true).socket(this.socket, this.route.getAddress().url().host(), this.source, this.sink).protocol(this.protocol).build();
/* 162 */       framedConnection.sendConnectionPreface();
/*     */       
/*     */ 
/* 165 */       this.framedConnection = framedConnection;
/*     */     }
/*     */   }
/*     */   
/*     */   private void connectTls(int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException
/*     */   {
/* 171 */     if (this.route.requiresTunnel()) {
/* 172 */       createTunnel(readTimeout, writeTimeout);
/*     */     }
/*     */     
/* 175 */     Address address = this.route.getAddress();
/* 176 */     SSLSocketFactory sslSocketFactory = address.getSslSocketFactory();
/* 177 */     boolean success = false;
/* 178 */     SSLSocket sslSocket = null;
/*     */     try
/*     */     {
/* 181 */       sslSocket = (SSLSocket)sslSocketFactory.createSocket(this.rawSocket, address
/* 182 */         .getUriHost(), address.getUriPort(), true);
/*     */       
/*     */ 
/* 185 */       ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
/* 186 */       if (connectionSpec.supportsTlsExtensions()) {
/* 187 */         Platform.get().configureTlsExtensions(sslSocket, address
/* 188 */           .getUriHost(), address.getProtocols());
/*     */       }
/*     */       
/*     */ 
/* 192 */       sslSocket.startHandshake();
/* 193 */       Handshake unverifiedHandshake = Handshake.get(sslSocket.getSession());
/*     */       
/*     */ 
/* 196 */       if (!address.getHostnameVerifier().verify(address.getUriHost(), sslSocket.getSession())) {
/* 197 */         X509Certificate cert = (X509Certificate)unverifiedHandshake.peerCertificates().get(0);
/*     */         
/*     */ 
/*     */ 
/* 201 */         throw new SSLPeerUnverifiedException("Hostname " + address.getUriHost() + " not verified:" + "\n    certificate: " + CertificatePinner.pin(cert) + "\n    DN: " + cert.getSubjectDN().getName() + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
/*     */       }
/*     */       
/*     */ 
/* 205 */       if (address.getCertificatePinner() != CertificatePinner.DEFAULT) {
/* 206 */         TrustRootIndex trustRootIndex = trustRootIndex(address.getSslSocketFactory());
/*     */         
/* 208 */         List<Certificate> certificates = new CertificateChainCleaner(trustRootIndex).clean(unverifiedHandshake.peerCertificates());
/* 209 */         address.getCertificatePinner().check(address.getUriHost(), certificates);
/*     */       }
/*     */       
/*     */ 
/*     */ 
/* 214 */       String maybeProtocol = connectionSpec.supportsTlsExtensions() ? Platform.get().getSelectedProtocol(sslSocket) : null;
/*     */       
/* 216 */       this.socket = sslSocket;
/* 217 */       this.source = Okio.buffer(Okio.source(this.socket));
/* 218 */       this.sink = Okio.buffer(Okio.sink(this.socket));
/* 219 */       this.handshake = unverifiedHandshake;
/*     */       
/* 221 */       this.protocol = (maybeProtocol != null ? Protocol.get(maybeProtocol) : Protocol.HTTP_1_1);
/*     */       
/* 223 */       success = true;
/*     */     } catch (AssertionError e) {
/* 225 */       if (Util.isAndroidGetsocknameError(e)) throw new IOException(e);
/* 226 */       throw e;
/*     */     } finally {
/* 228 */       if (sslSocket != null) {
/* 229 */         Platform.get().afterHandshake(sslSocket);
/*     */       }
/* 231 */       if (!success) {
/* 232 */         Util.closeQuietly(sslSocket);
/*     */       }
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
/*     */   private static synchronized TrustRootIndex trustRootIndex(SSLSocketFactory sslSocketFactory)
/*     */   {
/* 246 */     if (sslSocketFactory != lastSslSocketFactory) {
/* 247 */       X509TrustManager trustManager = Platform.get().trustManager(sslSocketFactory);
/* 248 */       lastTrustRootIndex = Platform.get().trustRootIndex(trustManager);
/* 249 */       lastSslSocketFactory = sslSocketFactory;
/*     */     }
/* 251 */     return lastTrustRootIndex;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void createTunnel(int readTimeout, int writeTimeout)
/*     */     throws IOException
/*     */   {
/* 261 */     Request tunnelRequest = createTunnelRequest();
/* 262 */     HttpUrl url = tunnelRequest.httpUrl();
/* 263 */     String requestLine = "CONNECT " + url.host() + ":" + url.port() + " HTTP/1.1";
/*     */     Response response;
/* 265 */     do { Http1xStream tunnelConnection = new Http1xStream(null, this.source, this.sink);
/* 266 */       this.source.timeout().timeout(readTimeout, TimeUnit.MILLISECONDS);
/* 267 */       this.sink.timeout().timeout(writeTimeout, TimeUnit.MILLISECONDS);
/* 268 */       tunnelConnection.writeRequest(tunnelRequest.headers(), requestLine);
/* 269 */       tunnelConnection.finishRequest();
/* 270 */       response = tunnelConnection.readResponse().request(tunnelRequest).build();
/*     */       
/*     */ 
/* 273 */       long contentLength = OkHeaders.contentLength(response);
/* 274 */       if (contentLength == -1L) {
/* 275 */         contentLength = 0L;
/*     */       }
/* 277 */       Source body = tunnelConnection.newFixedLengthSource(contentLength);
/* 278 */       Util.skipAll(body, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
/* 279 */       body.close();
/*     */       
/* 281 */       switch (response.code())
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/*     */       case 200: 
/* 287 */         if ((!this.source.buffer().exhausted()) || (!this.sink.buffer().exhausted())) {
/* 288 */           throw new IOException("TLS tunnel buffered too many bytes!");
/*     */         }
/* 290 */         return;
/*     */       
/*     */       case 407: 
/* 293 */         tunnelRequest = OkHeaders.processAuthHeader(this.route
/* 294 */           .getAddress().getAuthenticator(), response, this.route.getProxy()); }
/* 295 */        } while (tunnelRequest != null);
/* 296 */     throw new IOException("Failed to authenticate with proxy");
/*     */     
/*     */ 
/*     */ 
/* 300 */     throw new IOException("Unexpected response code for CONNECT: " + response.code());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private static TrustRootIndex lastTrustRootIndex;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Request createTunnelRequest()
/*     */     throws IOException
/*     */   {
/* 318 */     return new Request.Builder().url(this.route.getAddress().url()).header("Host", Util.hostHeader(this.route.getAddress().url())).header("Proxy-Connection", "Keep-Alive").header("User-Agent", Version.userAgent()).build();
/*     */   }
/*     */   
/*     */   boolean isConnected()
/*     */   {
/* 323 */     return this.protocol != null;
/*     */   }
/*     */   
/*     */   public Route getRoute() {
/* 327 */     return this.route;
/*     */   }
/*     */   
/*     */   public void cancel()
/*     */   {
/* 332 */     Util.closeQuietly(this.rawSocket);
/*     */   }
/*     */   
/*     */   public Socket getSocket() {
/* 336 */     return this.socket;
/*     */   }
/*     */   
/*     */   public int allocationLimit() {
/* 340 */     FramedConnection framedConnection = this.framedConnection;
/*     */     
/* 342 */     return framedConnection != null ? framedConnection.maxConcurrentStreams() : 1;
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean isHealthy(boolean doExtensiveChecks)
/*     */   {
/* 348 */     if ((this.socket.isClosed()) || (this.socket.isInputShutdown()) || (this.socket.isOutputShutdown())) {
/* 349 */       return false;
/*     */     }
/*     */     
/* 352 */     if (this.framedConnection != null) {
/* 353 */       return true;
/*     */     }
/*     */     
/* 356 */     if (doExtensiveChecks) {
/*     */       try {
/* 358 */         int readTimeout = this.socket.getSoTimeout();
/*     */         try {
/* 360 */           this.socket.setSoTimeout(1);
/* 361 */           boolean bool; if (this.source.exhausted()) {
/* 362 */             return false;
/*     */           }
/* 364 */           return true;
/*     */         } finally {
/* 366 */           this.socket.setSoTimeout(readTimeout);
/*     */         }
/*     */         
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 375 */         return true;
/*     */       }
/*     */       catch (SocketTimeoutException localSocketTimeoutException) {}catch (IOException e)
/*     */       {
/* 371 */         return false;
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public Handshake getHandshake()
/*     */   {
/* 379 */     return this.handshake;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean isMultiplexed()
/*     */   {
/* 387 */     return this.framedConnection != null;
/*     */   }
/*     */   
/*     */   public Protocol getProtocol() {
/* 391 */     return this.protocol != null ? this.protocol : Protocol.HTTP_1_1;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public String toString()
/*     */   {
/* 402 */     return "Connection{" + this.route.getAddress().url().host() + ":" + this.route.getAddress().url().port() + ", proxy=" + this.route.getProxy() + " hostAddress=" + this.route.getSocketAddress() + " cipherSuite=" + (this.handshake != null ? this.handshake.cipherSuite() : "none") + " protocol=" + this.protocol + '}';
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\io\RealConnection.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */