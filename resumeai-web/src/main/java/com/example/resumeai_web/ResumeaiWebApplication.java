package com.example.resumeai_web;

import com.example.resumeai_web.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(GatewayProperties.class)
public class ResumeaiWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeaiWebApplication.class, args);
	}

}
