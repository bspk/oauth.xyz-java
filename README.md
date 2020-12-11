XYZ is a GNAP implementation in Java.

To run, first start a mongo DB instance for the items to connect to. One is provided in a docker image that opens the appropriate ports:

`docker-compose up`

All components connect to each other over local HTTP connections. To facilitate testing under containerization, everything has been configured to use the hostname `host.docker.internal`. To access the web interfaces from localhost, it is helpful to alias `host.docker.internal` to the loopback address of `127.0.0.1` to run this.


To build locally, you'll need to install the library package from the `lib` directory by running this command from that directory:

`mvn install`


The authorization server is in the directory `/as/` and can be started using Spring Boot from that directory:

`mvn spring-boot:run`

The AS is accessible at <http://localhost:9834/as>


The client instance is in the director `/c/` and can be started using Spring Boot from that directory:

`mvn spring-boot:run`

The client is accessible at <http://localhost:9839/c>


The resources server is in the director `/rs/` and can be started using Spring Boot from that directory:

`mvn spring-boot:run`

The client is accessible at <http://localhost:9836/rs>

