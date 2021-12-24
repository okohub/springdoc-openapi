/*
 *
 *  *
 *  *  * Copyright 2019-2020 the original author or authors.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package test.org.springdoc.api.app154;

import java.util.function.Function;
import java.util.function.Supplier;

import reactor.core.publisher.Flux;
import test.org.springdoc.api.AbstractSpringDocFunctionTest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

public class SpringDocApp154Test extends AbstractSpringDocFunctionTest {

	@SpringBootApplication
	@ComponentScan(basePackages = { "org.springdoc", "test.org.springdoc.api.app154" })
	static class SpringDocTestApp {
		@Bean
		public Function<String, String> reverseString() {
			return value -> new StringBuilder(value).reverse().toString();
		}

		@Bean
		public Function<String, String> uppercase() {
			return value -> value.toUpperCase();
		}

		@Bean
		public Function<Flux<String>, Flux<String>> lowercase() {
			return flux -> flux.map(value -> value.toLowerCase());
		}

		@Bean(name = "titi")
		public Supplier<PersonDTO> hello() {
			return () -> new PersonDTO();
		}

		@Bean
		public Supplier<Flux<String>> words() {
			return () -> Flux.fromArray(new String[] {"foo", "bar"});
		}
	}



}
