/*    */ package com.squareup.okhttp.internal;
/*    */ 
/*    */ import com.squareup.okhttp.Route;
/*    */ import java.util.LinkedHashSet;
/*    */ import java.util.Set;
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
/*    */ public final class RouteDatabase
/*    */ {
/* 29 */   private final Set<Route> failedRoutes = new LinkedHashSet();
/*    */   
/*    */   public synchronized void failed(Route failedRoute)
/*    */   {
/* 33 */     this.failedRoutes.add(failedRoute);
/*    */   }
/*    */   
/*    */   public synchronized void connected(Route route)
/*    */   {
/* 38 */     this.failedRoutes.remove(route);
/*    */   }
/*    */   
/*    */   public synchronized boolean shouldPostpone(Route route)
/*    */   {
/* 43 */     return this.failedRoutes.contains(route);
/*    */   }
/*    */   
/*    */   public synchronized int failedRoutesCount() {
/* 47 */     return this.failedRoutes.size();
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\RouteDatabase.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */