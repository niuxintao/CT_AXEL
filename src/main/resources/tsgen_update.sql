# 航班查询测试
use tsgen;

insert into model( id,classname, Name,tWay)
VALUES (1,'airline', 'airline.Booking', 1);

insert into dataType( classname,typeName, Type, GTE, LTE, `VALUES`)
VALUES ('airline', '0-10', 'int', '0', '10', null),
       ('airline', '1-10', 'int', '1', '10', null),
       ('airline', 'city', 'enum', null, null, 'city'),
       ('airline', 'cityNotNull', 'enum', null, null, 'cityNotNull'),
       ('airline', 'bookingType', 'enum', null, null, 'bookingType');

insert into `values`(classname, name, value)
VALUES ('airline', 'city', 'Beijing'),
       ('airline', 'city', 'Shanghai'),
       ('airline', 'city', 'Nanjing'),
       ('airline', 'city', 'Dalian'),
       ('airline', 'city', 'Tokyo'),
       ('airline', 'city', 'Singapore'),
       ('airline', 'city', 'London'),
       ('airline', 'city', 'Paris'),
       ('airline', 'city', 'null'),
       ('airline', 'cityNotNull', 'Beijing'),
       ('airline', 'cityNotNull', 'Shanghai'),
       ('airline', 'cityNotNull', 'Nanjing'),
       ('airline', 'cityNotNull', 'Dalian'),
       ('airline', 'cityNotNull', 'Tokyo'),
       ('airline', 'cityNotNull', 'Singapore'),
       ('airline', 'cityNotNull', 'London'),
       ('airline', 'cityNotNull', 'Paris'),
       #bookingType
       ('airline', 'bookingType', 'one_way'),
       ('airline', 'bookingType', 'round_trip'),
       ('airline', 'bookingType', 'multi_trip');

insert into input_paras (projectName, Name, DataType)
VALUES ('airline.Booking', 'bookingType', 'bookingType'),
       ('airline.Booking', 'date1', '1-10'),
       ('airline.Booking', 'origin1', 'cityNotNull'),
       ('airline.Booking', 'destination1', 'cityNotNull'),
       ('airline.Booking', 'date2', '0-10'),
       ('airline.Booking', 'origin2', 'city'),
       ('airline.Booking', 'destination2', 'city');

insert into output_paras(projectName,Name, DataType)
VALUES ('airline.Booking', 'price', '0-10');

insert into constraints(projectName, Rules, ruleGroup)
VALUES  ('airline.Booking', 'origin1!=destination1', 20),
        ('airline.Booking', 'bookingType="one_way" => date2=0', 21),
        ('airline.Booking', 'bookingType="one_way" => origin2="null" ', 21),
        ('airline.Booking', 'bookingType="one_way" => destination2="null"', 21),
        ('airline.Booking', 'bookingType="round_trip" =>origin2!=destination2 ', 22),
        ('airline.Booking', 'bookingType="round_trip" =>origin2!="null"', 22),
        ('airline.Booking', 'bookingType="round_trip" =>destination2!="null"', 22),
        ('airline.Booking', 'bookingType="round_trip" =>date2!=0', 22),
        ('airline.Booking', 'bookingType="round_trip" =>date1<=date2', 22),
        ('airline.Booking', 'bookingType="round_trip" =>origin1=destination2', 22),
        ('airline.Booking', 'bookingType="round_trip" =>origin2=destination1', 22),
        ('airline.Booking', 'bookingType="multi_trip" =>origin2!=destination2 ', 23),
        ('airline.Booking', 'bookingType="multi_trip" =>origin2!="null"', 23),
        ('airline.Booking', 'bookingType="multi_trip" =>destination2!="null"', 23),
        ('airline.Booking', 'bookingType="multi_trip" =>date2!=0', 23),
        ('airline.Booking', 'bookingType="multi_trip" =>date1<=date2', 23),
        ('airline.Booking', 'bookingType="multi_trip" =>origin2=destination1', 23);

