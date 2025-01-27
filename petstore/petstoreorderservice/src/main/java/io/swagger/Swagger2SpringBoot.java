package io.swagger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

import com.chtrembl.petstore.order.api.StoreApiCacheInterceptor;
import com.chtrembl.petstore.order.model.ContainerEnvironment;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableCaching
@EnableSwagger2
@ComponentScan(basePackages = { "io.swagger", "com.chtrembl.petstore.order.api", "io.swagger.configuration" })
public class Swagger2SpringBoot implements CommandLineRunner {

	@Bean
	public CacheOperationSource cacheOperationSource() {
		return new AnnotationCacheOperationSource();
	}

	@Primary
	@Bean
	public CacheInterceptor cacheInterceptor() {
		CacheInterceptor interceptor = new StoreApiCacheInterceptor();
		interceptor.setCacheOperationSources(cacheOperationSource());
		return interceptor;
	}

	@Bean
	public ContainerEnvironment containerEnvvironment() {
		return new ContainerEnvironment();
	}

	@Override
	public void run(String... arg0) throws Exception {
		if (arg0.length > 0 && arg0[0].equals("exitcode")) {
			throw new ExitException();
		}
	}

	public static void main(String[] args) throws Exception {
		new SpringApplication(Swagger2SpringBoot.class).run(args);
	}

	class ExitException extends RuntimeException implements ExitCodeGenerator {
		private static final long serialVersionUID = 1L;

		@Override
		public int getExitCode() {
			return 10;
		}

	}
}
