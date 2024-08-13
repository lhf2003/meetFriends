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
    tags         varchar(1024)                      null comment '标签json列表'
)comment '用户表';


-- 队伍表
create table team
(
    id          bigint auto_increment comment 'id' primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    maxNum      int      default 1                 not null comment '最大人数',
    expireTime  datetime                           null comment '过期时间',
    userId      bigint comment '用户id',
    status      int      default 0                 not null comment '0 - 公开,1 - 私有,2 - 加密',
    password    varchar(512)                       null comment '密码',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete    tinyint  default 0                 not null comment '是否删除'
)comment '队伍';

-- 用户队伍关系表
create table user_team
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint comment '用户id',
    teamId     bigint comment '队伍id',
    joinTime   datetime                           null comment '加入时间',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0                 not null comment '是否删除'
)comment '用户队伍关系';