/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import java.lang.reflect.InvocationTargetException;
/*    */ import java.lang.reflect.Method;
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
/*    */ public final class RouteException
/*    */   extends Exception
/*    */ {
/*    */   private static final Method addSuppressedExceptionMethod;
/*    */   private IOException lastException;
/*    */   
/*    */   static
/*    */   {
/*    */     Method m;
/*    */     try
/*    */     {
/* 31 */       m = Throwable.class.getDeclaredMethod("addSuppressed", new Class[] { Throwable.class });
/*    */     } catch (Exception e) {
/* 33 */       m = null;
/*    */     }
/* 35 */     addSuppressedExceptionMethod = m;
/*    */   }
/*    */   
/*    */   public RouteException(IOException cause)
/*    */   {
/* 40 */     super(cause);
/* 41 */     this.lastException = cause;
/*    */   }
/*    */   
/*    */   public IOException getLastConnectException() {
/* 45 */     return this.lastException;
/*    */   }
/*    */   
/*    */   public void addConnectException(IOException e) {
/* 49 */     addSuppressedIfPossible(e, this.lastException);
/* 50 */     this.lastException = e;
/*    */   }
/*    */   
/*    */   private void addSuppressedIfPossible(IOException e, IOException suppressed) {
/* 54 */     if (addSuppressedExceptionMethod != null) {
/*    */       try {
/* 56 */         addSuppressedExceptionMethod.invoke(e, new Object[] { suppressed });
/*    */       }
/*    */       catch (InvocationTargetException|IllegalAccessException localInvocationTargetException) {}
/*    */     }
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\RouteException.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */