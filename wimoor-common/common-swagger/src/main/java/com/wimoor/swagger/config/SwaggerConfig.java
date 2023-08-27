package com.wimoor.swagger.config;


import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableOpenApi
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {

        //设置swagger生效的环境，如果是dev
        //Profiles profiles = Profiles.of("dev");
        //通过 environment.acceptsProfiles()判断是否自己处在设定的环境中，environment这个参数通过当前类构造器传入createRestApi(Environment environment)
        //boolean falg =  environment.acceptsProfiles(profiles);

        //swagger设置，基本信息，要解析的接口及路径等
        return new Docket(DocumentationType.OAS_30)
                // 作者信息
                .apiInfo(apiInfo())
                .groupName("冀金梁") //页面上面下拉框默认显示的，如果多人开发配置了多个docket的Bean，那么可以通过swagger页面上面的下拉框来选择
                .enable(true)  //false关闭swagger，默认是true开启的,这里可以传入falg对象来判断如果是生产环境则开启，上线环境和其它环境不开启
                // 配置要扫描接口的方式
                .select()
                // RequestHandlerSelectors 配置要扫描接口的方式，跟下面的.apis是and关系，写了select()才能写.apis
                    //basePackage 指定扫描的controller包
                    //any 扫描全部
                    //none 不扫描
                    //withMethodAnnotation  扫描方法上的注解，参数是一个注解类的.class对象，只会扫描方法上有@ApiOperation注解的contraller
                    //withClassAnnotation 扫描类上面的注解，参数是一个注解的反射对象
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                // 过滤路径
                    //any 过滤全部
                    //none 不过滤
                    //ant(PathSelectors.ant("com.kyrie.controller")) 只过滤某个包下的
                    //regex 正则表达式，过滤带有error的controller
                .paths(PathSelectors.regex("(?!/error.*).*"))
                .build()
                .securityContexts(Arrays.asList(securityContext()))
                // ApiKey的name需与SecurityReference的reference保持一致
                .securitySchemes(Arrays.asList(new ApiKey("jsessionid", "jsessionid", SecurityScheme.In.HEADER.name())));
    }

    @Bean
    public OpenAPI springShopOpenAPI() {

        return new OpenAPI()
                .components(components())
                // 2. 再在这里添加上Swagger要使用的安全策略
                // addList()中写上对应的key
                .addSecurityItem(new SecurityRequirement().addList("tokenScheme"));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                //.forPaths(PathSelectors.regex("/*.*"))
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
                = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Arrays.asList(new SecurityReference("jsessionid", authorizationScopes));
    }

    // 1. 先在组件中注册安全策略
    private Components components() {
        return new Components()
                // 第一个参数是key值，后面是初始化一个安全策略的参数
                .addSecuritySchemes("tokenScheme", new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("jsessionid"));
    }

    //生成接口信息，包括标题、联系人，联系方式等
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Wimoor 接口文档")
                .description("如有疑问，请联系开发工程师")
                .contact(new Contact("深圳市万墨信息科技", "https://www.wimoor.com", "developer@wimoor.com"))
                .version("1.0")
                .build();
    }
}