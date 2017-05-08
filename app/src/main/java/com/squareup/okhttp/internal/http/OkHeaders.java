/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.Authenticator;
/*     */ import com.squareup.okhttp.Challenge;
/*     */ import com.squareup.okhttp.Headers;
/*     */ import com.squareup.okhttp.Headers.Builder;
/*     */ import com.squareup.okhttp.Request;
/*     */ import com.squareup.okhttp.Request.Builder;
/*     */ import com.squareup.okhttp.Response;
/*     */ import com.squareup.okhttp.internal.Platform;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.io.IOException;
/*     */ import java.net.Proxy;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Comparator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Set;
/*     */ import java.util.TreeSet;
/*     */ 
/*     */ public final class OkHeaders
/*     */ {
/*  25 */   private static final Comparator<String> FIELD_NAME_COMPARATOR = new Comparator()
/*     */   {
/*     */     public int compare(String a, String b) {
/*  28 */       if (a == b)
/*  29 */         return 0;
/*  30 */       if (a == null)
/*  31 */         return -1;
/*  32 */       if (b == null) {
/*  33 */         return 1;
/*     */       }
/*  35 */       return String.CASE_INSENSITIVE_ORDER.compare(a, b);
/*     */     }
/*     */   };
/*     */   
/*     */ 
/*  40 */   static final String PREFIX = Platform.get().getPrefix();
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*  45 */   public static final String SENT_MILLIS = PREFIX + "-Sent-Millis";
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*  50 */   public static final String RECEIVED_MILLIS = PREFIX + "-Received-Millis";
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  56 */   public static final String SELECTED_PROTOCOL = PREFIX + "-Selected-Protocol";
/*     */   
/*     */ 
/*  59 */   public static final String RESPONSE_SOURCE = PREFIX + "-Response-Source";
/*     */   
/*     */ 
/*     */ 
/*     */   public static long contentLength(Request request)
/*     */   {
/*  65 */     return contentLength(request.headers());
/*     */   }
/*     */   
/*     */   public static long contentLength(Response response) {
/*  69 */     return contentLength(response.headers());
/*     */   }
/*     */   
/*     */   public static long contentLength(Headers headers) {
/*  73 */     return stringToLong(headers.get("Content-Length"));
/*     */   }
/*     */   
/*     */   private static long stringToLong(String s) {
/*  77 */     if (s == null) return -1L;
/*     */     try {
/*  79 */       return Long.parseLong(s);
/*     */     } catch (NumberFormatException e) {}
/*  81 */     return -1L;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Map<String, List<String>> toMultimap(Headers headers, String valueForNullKey)
/*     */   {
/*  92 */     Map<String, List<String>> result = new java.util.TreeMap(FIELD_NAME_COMPARATOR);
/*  93 */     int i = 0; for (int size = headers.size(); i < size; i++) {
/*  94 */       String fieldName = headers.name(i);
/*  95 */       String value = headers.value(i);
/*     */       
/*  97 */       List<String> allValues = new ArrayList();
/*  98 */       List<String> otherValues = (List)result.get(fieldName);
/*  99 */       if (otherValues != null) {
/* 100 */         allValues.addAll(otherValues);
/*     */       }
/* 102 */       allValues.add(value);
/* 103 */       result.put(fieldName, Collections.unmodifiableList(allValues));
/*     */     }
/* 105 */     if (valueForNullKey != null) {
/* 106 */       result.put(null, Collections.unmodifiableList(Collections.singletonList(valueForNullKey)));
/*     */     }
/* 108 */     return Collections.unmodifiableMap(result);
/*     */   }
/*     */   
/*     */   public static void addCookies(Request.Builder builder, Map<String, List<String>> cookieHeaders) {
/* 112 */     for (Map.Entry<String, List<String>> entry : cookieHeaders.entrySet()) {
/* 113 */       String key = (String)entry.getKey();
/* 114 */       if ((("Cookie".equalsIgnoreCase(key)) || ("Cookie2".equalsIgnoreCase(key))) && 
/* 115 */         (!((List)entry.getValue()).isEmpty())) {
/* 116 */         builder.addHeader(key, buildCookieHeader((List)entry.getValue()));
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private static String buildCookieHeader(List<String> cookies)
/*     */   {
/* 126 */     if (cookies.size() == 1) return (String)cookies.get(0);
/* 127 */     StringBuilder sb = new StringBuilder();
/* 128 */     int i = 0; for (int size = cookies.size(); i < size; i++) {
/* 129 */       if (i > 0) sb.append("; ");
/* 130 */       sb.append((String)cookies.get(i));
/*     */     }
/* 132 */     return sb.toString();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean varyMatches(Response cachedResponse, Headers cachedRequest, Request newRequest)
/*     */   {
/* 141 */     for (String field : varyFields(cachedResponse)) {
/* 142 */       if (!Util.equal(cachedRequest.values(field), newRequest.headers(field))) return false;
/*     */     }
/* 144 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean hasVaryAll(Response response)
/*     */   {
/* 152 */     return hasVaryAll(response.headers());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean hasVaryAll(Headers responseHeaders)
/*     */   {
/* 160 */     return varyFields(responseHeaders).contains("*");
/*     */   }
/*     */   
/*     */   private static Set<String> varyFields(Response response) {
/* 164 */     return varyFields(response.headers());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Set<String> varyFields(Headers responseHeaders)
/*     */   {
/* 172 */     Set<String> result = Collections.emptySet();
/* 173 */     int i = 0; for (int size = responseHeaders.size(); i < size; i++) {
/* 174 */       if ("Vary".equalsIgnoreCase(responseHeaders.name(i)))
/*     */       {
/* 176 */         String value = responseHeaders.value(i);
/* 177 */         if (result.isEmpty()) {
/* 178 */           result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
/*     */         }
/* 180 */         for (String varyField : value.split(","))
/* 181 */           result.add(varyField.trim());
/*     */       }
/*     */     }
/* 184 */     return result;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Headers varyHeaders(Response response)
/*     */   {
/* 195 */     Headers requestHeaders = response.networkResponse().request().headers();
/* 196 */     Headers responseHeaders = response.headers();
/* 197 */     return varyHeaders(requestHeaders, responseHeaders);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Headers varyHeaders(Headers requestHeaders, Headers responseHeaders)
/*     */   {
/* 205 */     Set<String> varyFields = varyFields(responseHeaders);
/* 206 */     if (varyFields.isEmpty()) { return new Headers.Builder().build();
/*     */     }
/* 208 */     Headers.Builder result = new Headers.Builder();
/* 209 */     int i = 0; for (int size = requestHeaders.size(); i < size; i++) {
/* 210 */       String fieldName = requestHeaders.name(i);
/* 211 */       if (varyFields.contains(fieldName)) {
/* 212 */         result.add(fieldName, requestHeaders.value(i));
/*     */       }
/*     */     }
/* 215 */     return result.build();
/*     */   }
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
/*     */   static boolean isEndToEnd(String fieldName)
/*     */   {
/* 230 */     return (!"Connection".equalsIgnoreCase(fieldName)) && (!"Keep-Alive".equalsIgnoreCase(fieldName)) && (!"Proxy-Authenticate".equalsIgnoreCase(fieldName)) && (!"Proxy-Authorization".equalsIgnoreCase(fieldName)) && (!"TE".equalsIgnoreCase(fieldName)) && (!"Trailers".equalsIgnoreCase(fieldName)) && (!"Transfer-Encoding".equalsIgnoreCase(fieldName)) && (!"Upgrade".equalsIgnoreCase(fieldName));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static List<Challenge> parseChallenges(Headers responseHeaders, String challengeHeader)
/*     */   {
/* 243 */     List<Challenge> result = new ArrayList();
/* 244 */     int i = 0; for (int size = responseHeaders.size(); i < size; i++)
/* 245 */       if (challengeHeader.equalsIgnoreCase(responseHeaders.name(i)))
/*     */       {
/*     */ 
/* 248 */         String value = responseHeaders.value(i);
/* 249 */         int pos = 0;
/* 250 */         while (pos < value.length()) {
/* 251 */           int tokenStart = pos;
/* 252 */           pos = HeaderParser.skipUntil(value, pos, " ");
/*     */           
/* 254 */           String scheme = value.substring(tokenStart, pos).trim();
/* 255 */           pos = HeaderParser.skipWhitespace(value, pos);
/*     */           
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 261 */           if (!value.regionMatches(true, pos, "realm=\"", 0, "realm=\"".length())) {
/*     */             break;
/*     */           }
/*     */           
/* 265 */           pos += "realm=\"".length();
/* 266 */           int realmStart = pos;
/* 267 */           pos = HeaderParser.skipUntil(value, pos, "\"");
/* 268 */           String realm = value.substring(realmStart, pos);
/* 269 */           pos++;
/* 270 */           pos = HeaderParser.skipUntil(value, pos, ",");
/* 271 */           pos++;
/* 272 */           pos = HeaderParser.skipWhitespace(value, pos);
/* 273 */           result.add(new Challenge(scheme, realm));
/*     */         }
/*     */       }
/* 276 */     return result;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Request processAuthHeader(Authenticator authenticator, Response response, Proxy proxy)
/*     */     throws IOException
/*     */   {
/* 288 */     return response.code() == 407 ? authenticator.authenticateProxy(proxy, response) : authenticator.authenticate(proxy, response);
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\OkHeaders.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */