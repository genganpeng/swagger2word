package org.word.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {
    @Bean
    public OpenAPI mallTinyOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Swagger to Word")
                        .description("SpringDoc/Springfox/swagger 接口文档转word")
                        .version("v1.0.0")
                        .license(new License().name("springdoc").url("https://springdoc.org/")))
                .externalDocs(new ExternalDocumentation()
                        .description("水中加点糖的博客")
                        .url("https://blog.csdn.net/puhaiyang"));
    }

}
