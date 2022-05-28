drop database if exists tsgen;
create database tsgen;
use tsgen;

#table model store the model under test, AXEL use the model id to generate test case with the coverage strength of tWay
#the models with the same classname share the dataType
create table if not exists model
(
    `id` INT    AUTO_INCREMENT primary key,
    `classname` varchar(255) not null ,
    `Name` varchar(255) unique,
    `tWay` INT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

#table dataType defines the dataTypes that can be used
#we support three types: int,enum,and bool
#for int type, use the gte(greater than equal) and lte(Less than equal) define the section
#for enum type, use the Values with the name in table values to define the enumerable values
#for bool, the value is TRUE or FALSE, so the gte,lte,Values should be null
create table if not exists `dataType`
(
    `id`         INT    AUTO_INCREMENT primary key,
    `classname`  varchar(100) not null ,
    `typeName`   varchar(100) ,
    `Type`       varchar(20),
    `gte`        varchar(100),
    `lte`        varchar(100),
    `Values`     varchar(100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

#the enumerable values
create table if not exists `values`
(
    `id`    INT  auto_increment primary key not null,
    `classname` varchar(255)             not null,
    `name`  varchar(20),
    `value` varchar(255)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

#input parameters of the system under test
#projectName is the system model name in the table model
#DataType can be the typeName in the table dataType
create table if not exists `input_paras`
(
    `id`    INT  auto_increment primary key not null,
    `projectName` varchar(255)             not null,
    `Name`        varchar(100)             not null,
    `DataType`    varchar(100)             not null,
    `Empty`       bool,
    `Lost`        bool
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

#output parameters of the system under test, the same as input_paras
create table if not exists `output_paras`
(
    `id`    INT  auto_increment primary key not null,
    `projectName` varchar(255)             not null,
    `Name`        varchar(100)             not null,
    `DataType`    varchar(100)             not null,
    `Empty`       bool,
    `Lost`        bool
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

#the constraint of the input parameters
#Rules is the expression to define the constraint
#ruleGroup is the id in table Infos. Describe Information
create table if not exists constraints
(
    `id`        INT auto_increment primary key not null,
    projectName varchar(255)                       not null,
    Rules       varchar(1000),
    ruleGroup   int
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

#the entities need to be covered
create table if not exists `coverages`
(
    `id`        INT auto_increment primary key not null,
    projectName varchar(255)              not null,
    Rules       varchar(1000),
    ruleGroup   int                       not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

# the first type of the oracle AXEL supports
# specified oracle, which formally defines rules between outputs and the inputs.
create table if not exists `assertions`
(
    `id`        INT auto_increment primary key not null,
    projectName varchar(255)              not null,
    Rules       varchar(1000),
    ruleGroup   int    default 10
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

# the second type of the oracle AXEL supports
# Metamorphic Relation. Source and follow test cases that satisfied the MR.
create table if not exists `metamorphics`
(
    `id`        INT auto_increment primary key not null,
    projectName varchar(255)              not null,
    Rules       varchar(1000),
    ruleGroup   int                       not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

create table if not exists `Infos`
(
    `ruleGroup`   INT auto_increment primary key not null,
    `Info`        varchar(1000)                      not null
);

#the output testcases of AXEL
CREATE TABLE if not exists `output`
(
    `id`         int       NOT NULL AUTO_INCREMENT primary key,
    `projectName` varchar(255)             not null,
    `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `content`    longtext
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


use tsgen;
INSERT INTO `Infos`(ruleGroup, Info)
VALUES (10,'assertion');
