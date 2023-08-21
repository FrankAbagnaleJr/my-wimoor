package com.wimoor.amazon.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

import com.wimoor.common.GeneralUtil;


public class ChartPoint {
	
	 public static class  SumType{
	  public static final String Daily="Daily";
	  public static final String Weekly="Weekly";
	  public static final String Monthly="Monthly";
	 }
	 
    public static String getKeyByTimeType(String sumtype,Calendar c){
    	String tempkey="";
    	Integer m= (c.get(Calendar.MONTH)+1);
    	String mstr=m<10?"0"+m.toString():m.toString();
    	Integer d=c.get(Calendar.DAY_OF_MONTH);
    	String dstr=d<10?"0"+d.toString():d.toString();
  	  if("Daily".equals(sumtype)){
		   tempkey =tempkey+  mstr;
		   tempkey =tempkey+ "."+dstr ; 
		  }
  	  else if("Weekly".equals(sumtype)){
  		 tempkey= GeneralUtil.getSundayOfThisWeek(c.getTime());
  		 tempkey=tempkey.substring(tempkey.length()-5, tempkey.length());
  	  }
     else if("Monthly".equals(sumtype)){
			   tempkey =tempkey+  (c.get(Calendar.YEAR));
			   tempkey =tempkey+ "-"+ mstr; 
		  }
		return tempkey;
    }
    public static String getKeyByTimeType(String sumtype,String time){
    	String tempkey="";
    	Calendar c=Calendar.getInstance();
    	c.setTime(GeneralUtil.getDatez(time));
    	Integer m= (c.get(Calendar.MONTH)+1);
    	String mstr=m<10?"0"+m.toString():m.toString();
    	Integer d=c.get(Calendar.DAY_OF_MONTH);
    	String dstr=d<10?"0"+d.toString():d.toString();
  	  if("Daily".equals(sumtype)){
		   tempkey =tempkey+  mstr;
		   tempkey =tempkey+ "."+dstr ; 
		  }
  	  else if("Weekly".equals(sumtype)){
  		 tempkey= GeneralUtil.getSundayOfThisWeek(c.getTime());
  		 tempkey=tempkey.substring(tempkey.length()-5, tempkey.length());
  	  }
     else if("Monthly".equals(sumtype)){
			   tempkey =tempkey+  (c.get(Calendar.YEAR));
			   tempkey =tempkey+ "-"+ mstr; 
		  }
		return tempkey;
    }
    
    public static Boolean checkTimeType(String sumtype,Calendar c,Date beginDate,Date endDate){
   	 Calendar calendar = Calendar.getInstance();      
   	    if(endDate != null){        
   	         calendar.setTime(endDate);      
   	    }        
   	    //int w = calendar.get(Calendar.DAY_OF_WEEK)  ;  
   	    int m = calendar.get(Calendar.MONTH);
   	    int y = calendar.get(Calendar.YEAR);
     if(c.get(Calendar.YEAR)<y) return true;
 	  if("Daily".equals(sumtype)){
		     return c.getTime().equals(endDate)||c.getTime().before(endDate);
	  }
 	  else if("Weekly".equals(sumtype)){
 		    return c.getTime().equals(endDate)||  GeneralUtil.getSundayOfThisWeek(c.getTime()).compareTo(GeneralUtil.getSundayOfThisWeek(endDate))<=0;
 	  }
	  else if("Monthly".equals(sumtype)){
		    return  c.get(Calendar.YEAR)==y?c.get(Calendar.MONTH)<=m:c.get(Calendar.YEAR)<y; 
	  }
	return null;
	 
   }
    
    public static void step(String sumtype,Calendar c,Date beginDate,Date endDate){
    	  if("Daily".equals(sumtype)){
  		    c.add(Calendar.DATE, 1);
  		  }
  	  else if("Weekly".equals(sumtype)){
  		    c.add(Calendar.DATE,7); 
  		  }
  	   else if("Monthly".equals(sumtype)){
  		   c.add(Calendar.MONTH,1); 
  		  }
   
  	 
      }
     
    public 	static  List<String> getLabels(String sumtype,Date beginDate,Date endDate) {
	   	 List<String> listLabel=new ArrayList<String>();
		 Calendar c=Calendar.getInstance();
	   	 for(c.setTime(beginDate);checkTimeType(sumtype, c,beginDate,endDate); step(sumtype, c,beginDate,endDate)){
				 String tempkey  =ChartPoint.getKeyByTimeType(sumtype, c);
				 listLabel.add(tempkey);
			 }
   	     return listLabel;
   }
    
    public 	static  List<String> getKeys(String sumtype,Date beginDate,Date endDate) {
	   	 List<String> listLabel=new ArrayList<String>();
		 Calendar c=Calendar.getInstance();
	   	 for(c.setTime(beginDate);checkTimeType(sumtype, c,beginDate,endDate); step(sumtype, c,beginDate,endDate)){
				 String tempkey  =GeneralUtil.formatDate(c.getTime());
				 listLabel.add(tempkey);
			 }
  	     return listLabel;
  }
    
    public 	static List<Object> getChartData(String sumtype,List<Map<String,Object>> map,String keyField,String feild,Date beginDate,Date endDate) {
		 List<Object> listData=new ArrayList<Object>();
		 Map<String,Object> dataMap=new HashedMap<String,Object>();
		 for(Map<String,Object> item:map) {
			 if(item.get(keyField)!=null) {
				 dataMap.put(item.get(keyField).toString(), item.get(feild));
			 }
		 }
		 List<String> keys = getKeys(sumtype,beginDate,endDate);
		 for(String key:keys) {
			if(dataMap.get(key)==null) {
				listData.add(0);
			}else {
				listData.add(dataMap.get(key));
			}
		 }
         return listData;
   }
    
    public 	static Map<String,Object> generateChartDate(String sumtype,Map<String,Object> map,Date beginDate,Date endDate) {
	   	 List<Object> listLabel=new ArrayList<Object>();
		 List<Object> listData=new ArrayList<Object>();
		 Calendar c=Calendar.getInstance();
		 Map<String,Object> result=new HashedMap<String,Object>();
    	for(c.setTime(beginDate);checkTimeType(sumtype, c,beginDate,endDate); step(sumtype, c,beginDate,endDate)){
			 String tempkey  =ChartPoint.getKeyByTimeType(sumtype, c);
			 listLabel.add(tempkey);
			 Object value =  map.get(tempkey);
			 if(value!=null) {
               if(value instanceof BigDecimal||value instanceof Double||value instanceof Float) {
            	   listData.add(new BigDecimal(value.toString()).setScale(4, BigDecimal.ROUND_HALF_UP));
               }else {
            	   listData.add(value);
               }
			 }
			 else listData.add(0);
    	}
    	result.put("listLabel", listLabel);
    	result.put("listData", listData);
    	return result;
    }
}
