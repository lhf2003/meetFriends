-- 用户表
create table user
(
    id           bigint auto_increment comment '用户id'
        primary key,
    userName     varchar(20)                        null comment '用户昵称',
    userAccount  varchar(20)                        not null comment '用户账号',
    userPassword varchar(256)                       not null comment '用户密码',
    age          int                                null comment '年龄',
    sex          tinyint                            null comment '性别',
    phone        varchar(20)                        null comment '电话',
    userProfile  varchar(512)                       null comment '用户简介',
    email        varchar(20)                        null comment '邮箱',
    userAvatar   varchar(1024)                      null comment '用户头像',
    userRole     int      default 0                 not null comment '用户角色 0-普通用户，1-管理员',
    userStatus   tinyint  default 0                 not null comment '用户状态，0-正常，1-封禁',
    isDelete     tinyint  default 0                 not null comment '是否删除，0-未删除，1-已删除',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    tags         varchar(1024)                      null comment '标签json列表',
    address      varchar(1024)                      null comment '用户地址'
);

-- 队伍表
create table team
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    maxNum      int      default 1                 not null comment '最大人数',
    expireTime  datetime                           null comment '过期时间',
    userId      bigint                             null comment '用户id',
    status      int      default 0                 not null comment '0 - 公开,1 - 私有,2 - 加密',
    password    varchar(512)                       null comment '密码',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete    tinyint  default 0                 not null comment '是否删除'
) comment '队伍';

-- 用户队伍关系表
create table user_team
(
    id         bigint auto_increment comment 'id'
        primary key,
    userId     bigint                             null comment '用户id',
    teamId     bigint                             null comment '队伍id',
    joinTime   datetime                           null comment '加入时间',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0                 not null comment '是否删除'
) comment '用户队伍关系';

-- 聊天消息表
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages`
(
    `chatId`     int       NOT NULL AUTO_INCREMENT COMMENT '会话唯一标识',
    `senderId`   int                DEFAULT NULL COMMENT '发送者id',
    `receiverId` int                DEFAULT NULL COMMENT '接收者id',
    `message`    text COMMENT '消息内容',
    `timestamp`  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `readStatus` tinyint(1)         DEFAULT '0' COMMENT '状态：0-未读，1-已读',
    PRIMARY KEY (`chatId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户消息表';

-- 用户关系表
DROP TABLE IF EXISTS `relationship`;
CREATE TABLE `relationship`
(
    `id`         bigint NOT NULL AUTO_INCREMENT,
    `followerId` bigint NOT NULL COMMENT '关注者ID',
    `followedId` bigint NOT NULL COMMENT '被关注者ID',
    `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    `isDelete`   tinyint  DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户关系表';

-- 用户在线状态表
DROP TABLE IF EXISTS `user_online_status`;
CREATE TABLE `user_online_status`
(
    `userId`     bigint    NOT NULL COMMENT '用户id',
    `isOnline`   tinyint(1)         DEFAULT '0' COMMENT '状态，0-离线，1-在线',
    `lastOnline` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后在线时间',
    PRIMARY KEY (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户在线状态表';
