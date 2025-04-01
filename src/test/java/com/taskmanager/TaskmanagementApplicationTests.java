package com.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;

@SpringBootTest
class TaskmanagementApplicationTests {

	@Test
	void contextLoads() {
		// Just testing if context loads successfully
	}

	@Bean
	@DependsOn("entityManagerFactory")
	public DataSourceInitializer dataSourceInitializer() {
		return new DataSourceInitializer();
	}
}