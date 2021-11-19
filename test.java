import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class test {
    public static void main(String[] args)
    {
        try
        {
            // String tempString = "8";
            // DateFormat df = new SimpleDateFormat("HH");
            // Date td = df.parse(tempString);
            // long tt = td.getTime();
            // System.out.println(tt);

            // td = new Date(50400000);
            // tempString = df.format(td);
            // System.out.println(tempString);
            // String[] s = null;
            // if(s == null) System.out.println(s.length);
            

            // String tempString2 = "12/31/2020 11:30";
            // DateFormat df2 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            // Date td2 = df2.parse(tempString2);
            // long tt2 = td2.getTime();
            // long tt = System.currentTimeMillis();
            // long tt2 = tt + (long)(3600000*2.5);

            // long difference = tt2 - tt;
            // long difference = System.currentTimeMillis();
            // Calendar c = Calendar.getInstance();
            // c.setTimeInMillis(difference);
            // int d = c.get(Calendar.LONG);
            // int h = c.get(Calendar.HOUR);
            // int m = c.get(Calendar.MINUTE);
            // long millis = System.currentTimeMillis();
            // Calendar c = Calendar.getInstance();
            // c.setTimeInMillis(millis);

            // int h = c.get(Calendar.HOUR);
            // int m = c.get(Calendar.MINUTE);

            // System.out.println(d + " " + h+":"+m);
        // long curr = System.currentTimeMillis();
        // Date d = new Date(curr);
        // Calendar c = Calendar.getInstance();
        // c.setTime(d);
        // int currWeek = c.get(Calendar.WEEK_OF_YEAR)-1; //starts at 0 ends at 52
        // int year = c.get(Calendar.YEAR);
        // System.out.println(year);
        // System.out.println(currWeek);
        // int currHour = c.get(Calendar.HOUR);
        // int currDay = c.get(Calendar.DATE);
        // int currMonth = c.get(Calendar.MONTH);
        // int currMinutes = c.get(Calendar.MINUTE);
        // long timeAfterCurrYear = (long)((currMinutes * 60000) + (currHour * 3600000) + (currDay * 86400000) + (currMonth * 2592000000L));
        // long currYear = curr - timeAfterCurrYear;
        // System.out.println(currYear);
        //604800000 milliseconds in a week
        // System.out.println((long)((long)currWeek * 604800000L));
        // long millisInCurrWeek = currYear + ((long)currWeek * 604800000L);
        // // long millisInCurrWeek = currYear + 8467200000L;
        // // System.out.println(millisInCurrWeek);
        // Date d1 = new Date(millisInCurrWeek);
        // DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        // String tempDate = df.format(d1);
        // System.out.println(tempDate);

        // DateFormat df = new SimpleDateFormat("yyyy");
        // long curr = System.currentTimeMillis();
        // Calendar c = Calendar.getInstance();
        // Date d = new Date(curr);
        // c.setTime(d);
        // Integer year = c.get(Calendar.YEAR);
        // int week = c.get(Calendar.WEEK_OF_YEAR)-1;
        // d = df.parse(year.toString());
        // long currYearInMilli = d.getTime();
        // System.out.println(currYearInMilli);
        // long currWeekInMilli = currYearInMilli + (long)((long)week * 604800000L);
        // System.out.println(currWeekInMilli);
        // df = new SimpleDateFormat("MM/dd/yyyy");
        // d = new Date(currWeekInMilli);
        // System.out.println(df.format(d));

        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
    }     
}