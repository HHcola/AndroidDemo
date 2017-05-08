/*    */ package com.squareup.okhttp.internal.http;
/*    */ 
/*    */ public final class HeaderParser
/*    */ {
/*    */   public static int skipUntil(String input, int pos, String characters)
/*    */   {
/* 26 */     for (; 
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
/* 26 */         pos < input.length(); pos++) {
/* 27 */       if (characters.indexOf(input.charAt(pos)) != -1) {
/*    */         break;
/*    */       }
/*    */     }
/* 31 */     return pos;
/*    */   }
/*    */   
/*    */   public static int skipWhitespace(String input, int pos)
/*    */   {
/* 39 */     for (; 
/*    */         
/*    */ 
/* 39 */         pos < input.length(); pos++) {
/* 40 */       char c = input.charAt(pos);
/* 41 */       if ((c != ' ') && (c != '\t')) {
/*    */         break;
/*    */       }
/*    */     }
/* 45 */     return pos;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */   public static int parseSeconds(String value, int defaultValue)
/*    */   {
/*    */     try
/*    */     {
/* 54 */       long seconds = Long.parseLong(value);
/* 55 */       if (seconds > 2147483647L)
/* 56 */         return Integer.MAX_VALUE;
/* 57 */       if (seconds < 0L) {
/* 58 */         return 0;
/*    */       }
/* 60 */       return (int)seconds;
/*    */     }
/*    */     catch (NumberFormatException e) {}
/* 63 */     return defaultValue;
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\HeaderParser.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */