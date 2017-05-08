/*    */ package com.squareup.okhttp;
/*    */ 
/*    */ import java.io.UnsupportedEncodingException;
/*    */ import okio.ByteString;
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
/*    */ public final class Credentials
/*    */ {
/*    */   public static String basic(String userName, String password)
/*    */   {
/*    */     try
/*    */     {
/* 29 */       String usernameAndPassword = userName + ":" + password;
/* 30 */       byte[] bytes = usernameAndPassword.getBytes("ISO-8859-1");
/* 31 */       String encoded = ByteString.of(bytes).base64();
/* 32 */       return "Basic " + encoded;
/*    */     } catch (UnsupportedEncodingException e) {
/* 34 */       throw new AssertionError();
/*    */     }
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Credentials.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */