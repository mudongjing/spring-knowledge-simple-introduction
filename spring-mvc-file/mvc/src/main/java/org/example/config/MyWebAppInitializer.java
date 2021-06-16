package org.example.config;

import org.example.filter.SessionFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.EnumSet;


public class MyWebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer{

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] { AppConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] { DispatcherConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

//    @Override
//    public void onStartup(ServletContext servletContext) throws ServletException {
//        String filterName = "myfilter";
//        FilterRegistration.Dynamic filterRegistration =
//                servletContext.addFilter(filterName, new SessionFilter());
//        filterRegistration.addMappingForUrlPatterns(
//                EnumSet.of(DispatcherType.REQUEST), false,
//                "/filter/*");
//        super.onStartup(servletContext);
//    }
}
