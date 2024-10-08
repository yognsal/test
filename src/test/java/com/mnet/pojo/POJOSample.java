package com.mnet.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

/*
 * https://projectlombok.org/setup/maven
 * Please set up the .pom file with both the dependency and annotationProcessorPath for maven-compiler-plugin (3.8.1)
 * https://projectlombok.org/setup/eclipse
 * Eclipse plugin can be installed by double clicking lombok-1.18.26.jar once the maven dependency is added.
 */

/**
 * Sample POJO class using Lombok for auto-generating boilerplate code.
 * @Data at the class level will generate a getter / setter / equality check.
 * POJOSample.equals(PojoSample) is automatically overridden to compare the values of applicable member variables.
 * To suppress generating a specific getter or setter, use @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE).
 * @AllArgsConstructor will generate a constructor which takes every member of the POJO class as an argument.
 * @RequiredArgsConstructor (included in @Data) will generate a constructor using final fields only.
 * Arguments appear in the order defined in the class.
 */
@Data @AllArgsConstructor 
public class POJOSample {

	@Setter(AccessLevel.NONE) // Setter will not be generated for this field. Can also be replicated for getters.
	private String stringA;
	
	private String stringB;
	private Integer number;
	
	private boolean flag; // Note for boolean primitives, the getter defined will be isFlag() instead of getFlag()
}
