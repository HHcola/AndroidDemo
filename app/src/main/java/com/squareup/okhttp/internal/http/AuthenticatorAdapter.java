/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ import com.squareup.okhttp.Challenge;
/*    */ import com.squareup.okhttp.Credentials;
/*    */ import com.squareup.okhttp.HttpUrl;
/*    */ import com.squareup.okhttp.Request;
/*    */ import com.squareup.okhttp.Request.Builder;
/*    */ import com.squareup.okhttp.Response;
/*    */ import java.io.IOException;
/*    */ import java.net.Authenticator.RequestorType;
/*    */ import java.net.InetAddress;
/*    */ import java.net.InetSocketAddress;
/*    */ import java.net.PasswordAuthentication;
/*    */ import java.net.Proxy;
/*    */ import java.net.Proxy.Type;
/*    */ import java.util.List;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class AuthenticatorAdapter
/*    */   implements com.squareup.okhttp.Authenticator
/*    */ {
/* 35 */   public static final com.squareup.okhttp.Authenticator INSTANCE = new AuthenticatorAdapter();
/*    */   
/*    */   public Request authenticate(Proxy proxy, Response response) throws IOException {
/* 38 */     List<Challenge> challenges = response.challenges();
/* 39 */     Request request = response.request();
/* 40 */     HttpUrl url = request.httpUrl();
/* 41 */     int i = 0; for (int size = challenges.size(); i < size; i++) {
/* 42 */       Challenge challenge = (Challenge)challenges.get(i);
/* 43 */       if ("Basic".equalsIgnoreCase(challenge.getScheme()))
/*    */       {
/* 45 */         PasswordAuthentication auth = java.net.Authenticator.requestPasswordAuthentication(url
/* 46 */           .host(), getConnectToInetAddress(proxy, url), url.port(), url.scheme(), challenge
/* 47 */           .getRealm(), challenge.getScheme(), url.url(), Authenticator.RequestorType.SERVER);
/* 48 */         if (auth != null)
/*    */         {
/* 50 */           String credential = Credentials.basic(auth.getUserName(), new String(auth.getPassword()));
/*    */           
/*    */ 
/* 53 */           return request.newBuilder().header("Authorization", credential).build();
/*    */         } } }
/* 55 */     return null;
/*    */   }
/*    */   
/*    */   public Request authenticateProxy(Proxy proxy, Response response) throws IOException
/*    */   {
/* 60 */     List<Challenge> challenges = response.challenges();
/* 61 */     Request request = response.request();
/* 62 */     HttpUrl url = request.httpUrl();
/* 63 */     int i = 0; for (int size = challenges.size(); i < size; i++) {
/* 64 */       Challenge challenge = (Challenge)challenges.get(i);
/* 65 */       if ("Basic".equalsIgnoreCase(challenge.getScheme()))
/*    */       {
/* 67 */         InetSocketAddress proxyAddress = (InetSocketAddress)proxy.address();
/* 68 */         PasswordAuthentication auth = java.net.Authenticator.requestPasswordAuthentication(proxyAddress
/* 69 */           .getHostName(), getConnectToInetAddress(proxy, url), proxyAddress.getPort(), url
/* 70 */           .scheme(), challenge.getRealm(), challenge.getScheme(), url.url(), Authenticator.RequestorType.PROXY);
/*    */         
/* 72 */         if (auth != null)
/*    */         {
/* 74 */           String credential = Credentials.basic(auth.getUserName(), new String(auth.getPassword()));
/*    */           
/*    */ 
/* 77 */           return request.newBuilder().header("Proxy-Authorization", credential).build();
/*    */         } } }
/* 79 */     return null;
/*    */   }
/*    */   
/*    */   private InetAddress getConnectToInetAddress(Proxy proxy, HttpUrl url)
/*    */     throws IOException
/*    */   {
/* 85 */     return (proxy != null) && (proxy.type() != Proxy.Type.DIRECT) ? ((InetSocketAddress)proxy.address()).getAddress() : InetAddress.getByName(url.host());
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\AuthenticatorAdapter.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */