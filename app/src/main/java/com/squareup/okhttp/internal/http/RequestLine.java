/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ import com.squareup.okhttp.HttpUrl;
/*    */ import com.squareup.okhttp.Request;
/*    */ import java.net.Proxy.Type;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class RequestLine
/*    */ {
/*    */   static String get(Request request, Proxy.Type proxyType)
/*    */   {
/* 18 */     StringBuilder result = new StringBuilder();
/* 19 */     result.append(request.method());
/* 20 */     result.append(' ');
/*    */     
/* 22 */     if (includeAuthorityInRequestLine(request, proxyType)) {
/* 23 */       result.append(request.httpUrl());
/*    */     } else {
/* 25 */       result.append(requestPath(request.httpUrl()));
/*    */     }
/*    */     
/* 28 */     result.append(" HTTP/1.1");
/* 29 */     return result.toString();
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   private static boolean includeAuthorityInRequestLine(Request request, Proxy.Type proxyType)
/*    */   {
/* 38 */     return (!request.isHttps()) && (proxyType == Proxy.Type.HTTP);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public static String requestPath(HttpUrl url)
/*    */   {
/* 46 */     String path = url.encodedPath();
/* 47 */     String query = url.encodedQuery();
/* 48 */     return query != null ? path + '?' + query : path;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\RequestLine.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */