//=============================================================================
//This file is part of The PADRES Project.
//
//For more information, see http://www.msrg.utoronto.ca
//
//Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
//=============================================================================
//$Id$
//=============================================================================
package ca.utoronto.msrg.padres.demo.workflow.agent;

import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
/**
 * @author Pengcheng Wan
 *
 * Simple class to verify the schedule, right now it only support
 * DAILY (Every day)
 * WORK DAY (Except Sunday and Saturday, not consider holidays)
 * MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
 * 
 * It suppose the broker will publish the trigger according to date format
 * YYYY.MM.DD and not consider time.
 */
public class SimpleDate {
    
    Calendar calendar = null;
    Date date = null;
    
    /**
     * Default constructor use system date time as basic schedule
     */
    public SimpleDate() {
        calendar = Calendar.getInstance();
        date = Calendar.getInstance().getTime();
    }

    /**
     * Constructor initialized with trigger date which has this format: yyyy.MM.dd
     * @param trigger date of trigger
     */
    public SimpleDate(String trigger){
        try{
            //SimpleDateFormat timeFormat = new SimpleDateFormat();
    		//SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            //SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy.MM.dd");

            calendar = timeFormat.getCalendar();
            //System.out.println(calendar);
            
            //Date date = timeFormat.parse("2001.07.04 AD at 12:08:56 PDT");
            //Date date = timeFormat.parse("2001.07.04");
            Date date = timeFormat.parse(trigger);
            System.out.println(date);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    /**
     * Transfter the "day of week" into valid Calendar date field
     * @param schedule
     */
    public int map(String schedule){
        if (schedule.equalsIgnoreCase(ScheduleTerms.SUNDAY)){
            return Calendar.SUNDAY;
        }else if (schedule.equalsIgnoreCase(ScheduleTerms.MONDAY)){
            return Calendar.MONDAY;
        }else if (schedule.equalsIgnoreCase(ScheduleTerms.TUESDAY)){
            return Calendar.TUESDAY;
        }else if (schedule.equalsIgnoreCase(ScheduleTerms.WEDNESDAY)){
            return Calendar.WEDNESDAY;
        }else if (schedule.equalsIgnoreCase(ScheduleTerms.THURSDAY)){
            return Calendar.THURSDAY;
        }else if (schedule.equalsIgnoreCase(ScheduleTerms.FRIDAY)){
            return Calendar.FRIDAY;
        }else if (schedule.equalsIgnoreCase(ScheduleTerms.SATURDAY)){
            return Calendar.SATURDAY;
        }
        return -1;
    }
    
    /**
     * Support DAILY, WORK_DAYS, DAY OF WEEK which is Monday, Tuesday
     * Wednesday, Thursday, Friday, Saturday and Sunday
     * @param schedule
     * @return if this date is matched to passed in schedule
     */
    public boolean match(String schedule){
        String upperCase = schedule.toUpperCase();
        if (upperCase.indexOf((ScheduleTerms.DAILY).toUpperCase()) != -1){
            // it is a daily term
            return true;
        }else if (upperCase.indexOf((ScheduleTerms.WORK_DAYS).toUpperCase()) != -1){
            // it is a work day, need check the trigger
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if ((day == Calendar.SATURDAY) || (day == Calendar.SUNDAY)){
                System.out.println("This date is weekend!");
                return false;
            }else{
                System.out.println("This date is workday!");
                return true;
            }
        }else {	// it is specicial day: Monday, Tuesday, and so on
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if (this.map(schedule) == day){
                return true;
            }
        }

        return false;
    }
    
    
    //=========================================================================
    //====================Testing will be removed =============================
    public static void main(String[] args) {

        
        DateFormat df = DateFormat.getDateInstance();
        try{
    		//SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            //SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
            //SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy.MM.dd");
            //SimpleDateFormat timeFormat = new SimpleDateFormat();
            //Date schedule = timeFormat.getCalendar().getTime();
            //Date test = timeFormat.parse("2001.07.04 AD at 12:08:56 PDT");
            //Date test = timeFormat.parse("2001.07.04");

            //System.out.println(schedule);
            //System.out.println(test);
            //Date schedules = DateFormat.getInstance().parse("2004.07.21");
        }catch (Exception e){
            e.printStackTrace();
        }

        //=======================================================================
        SimpleDate simple = new SimpleDate("2004.9.10");
        System.out.println(simple.match("workdays"));
        
    }
}
