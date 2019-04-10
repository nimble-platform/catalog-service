package eu.nimble.service.catalogue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Value("${nimble.platformHost}")
    private String platformHost;

    @Bean
    public Docket api() {

        platformHost = platformHost.replace("https://", "");
        platformHost = platformHost.replace("http://","");

        return new Docket(DocumentationType.SWAGGER_2)
                .host(platformHost)
                .select()
                .apis(RequestHandlerSelectors.basePackage("eu.nimble"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(metaData());
    }

    private ApiInfo metaData(){
        return new ApiInfo(
                "NIMBLE Catalogue REST API",
                "Catalogue service lets users to manage catalogue, catalogue lines, price options, binary content and units on NIMBLE." +
                        " Detailed documentation about concepts and data models are provided at https://www.nimble-project.org/wp-content/uploads/2018/12/Catalogue-Service-REST-API.docx.",
                "1.0",
                "",
                "",
                "",
                ""
        );
    }
}

