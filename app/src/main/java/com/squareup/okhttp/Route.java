/*    */ package com.squareup.okhttp;
/*    */ 
/*    */ import java.net.InetSocketAddress;
/*    */ import java.net.Proxy;
/*    */ import java.net.Proxy.Type;
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
/*    */ public final class Route
/*    */ {
/*    */   final Address address;
/*    */   final Proxy proxy;
/*    */   final InetSocketAddress inetSocketAddress;
/*    */   
/*    */   public Route(Address address, Proxy proxy, InetSocketAddress inetSocketAddress)
/*    */   {
/* 40 */     if (address == null) {
/* 41 */       throw new NullPointerException("address == null");
/*    */     }
/* 43 */     if (proxy == null) {
/* 44 */       throw new NullPointerException("proxy == null");
/*    */     }
/* 46 */     if (inetSocketAddress == null) {
/* 47 */       throw new NullPointerException("inetSocketAddress == null");
/*    */     }
/* 49 */     this.address = address;
/* 50 */     this.proxy = proxy;
/* 51 */     this.inetSocketAddress = inetSocketAddress;
/*    */   }
/*    */   
/*    */   public Address getAddress() {
/* 55 */     return this.address;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public Proxy getProxy()
/*    */   {
/* 66 */     return this.proxy;
/*    */   }
/*    */   
/*    */   public InetSocketAddress getSocketAddress() {
/* 70 */     return this.inetSocketAddress;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public boolean requiresTunnel()
/*    */   {
/* 78 */     return (this.address.sslSocketFactory != null) && (this.proxy.type() == Proxy.Type.HTTP);
/*    */   }
/*    */   
/*    */   public boolean equals(Object obj) {
/* 82 */     if ((obj instanceof Route)) {
/* 83 */       Route other = (Route)obj;
/*    */       
/*    */ 
/* 86 */       return (this.address.equals(other.address)) && (this.proxy.equals(other.proxy)) && (this.inetSocketAddress.equals(other.inetSocketAddress));
/*    */     }
/* 88 */     return false;
/*    */   }
/*    */   
/*    */   public int hashCode() {
/* 92 */     int result = 17;
/* 93 */     result = 31 * result + this.address.hashCode();
/* 94 */     result = 31 * result + this.proxy.hashCode();
/* 95 */     result = 31 * result + this.inetSocketAddress.hashCode();
/* 96 */     return result;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Route.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */