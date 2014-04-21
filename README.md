Enterprise-ready production-ready batch applications powered by Spring Boot
=============================
The project spring-boot-starter-batch-web is a Spring Boot starter for Spring Batch taking care of everything except writing the jobs. Features include:

* Starting up a web application and automatically deploying jobs to it (JavaConfig or XML).
* Log file separation, one log file for each job execution.
* An operations http endpoint for starting and stopping jobs, for retrieving the BatchStatus and the log file.
* A monitoring http endpoint for retrieving detailed information on a job execution, for knowing all deployed jobs and all running job executions.

All you have to do is configure your job in XML or JavaConfig, specify the DataSource for the job meta data (or don't, and you get an in-memory database), add a dependency to spring-boot-starter-batch-web, create a Spring Boot fat jar with maven package and start your server up by running java -jar. And, of course, you can benefit from Spring Boot's features, for example spring-boot-actuator is included by default.