/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.net.Proxy;
/*     */ import java.net.ProxySelector;
/*     */ import java.util.List;
/*     */ import javax.net.SocketFactory;
/*     */ import javax.net.ssl.HostnameVerifier;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Address
/*     */ {
/*     */   final HttpUrl url;
/*     */   final Dns dns;
/*     */   final SocketFactory socketFactory;
/*     */   final Authenticator authenticator;
/*     */   final List<Protocol> protocols;
/*     */   final List<ConnectionSpec> connectionSpecs;
/*     */   final ProxySelector proxySelector;
/*     */   final Proxy proxy;
/*     */   final SSLSocketFactory sslSocketFactory;
/*     */   final HostnameVerifier hostnameVerifier;
/*     */   final CertificatePinner certificatePinner;
/*     */   
/*     */   public Address(String uriHost, int uriPort, Dns dns, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, CertificatePinner certificatePinner, Authenticator authenticator, Proxy proxy, List<Protocol> protocols, List<ConnectionSpec> connectionSpecs, ProxySelector proxySelector)
/*     */   {
/*  59 */     this.url = new HttpUrl.Builder().scheme(sslSocketFactory != null ? "https" : "http").host(uriHost).port(uriPort).build();
/*     */     
/*  61 */     if (dns == null) throw new IllegalArgumentException("dns == null");
/*  62 */     this.dns = dns;
/*     */     
/*  64 */     if (socketFactory == null) throw new IllegalArgumentException("socketFactory == null");
/*  65 */     this.socketFactory = socketFactory;
/*     */     
/*  67 */     if (authenticator == null) throw new IllegalArgumentException("authenticator == null");
/*  68 */     this.authenticator = authenticator;
/*     */     
/*  70 */     if (protocols == null) throw new IllegalArgumentException("protocols == null");
/*  71 */     this.protocols = Util.immutableList(protocols);
/*     */     
/*  73 */     if (connectionSpecs == null) throw new IllegalArgumentException("connectionSpecs == null");
/*  74 */     this.connectionSpecs = Util.immutableList(connectionSpecs);
/*     */     
/*  76 */     if (proxySelector == null) throw new IllegalArgumentException("proxySelector == null");
/*  77 */     this.proxySelector = proxySelector;
/*     */     
/*  79 */     this.proxy = proxy;
/*  80 */     this.sslSocketFactory = sslSocketFactory;
/*  81 */     this.hostnameVerifier = hostnameVerifier;
/*  82 */     this.certificatePinner = certificatePinner;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public HttpUrl url()
/*     */   {
/*  90 */     return this.url;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   @Deprecated
/*     */   public String getUriHost()
/*     */   {
/* 100 */     return this.url.host();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   @Deprecated
/*     */   public int getUriPort()
/*     */   {
/* 111 */     return this.url.port();
/*     */   }
/*     */   
/*     */   public Dns getDns()
/*     */   {
/* 116 */     return this.dns;
/*     */   }
/*     */   
/*     */   public SocketFactory getSocketFactory()
/*     */   {
/* 121 */     return this.socketFactory;
/*     */   }
/*     */   
/*     */   public Authenticator getAuthenticator()
/*     */   {
/* 126 */     return this.authenticator;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Protocol> getProtocols()
/*     */   {
/* 134 */     return this.protocols;
/*     */   }
/*     */   
/*     */   public List<ConnectionSpec> getConnectionSpecs() {
/* 138 */     return this.connectionSpecs;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public ProxySelector getProxySelector()
/*     */   {
/* 146 */     return this.proxySelector;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Proxy getProxy()
/*     */   {
/* 154 */     return this.proxy;
/*     */   }
/*     */   
/*     */   public SSLSocketFactory getSslSocketFactory()
/*     */   {
/* 159 */     return this.sslSocketFactory;
/*     */   }
/*     */   
/*     */   public HostnameVerifier getHostnameVerifier()
/*     */   {
/* 164 */     return this.hostnameVerifier;
/*     */   }
/*     */   
/*     */   public CertificatePinner getCertificatePinner()
/*     */   {
/* 169 */     return this.certificatePinner;
/*     */   }
/*     */   
/*     */   public boolean equals(Object other) {
/* 173 */     if ((other instanceof Address)) {
/* 174 */       Address that = (Address)other;
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 184 */       return (this.url.equals(that.url)) && (this.dns.equals(that.dns)) && (this.authenticator.equals(that.authenticator)) && (this.protocols.equals(that.protocols)) && (this.connectionSpecs.equals(that.connectionSpecs)) && (this.proxySelector.equals(that.proxySelector)) && (Util.equal(this.proxy, that.proxy)) && (Util.equal(this.sslSocketFactory, that.sslSocketFactory)) && (Util.equal(this.hostnameVerifier, that.hostnameVerifier)) && (Util.equal(this.certificatePinner, that.certificatePinner));
/*     */     }
/* 186 */     return false;
/*     */   }
/*     */   
/*     */   public int hashCode() {
/* 190 */     int result = 17;
/* 191 */     result = 31 * result + this.url.hashCode();
/* 192 */     result = 31 * result + this.dns.hashCode();
/* 193 */     result = 31 * result + this.authenticator.hashCode();
/* 194 */     result = 31 * result + this.protocols.hashCode();
/* 195 */     result = 31 * result + this.connectionSpecs.hashCode();
/* 196 */     result = 31 * result + this.proxySelector.hashCode();
/* 197 */     result = 31 * result + (this.proxy != null ? this.proxy.hashCode() : 0);
/* 198 */     result = 31 * result + (this.sslSocketFactory != null ? this.sslSocketFactory.hashCode() : 0);
/* 199 */     result = 31 * result + (this.hostnameVerifier != null ? this.hostnameVerifier.hashCode() : 0);
/* 200 */     result = 31 * result + (this.certificatePinner != null ? this.certificatePinner.hashCode() : 0);
/* 201 */     return result;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Address.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */