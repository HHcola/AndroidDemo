/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ import java.io.IOException;
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
/*    */ public final class RequestException
/*    */   extends Exception
/*    */ {
/*    */   public RequestException(IOException cause)
/*    */   {
/* 27 */     super(cause);
/*    */   }
/*    */   
/*    */   public IOException getCause()
/*    */   {
/* 32 */     return (IOException)super.getCause();
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\RequestException.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */