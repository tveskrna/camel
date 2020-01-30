# Camel Rest OpenApi example

This example shows how to call a REST service defined using OpenApi
specification with the help of Camel Rest OpenApi component.

The example is a standalong Spring Boot application that acts as a REST
client, you can run simply by issuing:

    $ mvn spring-boot:run

Or by packaging it and running it using `java` CLI:

    $ mvn package
    $ java -jar target/camel-example-rest-openapi-*.jar

The example by default uses the PetStore demo hosted on swagger.io and
invokes the `getInventory` operation. You can make it call any API
that you have OpenApi specification for and any operation with simple
arguments, for instance this retrives a pet from the PetStore demo with
ID `14`:

    $ java -jar target/camel-example-rest-openapi-*.jar \
        --openapi=https://petstore3.swagger.io/api/v3/openapi.json \
        --operation=getPetById \
        --petId=14

## More information

You can find more information about Apache Camel at the website: 
http://camel.apache.org/
