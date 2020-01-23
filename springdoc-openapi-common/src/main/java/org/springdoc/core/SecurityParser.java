/*
 *
 *  * Copyright 2019-2020 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springdoc.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.commons.lang3.StringUtils;

import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;

class SecurityParser {

	private final PropertyResolverUtils propertyResolverUtils;

	public SecurityParser(PropertyResolverUtils propertyResolverUtils) {
		this.propertyResolverUtils = propertyResolverUtils;
	}

	private static boolean isEmpty(io.swagger.v3.oas.annotations.security.OAuthFlows oAuthFlows) {
		boolean result;
		if (oAuthFlows == null) {
			result = true;
		}
		else if (!isEmpty(oAuthFlows.implicit()) || !isEmpty(oAuthFlows.authorizationCode()) || !isEmpty(oAuthFlows.clientCredentials()) || !isEmpty(oAuthFlows.password())) {
			result = false;
		}
		else result = oAuthFlows.extensions().length <= 0;
		return result;
	}

	private static boolean isEmpty(io.swagger.v3.oas.annotations.security.OAuthFlow oAuthFlow) {
		boolean result;
		if (oAuthFlow == null) {
			result = true;
		}
		else if (!StringUtils.isBlank(oAuthFlow.authorizationUrl()) || !StringUtils.isBlank(oAuthFlow.refreshUrl()) || !StringUtils.isBlank(oAuthFlow.tokenUrl()) || !isEmpty(oAuthFlow.scopes())) {
			result = false;
		}
		else result = oAuthFlow.extensions().length <= 0;
		return result;
	}

	private static boolean isEmpty(OAuthScope[] scopes) {
		boolean result = false;
		if (scopes == null || scopes.length == 0) {
			result = true;
		}
		return result;
	}

	public io.swagger.v3.oas.annotations.security.SecurityRequirement[] getSecurityRequirements(
			HandlerMethod method) {
		// class SecurityRequirements
		io.swagger.v3.oas.annotations.security.SecurityRequirements classSecurity = ReflectionUtils
				.getAnnotation(method.getBeanType(), io.swagger.v3.oas.annotations.security.SecurityRequirements.class);
		// method SecurityRequirements
		io.swagger.v3.oas.annotations.security.SecurityRequirements methodSecurity = ReflectionUtils
				.getAnnotation(method.getMethod(), io.swagger.v3.oas.annotations.security.SecurityRequirements.class);

		Set<io.swagger.v3.oas.annotations.security.SecurityRequirement> allSecurityTags = null;

		if (classSecurity != null) {
			allSecurityTags = new HashSet<>(Arrays.asList(classSecurity.value()));
		}
		if (methodSecurity != null) {
			allSecurityTags = addSecurityRequirements(allSecurityTags, Arrays.asList(methodSecurity.value()));
		}

		if (CollectionUtils.isEmpty(allSecurityTags)) {
			// class SecurityRequirement
			List<io.swagger.v3.oas.annotations.security.SecurityRequirement> securityRequirementsClassList = ReflectionUtils
					.getRepeatableAnnotations(method.getBeanType(),
							io.swagger.v3.oas.annotations.security.SecurityRequirement.class);
			// method SecurityRequirement
			List<io.swagger.v3.oas.annotations.security.SecurityRequirement> securityRequirementsMethodList = ReflectionUtils
					.getRepeatableAnnotations(method.getMethod(),
							io.swagger.v3.oas.annotations.security.SecurityRequirement.class);
			if (!CollectionUtils.isEmpty(securityRequirementsClassList)) {
				allSecurityTags = addSecurityRequirements(allSecurityTags, securityRequirementsClassList);
			}
			if (!CollectionUtils.isEmpty(securityRequirementsMethodList)) {
				allSecurityTags = addSecurityRequirements(allSecurityTags, securityRequirementsMethodList);
			}
		}

		return (allSecurityTags != null) ? allSecurityTags.toArray(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0]) : null;
	}

	private Set<io.swagger.v3.oas.annotations.security.SecurityRequirement> addSecurityRequirements(Set<io.swagger.v3.oas.annotations.security.SecurityRequirement> allSecurityTags, List<io.swagger.v3.oas.annotations.security.SecurityRequirement> securityRequirementsClassList) {
		if (allSecurityTags == null)
			allSecurityTags = new HashSet<>();
		allSecurityTags.addAll(securityRequirementsClassList);
		return allSecurityTags;
	}

	public Optional<List<SecurityRequirement>> getSecurityRequirements(
			io.swagger.v3.oas.annotations.security.SecurityRequirement[] securityRequirementsApi) {
		if (securityRequirementsApi == null || securityRequirementsApi.length == 0) {
			return Optional.empty();
		}
		List<SecurityRequirement> securityRequirements = new ArrayList<>();
		for (io.swagger.v3.oas.annotations.security.SecurityRequirement securityRequirementApi : securityRequirementsApi) {
			if (StringUtils.isBlank(securityRequirementApi.name())) {
				continue;
			}
			SecurityRequirement securityRequirement = new SecurityRequirement();
			if (securityRequirementApi.scopes().length > 0) {
				securityRequirement.addList(securityRequirementApi.name(),
						Arrays.asList(securityRequirementApi.scopes()));
			}
			else {
				securityRequirement.addList(securityRequirementApi.name());
			}
			securityRequirements.add(securityRequirement);
		}
		if (securityRequirements.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(securityRequirements);
	}

	public Optional<SecuritySchemePair> getSecurityScheme(
			io.swagger.v3.oas.annotations.security.SecurityScheme securityScheme) {
		if (securityScheme == null) {
			return Optional.empty();
		}
		String key = null;
		SecurityScheme securitySchemeObject = new SecurityScheme();

		if (StringUtils.isNotBlank(securityScheme.in().toString())) {
			securitySchemeObject.setIn(getIn(securityScheme.in().toString()));
		}
		if (StringUtils.isNotBlank(securityScheme.type().toString())) {
			securitySchemeObject.setType(getType(securityScheme.type().toString()));
		}

		if (StringUtils.isNotBlank(securityScheme.openIdConnectUrl())) {
			securitySchemeObject.setOpenIdConnectUrl(propertyResolverUtils.resolve(securityScheme.openIdConnectUrl()));
		}
		if (StringUtils.isNotBlank(securityScheme.scheme())) {
			securitySchemeObject.setScheme(securityScheme.scheme());
		}

		if (StringUtils.isNotBlank(securityScheme.bearerFormat())) {
			securitySchemeObject.setBearerFormat(securityScheme.bearerFormat());
		}
		if (StringUtils.isNotBlank(securityScheme.description())) {
			securitySchemeObject.setDescription(securityScheme.description());
		}
		if (StringUtils.isNotBlank(securityScheme.paramName())) {
			securitySchemeObject.setName(securityScheme.paramName());
		}
		if (StringUtils.isNotBlank(securityScheme.ref())) {
			securitySchemeObject.set$ref(securityScheme.ref());
		}
		if (StringUtils.isNotBlank(securityScheme.name())) {
			key = securityScheme.name();
			if (SecuritySchemeType.APIKEY.toString().equals(securitySchemeObject.getType().toString()))
				securitySchemeObject.setName(securityScheme.name());
		}

		if (securityScheme.extensions().length > 0) {
			Map<String, Object> extensions = AnnotationsUtils.getExtensions(securityScheme.extensions());
			extensions.forEach(securitySchemeObject::addExtension);
		}

		getOAuthFlows(securityScheme.flows()).ifPresent(securitySchemeObject::setFlows);

		SecuritySchemePair result = new SecuritySchemePair(key, securitySchemeObject);
		return Optional.of(result);
	}

	public void buildSecurityRequirement(
			io.swagger.v3.oas.annotations.security.SecurityRequirement[] securityRequirements, Operation operation) {
		Optional<List<SecurityRequirement>> requirementsObject = this.getSecurityRequirements(securityRequirements);
		requirementsObject.ifPresent(requirements -> requirements.stream()
				.filter(r -> operation.getSecurity() == null || !operation.getSecurity().contains(r))
				.forEach(operation::addSecurityItem));
	}

	private Optional<OAuthFlows> getOAuthFlows(io.swagger.v3.oas.annotations.security.OAuthFlows oAuthFlows) {
		if (isEmpty(oAuthFlows)) {
			return Optional.empty();
		}
		OAuthFlows oAuthFlowsObject = new OAuthFlows();
		if (oAuthFlows.extensions().length > 0) {
			Map<String, Object> extensions = AnnotationsUtils.getExtensions(oAuthFlows.extensions());
			extensions.forEach(oAuthFlowsObject::addExtension);
		}
		getOAuthFlow(oAuthFlows.authorizationCode()).ifPresent(oAuthFlowsObject::setAuthorizationCode);
		getOAuthFlow(oAuthFlows.clientCredentials()).ifPresent(oAuthFlowsObject::setClientCredentials);
		getOAuthFlow(oAuthFlows.implicit()).ifPresent(oAuthFlowsObject::setImplicit);
		getOAuthFlow(oAuthFlows.password()).ifPresent(oAuthFlowsObject::setPassword);
		return Optional.of(oAuthFlowsObject);
	}

	private Optional<OAuthFlow> getOAuthFlow(io.swagger.v3.oas.annotations.security.OAuthFlow oAuthFlow) {
		if (isEmpty(oAuthFlow)) {
			return Optional.empty();
		}
		OAuthFlow oAuthFlowObject = new OAuthFlow();
		if (StringUtils.isNotBlank(oAuthFlow.authorizationUrl())) {
			oAuthFlowObject.setAuthorizationUrl(propertyResolverUtils.resolve(oAuthFlow.authorizationUrl()));
		}
		if (StringUtils.isNotBlank(oAuthFlow.refreshUrl())) {
			oAuthFlowObject.setRefreshUrl(propertyResolverUtils.resolve(oAuthFlow.refreshUrl()));
		}
		if (StringUtils.isNotBlank(oAuthFlow.tokenUrl())) {
			oAuthFlowObject.setTokenUrl(propertyResolverUtils.resolve(oAuthFlow.tokenUrl()));
		}
		if (oAuthFlow.extensions().length > 0) {
			Map<String, Object> extensions = AnnotationsUtils.getExtensions(oAuthFlow.extensions());
			extensions.forEach(oAuthFlowObject::addExtension);
		}
		getScopes(oAuthFlow.scopes()).ifPresent(oAuthFlowObject::setScopes);
		return Optional.of(oAuthFlowObject);
	}

	private Optional<Scopes> getScopes(OAuthScope[] scopes) {
		if (isEmpty(scopes)) {
			return Optional.empty();
		}
		Scopes scopesObject = new Scopes();
		Arrays.stream(scopes).forEach(scope -> scopesObject.addString(scope.name(), scope.description()));
		return Optional.of(scopesObject);
	}

	private SecurityScheme.In getIn(String value) {
		return Arrays.stream(SecurityScheme.In.values()).filter(i -> i.toString().equals(value)).findFirst()
				.orElse(null);
	}

	private SecurityScheme.Type getType(String value) {
		return Arrays.stream(SecurityScheme.Type.values()).filter(i -> i.toString().equals(value)).findFirst()
				.orElse(null);
	}

}