# 描述输入参数与输出参数之间关系的约束
insert into assertions(projectName, Rules, ruleGroup)
VALUES
#单程国内航班
('airline.Booking', 'bookingType=one_way&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)&&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)=>price=1',10),
('airline.Booking', 'bookingType=one_way&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)&&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)=>price=1',10),
#单程国际航班
('airline.Booking', 'bookingType=one_way&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)&&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)&&(date1=1||date1=3||date1=5||date1=7||date1=9)=>price=0',10),
('airline.Booking', 'bookingType=one_way&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)&&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)&&(date1=2||date1=4||date1=6||date1=8||date1=10)=>price=2',10),
('airline.Booking', 'bookingType=one_way&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)&&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)=>price=2',10),
#往返国内航班
('airline.Booking', 'bookingType=round_trip&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)&&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)=>price=2',10),
('airline.Booking', 'bookingType=round_trip&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)&&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)=>price=2',10),
#往返国际航班
('airline.Booking', 'bookingType=round_trip&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)&&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)&&date1!=date2=>price=4',10),
('airline.Booking', 'bookingType=round_trip&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)&&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)&&date1!=date2=>price=4',10),
#多程国内航班
('airline.Booking', 'bookingType=multi_trip&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)
                                        &&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)
                                        &&(destination2=Beijing||destination2=Shanghai||destination2=Nanjing||destination2=Dalian)=>price=2',10),
('airline.Booking', 'bookingType=multi_trip&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)
                                        &&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)
                                        &&(destination2=Tokyo||destination2=Singapore||destination2=London||destination2=Paris)=>price=2',10),

('airline.Booking', 'bookingType=multi_trip&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)
                                        &&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)
                                        &&(destination2=Tokyo||destination2=Singapore||destination2=London||destination2=Paris)=>price=3',10),
('airline.Booking', 'bookingType=multi_trip&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)
                                        &&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)
                                        &&(destination2=Tokyo||destination2=Singapore||destination2=London||destination2=Paris)=>price=3',10),
('airline.Booking', 'bookingType=multi_trip&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)
                                        &&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)
                                        &&(destination2=Beijing||destination2=Shanghai||destination2=Nanjing||destination2=Dalian)=>price=3',10),
('airline.Booking', 'bookingType=multi_trip&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)
                                        &&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)
                                        &&(destination2=Beijing||destination2=Shanghai||destination2=Nanjing||destination2=Dalian)=>price=3',10),
