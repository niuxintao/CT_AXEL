# 航班查询测试
use tsgen;

insert into model( id,classname, Name,tWay)
VALUES (1,'airline', 'airline.Booking', 2);

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
VALUES ('airline.Booking', 'time', '0-10');

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


INSERT INTO `metamorphics`(projectName, Rules, ruleGroup)
VALUES ('airline.Booking','s.origin1 = f.origin1',101),
       ('airline.Booking','s.destination1 = f.destination1',101),
       ('airline.Booking','f.date1 = s.date1',101),
       ('airline.Booking','s.bookingType = "one_way"',101),
       ('airline.Booking','f.bookingType = "round_trip"',101),

       ('airline.Booking','s.origin1 = f.origin1',102),
       ('airline.Booking','s.destination1 = f.destination1',102),
       ('airline.Booking','f.date1 = s.date1',102),
       ('airline.Booking','s.bookingType = "round_trip"',102),
       ('airline.Booking','f.bookingType = "one_way"',102),

       ('airline.Booking','s.origin1 = f.origin1',104),
       ('airline.Booking','s.destination1 = f.destination2',104),
       ('airline.Booking','f.date1 = s.date1',104),
       ('airline.Booking','s.bookingType = "one_way"',104),
       ('airline.Booking','f.bookingType = "multi_trip"',104),

       ('airline.Booking','f.origin1 = s.origin1',105),
       ('airline.Booking','f.destination1 = s.destination2',105),
       ('airline.Booking','f.date1 = s.date1',105),
       ('airline.Booking','f.bookingType = "one_way"',105),
       ('airline.Booking','s.bookingType = "multi_trip"',105)
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

       (101,'mr1:one_way->round_trip the output time should be double'),
       (102,'mr2:round_trip->one_way the output time should be half'),
       (104,'mr3: direct->indirect the output time should be more'),
       (105,'mr4: indirect->direct the output time should be less');
