package au.edu.rmit.bdm.Torch.queryEngine.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class TorchDate implements Comparable<TorchDate>{
    transient Logger logger = LoggerFactory.getLogger(TorchDate.class);
    transient Date date;
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;

    public TorchDate(){
        date = new Date();
    }

    public TorchDate setDay(int day){
        date.setDate(day);
        this.day = day;
        return this;
    }

    public TorchDate setMonth(int month){
        date.setMonth(month - 1);
        this.month = month;
        return this;
    }

    public TorchDate setYear(int year){
        date.setYear(year - 1900);
        this.year = year;
        return null;
    }

    public TorchDate setMinutes(int minute){
        date.setMinutes(minute);
        this.minute = minute;
        return this;
    }

    public TorchDate setSeconds(int second){
        date.setSeconds(second);
        this.second  = second;
        return this;
    }

    public TorchDate setHours(int hour){
        date.setHours(hour);
        this.hour = hour;
        return this;
    }

    public TorchDate setAll(long miliseconds){
        date.setTime(miliseconds);
        this.year = date.getYear() + 1900;
        this.month = date.getMonth() + 1;
        this.day = date.getDay();
        this.hour = date.getHours();
        this.minute = date.getMinutes();
        this.second = date.getSeconds();
        return this;
    }

    /**
     * Model string to TorchDate.
     *
     * @param _date String of format: yyyy-MM-dd hh:mm:ss (example: 2018-03-17 06:51:55)
     * @return TorchDate model
     */
    public TorchDate setAll(String _date){
        String[] date = _date.split(" ");
        String[] ymd = date[0].split("-");
        String[] hms = date[1].split(":");

        setYear(Integer.valueOf(ymd[0]));
        setMonth(Integer.valueOf(ymd[1]));
        setDay(Integer.valueOf(ymd[2]));

        setHours(Integer.valueOf(hms[0]));
        setMinutes(Integer.valueOf(hms[1]));
        setSeconds(Integer.valueOf(hms[2]));



        return this;
    }

    public long getTimeInMilliSec(){
        return date.getTime();
    }

    @Override
    public int compareTo(TorchDate o) {
        return Long.compare(this.getTimeInMilliSec(), o.getTimeInMilliSec());
    }
}
