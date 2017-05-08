/*      */ package com.squareup.okhttp;
/*      */ 
/*      */ import java.net.IDN;
/*      */ import java.net.InetAddress;
/*      */ import java.net.MalformedURLException;
/*      */ import java.net.URI;
/*      */ import java.net.URISyntaxException;
/*      */ import java.net.URL;
/*      */ import java.net.UnknownHostException;
/*      */ import java.util.ArrayList;
/*      */ import java.util.Arrays;
/*      */ import java.util.Collections;
/*      */ import java.util.LinkedHashSet;
/*      */ import java.util.List;
/*      */ import java.util.Locale;
/*      */ import java.util.Set;
/*      */ import okio.Buffer;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ public final class HttpUrl
/*      */ {
/*  256 */   private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
/*      */   
/*      */ 
/*      */   static final String USERNAME_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
/*      */   
/*      */ 
/*      */   static final String PASSWORD_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
/*      */   
/*      */ 
/*      */   static final String PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#";
/*      */   
/*      */ 
/*      */   static final String PATH_SEGMENT_ENCODE_SET_URI = "[]";
/*      */   
/*      */ 
/*      */   static final String QUERY_ENCODE_SET = " \"'<>#";
/*      */   
/*      */ 
/*      */   static final String QUERY_COMPONENT_ENCODE_SET = " \"'<>#&=";
/*      */   
/*      */   static final String QUERY_COMPONENT_ENCODE_SET_URI = "\\^`{|}";
/*      */   
/*      */   static final String FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~";
/*      */   
/*      */   static final String FRAGMENT_ENCODE_SET = "";
/*      */   
/*      */   static final String FRAGMENT_ENCODE_SET_URI = " \"#<>\\^`{|}";
/*      */   
/*      */   private final String scheme;
/*      */   
/*      */   private final String username;
/*      */   
/*      */   private final String password;
/*      */   
/*      */   private final String host;
/*      */   
/*      */   private final int port;
/*      */   
/*      */   private final List<String> pathSegments;
/*      */   
/*      */   private final List<String> queryNamesAndValues;
/*      */   
/*      */   private final String fragment;
/*      */   
/*      */   private final String url;
/*      */   
/*      */ 
/*      */   private HttpUrl(Builder builder)
/*      */   {
/*  305 */     this.scheme = builder.scheme;
/*  306 */     this.username = percentDecode(builder.encodedUsername, false);
/*  307 */     this.password = percentDecode(builder.encodedPassword, false);
/*  308 */     this.host = builder.host;
/*  309 */     this.port = builder.effectivePort();
/*  310 */     this.pathSegments = percentDecode(builder.encodedPathSegments, false);
/*      */     
/*  312 */     this.queryNamesAndValues = (builder.encodedQueryNamesAndValues != null ? percentDecode(builder.encodedQueryNamesAndValues, true) : null);
/*      */     
/*      */ 
/*  315 */     this.fragment = (builder.encodedFragment != null ? percentDecode(builder.encodedFragment, false) : null);
/*      */     
/*  317 */     this.url = builder.toString();
/*      */   }
/*      */   
/*      */   public URL url()
/*      */   {
/*      */     try {
/*  323 */       return new URL(this.url);
/*      */     } catch (MalformedURLException e) {
/*  325 */       throw new RuntimeException(e);
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public URI uri()
/*      */   {
/*      */     try
/*      */     {
/*  339 */       String uri = newBuilder().reencodeForUri().toString();
/*  340 */       return new URI(uri);
/*      */     } catch (URISyntaxException e) {
/*  342 */       throw new IllegalStateException("not valid as a java.net.URI: " + this.url);
/*      */     }
/*      */   }
/*      */   
/*      */   public String scheme()
/*      */   {
/*  348 */     return this.scheme;
/*      */   }
/*      */   
/*      */   public boolean isHttps() {
/*  352 */     return this.scheme.equals("https");
/*      */   }
/*      */   
/*      */   public String encodedUsername()
/*      */   {
/*  357 */     if (this.username.isEmpty()) return "";
/*  358 */     int usernameStart = this.scheme.length() + 3;
/*  359 */     int usernameEnd = delimiterOffset(this.url, usernameStart, this.url.length(), ":@");
/*  360 */     return this.url.substring(usernameStart, usernameEnd);
/*      */   }
/*      */   
/*      */   public String username() {
/*  364 */     return this.username;
/*      */   }
/*      */   
/*      */   public String encodedPassword()
/*      */   {
/*  369 */     if (this.password.isEmpty()) return "";
/*  370 */     int passwordStart = this.url.indexOf(':', this.scheme.length() + 3) + 1;
/*  371 */     int passwordEnd = this.url.indexOf('@');
/*  372 */     return this.url.substring(passwordStart, passwordEnd);
/*      */   }
/*      */   
/*      */   public String password()
/*      */   {
/*  377 */     return this.password;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public String host()
/*      */   {
/*  391 */     return this.host;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public int port()
/*      */   {
/*  400 */     return this.port;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public static int defaultPort(String scheme)
/*      */   {
/*  408 */     if (scheme.equals("http"))
/*  409 */       return 80;
/*  410 */     if (scheme.equals("https")) {
/*  411 */       return 443;
/*      */     }
/*  413 */     return -1;
/*      */   }
/*      */   
/*      */   public int pathSize()
/*      */   {
/*  418 */     return this.pathSegments.size();
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public String encodedPath()
/*      */   {
/*  426 */     int pathStart = this.url.indexOf('/', this.scheme.length() + 3);
/*  427 */     int pathEnd = delimiterOffset(this.url, pathStart, this.url.length(), "?#");
/*  428 */     return this.url.substring(pathStart, pathEnd);
/*      */   }
/*      */   
/*      */   static void pathSegmentsToString(StringBuilder out, List<String> pathSegments) {
/*  432 */     int i = 0; for (int size = pathSegments.size(); i < size; i++) {
/*  433 */       out.append('/');
/*  434 */       out.append((String)pathSegments.get(i));
/*      */     }
/*      */   }
/*      */   
/*      */   public List<String> encodedPathSegments() {
/*  439 */     int pathStart = this.url.indexOf('/', this.scheme.length() + 3);
/*  440 */     int pathEnd = delimiterOffset(this.url, pathStart, this.url.length(), "?#");
/*  441 */     List<String> result = new ArrayList();
/*  442 */     for (int i = pathStart; i < pathEnd;) {
/*  443 */       i++;
/*  444 */       int segmentEnd = delimiterOffset(this.url, i, pathEnd, "/");
/*  445 */       result.add(this.url.substring(i, segmentEnd));
/*  446 */       i = segmentEnd;
/*      */     }
/*  448 */     return result;
/*      */   }
/*      */   
/*      */   public List<String> pathSegments() {
/*  452 */     return this.pathSegments;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public String encodedQuery()
/*      */   {
/*  461 */     if (this.queryNamesAndValues == null) return null;
/*  462 */     int queryStart = this.url.indexOf('?') + 1;
/*  463 */     int queryEnd = delimiterOffset(this.url, queryStart + 1, this.url.length(), "#");
/*  464 */     return this.url.substring(queryStart, queryEnd);
/*      */   }
/*      */   
/*      */   static void namesAndValuesToQueryString(StringBuilder out, List<String> namesAndValues) {
/*  468 */     int i = 0; for (int size = namesAndValues.size(); i < size; i += 2) {
/*  469 */       String name = (String)namesAndValues.get(i);
/*  470 */       String value = (String)namesAndValues.get(i + 1);
/*  471 */       if (i > 0) out.append('&');
/*  472 */       out.append(name);
/*  473 */       if (value != null) {
/*  474 */         out.append('=');
/*  475 */         out.append(value);
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   static List<String> queryStringToNamesAndValues(String encodedQuery)
/*      */   {
/*  487 */     List<String> result = new ArrayList();
/*  488 */     for (int pos = 0; pos <= encodedQuery.length();) {
/*  489 */       int ampersandOffset = encodedQuery.indexOf('&', pos);
/*  490 */       if (ampersandOffset == -1) { ampersandOffset = encodedQuery.length();
/*      */       }
/*  492 */       int equalsOffset = encodedQuery.indexOf('=', pos);
/*  493 */       if ((equalsOffset == -1) || (equalsOffset > ampersandOffset)) {
/*  494 */         result.add(encodedQuery.substring(pos, ampersandOffset));
/*  495 */         result.add(null);
/*      */       } else {
/*  497 */         result.add(encodedQuery.substring(pos, equalsOffset));
/*  498 */         result.add(encodedQuery.substring(equalsOffset + 1, ampersandOffset));
/*      */       }
/*  500 */       pos = ampersandOffset + 1;
/*      */     }
/*  502 */     return result;
/*      */   }
/*      */   
/*      */   public String query() {
/*  506 */     if (this.queryNamesAndValues == null) return null;
/*  507 */     StringBuilder result = new StringBuilder();
/*  508 */     namesAndValuesToQueryString(result, this.queryNamesAndValues);
/*  509 */     return result.toString();
/*      */   }
/*      */   
/*      */   public int querySize() {
/*  513 */     return this.queryNamesAndValues != null ? this.queryNamesAndValues.size() / 2 : 0;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public String queryParameter(String name)
/*      */   {
/*  521 */     if (this.queryNamesAndValues == null) return null;
/*  522 */     int i = 0; for (int size = this.queryNamesAndValues.size(); i < size; i += 2) {
/*  523 */       if (name.equals(this.queryNamesAndValues.get(i))) {
/*  524 */         return (String)this.queryNamesAndValues.get(i + 1);
/*      */       }
/*      */     }
/*  527 */     return null;
/*      */   }
/*      */   
/*      */   public Set<String> queryParameterNames() {
/*  531 */     if (this.queryNamesAndValues == null) return Collections.emptySet();
/*  532 */     Set<String> result = new LinkedHashSet();
/*  533 */     int i = 0; for (int size = this.queryNamesAndValues.size(); i < size; i += 2) {
/*  534 */       result.add(this.queryNamesAndValues.get(i));
/*      */     }
/*  536 */     return Collections.unmodifiableSet(result);
/*      */   }
/*      */   
/*      */   public List<String> queryParameterValues(String name) {
/*  540 */     if (this.queryNamesAndValues == null) return Collections.emptyList();
/*  541 */     List<String> result = new ArrayList();
/*  542 */     int i = 0; for (int size = this.queryNamesAndValues.size(); i < size; i += 2) {
/*  543 */       if (name.equals(this.queryNamesAndValues.get(i))) {
/*  544 */         result.add(this.queryNamesAndValues.get(i + 1));
/*      */       }
/*      */     }
/*  547 */     return Collections.unmodifiableList(result);
/*      */   }
/*      */   
/*      */   public String queryParameterName(int index) {
/*  551 */     return (String)this.queryNamesAndValues.get(index * 2);
/*      */   }
/*      */   
/*      */   public String queryParameterValue(int index) {
/*  555 */     return (String)this.queryNamesAndValues.get(index * 2 + 1);
/*      */   }
/*      */   
/*      */   public String encodedFragment() {
/*  559 */     if (this.fragment == null) return null;
/*  560 */     int fragmentStart = this.url.indexOf('#') + 1;
/*  561 */     return this.url.substring(fragmentStart);
/*      */   }
/*      */   
/*      */   public String fragment() {
/*  565 */     return this.fragment;
/*      */   }
/*      */   
/*      */   public HttpUrl resolve(String link)
/*      */   {
/*  570 */     Builder builder = new Builder();
/*  571 */     HttpUrl.Builder.ParseResult result = builder.parse(this, link);
/*  572 */     return result == HttpUrl.Builder.ParseResult.SUCCESS ? builder.build() : null;
/*      */   }
/*      */   
/*      */   public Builder newBuilder() {
/*  576 */     Builder result = new Builder();
/*  577 */     result.scheme = this.scheme;
/*  578 */     result.encodedUsername = encodedUsername();
/*  579 */     result.encodedPassword = encodedPassword();
/*  580 */     result.host = this.host;
/*      */     
/*  582 */     result.port = (this.port != defaultPort(this.scheme) ? this.port : -1);
/*  583 */     result.encodedPathSegments.clear();
/*  584 */     result.encodedPathSegments.addAll(encodedPathSegments());
/*  585 */     result.encodedQuery(encodedQuery());
/*  586 */     result.encodedFragment = encodedFragment();
/*  587 */     return result;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public static HttpUrl parse(String url)
/*      */   {
/*  595 */     Builder builder = new Builder();
/*  596 */     HttpUrl.Builder.ParseResult result = builder.parse(null, url);
/*  597 */     return result == HttpUrl.Builder.ParseResult.SUCCESS ? builder.build() : null;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public static HttpUrl get(URL url)
/*      */   {
/*  605 */     return parse(url.toString());
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   static HttpUrl getChecked(String url)
/*      */     throws MalformedURLException, UnknownHostException
/*      */   {
/*  616 */     Builder builder = new Builder();
/*  617 */     HttpUrl.Builder.ParseResult result = builder.parse(null, url);
/*  618 */     switch (result) {
/*      */     case SUCCESS: 
/*  620 */       return builder.build();
/*      */     case INVALID_HOST: 
/*  622 */       throw new UnknownHostException("Invalid host: " + url);
/*      */     }
/*      */     
/*      */     
/*      */ 
/*  627 */     throw new MalformedURLException("Invalid URL: " + result + " for " + url);
/*      */   }
/*      */   
/*      */   public static HttpUrl get(URI uri)
/*      */   {
/*  632 */     return parse(uri.toString());
/*      */   }
/*      */   
/*      */   public boolean equals(Object o) {
/*  636 */     return ((o instanceof HttpUrl)) && (((HttpUrl)o).url.equals(this.url));
/*      */   }
/*      */   
/*      */   public int hashCode() {
/*  640 */     return this.url.hashCode();
/*      */   }
/*      */   
/*      */   public String toString() {
/*  644 */     return this.url;
/*      */   }
/*      */   
/*      */   public static final class Builder {
/*      */     String scheme;
/*  649 */     String encodedUsername = "";
/*  650 */     String encodedPassword = "";
/*      */     String host;
/*  652 */     int port = -1;
/*  653 */     final List<String> encodedPathSegments = new ArrayList();
/*      */     List<String> encodedQueryNamesAndValues;
/*      */     String encodedFragment;
/*      */     
/*      */     public Builder() {
/*  658 */       this.encodedPathSegments.add("");
/*      */     }
/*      */     
/*      */     public Builder scheme(String scheme) {
/*  662 */       if (scheme == null)
/*  663 */         throw new IllegalArgumentException("scheme == null");
/*  664 */       if (scheme.equalsIgnoreCase("http")) {
/*  665 */         this.scheme = "http";
/*  666 */       } else if (scheme.equalsIgnoreCase("https")) {
/*  667 */         this.scheme = "https";
/*      */       } else {
/*  669 */         throw new IllegalArgumentException("unexpected scheme: " + scheme);
/*      */       }
/*  671 */       return this;
/*      */     }
/*      */     
/*      */     public Builder username(String username) {
/*  675 */       if (username == null) throw new IllegalArgumentException("username == null");
/*  676 */       this.encodedUsername = HttpUrl.canonicalize(username, " \"':;<=>@[]^`{}|/\\?#", false, false, true);
/*  677 */       return this;
/*      */     }
/*      */     
/*      */     public Builder encodedUsername(String encodedUsername) {
/*  681 */       if (encodedUsername == null) throw new IllegalArgumentException("encodedUsername == null");
/*  682 */       this.encodedUsername = HttpUrl.canonicalize(encodedUsername, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
/*  683 */       return this;
/*      */     }
/*      */     
/*      */     public Builder password(String password) {
/*  687 */       if (password == null) throw new IllegalArgumentException("password == null");
/*  688 */       this.encodedPassword = HttpUrl.canonicalize(password, " \"':;<=>@[]^`{}|/\\?#", false, false, true);
/*  689 */       return this;
/*      */     }
/*      */     
/*      */     public Builder encodedPassword(String encodedPassword) {
/*  693 */       if (encodedPassword == null) throw new IllegalArgumentException("encodedPassword == null");
/*  694 */       this.encodedPassword = HttpUrl.canonicalize(encodedPassword, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
/*  695 */       return this;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     public Builder host(String host)
/*      */     {
/*  703 */       if (host == null) throw new IllegalArgumentException("host == null");
/*  704 */       String encoded = canonicalizeHost(host, 0, host.length());
/*  705 */       if (encoded == null) throw new IllegalArgumentException("unexpected host: " + host);
/*  706 */       this.host = encoded;
/*  707 */       return this;
/*      */     }
/*      */     
/*      */     public Builder port(int port) {
/*  711 */       if ((port <= 0) || (port > 65535)) throw new IllegalArgumentException("unexpected port: " + port);
/*  712 */       this.port = port;
/*  713 */       return this;
/*      */     }
/*      */     
/*      */     int effectivePort() {
/*  717 */       return this.port != -1 ? this.port : HttpUrl.defaultPort(this.scheme);
/*      */     }
/*      */     
/*      */     public Builder addPathSegment(String pathSegment) {
/*  721 */       if (pathSegment == null) throw new IllegalArgumentException("pathSegment == null");
/*  722 */       push(pathSegment, 0, pathSegment.length(), false, false);
/*  723 */       return this;
/*      */     }
/*      */     
/*      */     public Builder addEncodedPathSegment(String encodedPathSegment) {
/*  727 */       if (encodedPathSegment == null) {
/*  728 */         throw new IllegalArgumentException("encodedPathSegment == null");
/*      */       }
/*  730 */       push(encodedPathSegment, 0, encodedPathSegment.length(), false, true);
/*  731 */       return this;
/*      */     }
/*      */     
/*      */     public Builder setPathSegment(int index, String pathSegment) {
/*  735 */       if (pathSegment == null) throw new IllegalArgumentException("pathSegment == null");
/*  736 */       String canonicalPathSegment = HttpUrl.canonicalize(pathSegment, 0, pathSegment
/*  737 */         .length(), " \"<>^`{}|/\\?#", false, false, true);
/*  738 */       if ((isDot(canonicalPathSegment)) || (isDotDot(canonicalPathSegment))) {
/*  739 */         throw new IllegalArgumentException("unexpected path segment: " + pathSegment);
/*      */       }
/*  741 */       this.encodedPathSegments.set(index, canonicalPathSegment);
/*  742 */       return this;
/*      */     }
/*      */     
/*      */     public Builder setEncodedPathSegment(int index, String encodedPathSegment) {
/*  746 */       if (encodedPathSegment == null) {
/*  747 */         throw new IllegalArgumentException("encodedPathSegment == null");
/*      */       }
/*  749 */       String canonicalPathSegment = HttpUrl.canonicalize(encodedPathSegment, 0, encodedPathSegment
/*  750 */         .length(), " \"<>^`{}|/\\?#", true, false, true);
/*  751 */       this.encodedPathSegments.set(index, canonicalPathSegment);
/*  752 */       if ((isDot(canonicalPathSegment)) || (isDotDot(canonicalPathSegment))) {
/*  753 */         throw new IllegalArgumentException("unexpected path segment: " + encodedPathSegment);
/*      */       }
/*  755 */       return this;
/*      */     }
/*      */     
/*      */     public Builder removePathSegment(int index) {
/*  759 */       this.encodedPathSegments.remove(index);
/*  760 */       if (this.encodedPathSegments.isEmpty()) {
/*  761 */         this.encodedPathSegments.add("");
/*      */       }
/*  763 */       return this;
/*      */     }
/*      */     
/*      */     public Builder encodedPath(String encodedPath) {
/*  767 */       if (encodedPath == null) throw new IllegalArgumentException("encodedPath == null");
/*  768 */       if (!encodedPath.startsWith("/")) {
/*  769 */         throw new IllegalArgumentException("unexpected encodedPath: " + encodedPath);
/*      */       }
/*  771 */       resolvePath(encodedPath, 0, encodedPath.length());
/*  772 */       return this;
/*      */     }
/*      */     
/*      */     public Builder query(String query)
/*      */     {
/*  777 */       this.encodedQueryNamesAndValues = (query != null ? HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(query, " \"'<>#", false, true, true)) : null);
/*      */       
/*  779 */       return this;
/*      */     }
/*      */     
/*      */     public Builder encodedQuery(String encodedQuery)
/*      */     {
/*  784 */       this.encodedQueryNamesAndValues = (encodedQuery != null ? HttpUrl.queryStringToNamesAndValues(
/*  785 */         HttpUrl.canonicalize(encodedQuery, " \"'<>#", true, true, true)) : null);
/*      */       
/*  787 */       return this;
/*      */     }
/*      */     
/*      */     public Builder addQueryParameter(String name, String value)
/*      */     {
/*  792 */       if (name == null) throw new IllegalArgumentException("name == null");
/*  793 */       if (this.encodedQueryNamesAndValues == null) this.encodedQueryNamesAndValues = new ArrayList();
/*  794 */       this.encodedQueryNamesAndValues.add(
/*  795 */         HttpUrl.canonicalize(name, " \"'<>#&=", false, true, true));
/*  796 */       this.encodedQueryNamesAndValues.add(value != null ? 
/*  797 */         HttpUrl.canonicalize(value, " \"'<>#&=", false, true, true) : null);
/*      */       
/*  799 */       return this;
/*      */     }
/*      */     
/*      */     public Builder addEncodedQueryParameter(String encodedName, String encodedValue)
/*      */     {
/*  804 */       if (encodedName == null) throw new IllegalArgumentException("encodedName == null");
/*  805 */       if (this.encodedQueryNamesAndValues == null) this.encodedQueryNamesAndValues = new ArrayList();
/*  806 */       this.encodedQueryNamesAndValues.add(
/*  807 */         HttpUrl.canonicalize(encodedName, " \"'<>#&=", true, true, true));
/*  808 */       this.encodedQueryNamesAndValues.add(encodedValue != null ? 
/*  809 */         HttpUrl.canonicalize(encodedValue, " \"'<>#&=", true, true, true) : null);
/*      */       
/*  811 */       return this;
/*      */     }
/*      */     
/*      */     public Builder setQueryParameter(String name, String value) {
/*  815 */       removeAllQueryParameters(name);
/*  816 */       addQueryParameter(name, value);
/*  817 */       return this;
/*      */     }
/*      */     
/*      */     public Builder setEncodedQueryParameter(String encodedName, String encodedValue) {
/*  821 */       removeAllEncodedQueryParameters(encodedName);
/*  822 */       addEncodedQueryParameter(encodedName, encodedValue);
/*  823 */       return this;
/*      */     }
/*      */     
/*      */     public Builder removeAllQueryParameters(String name) {
/*  827 */       if (name == null) throw new IllegalArgumentException("name == null");
/*  828 */       if (this.encodedQueryNamesAndValues == null) return this;
/*  829 */       String nameToRemove = HttpUrl.canonicalize(name, " \"'<>#&=", false, true, true);
/*  830 */       removeAllCanonicalQueryParameters(nameToRemove);
/*  831 */       return this;
/*      */     }
/*      */     
/*      */     public Builder removeAllEncodedQueryParameters(String encodedName) {
/*  835 */       if (encodedName == null) throw new IllegalArgumentException("encodedName == null");
/*  836 */       if (this.encodedQueryNamesAndValues == null) return this;
/*  837 */       removeAllCanonicalQueryParameters(
/*  838 */         HttpUrl.canonicalize(encodedName, " \"'<>#&=", true, true, true));
/*  839 */       return this;
/*      */     }
/*      */     
/*      */     private void removeAllCanonicalQueryParameters(String canonicalName) {
/*  843 */       for (int i = this.encodedQueryNamesAndValues.size() - 2; i >= 0; i -= 2) {
/*  844 */         if (canonicalName.equals(this.encodedQueryNamesAndValues.get(i))) {
/*  845 */           this.encodedQueryNamesAndValues.remove(i + 1);
/*  846 */           this.encodedQueryNamesAndValues.remove(i);
/*  847 */           if (this.encodedQueryNamesAndValues.isEmpty()) {
/*  848 */             this.encodedQueryNamesAndValues = null;
/*  849 */             return;
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*      */     
/*      */     public Builder fragment(String fragment)
/*      */     {
/*  857 */       this.encodedFragment = (fragment != null ? HttpUrl.canonicalize(fragment, "", false, false, false) : null);
/*      */       
/*  859 */       return this;
/*      */     }
/*      */     
/*      */     public Builder encodedFragment(String encodedFragment)
/*      */     {
/*  864 */       this.encodedFragment = (encodedFragment != null ? HttpUrl.canonicalize(encodedFragment, "", true, false, false) : null);
/*      */       
/*  866 */       return this;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     Builder reencodeForUri()
/*      */     {
/*  874 */       int i = 0; for (int size = this.encodedPathSegments.size(); i < size; i++) {
/*  875 */         String pathSegment = (String)this.encodedPathSegments.get(i);
/*  876 */         this.encodedPathSegments.set(i, 
/*  877 */           HttpUrl.canonicalize(pathSegment, "[]", true, false, true));
/*      */       }
/*  879 */       if (this.encodedQueryNamesAndValues != null) {
/*  880 */         int i = 0; for (int size = this.encodedQueryNamesAndValues.size(); i < size; i++) {
/*  881 */           String component = (String)this.encodedQueryNamesAndValues.get(i);
/*  882 */           if (component != null) {
/*  883 */             this.encodedQueryNamesAndValues.set(i, 
/*  884 */               HttpUrl.canonicalize(component, "\\^`{|}", true, true, true));
/*      */           }
/*      */         }
/*      */       }
/*  888 */       if (this.encodedFragment != null) {
/*  889 */         this.encodedFragment = HttpUrl.canonicalize(this.encodedFragment, " \"#<>\\^`{|}", true, false, false);
/*      */       }
/*      */       
/*  892 */       return this;
/*      */     }
/*      */     
/*      */     public HttpUrl build() {
/*  896 */       if (this.scheme == null) throw new IllegalStateException("scheme == null");
/*  897 */       if (this.host == null) throw new IllegalStateException("host == null");
/*  898 */       return new HttpUrl(this, null);
/*      */     }
/*      */     
/*      */     public String toString() {
/*  902 */       StringBuilder result = new StringBuilder();
/*  903 */       result.append(this.scheme);
/*  904 */       result.append("://");
/*      */       
/*  906 */       if ((!this.encodedUsername.isEmpty()) || (!this.encodedPassword.isEmpty())) {
/*  907 */         result.append(this.encodedUsername);
/*  908 */         if (!this.encodedPassword.isEmpty()) {
/*  909 */           result.append(':');
/*  910 */           result.append(this.encodedPassword);
/*      */         }
/*  912 */         result.append('@');
/*      */       }
/*      */       
/*  915 */       if (this.host.indexOf(':') != -1)
/*      */       {
/*  917 */         result.append('[');
/*  918 */         result.append(this.host);
/*  919 */         result.append(']');
/*      */       } else {
/*  921 */         result.append(this.host);
/*      */       }
/*      */       
/*  924 */       int effectivePort = effectivePort();
/*  925 */       if (effectivePort != HttpUrl.defaultPort(this.scheme)) {
/*  926 */         result.append(':');
/*  927 */         result.append(effectivePort);
/*      */       }
/*      */       
/*  930 */       HttpUrl.pathSegmentsToString(result, this.encodedPathSegments);
/*      */       
/*  932 */       if (this.encodedQueryNamesAndValues != null) {
/*  933 */         result.append('?');
/*  934 */         HttpUrl.namesAndValuesToQueryString(result, this.encodedQueryNamesAndValues);
/*      */       }
/*      */       
/*  937 */       if (this.encodedFragment != null) {
/*  938 */         result.append('#');
/*  939 */         result.append(this.encodedFragment);
/*      */       }
/*      */       
/*  942 */       return result.toString();
/*      */     }
/*      */     
/*      */     static enum ParseResult {
/*  946 */       SUCCESS, 
/*  947 */       MISSING_SCHEME, 
/*  948 */       UNSUPPORTED_SCHEME, 
/*  949 */       INVALID_PORT, 
/*  950 */       INVALID_HOST;
/*      */       
/*      */       private ParseResult() {} }
/*      */     
/*  954 */     ParseResult parse(HttpUrl base, String input) { int pos = skipLeadingAsciiWhitespace(input, 0, input.length());
/*  955 */       int limit = skipTrailingAsciiWhitespace(input, pos, input.length());
/*      */       
/*      */ 
/*  958 */       int schemeDelimiterOffset = schemeDelimiterOffset(input, pos, limit);
/*  959 */       if (schemeDelimiterOffset != -1) {
/*  960 */         if (input.regionMatches(true, pos, "https:", 0, 6)) {
/*  961 */           this.scheme = "https";
/*  962 */           pos += "https:".length();
/*  963 */         } else if (input.regionMatches(true, pos, "http:", 0, 5)) {
/*  964 */           this.scheme = "http";
/*  965 */           pos += "http:".length();
/*      */         } else {
/*  967 */           return ParseResult.UNSUPPORTED_SCHEME;
/*      */         }
/*  969 */       } else if (base != null) {
/*  970 */         this.scheme = base.scheme;
/*      */       } else {
/*  972 */         return ParseResult.MISSING_SCHEME;
/*      */       }
/*      */       
/*      */ 
/*  976 */       boolean hasUsername = false;
/*  977 */       boolean hasPassword = false;
/*  978 */       int slashCount = slashCount(input, pos, limit);
/*  979 */       if ((slashCount >= 2) || (base == null) || (!base.scheme.equals(this.scheme)))
/*      */       {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*  989 */         pos += slashCount;
/*      */         for (;;)
/*      */         {
/*  992 */           int componentDelimiterOffset = HttpUrl.delimiterOffset(input, pos, limit, "@/\\?#");
/*      */           
/*  994 */           int c = componentDelimiterOffset != limit ? input.charAt(componentDelimiterOffset) : -1;
/*      */           
/*  996 */           switch (c)
/*      */           {
/*      */           case 64: 
/*  999 */             if (!hasPassword) {
/* 1000 */               int passwordColonOffset = HttpUrl.delimiterOffset(input, pos, componentDelimiterOffset, ":");
/*      */               
/* 1002 */               String canonicalUsername = HttpUrl.canonicalize(input, pos, passwordColonOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
/*      */               
/* 1004 */               this.encodedUsername = (hasUsername ? this.encodedUsername + "%40" + canonicalUsername : canonicalUsername);
/*      */               
/*      */ 
/* 1007 */               if (passwordColonOffset != componentDelimiterOffset) {
/* 1008 */                 hasPassword = true;
/* 1009 */                 this.encodedPassword = HttpUrl.canonicalize(input, passwordColonOffset + 1, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
/*      */               }
/*      */               
/* 1012 */               hasUsername = true;
/*      */             } else {
/* 1014 */               this.encodedPassword = (this.encodedPassword + "%40" + HttpUrl.canonicalize(input, pos, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, true));
/*      */             }
/*      */             
/* 1017 */             pos = componentDelimiterOffset + 1;
/* 1018 */             break;
/*      */           
/*      */ 
/*      */           case -1: 
/*      */           case 35: 
/*      */           case 47: 
/*      */           case 63: 
/*      */           case 92: 
/* 1026 */             int portColonOffset = portColonOffset(input, pos, componentDelimiterOffset);
/* 1027 */             if (portColonOffset + 1 < componentDelimiterOffset) {
/* 1028 */               this.host = canonicalizeHost(input, pos, portColonOffset);
/* 1029 */               this.port = parsePort(input, portColonOffset + 1, componentDelimiterOffset);
/* 1030 */               if (this.port == -1) return ParseResult.INVALID_PORT;
/*      */             } else {
/* 1032 */               this.host = canonicalizeHost(input, pos, portColonOffset);
/* 1033 */               this.port = HttpUrl.defaultPort(this.scheme);
/*      */             }
/* 1035 */             if (this.host == null) return ParseResult.INVALID_HOST;
/* 1036 */             pos = componentDelimiterOffset;
/*      */             break label588;
/*      */           }
/*      */           
/*      */         }
/*      */       }
/* 1042 */       this.encodedUsername = base.encodedUsername();
/* 1043 */       this.encodedPassword = base.encodedPassword();
/* 1044 */       this.host = base.host;
/* 1045 */       this.port = base.port;
/* 1046 */       this.encodedPathSegments.clear();
/* 1047 */       this.encodedPathSegments.addAll(base.encodedPathSegments());
/* 1048 */       if ((pos == limit) || (input.charAt(pos) == '#')) {
/* 1049 */         encodedQuery(base.encodedQuery());
/*      */       }
/*      */       
/*      */       label588:
/*      */       
/* 1054 */       int pathDelimiterOffset = HttpUrl.delimiterOffset(input, pos, limit, "?#");
/* 1055 */       resolvePath(input, pos, pathDelimiterOffset);
/* 1056 */       pos = pathDelimiterOffset;
/*      */       
/*      */ 
/* 1059 */       if ((pos < limit) && (input.charAt(pos) == '?')) {
/* 1060 */         int queryDelimiterOffset = HttpUrl.delimiterOffset(input, pos, limit, "#");
/* 1061 */         this.encodedQueryNamesAndValues = HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(input, pos + 1, queryDelimiterOffset, " \"'<>#", true, true, true));
/*      */         
/* 1063 */         pos = queryDelimiterOffset;
/*      */       }
/*      */       
/*      */ 
/* 1067 */       if ((pos < limit) && (input.charAt(pos) == '#')) {
/* 1068 */         this.encodedFragment = HttpUrl.canonicalize(input, pos + 1, limit, "", true, false, false);
/*      */       }
/*      */       
/*      */ 
/* 1072 */       return ParseResult.SUCCESS;
/*      */     }
/*      */     
/*      */     private void resolvePath(String input, int pos, int limit)
/*      */     {
/* 1077 */       if (pos == limit)
/*      */       {
/* 1079 */         return;
/*      */       }
/* 1081 */       char c = input.charAt(pos);
/* 1082 */       if ((c == '/') || (c == '\\'))
/*      */       {
/* 1084 */         this.encodedPathSegments.clear();
/* 1085 */         this.encodedPathSegments.add("");
/* 1086 */         pos++;
/*      */       }
/*      */       else {
/* 1089 */         this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, "");
/*      */       }
/*      */       
/*      */ 
/* 1093 */       for (int i = pos; i < limit;) {
/* 1094 */         int pathSegmentDelimiterOffset = HttpUrl.delimiterOffset(input, i, limit, "/\\");
/* 1095 */         boolean segmentHasTrailingSlash = pathSegmentDelimiterOffset < limit;
/* 1096 */         push(input, i, pathSegmentDelimiterOffset, segmentHasTrailingSlash, true);
/* 1097 */         i = pathSegmentDelimiterOffset;
/* 1098 */         if (segmentHasTrailingSlash) { i++;
/*      */         }
/*      */       }
/*      */     }
/*      */     
/*      */     private void push(String input, int pos, int limit, boolean addTrailingSlash, boolean alreadyEncoded)
/*      */     {
/* 1105 */       String segment = HttpUrl.canonicalize(input, pos, limit, " \"<>^`{}|/\\?#", alreadyEncoded, false, true);
/*      */       
/* 1107 */       if (isDot(segment)) {
/* 1108 */         return;
/*      */       }
/* 1110 */       if (isDotDot(segment)) {
/* 1111 */         pop();
/* 1112 */         return;
/*      */       }
/* 1114 */       if (((String)this.encodedPathSegments.get(this.encodedPathSegments.size() - 1)).isEmpty()) {
/* 1115 */         this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, segment);
/*      */       } else {
/* 1117 */         this.encodedPathSegments.add(segment);
/*      */       }
/* 1119 */       if (addTrailingSlash) {
/* 1120 */         this.encodedPathSegments.add("");
/*      */       }
/*      */     }
/*      */     
/*      */     private boolean isDot(String input) {
/* 1125 */       return (input.equals(".")) || (input.equalsIgnoreCase("%2e"));
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */     private boolean isDotDot(String input)
/*      */     {
/* 1132 */       return (input.equals("..")) || (input.equalsIgnoreCase("%2e.")) || (input.equalsIgnoreCase(".%2e")) || (input.equalsIgnoreCase("%2e%2e"));
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     private void pop()
/*      */     {
/* 1146 */       String removed = (String)this.encodedPathSegments.remove(this.encodedPathSegments.size() - 1);
/*      */       
/*      */ 
/* 1149 */       if ((removed.isEmpty()) && (!this.encodedPathSegments.isEmpty())) {
/* 1150 */         this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, "");
/*      */       } else {
/* 1152 */         this.encodedPathSegments.add("");
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     private int skipLeadingAsciiWhitespace(String input, int pos, int limit)
/*      */     {
/* 1161 */       for (int i = pos; i < limit; i++) {
/* 1162 */         switch (input.charAt(i)) {
/*      */         case '\t': 
/*      */         case '\n': 
/*      */         case '\f': 
/*      */         case '\r': 
/*      */         case ' ': 
/*      */           break;
/*      */         default: 
/* 1170 */           return i;
/*      */         }
/*      */       }
/* 1173 */       return limit;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     private int skipTrailingAsciiWhitespace(String input, int pos, int limit)
/*      */     {
/* 1181 */       for (int i = limit - 1; i >= pos; i--) {
/* 1182 */         switch (input.charAt(i)) {
/*      */         case '\t': 
/*      */         case '\n': 
/*      */         case '\f': 
/*      */         case '\r': 
/*      */         case ' ': 
/*      */           break;
/*      */         default: 
/* 1190 */           return i + 1;
/*      */         }
/*      */       }
/* 1193 */       return pos;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     private static int schemeDelimiterOffset(String input, int pos, int limit)
/*      */     {
/* 1201 */       if (limit - pos < 2) { return -1;
/*      */       }
/* 1203 */       char c0 = input.charAt(pos);
/* 1204 */       if (((c0 < 'a') || (c0 > 'z')) && ((c0 < 'A') || (c0 > 'Z'))) { return -1;
/*      */       }
/* 1206 */       for (int i = pos + 1; i < limit; i++) {
/* 1207 */         char c = input.charAt(i);
/*      */         
/* 1209 */         if (((c < 'a') || (c > 'z')) && ((c < 'A') || (c > 'Z')) && ((c < '0') || (c > '9')) && (c != '+') && (c != '-') && (c != '.'))
/*      */         {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/* 1216 */           if (c == ':') {
/* 1217 */             return i;
/*      */           }
/* 1219 */           return -1;
/*      */         }
/*      */       }
/*      */       
/* 1223 */       return -1;
/*      */     }
/*      */     
/*      */     private static int slashCount(String input, int pos, int limit)
/*      */     {
/* 1228 */       int slashCount = 0;
/* 1229 */       while (pos < limit) {
/* 1230 */         char c = input.charAt(pos);
/* 1231 */         if ((c != '\\') && (c != '/')) break;
/* 1232 */         slashCount++;
/* 1233 */         pos++;
/*      */       }
/*      */       
/*      */ 
/*      */ 
/* 1238 */       return slashCount;
/*      */     }
/*      */     
/*      */     private static int portColonOffset(String input, int pos, int limit)
/*      */     {
/* 1243 */       for (int i = pos; i < limit; i++) {
/* 1244 */         switch (input.charAt(i)) {
/*      */         case '[':  do {
/* 1246 */             i++; if (i >= limit) break;
/* 1247 */           } while (input.charAt(i) != ']'); break;
/*      */         
/*      */ 
/*      */         case ':': 
/* 1251 */           return i;
/*      */         }
/*      */       }
/* 1254 */       return limit;
/*      */     }
/*      */     
/*      */ 
/*      */     private static String canonicalizeHost(String input, int pos, int limit)
/*      */     {
/* 1260 */       String percentDecoded = HttpUrl.percentDecode(input, pos, limit, false);
/*      */       
/*      */ 
/* 1263 */       if ((percentDecoded.startsWith("[")) && (percentDecoded.endsWith("]"))) {
/* 1264 */         InetAddress inetAddress = decodeIpv6(percentDecoded, 1, percentDecoded.length() - 1);
/* 1265 */         if (inetAddress == null) return null;
/* 1266 */         byte[] address = inetAddress.getAddress();
/* 1267 */         if (address.length == 16) return inet6AddressToAscii(address);
/* 1268 */         throw new AssertionError();
/*      */       }
/*      */       
/* 1271 */       return domainToAscii(percentDecoded);
/*      */     }
/*      */     
/*      */     private static InetAddress decodeIpv6(String input, int pos, int limit)
/*      */     {
/* 1276 */       byte[] address = new byte[16];
/* 1277 */       int b = 0;
/* 1278 */       int compress = -1;
/* 1279 */       int groupOffset = -1;
/*      */       
/* 1281 */       for (int i = pos; i < limit;) {
/* 1282 */         if (b == address.length) { return null;
/*      */         }
/*      */         
/* 1285 */         if ((i + 2 <= limit) && (input.regionMatches(i, "::", 0, 2)))
/*      */         {
/* 1287 */           if (compress != -1) return null;
/* 1288 */           i += 2;
/* 1289 */           b += 2;
/* 1290 */           compress = b;
/* 1291 */           if (i == limit) break;
/* 1292 */         } else if (b != 0)
/*      */         {
/* 1294 */           if (input.regionMatches(i, ":", 0, 1)) {
/* 1295 */             i++;
/* 1296 */           } else { if (input.regionMatches(i, ".", 0, 1))
/*      */             {
/* 1298 */               if (!decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) return null;
/* 1299 */               b += 2;
/* 1300 */               break;
/*      */             }
/* 1302 */             return null;
/*      */           }
/*      */         }
/*      */         
/*      */ 
/* 1307 */         int value = 0;
/* 1308 */         groupOffset = i;
/* 1309 */         for (; i < limit; i++) {
/* 1310 */           char c = input.charAt(i);
/* 1311 */           int hexDigit = HttpUrl.decodeHexDigit(c);
/* 1312 */           if (hexDigit == -1) break;
/* 1313 */           value = (value << 4) + hexDigit;
/*      */         }
/* 1315 */         int groupLength = i - groupOffset;
/* 1316 */         if ((groupLength == 0) || (groupLength > 4)) { return null;
/*      */         }
/*      */         
/* 1319 */         address[(b++)] = ((byte)(value >>> 8 & 0xFF));
/* 1320 */         address[(b++)] = ((byte)(value & 0xFF));
/*      */       }
/*      */       
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/* 1332 */       if (b != address.length) {
/* 1333 */         if (compress == -1) return null;
/* 1334 */         System.arraycopy(address, compress, address, address.length - (b - compress), b - compress);
/* 1335 */         Arrays.fill(address, compress, compress + (address.length - b), (byte)0);
/*      */       }
/*      */       try
/*      */       {
/* 1339 */         return InetAddress.getByAddress(address);
/*      */       } catch (UnknownHostException e) {
/* 1341 */         throw new AssertionError();
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*      */     private static boolean decodeIpv4Suffix(String input, int pos, int limit, byte[] address, int addressOffset)
/*      */     {
/* 1348 */       int b = addressOffset;
/*      */       
/* 1350 */       for (int i = pos; i < limit;) {
/* 1351 */         if (b == address.length) { return false;
/*      */         }
/*      */         
/* 1354 */         if (b != addressOffset) {
/* 1355 */           if (input.charAt(i) != '.') return false;
/* 1356 */           i++;
/*      */         }
/*      */         
/*      */ 
/* 1360 */         int value = 0;
/* 1361 */         int groupOffset = i;
/* 1362 */         for (; i < limit; i++) {
/* 1363 */           char c = input.charAt(i);
/* 1364 */           if ((c < '0') || (c > '9')) break;
/* 1365 */           if ((value == 0) && (groupOffset != i)) return false;
/* 1366 */           value = value * 10 + c - 48;
/* 1367 */           if (value > 255) return false;
/*      */         }
/* 1369 */         int groupLength = i - groupOffset;
/* 1370 */         if (groupLength == 0) { return false;
/*      */         }
/*      */         
/* 1373 */         address[(b++)] = ((byte)value);
/*      */       }
/*      */       
/* 1376 */       if (b != addressOffset + 4) return false;
/* 1377 */       return true;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     private static String domainToAscii(String input)
/*      */     {
/*      */       try
/*      */       {
/* 1388 */         String result = IDN.toASCII(input).toLowerCase(Locale.US);
/* 1389 */         if (result.isEmpty()) { return null;
/*      */         }
/*      */         
/* 1392 */         if (containsInvalidHostnameAsciiCodes(result)) {
/* 1393 */           return null;
/*      */         }
/*      */         
/* 1396 */         return result;
/*      */       } catch (IllegalArgumentException e) {}
/* 1398 */       return null;
/*      */     }
/*      */     
/*      */     private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii)
/*      */     {
/* 1403 */       for (int i = 0; i < hostnameAscii.length(); i++) {
/* 1404 */         char c = hostnameAscii.charAt(i);
/*      */         
/*      */ 
/*      */ 
/* 1408 */         if ((c <= '\037') || (c >= '')) {
/* 1409 */           return true;
/*      */         }
/*      */         
/*      */ 
/*      */ 
/* 1414 */         if (" #%/:?@[\\]".indexOf(c) != -1) {
/* 1415 */           return true;
/*      */         }
/*      */       }
/* 1418 */       return false;
/*      */     }
/*      */     
/*      */     private static String inet6AddressToAscii(byte[] address)
/*      */     {
/* 1423 */       int longestRunOffset = -1;
/* 1424 */       int longestRunLength = 0;
/* 1425 */       for (int i = 0; i < address.length; i += 2) {
/* 1426 */         int currentRunOffset = i;
/* 1427 */         while ((i < 16) && (address[i] == 0) && (address[(i + 1)] == 0)) {
/* 1428 */           i += 2;
/*      */         }
/* 1430 */         int currentRunLength = i - currentRunOffset;
/* 1431 */         if (currentRunLength > longestRunLength) {
/* 1432 */           longestRunOffset = currentRunOffset;
/* 1433 */           longestRunLength = currentRunLength;
/*      */         }
/*      */       }
/*      */       
/*      */ 
/* 1438 */       Buffer result = new Buffer();
/* 1439 */       for (int i = 0; i < address.length;) {
/* 1440 */         if (i == longestRunOffset) {
/* 1441 */           result.writeByte(58);
/* 1442 */           i += longestRunLength;
/* 1443 */           if (i == 16) result.writeByte(58);
/*      */         } else {
/* 1445 */           if (i > 0) result.writeByte(58);
/* 1446 */           int group = (address[i] & 0xFF) << 8 | address[(i + 1)] & 0xFF;
/* 1447 */           result.writeHexadecimalUnsignedLong(group);
/* 1448 */           i += 2;
/*      */         }
/*      */       }
/* 1451 */       return result.readUtf8();
/*      */     }
/*      */     
/*      */     private static int parsePort(String input, int pos, int limit)
/*      */     {
/*      */       try {
/* 1457 */         String portString = HttpUrl.canonicalize(input, pos, limit, "", false, false, true);
/* 1458 */         int i = Integer.parseInt(portString);
/* 1459 */         if ((i > 0) && (i <= 65535)) return i;
/* 1460 */         return -1;
/*      */       } catch (NumberFormatException e) {}
/* 1462 */       return -1;
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   private static int delimiterOffset(String input, int pos, int limit, String delimiters)
/*      */   {
/* 1472 */     for (int i = pos; i < limit; i++) {
/* 1473 */       if (delimiters.indexOf(input.charAt(i)) != -1) return i;
/*      */     }
/* 1475 */     return limit;
/*      */   }
/*      */   
/*      */   static String percentDecode(String encoded, boolean plusIsSpace) {
/* 1479 */     return percentDecode(encoded, 0, encoded.length(), plusIsSpace);
/*      */   }
/*      */   
/*      */   private List<String> percentDecode(List<String> list, boolean plusIsSpace) {
/* 1483 */     List<String> result = new ArrayList(list.size());
/* 1484 */     for (String s : list) {
/* 1485 */       result.add(s != null ? percentDecode(s, plusIsSpace) : null);
/*      */     }
/* 1487 */     return Collections.unmodifiableList(result);
/*      */   }
/*      */   
/*      */   static String percentDecode(String encoded, int pos, int limit, boolean plusIsSpace) {
/* 1491 */     for (int i = pos; i < limit; i++) {
/* 1492 */       char c = encoded.charAt(i);
/* 1493 */       if ((c == '%') || ((c == '+') && (plusIsSpace)))
/*      */       {
/* 1495 */         Buffer out = new Buffer();
/* 1496 */         out.writeUtf8(encoded, pos, i);
/* 1497 */         percentDecode(out, encoded, i, limit, plusIsSpace);
/* 1498 */         return out.readUtf8();
/*      */       }
/*      */     }
/*      */     
/*      */ 
/* 1503 */     return encoded.substring(pos, limit);
/*      */   }
/*      */   
/*      */   static void percentDecode(Buffer out, String encoded, int pos, int limit, boolean plusIsSpace) {
/*      */     int codePoint;
/* 1508 */     for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
/* 1509 */       codePoint = encoded.codePointAt(i);
/* 1510 */       if ((codePoint == 37) && (i + 2 < limit)) {
/* 1511 */         int d1 = decodeHexDigit(encoded.charAt(i + 1));
/* 1512 */         int d2 = decodeHexDigit(encoded.charAt(i + 2));
/* 1513 */         if ((d1 != -1) && (d2 != -1)) {
/* 1514 */           out.writeByte((d1 << 4) + d2);
/* 1515 */           i += 2;
/* 1516 */           continue;
/*      */         }
/* 1518 */       } else if ((codePoint == 43) && (plusIsSpace)) {
/* 1519 */         out.writeByte(32);
/* 1520 */         continue;
/*      */       }
/* 1522 */       out.writeUtf8CodePoint(codePoint);
/*      */     }
/*      */   }
/*      */   
/*      */   static int decodeHexDigit(char c) {
/* 1527 */     if ((c >= '0') && (c <= '9')) return c - '0';
/* 1528 */     if ((c >= 'a') && (c <= 'f')) return c - 'a' + 10;
/* 1529 */     if ((c >= 'A') && (c <= 'F')) return c - 'A' + 10;
/* 1530 */     return -1;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   static String canonicalize(String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean plusIsSpace, boolean asciiOnly)
/*      */   {
/*      */     int codePoint;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/* 1551 */     for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
/* 1552 */       codePoint = input.codePointAt(i);
/* 1553 */       if ((codePoint < 32) || (codePoint == 127) || ((codePoint >= 128) && (asciiOnly)) || 
/*      */       
/*      */ 
/* 1556 */         (encodeSet.indexOf(codePoint) != -1) || ((codePoint == 37) && (!alreadyEncoded)) || ((codePoint == 43) && (plusIsSpace)))
/*      */       {
/*      */ 
/*      */ 
/* 1560 */         Buffer out = new Buffer();
/* 1561 */         out.writeUtf8(input, pos, i);
/* 1562 */         canonicalize(out, input, i, limit, encodeSet, alreadyEncoded, plusIsSpace, asciiOnly);
/* 1563 */         return out.readUtf8();
/*      */       }
/*      */     }
/*      */     
/*      */ 
/* 1568 */     return input.substring(pos, limit);
/*      */   }
/*      */   
/*      */   static void canonicalize(Buffer out, String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean plusIsSpace, boolean asciiOnly)
/*      */   {
/* 1573 */     Buffer utf8Buffer = null;
/*      */     int codePoint;
/* 1575 */     for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
/* 1576 */       codePoint = input.codePointAt(i);
/* 1577 */       if ((!alreadyEncoded) || ((codePoint != 9) && (codePoint != 10) && (codePoint != 12) && (codePoint != 13)))
/*      */       {
/*      */ 
/* 1580 */         if ((codePoint == 43) && (plusIsSpace))
/*      */         {
/* 1582 */           out.writeUtf8(alreadyEncoded ? "+" : "%2B");
/* 1583 */         } else { if ((codePoint < 32) || (codePoint == 127) || ((codePoint >= 128) && (asciiOnly)) || 
/*      */           
/*      */ 
/* 1586 */             (encodeSet.indexOf(codePoint) != -1) || ((codePoint == 37) && (!alreadyEncoded)))
/*      */           {
/*      */ 
/* 1589 */             if (utf8Buffer == null) {
/* 1590 */               utf8Buffer = new Buffer();
/*      */             }
/* 1592 */             utf8Buffer.writeUtf8CodePoint(codePoint); }
/* 1593 */           while (!utf8Buffer.exhausted()) {
/* 1594 */             int b = utf8Buffer.readByte() & 0xFF;
/* 1595 */             out.writeByte(37);
/* 1596 */             out.writeByte(HEX_DIGITS[(b >> 4 & 0xF)]);
/* 1597 */             out.writeByte(HEX_DIGITS[(b & 0xF)]);
/* 1598 */             continue;
/*      */             
/*      */ 
/* 1601 */             out.writeUtf8CodePoint(codePoint);
/*      */           }
/*      */         } }
/*      */     }
/*      */   }
/*      */   
/*      */   static String canonicalize(String input, String encodeSet, boolean alreadyEncoded, boolean plusIsSpace, boolean asciiOnly) {
/* 1608 */     return canonicalize(input, 0, input
/* 1609 */       .length(), encodeSet, alreadyEncoded, plusIsSpace, asciiOnly);
/*      */   }
/*      */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\HttpUrl.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */