package edu.thu.keg.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZY on 2017/6/19.
 */
public class ExpData {
    public List<String> data;
    public ExpData(){
        data = new ArrayList<>();
    }

    public void export(String fileName){
        try {
            FileUtils.writeLines(new File(fileName),data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addData(String line) {
        if (line == null){
            return;
        }
        data.add(line);
    }
}
