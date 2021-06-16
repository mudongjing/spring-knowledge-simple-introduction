package org.example.convertorwrap;

import com.google.gson.Gson;
import org.example.bean.Student;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

public class wrapper {
    public static void main(String[] args){
        Student student=new Student();
        DataBinder databinder=new DataBinder(student);
        databinder.addCustomFormatter(new DateFormatter("yyyy-MM-dd"));
        databinder.addCustomFormatter(new UserFormatter());
        MutablePropertyValues propertyValues=new MutablePropertyValues();
        propertyValues.add("id","8989");
        propertyValues.add("date","8989-12-12");
        propertyValues.add("user","user 1 ,2021-05-01");
        databinder.bind(propertyValues);
        BindingResult bindingResult=databinder.getBindingResult();
        student=(Student) bindingResult.getTarget();
        System.out.println((new Gson()).toJson(student));
    }
}
