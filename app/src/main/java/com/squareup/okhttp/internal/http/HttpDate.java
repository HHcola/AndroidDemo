/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import java.text.DateFormat;
/*     */ import java.text.ParsePosition;
/*     */ import java.text.SimpleDateFormat;
/*     */ import java.util.Date;
/*     */ import java.util.Locale;
/*     */ import java.util.TimeZone;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class HttpDate
/*     */ {
/*  31 */   private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  37 */   private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT = new ThreadLocal()
/*     */   {
/*     */     protected DateFormat initialValue()
/*     */     {
/*  41 */       DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
/*  42 */       rfc1123.setLenient(false);
/*  43 */       rfc1123.setTimeZone(HttpDate.GMT);
/*  44 */       return rfc1123;
/*     */     }
/*     */   };
/*     */   
/*     */ 
/*  49 */   private static final String[] BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS = { "EEE, dd MMM yyyy HH:mm:ss zzz", "EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MMM-yyyy HH-mm-ss z", "EEE, dd MMM yy HH:mm:ss z", "EEE dd-MMM-yyyy HH:mm:ss z", "EEE dd MMM yyyy HH:mm:ss z", "EEE dd-MMM-yyyy HH-mm-ss z", "EEE dd-MMM-yy HH:mm:ss z", "EEE dd MMM yy HH:mm:ss z", "EEE,dd-MMM-yy HH:mm:ss z", "EEE,dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MM-yyyy HH:mm:ss z", "EEE MMM d yyyy HH:mm:ss z" };
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  71 */   private static final DateFormat[] BROWSER_COMPATIBLE_DATE_FORMATS = new DateFormat[BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length];
/*     */   
/*     */ 
/*     */   public static Date parse(String value)
/*     */   {
/*  76 */     if (value.length() == 0) {
/*  77 */       return null;
/*     */     }
/*     */     
/*  80 */     ParsePosition position = new ParsePosition(0);
/*  81 */     Date result = ((DateFormat)STANDARD_DATE_FORMAT.get()).parse(value, position);
/*  82 */     if (position.getIndex() == value.length())
/*     */     {
/*     */ 
/*  85 */       return result;
/*     */     }
/*  87 */     synchronized (BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS) {
/*  88 */       int i = 0; for (int count = BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length; i < count; i++) {
/*  89 */         DateFormat format = BROWSER_COMPATIBLE_DATE_FORMATS[i];
/*  90 */         if (format == null) {
/*  91 */           format = new SimpleDateFormat(BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS[i], Locale.US);
/*     */           
/*     */ 
/*  94 */           format.setTimeZone(GMT);
/*  95 */           BROWSER_COMPATIBLE_DATE_FORMATS[i] = format;
/*     */         }
/*  97 */         position.setIndex(0);
/*  98 */         result = format.parse(value, position);
/*  99 */         if (position.getIndex() != 0)
/*     */         {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 105 */           return result;
/*     */         }
/*     */       }
/*     */     }
/* 109 */     return null;
/*     */   }
/*     */   
/*     */   public static String format(Date value)
/*     */   {
/* 114 */     return ((DateFormat)STANDARD_DATE_FORMAT.get()).format(value);
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\HttpDate.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */