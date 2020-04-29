/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.undertow.spring.security.springboot;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.camel.component.spring.security.SpringSecurityConfiguration;
import org.apache.camel.component.spring.security.keycloak.KeycloakJwtAuthenticationConverter;
import org.apache.camel.component.spring.security.keycloak.KeycloakUsernameSubClaimAdapter;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.spring.security.springboot.providers.AbstractProviderConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.apache.camel.spring.boot.util.GroupCondition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;



@Configuration
@Conditional({ConditionalOnCamelContextAndAutoConfigurationBeans.class,
        UndertowSpringSecurityComponentAutoConfiguration.GroupConditions.class})
@AutoConfigureAfter(CamelAutoConfiguration.class)
@EnableConfigurationProperties({ComponentConfigurationProperties.class, UndertowSpringSecurityComponentConfiguration.class})
public class UndertowSpringSecurityComponentAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(UndertowSpringSecurityComponentAutoConfiguration.class);

    private AbstractProviderConfiguration provider;
    private ClientRegistration clientRegistration;

    @Autowired
    private UndertowSpringSecurityComponentConfiguration configuration;

    @Autowired
    private DelegatingFilterProxyRegistrationBean delegatingFilterProxyRegistrationBean;

    static class GroupConditions extends GroupCondition {
        public GroupConditions() {
            super("camel.component", "camel.component.undertow");
        }
    }

    @EnableWebSecurity
    public class OAuth2Security extends WebSecurityConfigurerAdapter {

        @Override
        public void init(WebSecurity web) throws Exception {
            super.init(web);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt()
                    .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter());
        }
    }

    @Bean
    public JwtDecoder jwtDecoderByIssuerUri(ClientRegistrationRepository repository) {
        final String jwkSetUri = repository.findByRegistrationId("keycloak").getProviderDetails().getJwkSetUri();
        final NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(jwkSetUri);
        // Use preferred_username from claims as authentication name, instead of UUID subject
        jwtDecoder.setClaimSetConverter(new KeycloakUsernameSubClaimAdapter(getProvider().getUserNameAttribute()));
        return jwtDecoder;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(Collections.singletonList(getClientRegistration()));
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService() {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository());
    }

    @Bean
    public SpringSecurityConfiguration securityConfiguration(UndertowComponent undertowComponent) {
        SpringSecurityConfiguration securityConfiguration = () -> delegatingFilterProxyRegistrationBean.getFilter();
        undertowComponent.setSecurityConfiguration(securityConfiguration);
        return securityConfiguration;
    }

    //----------------------------------------------- provider configuration helper methods --------------------------------------

    private enum ProviderType {
        keycloak;
    }

    private AbstractProviderConfiguration getProvider() {
        if (provider == null) {
            LinkedList<AbstractProviderConfiguration> definedProviders = new LinkedList<>();

            if (configuration.getKeycloak() != null) {
                definedProviders.add(configuration.getKeycloak());
            }

            if (definedProviders.isEmpty()) {
                throw new IllegalArgumentException(String.format("Properties camel.component.undertow.spring.security.provider.* are not defined. Allowed providers are (%s)", ProviderType.values()));
            }
            if (definedProviders.size() > 1) {
                throw new IllegalArgumentException(String.format("Two or more providers are defined (%s)", definedProviders));
            }

            provider = definedProviders.getFirst();
        }

        return provider;
    }

    private ClientRegistration getClientRegistration() {
        if (clientRegistration == null) {
            try {
                clientRegistration = getProvider().getClientRegistration();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Client url is not correct.", e);
            }
        }
        return clientRegistration;
    }

}