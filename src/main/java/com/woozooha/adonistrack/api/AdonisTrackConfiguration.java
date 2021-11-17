package com.woozooha.adonistrack.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AdonisTrackConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AdonisTrackInvocationController adonisTrackInvocationController() {
        return new AdonisTrackInvocationController();
    }

}
