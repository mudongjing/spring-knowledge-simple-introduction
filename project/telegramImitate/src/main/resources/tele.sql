create database IF NOT EXISTS tele;
use tele;
drop table if exists user;
drop table if exists channel;
drop table if exists `group`;
drop table if exists `message-123`;
-- 用户数据表-----------------
create table IF NOT EXISTS `user` (
                                      `user_id`  int(10) NOT NULL AUTO_INCREMENT,
                                      `user_name` varchar(255) not null,
                                      `user_friends` varchar(255)  default null,
                                      `friends_type` int(1) not null,
                                      `user_groups` varchar(255) default null,
                                      `group_type` int(1) not null,
                                      primary key(`user_id`)
)engine =InnoDB default charset=utf8;

-- 群组数据表------------
create table IF NOT EXISTS `group` (
                                       `group_id`  int(10) not null auto_increment ,
                                       `group_name` varchar(255) not null,
                                       `group_creator` int(10) not null,
                                       `group_members` varchar(255) not null,
                                       `group_message` varchar(255) not null,
                                       primary key(`group_id`)
)engine =InnoDB default charset=utf8;

-- 消息数据表
create table  IF NOT EXISTS `message-123` (
                                              `message_id` int(10) not null auto_increment,
                                              `message_content` varchar(255) not null,
                                              `message_type` int(2) not null,
                                              `message_creator` int(10) not null,
                                              `message_date`  date not null,
                                              `message_time` time not null,
                                              primary key(`message_id`)
)engine=InnoDB default charset=utf8;