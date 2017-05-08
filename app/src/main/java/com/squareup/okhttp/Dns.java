/*    */ package com.squareup.okhttp;
/*    */ 
/*    */ import java.net.InetAddress;
/*    */ import java.net.UnknownHostException;
/*    */ import java.util.Arrays;
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
/*    */ public abstract interface Dns
/*    */ {
/* 36 */   public static final Dns SYSTEM = new Dns() {
/*    */     public List<InetAddress> lookup(String hostname) throws UnknownHostException {
/* 38 */       if (hostname == null) throw new UnknownHostException("hostname == null");
/* 39 */       return Arrays.asList(InetAddress.getAllByName(hostname));
/*    */     }
/*    */   };
/*    */   
/*    */   public abstract List<InetAddress> lookup(String paramString)
/*    */     throws UnknownHostException;
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Dns.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */