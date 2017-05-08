/*    */ package com.squareup.okhttp.internal.http;
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
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class HttpMethod
/*    */ {
/*    */   public static boolean invalidatesCache(String method)
/*    */   {
/* 24 */     return (method.equals("POST")) || (method.equals("PATCH")) || (method.equals("PUT")) || (method.equals("DELETE")) || (method.equals("MOVE"));
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public static boolean requiresRequestBody(String method)
/*    */   {
/* 32 */     return (method.equals("POST")) || (method.equals("PUT")) || (method.equals("PATCH")) || (method.equals("PROPPATCH")) || (method.equals("REPORT"));
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static boolean permitsRequestBody(String method)
/*    */   {
/* 41 */     return (requiresRequestBody(method)) || (method.equals("OPTIONS")) || (method.equals("DELETE")) || (method.equals("PROPFIND")) || (method.equals("MKCOL")) || (method.equals("LOCK"));
/*    */   }
/*    */   
/*    */   public static boolean redirectsToGet(String method)
/*    */   {
/* 46 */     return !method.equals("PROPFIND");
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\HttpMethod.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */