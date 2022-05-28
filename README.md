# AXEL

AXEL is a combinatorial testing tool for flexible interaction covering and automated oracle supporting. AXEL  focuses on making CT more practically applicable. On the one hand, AXEL offers a flexible mechanism to specify the entities to cover, without limitations on the degrees of these entities. Therefore, AXEL stretches the ability of CT generators to satisfy more coverage criteria. On the other hand, AXEL supports the application of metamorphic relation to covering array, such that it can automatically determine the pass or fail of generated test cases. 

### Getting Started

AXEL finally provides interface calls in the form of service to generate test cases. The post request needs the information of the database that stores the model. For that, the first step is modeling the system under test into the database.

The detail of the creation of the database can be seen from `tsgen_create.sql`. We use MySQL as the database, and other databases are all supported. That's because the post request provided by AEXL uses a JDBC URL to connect to the database.

We were given two example systems in  `tsgen_update.sql`. The model with the id of 1 is an airline ticket booking system to be tested. The model with the id of 2 is a triangle system, which needs to input the three sides of the triangle and output the triangle type.

Use the source command to import data for MySQL.

```mysql
source tsgen_create.sql
source tsgen_update.sql
```

### Tool Execution

There is a jar named AXEL-1.0-exec.jar in the folder of Executables. AXEL can be executed using the following command

```shell
jar -jar AXEL-1.0-exec.jar
```

The default port used by AXEL is 8083. The service can be called by the post request `localhost:8083/tsgen?id=?&url=?&user=?&pwd=?`. This service needs four parameters. The parameter of url ,user, and pwd are used by JDBC to connect to the database that the system under test stored. The parameter of id is the id of the system in the database.

To generate testcases for the airline ticket booking system, post request `localhost:8083/tsgen?id=1&url=jdbc:mysql://localhost:3306/tsgen&user=root&pwd=password`

To generate testcases for the triangle system, post request `localhost:8083/tsgen?id=2&url=jdbc:mysql://localhost:3306/tsgen&user=root&pwd=password`

### AXEL Experiments

The system used for the experiment and the experiment results are in the folder Experiments