('airline.Booking', 'bookingType=multi_trip&&(origin1=Beijing||origin1=Shanghai||origin1=Nanjing||origin1=Dalian)
                                        &&(destination1=Tokyo||destination1=Singapore||destination1=London||destination1=Paris)
                                        &&(destination2=Beijing||destination2=Shanghai||destination2=Nanjing||destination2=Dalian)=>price=4',10),
('airline.Booking', 'bookingType=multi_trip&&(origin1=Tokyo||origin1=Singapore||origin1=London||origin1=Paris)
                                        &&(destination1=Beijing||destination1=Shanghai||destination1=Nanjing||destination1=Dalian)
                                        &&(destination2=Tokyo||destination2=Singapore||destination2=London||destination2=Paris)=>price=4',10);

INSERT INTO coverages (projectName, Rules, ruleGroup)
VALUES ('airline.Booking', 'bookingType=one_way&&origin1=Dalian&&destination1=Tokyo', 30),
       ('airline.Booking', 'bookingType=multi_trip&&origin1=Dalian&&destination1=Beijing&&destination2=Tokyo', 31),
       ('airline.Booking', 'origin1=Dalian&&destination1=Beijing&&destination2=Tokyo&&date1=date2', 31),
       ('airline.Booking', 'origin1=Dalian&&destination1=Beijing&&destination2=Tokyo&&date1-date2<=1', 31),
       ('airline.Booking', 'origin1=Nanjing&&destination1=Dalian&&date1=5', 32),
       ('airline.Booking', 'origin1=Nanjing&&destination1=Dalian&&date1=6', 33),
       ('airline.Booking', 'origin1=Beijing&&destination1=Shanghai&&date1=date2', 34),
       ('airline.Booking', 'bookingType=round_trip&&origin1=Beijing&&destination1=Singapore&&date1=date2', 35),
       ('airline.Booking', 'bookingType=round_trip&&origin1=Nanjing&&date2-date1=7', 36),
       ('airline.Booking', 'bookingType=round_trip&&origin1=Paris&&date2-date1>=3&&date2-date1<=5', 37),
       ('airline.Booking', 'origin1=Shanghai&&destination1=London&&date1=1', 38),
       ('airline.Booking', 'origin1=Shanghai&&destination1=London&&date1=2', 39);

INSERT INTO `metamorphics`(projectName, Rules, ruleGroup)
VALUES ('airline.Booking','s.origin1 = f.origin1',100),
       ('airline.Booking','s.destination1 = f.destination1',100),
       ('airline.Booking','s.bookingType = "one_way"',100),
       ('airline.Booking','f.bookingType = "round_trip"',100),
       ('airline.Booking','s.origin1 = f.origin1',101),
       ('airline.Booking','s.destination1 = f.destination1',101),
       ('airline.Booking','s.bookingType = "round_trip"',101),
       ('airline.Booking','f.bookingType = "one_way"',101),
       ('airline.Booking','s.origin1 = f.origin1&&s.destination1 = f.destination1&&s.bookingType = "multi_trip"&&f.bookingType = "one_way"',102)
;
# 插入断言的相关信息
INSERT INTO `Infos`(ruleGroup, Info)
VALUES (20, 'constraints for all'),
       (21, 'constraints for one_way'),
       (22, 'constraints for round_trip'),
       (23, 'constraints for multi_trip'),
       (30, 'oneWay'),
       (31, 'date1=date2'),
       (32, 'date1+7=date2'),
       (33, 'Beijing->Shanghai->Guangzhou'),
       (34, 'Beijing->Shanghai->zhou'),
       (35, 'Beijing->Singapore round_trip in one day'),
       (36, 'Nanjing round_trip a week later'),
       (37, 'Paris round_trip in 3-5 days '),
       (38, 'Shanghai->London in day1'),
       (39, 'Shanghai->London in day2'),
       (100,'mr1:one_way->round_trip the output price should be double'),
       (101,'mr2:round_trip->one_way the output price should be half'),
       (102,'mr3:multi_trip->one_way the output price should be less'),
       (103,'mr4: direct->indirect'),
       (103,'mr5: indirect->direct');


#triangle
insert into model(id, classname, Name,tWay)
values (2, 'triangle', 'triangle.typeTest',1);
insert into dataType( classname, typeName, Type, GTE, LTE, `VALUES`)
values ( 'triangle', 'int', 'int', 0, 10, 'int'),
       ( 'triangle', 'bool', 'bool', null, null, 'bool'),
       ( 'triangle', 'enum', 'enum', null, null, 'enum');
insert into `values`(classname, name, value)
VALUES  ('triangle','enum','normal' ),
        ('triangle','enum','equilateral'),
        ('triangle','enum','isosceles');
insert into input_paras (Name, projectName, DataType)
values ('a', 'triangle.typeTest', 'int'),
       ('b', 'triangle.typeTest', 'int'),
       ('c', 'triangle.typeTest', 'int'),
       ('istrue', 'triangle.typeTest', 'bool');
insert into output_paras(Name, projectName, DataType)
values ('type', 'triangle.typeTest', 'enum');
insert into constraints(projectName, Rules, ruleGroup)
values ('triangle.typeTest','a+b>c && a+c>b && b+c>a',1002);
insert into coverages(projectName, Rules, ruleGroup)
values ('triangle.typeTest','a=3&&b=3&&c=3' ,1101),
       ('triangle.typeTest','a=3&&b=4&&c=5' ,1102);;
insert into assertions(projectName, Rules,ruleGroup)
values ('triangle.typeTest','(a=b&&b=c)=>type=equilateral',10),
       ('triangle.typeTest','((a=b&&a!=c)||(a=c&&a!=b)||(b=c&&b!=a))=>type=isosceles',10),
       ('triangle.typeTest','(a!=b&&b!=c&&a!=c)=>type=normal',10);
INSERT INTO `metamorphics`(projectName, Rules, ruleGroup)
values ('triangle.typeTest','s.a = f.a',1200),
       ('triangle.typeTest','s.b = f.c',1200),
       ('triangle.typeTest','s.c = f.b',1200),
       ('triangle.typeTest','f.a * 2 = s.a ',1201),
       ('triangle.typeTest','f.b * 2 = s.b ',1201),
       ('triangle.typeTest','f.c * 2 = s.c',1201),
       ('triangle.typeTest','s.a * 2 = f.a ',1202),
       ('triangle.typeTest','s.b * 2 = f.b ',1202),
       ('triangle.typeTest','s.c * 2 = f.c',1202);


INSERT INTO `Infos`(ruleGroup, Info)
VALUES (1100, 'triangle'),
       (1101, '333'),
       (1102,'345'),
       (1200,'mr1'),
       (1201,'mr2'),
       (1202,'mr3');