/*     */ package com.squareup.okhttp.internal;
/*     */ 
/*     */ import com.squareup.okhttp.ConnectionSpec;
/*     */ import java.io.IOException;
/*     */ import java.io.InterruptedIOException;
/*     */ import java.net.ProtocolException;
/*     */ import java.net.UnknownServiceException;
/*     */ import java.security.cert.CertificateException;
/*     */ import java.util.Arrays;
/*     */ import java.util.List;
/*     */ import javax.net.ssl.SSLHandshakeException;
/*     */ import javax.net.ssl.SSLPeerUnverifiedException;
/*     */ import javax.net.ssl.SSLProtocolException;
/*     */ import javax.net.ssl.SSLSocket;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class ConnectionSpecSelector
/*     */ {
/*     */   private final List<ConnectionSpec> connectionSpecs;
/*     */   private int nextModeIndex;
/*     */   private boolean isFallbackPossible;
/*     */   private boolean isFallback;
/*     */   
/*     */   public ConnectionSpecSelector(List<ConnectionSpec> connectionSpecs)
/*     */   {
/*  45 */     this.nextModeIndex = 0;
/*  46 */     this.connectionSpecs = connectionSpecs;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public ConnectionSpec configureSecureSocket(SSLSocket sslSocket)
/*     */     throws IOException
/*     */   {
/*  56 */     ConnectionSpec tlsConfiguration = null;
/*  57 */     int i = this.nextModeIndex; for (int size = this.connectionSpecs.size(); i < size; i++) {
/*  58 */       ConnectionSpec connectionSpec = (ConnectionSpec)this.connectionSpecs.get(i);
/*  59 */       if (connectionSpec.isCompatible(sslSocket)) {
/*  60 */         tlsConfiguration = connectionSpec;
/*  61 */         this.nextModeIndex = (i + 1);
/*  62 */         break;
/*     */       }
/*     */     }
/*     */     
/*  66 */     if (tlsConfiguration == null)
/*     */     {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  73 */       throw new UnknownServiceException("Unable to find acceptable protocols. isFallback=" + this.isFallback + ", modes=" + this.connectionSpecs + ", supported protocols=" + Arrays.toString(sslSocket.getEnabledProtocols()));
/*     */     }
/*     */     
/*  76 */     this.isFallbackPossible = isFallbackPossible(sslSocket);
/*     */     
/*  78 */     Internal.instance.apply(tlsConfiguration, sslSocket, this.isFallback);
/*     */     
/*  80 */     return tlsConfiguration;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean connectionFailed(IOException e)
/*     */   {
/*  92 */     this.isFallback = true;
/*     */     
/*  94 */     if (!this.isFallbackPossible) {
/*  95 */       return false;
/*     */     }
/*     */     
/*     */ 
/*  99 */     if ((e instanceof ProtocolException)) {
/* 100 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 106 */     if ((e instanceof InterruptedIOException)) {
/* 107 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 112 */     if ((e instanceof SSLHandshakeException))
/*     */     {
/*     */ 
/* 115 */       if ((e.getCause() instanceof CertificateException)) {
/* 116 */         return false;
/*     */       }
/*     */     }
/* 119 */     if ((e instanceof SSLPeerUnverifiedException))
/*     */     {
/* 121 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 127 */     return ((e instanceof SSLHandshakeException)) || ((e instanceof SSLProtocolException));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private boolean isFallbackPossible(SSLSocket socket)
/*     */   {
/* 136 */     for (int i = this.nextModeIndex; i < this.connectionSpecs.size(); i++) {
/* 137 */       if (((ConnectionSpec)this.connectionSpecs.get(i)).isCompatible(socket)) {
/* 138 */         return true;
/*     */       }
/*     */     }
/* 141 */     return false;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\ConnectionSpecSelector.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */