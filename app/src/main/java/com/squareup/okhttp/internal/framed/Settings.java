/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import java.util.Arrays;
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
/*     */ public final class Settings
/*     */ {
/*     */   static final int DEFAULT_INITIAL_WINDOW_SIZE = 65536;
/*     */   static final int FLAG_CLEAR_PREVIOUSLY_PERSISTED_SETTINGS = 1;
/*     */   static final int PERSIST_VALUE = 1;
/*     */   static final int PERSISTED = 2;
/*     */   static final int UPLOAD_BANDWIDTH = 1;
/*     */   static final int HEADER_TABLE_SIZE = 1;
/*     */   static final int DOWNLOAD_BANDWIDTH = 2;
/*     */   static final int ENABLE_PUSH = 2;
/*     */   static final int ROUND_TRIP_TIME = 3;
/*     */   static final int MAX_CONCURRENT_STREAMS = 4;
/*     */   static final int CURRENT_CWND = 5;
/*     */   static final int MAX_FRAME_SIZE = 5;
/*     */   static final int DOWNLOAD_RETRANS_RATE = 6;
/*     */   static final int MAX_HEADER_LIST_SIZE = 6;
/*     */   static final int INITIAL_WINDOW_SIZE = 7;
/*     */   static final int CLIENT_CERTIFICATE_VECTOR_SIZE = 8;
/*     */   static final int FLOW_CONTROL_OPTIONS = 10;
/*     */   static final int COUNT = 10;
/*     */   static final int FLOW_CONTROL_OPTIONS_DISABLED = 1;
/*     */   private int set;
/*     */   private int persistValue;
/*     */   private int persisted;
/*  82 */   private final int[] values = new int[10];
/*     */   
/*     */   void clear() {
/*  85 */     this.set = (this.persistValue = this.persisted = 0);
/*  86 */     Arrays.fill(this.values, 0);
/*     */   }
/*     */   
/*     */   Settings set(int id, int idFlags, int value) {
/*  90 */     if (id >= this.values.length) {
/*  91 */       return this;
/*     */     }
/*     */     
/*  94 */     int bit = 1 << id;
/*  95 */     this.set |= bit;
/*  96 */     if ((idFlags & 0x1) != 0) {
/*  97 */       this.persistValue |= bit;
/*     */     } else {
/*  99 */       this.persistValue &= (bit ^ 0xFFFFFFFF);
/*     */     }
/* 101 */     if ((idFlags & 0x2) != 0) {
/* 102 */       this.persisted |= bit;
/*     */     } else {
/* 104 */       this.persisted &= (bit ^ 0xFFFFFFFF);
/*     */     }
/*     */     
/* 107 */     this.values[id] = value;
/* 108 */     return this;
/*     */   }
/*     */   
/*     */   boolean isSet(int id)
/*     */   {
/* 113 */     int bit = 1 << id;
/* 114 */     return (this.set & bit) != 0;
/*     */   }
/*     */   
/*     */   int get(int id)
/*     */   {
/* 119 */     return this.values[id];
/*     */   }
/*     */   
/*     */   int flags(int id)
/*     */   {
/* 124 */     int result = 0;
/* 125 */     if (isPersisted(id)) result |= 0x2;
/* 126 */     if (persistValue(id)) result |= 0x1;
/* 127 */     return result;
/*     */   }
/*     */   
/*     */   int size()
/*     */   {
/* 132 */     return Integer.bitCount(this.set);
/*     */   }
/*     */   
/*     */   int getUploadBandwidth(int defaultValue)
/*     */   {
/* 137 */     int bit = 2;
/* 138 */     return (bit & this.set) != 0 ? this.values[1] : defaultValue;
/*     */   }
/*     */   
/*     */   int getHeaderTableSize()
/*     */   {
/* 143 */     int bit = 2;
/* 144 */     return (bit & this.set) != 0 ? this.values[1] : -1;
/*     */   }
/*     */   
/*     */   int getDownloadBandwidth(int defaultValue)
/*     */   {
/* 149 */     int bit = 4;
/* 150 */     return (bit & this.set) != 0 ? this.values[2] : defaultValue;
/*     */   }
/*     */   
/*     */ 
/*     */   boolean getEnablePush(boolean defaultValue)
/*     */   {
/* 156 */     int bit = 4;
/* 157 */     return (defaultValue ? 1 : (bit & this.set) != 0 ? this.values[2] : 0) == 1;
/*     */   }
/*     */   
/*     */   int getRoundTripTime(int defaultValue)
/*     */   {
/* 162 */     int bit = 8;
/* 163 */     return (bit & this.set) != 0 ? this.values[3] : defaultValue;
/*     */   }
/*     */   
/*     */   int getMaxConcurrentStreams(int defaultValue)
/*     */   {
/* 168 */     int bit = 16;
/* 169 */     return (bit & this.set) != 0 ? this.values[4] : defaultValue;
/*     */   }
/*     */   
/*     */   int getCurrentCwnd(int defaultValue)
/*     */   {
/* 174 */     int bit = 32;
/* 175 */     return (bit & this.set) != 0 ? this.values[5] : defaultValue;
/*     */   }
/*     */   
/*     */   int getMaxFrameSize(int defaultValue)
/*     */   {
/* 180 */     int bit = 32;
/* 181 */     return (bit & this.set) != 0 ? this.values[5] : defaultValue;
/*     */   }
/*     */   
/*     */   int getDownloadRetransRate(int defaultValue)
/*     */   {
/* 186 */     int bit = 64;
/* 187 */     return (bit & this.set) != 0 ? this.values[6] : defaultValue;
/*     */   }
/*     */   
/*     */   int getMaxHeaderListSize(int defaultValue)
/*     */   {
/* 192 */     int bit = 64;
/* 193 */     return (bit & this.set) != 0 ? this.values[6] : defaultValue;
/*     */   }
/*     */   
/*     */   int getInitialWindowSize(int defaultValue) {
/* 197 */     int bit = 128;
/* 198 */     return (bit & this.set) != 0 ? this.values[7] : defaultValue;
/*     */   }
/*     */   
/*     */   int getClientCertificateVectorSize(int defaultValue)
/*     */   {
/* 203 */     int bit = 256;
/* 204 */     return (bit & this.set) != 0 ? this.values[8] : defaultValue;
/*     */   }
/*     */   
/*     */   boolean isFlowControlDisabled()
/*     */   {
/* 209 */     int bit = 1024;
/* 210 */     int value = (bit & this.set) != 0 ? this.values[10] : 0;
/* 211 */     return (value & 0x1) != 0;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   boolean persistValue(int id)
/*     */   {
/* 219 */     int bit = 1 << id;
/* 220 */     return (this.persistValue & bit) != 0;
/*     */   }
/*     */   
/*     */   boolean isPersisted(int id)
/*     */   {
/* 225 */     int bit = 1 << id;
/* 226 */     return (this.persisted & bit) != 0;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   void merge(Settings other)
/*     */   {
/* 234 */     for (int i = 0; i < 10; i++) {
/* 235 */       if (other.isSet(i)) {
/* 236 */         set(i, other.flags(i), other.get(i));
/*     */       }
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\Settings.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */