/*    */ package com.squareup.okhttp.internal;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public abstract class NamedRunnable
/*    */   implements Runnable
/*    */ {
/*    */   protected final String name;
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public NamedRunnable(String format, Object... args)
/*    */   {
/* 26 */     this.name = String.format(format, args);
/*    */   }
/*    */   
/*    */   /* Error */
/*    */   public final void run()
/*    */   {
/*    */     // Byte code:
/*    */     //   0: invokestatic 4	java/lang/Thread:currentThread	()Ljava/lang/Thread;
/*    */     //   3: invokevirtual 5	java/lang/Thread:getName	()Ljava/lang/String;
/*    */     //   6: astore_1
/*    */     //   7: invokestatic 4	java/lang/Thread:currentThread	()Ljava/lang/Thread;
/*    */     //   10: aload_0
/*    */     //   11: getfield 3	com/squareup/okhttp/internal/NamedRunnable:name	Ljava/lang/String;
/*    */     //   14: invokevirtual 6	java/lang/Thread:setName	(Ljava/lang/String;)V
/*    */     //   17: aload_0
/*    */     //   18: invokevirtual 7	com/squareup/okhttp/internal/NamedRunnable:execute	()V
/*    */     //   21: invokestatic 4	java/lang/Thread:currentThread	()Ljava/lang/Thread;
/*    */     //   24: aload_1
/*    */     //   25: invokevirtual 6	java/lang/Thread:setName	(Ljava/lang/String;)V
/*    */     //   28: goto +13 -> 41
/*    */     //   31: astore_2
/*    */     //   32: invokestatic 4	java/lang/Thread:currentThread	()Ljava/lang/Thread;
/*    */     //   35: aload_1
/*    */     //   36: invokevirtual 6	java/lang/Thread:setName	(Ljava/lang/String;)V
/*    */     //   39: aload_2
/*    */     //   40: athrow
/*    */     //   41: return
/*    */     // Line number table:
/*    */     //   Java source line #30	-> byte code offset #0
/*    */     //   Java source line #31	-> byte code offset #7
/*    */     //   Java source line #33	-> byte code offset #17
/*    */     //   Java source line #35	-> byte code offset #21
/*    */     //   Java source line #36	-> byte code offset #28
/*    */     //   Java source line #35	-> byte code offset #31
/*    */     //   Java source line #37	-> byte code offset #41
/*    */     // Local variable table:
/*    */     //   start	length	slot	name	signature
/*    */     //   0	42	0	this	NamedRunnable
/*    */     //   6	30	1	oldName	String
/*    */     //   31	9	2	localObject	Object
/*    */     // Exception table:
/*    */     //   from	to	target	type
/*    */     //   17	21	31	finally
/*    */   }
/*    */   
/*    */   protected abstract void execute();
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\NamedRunnable.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */