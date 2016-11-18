/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.vgorcinschi.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Common utility methods and functional interfaces
 * @author vgorcinschi
 */
public class CommonUtil {

    /*
        function to transform LocalDateTime into JDBC acceptable Timestamp
    */
    public static Function<LocalDateTime, Timestamp> localToSql = l -> {
        if (l != null) {
            //do the conversion
            return new Timestamp(l.getYear()-1900, l.getMonthValue(), l.getDayOfMonth(), 
                    l.getHour(), l.getMinute(), l.getSecond(), l.getNano());
        }
        //garbage-in - garbage-out
        return null;
    };
}
