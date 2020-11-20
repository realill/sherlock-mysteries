package com.sherlockmysteries.pdata;

import org.junit.Assert;
import org.junit.Test;

public class SMDataManagerTest {
  
  @Test
  public void personName() {
    Assert.assertEquals("Holmes, Sherlock", SMDataManager.personName("Sherlock Holmes"));
    Assert.assertEquals("Watson, Dr. John", SMDataManager.personName("Dr. John Watson"));
    Assert.assertEquals("Meeks, Sir Jasper", SMDataManager.personName("Sir Jasper Meeks"));
  }
}
