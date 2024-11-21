package nc.gateway.config;


import nc.gateway.filter.AntiSpamFilter;
import nc.gateway.filter.CorsFilter;
import nc.gateway.filter.PpuCheckFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAppConfig {

    @Bean
    public FilterRegistrationBean addCorsFilter(){
        FilterRegistrationBean cors = new FilterRegistrationBean();
        cors.setFilter(new CorsFilter());
        cors.addUrlPatterns("/*");
        cors.setOrder(1);
        return cors;
    }

    @Bean
    public FilterRegistrationBean addAntiSpamFilter(){
        FilterRegistrationBean antiSpam = new FilterRegistrationBean();
        antiSpam.setFilter(new AntiSpamFilter());
        antiSpam.addUrlPatterns("/*");
        antiSpam.setOrder(2);
        return antiSpam;
    }

    @Bean
    public FilterRegistrationBean addPpuCheckFilter(){
        FilterRegistrationBean ppuCheck = new FilterRegistrationBean();
        ppuCheck.setFilter(new PpuCheckFilter());
        ppuCheck.addUrlPatterns("/*");
        ppuCheck.setOrder(3);
        return ppuCheck;
    }
}
