package com.scalar.dl.ledger.util;

import java.util.Calendar;
import java.util.TimeZone;

public class Time {

  public static long getCurrentUtcTimeInMillis() {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
  }
}
