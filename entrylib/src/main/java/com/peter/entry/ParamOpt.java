package com.peter.entry;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Administrator on 2016/12/26.
 */

public class ParamOpt {

    public static Map<String,String> parseParam(String[] args){
        HashMap<String,String> map = new HashMap<>();
        if(args != null && args.length >0){
            String currenttype = "";
            for(String param:args){
                if(param.startsWith("-")){
                    if(currenttype != null && currenttype.length() > 0){
                        map.put(currenttype,"");
                    }
                    currenttype = param.substring(1);
                }else{
                    if(param != null && currenttype != null && currenttype.length() > 0){
                        map.put(currenttype,param);
                        currenttype = "";
                    }
                }
            }
        }
        return map;
    }
}
